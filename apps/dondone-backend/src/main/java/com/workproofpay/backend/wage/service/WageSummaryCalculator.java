package com.workproofpay.backend.wage.service;

import com.workproofpay.backend.wage.model.WageDeposit;
import com.workproofpay.backend.workproof.service.WorkProofMonthlyMetrics;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Component
public class WageSummaryCalculator {

    private static final BigDecimal DEDUCTIONS_KNOWN_TRIGGER_RATIO = BigDecimal.valueOf(0.02);
    private static final BigDecimal DEDUCTIONS_UNKNOWN_TRIGGER_RATIO = BigDecimal.valueOf(0.03);
    private static final long DEDUCTIONS_KNOWN_TRIGGER_CAP = 30_000L;
    private static final long DEDUCTIONS_UNKNOWN_TRIGGER_CAP = 50_000L;
    private static final long MIN_TRIGGER_AMOUNT = 1L;
    private static final int MINUTES_PER_HOUR = 60;

    public WageSummarySnapshot summarize(WorkProofMonthlyMetrics metrics,
                                         long normalizedHourlyWage,
                                         WageDeposit latestDeposit) {
        long estimatedBaseAmount = prorateAmount(metrics.totalWorkedMinutes(), normalizedHourlyWage, BigDecimal.ONE);
        long estimatedOvertimePremiumAmount = prorateAmount(metrics.totalOvertimeMinutes(), normalizedHourlyWage, BigDecimal.valueOf(0.5));
        long estimatedNightPremiumAmount = prorateAmount(metrics.totalNightMinutes(), normalizedHourlyWage, BigDecimal.valueOf(0.5));
        long estimatedTotalAmount = estimatedBaseAmount + estimatedOvertimePremiumAmount + estimatedNightPremiumAmount;

        boolean deductionsKnown = latestDeposit != null && latestDeposit.isDeductionsKnown();
        Long actualDepositAmount = latestDeposit == null ? null : latestDeposit.getActualDepositAmount();
        Long differenceAmount = actualDepositAmount == null ? null : estimatedTotalAmount - actualDepositAmount;

        long triggerAmount = calculateTriggerAmount(estimatedTotalAmount, deductionsKnown);
        boolean anomalyDetected = differenceAmount != null && Math.abs(differenceAmount) >= triggerAmount;
        String status = actualDepositAmount == null
                ? "NOT_RECORDED"
                : anomalyDetected ? "REVIEW_NEEDED" : "WITHIN_THRESHOLD";

        return new WageSummarySnapshot(
                normalizedHourlyWage,
                minutesToHours(metrics.totalWorkedMinutes()),
                minutesToHours(metrics.totalOvertimeMinutes()),
                minutesToHours(metrics.totalNightMinutes()),
                estimatedBaseAmount,
                estimatedOvertimePremiumAmount,
                estimatedNightPremiumAmount,
                estimatedTotalAmount,
                actualDepositAmount,
                latestDeposit == null ? null : latestDeposit.getDepositDate(),
                latestDeposit == null ? null : latestDeposit.getDepositDate().getDayOfMonth(),
                deductionsKnown,
                differenceAmount,
                triggerAmount,
                anomalyDetected,
                status
        );
    }

    public BigDecimal minutesToHours(long minutes) {
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), 1, RoundingMode.HALF_UP);
    }

    private long calculateTriggerAmount(long estimatedTotalAmount, boolean deductionsKnown) {
        BigDecimal ratio = deductionsKnown ? DEDUCTIONS_KNOWN_TRIGGER_RATIO : DEDUCTIONS_UNKNOWN_TRIGGER_RATIO;
        long flatCap = deductionsKnown ? DEDUCTIONS_KNOWN_TRIGGER_CAP : DEDUCTIONS_UNKNOWN_TRIGGER_CAP;
        long ratioAmount = BigDecimal.valueOf(estimatedTotalAmount)
                .multiply(ratio)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        return Math.max(MIN_TRIGGER_AMOUNT, Math.min(flatCap, ratioAmount));
    }

    private long prorateAmount(long minutes, long hourlyWage, BigDecimal multiplier) {
        return BigDecimal.valueOf(minutes)
                .multiply(BigDecimal.valueOf(hourlyWage))
                .multiply(multiplier)
                .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), 0, RoundingMode.HALF_UP)
                .longValue();
    }

    public record WageSummarySnapshot(
            long normalizedHourlyWage,
            BigDecimal totalWorkedHours,
            BigDecimal overtimeHours,
            BigDecimal nightHours,
            long estimatedBaseAmount,
            long estimatedOvertimePremiumAmount,
            long estimatedNightPremiumAmount,
            long estimatedTotalAmount,
            Long actualDepositAmount,
            LocalDate actualDepositRecordedDate,
            Integer actualDepositRecordedDay,
            boolean deductionsKnown,
            Long differenceAmount,
            long anomalyTriggerAmount,
            boolean anomalyDetected,
            String status
    ) {
    }
}
