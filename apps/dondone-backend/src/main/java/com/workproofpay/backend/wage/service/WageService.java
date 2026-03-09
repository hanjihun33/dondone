package com.workproofpay.backend.wage.service;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.wage.api.dto.request.CreateWageDepositRequest;
import com.workproofpay.backend.wage.api.dto.response.WageDepositResponse;
import com.workproofpay.backend.wage.api.dto.response.WageSummaryResponse;
import com.workproofpay.backend.wage.model.WageDeposit;
import com.workproofpay.backend.wage.repo.WageDepositRepository;
import com.workproofpay.backend.workproof.service.WorkProofMonthlyMetrics;
import com.workproofpay.backend.workproof.service.WorkProofService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WageService {

    private static final String REFERENCE_ONLY_DISCLAIMER =
            "Reference-only estimate. Actual payment and deductions can differ by contract, payroll rules, and payslip details.";

    private final WageDepositRepository wageDepositRepository;
    private final UserRepository userRepository;
    private final WorkProofService workProofService;

    @Transactional
    public WageDepositResponse createDeposit(Long userId, CreateWageDepositRequest request) {
        YearMonth yearMonth = workProofService.parseYearMonth(request.yearMonth());
        if (!YearMonth.from(request.depositDate()).equals(yearMonth)) {
            throw new ApiException(ErrorCode.INVALID_DEPOSIT_DATE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        WageDeposit saved = wageDepositRepository.save(new WageDeposit(
                user,
                yearMonth.toString(),
                request.depositDate(),
                request.actualDepositAmount(),
                request.deductionsKnown(),
                request.note()
        ));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public WageSummaryResponse getSummary(Long userId,
                                          String yearMonth,
                                          LocalDate asOf,
                                          long normalizedHourlyWage,
                                          int paydayDay) {
        if (normalizedHourlyWage <= 0) {
            throw new ApiException(ErrorCode.INVALID_HOURLY_WAGE);
        }
        if (paydayDay < 1 || paydayDay > 31) {
            throw new ApiException(ErrorCode.INVALID_PAYDAY);
        }

        WorkProofMonthlyMetrics metrics = workProofService.getMonthlyMetrics(userId, yearMonth, asOf);
        WageDeposit latestDeposit = findLatestDeposit(userId, metrics.yearMonth(), metrics.asOf());

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

        return new WageSummaryResponse(
                metrics.yearMonth(),
                metrics.asOf(),
                metrics.totalWorkDays(),
                metrics.totalWorkedMinutes(),
                workProofService.minutesToHours(metrics.totalWorkedMinutes()),
                metrics.totalOvertimeMinutes(),
                workProofService.minutesToHours(metrics.totalOvertimeMinutes()),
                metrics.totalNightMinutes(),
                workProofService.minutesToHours(metrics.totalNightMinutes()),
                normalizedHourlyWage,
                estimatedBaseAmount,
                estimatedOvertimePremiumAmount,
                estimatedNightPremiumAmount,
                estimatedTotalAmount,
                actualDepositAmount,
                latestDeposit == null ? null : latestDeposit.getDepositDate(),
                latestDeposit == null ? null : latestDeposit.getDepositDate().getDayOfMonth(),
                deductionsKnown,
                paydayDay,
                differenceAmount,
                triggerAmount,
                anomalyDetected,
                status,
                REFERENCE_ONLY_DISCLAIMER,
                metrics.editedRecordCount(),
                metrics.reflectedRecordCount(),
                metrics.pendingRecordCount(),
                metrics.reflectedWorkProofIds()
        );
    }

    private WageDepositResponse toResponse(WageDeposit deposit) {
        return new WageDepositResponse(
                deposit.getId(),
                deposit.getYearMonth(),
                deposit.getDepositDate(),
                deposit.getActualDepositAmount(),
                deposit.isDeductionsKnown(),
                deposit.getNote(),
                deposit.getCreatedAt(),
                deposit.getUpdatedAt()
        );
    }

    private WageDeposit findLatestDeposit(Long userId, String yearMonth, LocalDate asOf) {
        List<WageDeposit> deposits = wageDepositRepository.findByUserIdAndYearMonthOrderByDepositDateDescCreatedAtDesc(userId, yearMonth);
        return deposits.stream()
                .filter(deposit -> asOf == null || !deposit.getDepositDate().isAfter(asOf))
                .findFirst()
                .orElse(null);
    }

    private long calculateTriggerAmount(long estimatedTotalAmount, boolean deductionsKnown) {
        BigDecimal ratio = deductionsKnown ? BigDecimal.valueOf(0.02) : BigDecimal.valueOf(0.03);
        long flat = deductionsKnown ? 30_000L : 50_000L;
        long ratioAmount = BigDecimal.valueOf(estimatedTotalAmount)
                .multiply(ratio)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        return Math.min(flat, ratioAmount);
    }

    private long prorateAmount(long minutes, long hourlyWage, BigDecimal multiplier) {
        return BigDecimal.valueOf(minutes)
                .multiply(BigDecimal.valueOf(hourlyWage))
                .multiply(multiplier)
                .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP)
                .longValue();
    }
}
