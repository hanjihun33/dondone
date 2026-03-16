package com.workproofpay.backend.wage.service;

import com.workproofpay.backend.wage.model.WageDeposit;
import com.workproofpay.backend.workproof.api.dto.response.CurrentContractResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryContractResponse;
import com.workproofpay.backend.workproof.service.WorkProofMonthlyMetrics;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
/**
 * Wage 계산 규칙을 한 곳에 모아 기존 summary 흐름과 lane 1 estimate 흐름이 같은 기준을 공유하게 한다.
 */
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
        List<WageDifferenceReason> reasons = buildReasons(metrics, actualDepositAmount, differenceAmount, anomalyDetected);

        return new WageSummarySnapshot(
                reasons,
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

    public WageEstimateSnapshot estimate(CurrentContractResponse contract,
                                         WorkProofMonthlySummaryContractResponse summary) {
        // P0 규칙대로 기본급/연장/야간을 항목별로 따로 계산하고 floor 성격으로 내린다.
        long baseEstimate = prorateAmount(
                summary.integrity().verifiedMinutes(),
                contract.normalizedHourlyWage(),
                BigDecimal.ONE,
                RoundingMode.DOWN
        );
        long overtimePremium = prorateAmount(
                summary.overtimeMinutes(),
                contract.normalizedHourlyWage(),
                BigDecimal.valueOf(0.5),
                RoundingMode.DOWN
        );
        long nightPremium = prorateAmount(
                summary.nightMinutes(),
                contract.normalizedHourlyWage(),
                BigDecimal.valueOf(0.5),
                RoundingMode.DOWN
        );

        return new WageEstimateSnapshot(
                baseEstimate,
                overtimePremium,
                nightPremium,
                baseEstimate + overtimePremium + nightPremium
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
        return prorateAmount(minutes, BigDecimal.valueOf(hourlyWage), multiplier, RoundingMode.HALF_UP);
    }

    private long prorateAmount(long minutes,
                               BigDecimal hourlyWage,
                               BigDecimal multiplier,
                               RoundingMode roundingMode) {
        return BigDecimal.valueOf(minutes)
                .multiply(hourlyWage)
                .multiply(multiplier)
                .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), 0, roundingMode)
                .longValue();
    }

    private List<WageDifferenceReason> buildReasons(WorkProofMonthlyMetrics metrics,
                                                    Long actualDepositAmount,
                                                    Long differenceAmount,
                                                    boolean anomalyDetected) {
        List<Long> relatedWorkProofIds = metrics.reflectedWorkProofIds();
        List<WageDifferenceReason> reasons = new ArrayList<>();

        if (actualDepositAmount == null) {
            reasons.add(new WageDifferenceReason(
                    "DEPOSIT_MISSING",
                    "실입금 입력이 아직 없습니다",
                    "실제 입금액이 입력되지 않아 차액 판단은 잠겨 있습니다. 먼저 입금 기록을 남긴 뒤 비교를 이어가세요.",
                    relatedWorkProofIds
            ));
        }
        if (metrics.totalOvertimeMinutes() > 0) {
            reasons.add(new WageDifferenceReason(
                    "OVERTIME_INCLUDED",
                    "연장 근무가 추정에 포함됐습니다",
                    "하루 8시간을 초과한 reflected 근무가 참고용 추정 급여에 반영됐습니다.",
                    relatedWorkProofIds
            ));
        }
        if (metrics.totalNightMinutes() > 0) {
            reasons.add(new WageDifferenceReason(
                    "NIGHT_SHIFT_INCLUDED",
                    "야간 근무가 추정에 포함됐습니다",
                    "22:00-06:00 구간의 reflected 근무가 참고용 추정 급여에 반영됐습니다.",
                    relatedWorkProofIds
            ));
        }
        if (anomalyDetected && differenceAmount != null) {
            reasons.add(new WageDifferenceReason(
                    "DIFFERENCE_OVER_THRESHOLD",
                    "확인 필요한 차이가 감지됐습니다",
                    differenceAmount >= 0
                            ? "참고용 추정 급여가 실제 입금액보다 임계값 이상 크게 계산돼 근거 확인이 필요합니다."
                            : "실제 입금액과 참고용 추정 급여 사이에 임계값 이상 차이가 있어 근거 확인이 필요합니다.",
                    relatedWorkProofIds
            ));
        }

        return List.copyOf(reasons);
    }

    public record WageDifferenceReason(
            String code,
            String title,
            String description,
            List<Long> relatedWorkProofIds
    ) {
    }

    public record WageSummarySnapshot(
            List<WageDifferenceReason> reasons,
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

    public record WageEstimateSnapshot(
            long baseEstimate,
            long overtimePremium,
            long nightPremium,
            long estimatedTotal
    ) {
    }
}
