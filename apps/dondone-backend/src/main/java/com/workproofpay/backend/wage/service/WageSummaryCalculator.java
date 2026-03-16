package com.workproofpay.backend.wage.service;

import com.workproofpay.backend.wage.model.WageDeposit;
import com.workproofpay.backend.wage.model.WageVerificationResolutionStage;
import com.workproofpay.backend.wage.model.WageVerificationStatus;
import com.workproofpay.backend.workproof.api.dto.response.CurrentContractResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryContractResponse;
import com.workproofpay.backend.workproof.service.WorkProofMonthlyMetrics;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * summary, estimate, verification이 같은 lane 1 계산 규칙을 공유하도록
 * Wage 계산 규칙을 한 곳에 모은다.
 */
@Component
public class WageSummaryCalculator {

    private static final BigDecimal DEDUCTIONS_KNOWN_TRIGGER_RATIO = BigDecimal.valueOf(0.02);
    private static final BigDecimal DEDUCTIONS_UNKNOWN_TRIGGER_RATIO = BigDecimal.valueOf(0.03);
    private static final long DEDUCTIONS_KNOWN_TRIGGER_CAP = 30_000L;
    private static final long DEDUCTIONS_UNKNOWN_TRIGGER_CAP = 50_000L;
    private static final long MIN_TRIGGER_AMOUNT = 1L;
    private static final int MINUTES_PER_HOUR = 60;
    // lane 1은 아직 참고용 단계라 급여 정책이 정해질 때까지 보수적인 내림 기준을 유지한다.
    private static final RoundingMode LANE1_ESTIMATE_ROUNDING_MODE = RoundingMode.DOWN;

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

    // 이는 확정 급여 정책이 아니라 lane 1의 임시 가정이다.
    public WageEstimateSnapshot estimate(CurrentContractResponse contract,
                                         WorkProofMonthlySummaryContractResponse summary) {
        return calculateLane1EstimateBreakdown(contract, summary).toSnapshot();
    }

    /**
     * worker가 입력한 실제 입금액을 lane 1 estimate와 같은 계산 기준으로 비교해
     * verification 스냅샷으로 변환한다.
     */
    public WageVerificationSnapshot verify(CurrentContractResponse contract,
                                           WorkProofMonthlySummaryContractResponse summary,
                                           long actualDepositAmount,
                                           boolean deductionsKnown) {
        Lane1EstimateBreakdown breakdown = calculateLane1EstimateBreakdown(contract, summary);
        WageEstimateSnapshot estimate = breakdown.toSnapshot();
        long differenceAmount = estimate.estimatedTotal() - actualDepositAmount;
        ThresholdSnapshot threshold = calculateThresholdSnapshot(estimate.estimatedTotal(), deductionsKnown);
        boolean checkRequired = Math.abs(differenceAmount) >= threshold.absoluteWon();

        return new WageVerificationSnapshot(
                estimate,
                differenceAmount,
                calculateDifferenceRate(estimate.estimatedTotal(), differenceAmount),
                threshold,
                checkRequired ? WageVerificationStatus.CHECK_REQUIRED : WageVerificationStatus.MATCHED,
                checkRequired
                        ? WageVerificationResolutionStage.EMPLOYER_CONFIRMATION_RECOMMENDED
                        : WageVerificationResolutionStage.SELF_CHECK,
                buildVerificationCauses(summary.overtimeMinutes(), summary.nightMinutes(), differenceAmount, checkRequired)
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

    /**
     * create/detail 응답이 같은 threshold 기준을 쓰도록 계산 기준을 한 곳에 묶는다.
     */
    private ThresholdSnapshot calculateThresholdSnapshot(long estimatedTotalAmount, boolean deductionsKnown) {
        return new ThresholdSnapshot(
                calculateTriggerAmount(estimatedTotalAmount, deductionsKnown),
                deductionsKnown ? DEDUCTIONS_KNOWN_TRIGGER_RATIO : DEDUCTIONS_UNKNOWN_TRIGGER_RATIO,
                !deductionsKnown
        );
    }

    private BigDecimal calculateDifferenceRate(long estimatedTotalAmount, long differenceAmount) {
        if (estimatedTotalAmount == 0) {
            return differenceAmount == 0 ? BigDecimal.ZERO : BigDecimal.ONE.setScale(4);
        }
        return BigDecimal.valueOf(Math.abs(differenceAmount))
                .divide(BigDecimal.valueOf(estimatedTotalAmount), 4, RoundingMode.HALF_UP);
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
        for (CauseDescription cause : buildCauseDescriptions(
                metrics.totalOvertimeMinutes(),
                metrics.totalNightMinutes(),
                differenceAmount,
                anomalyDetected
        )) {
            reasons.add(new WageDifferenceReason(
                    cause.code(),
                    cause.title(),
                    cause.detail(),
                    relatedWorkProofIds
            ));
        }

        return List.copyOf(reasons);
    }

    /**
     * verification은 evidence record 소유권을 스냅샷이 들고 있으므로,
     * cause에는 record id를 직접 붙이지 않고 문구만 분리해 저장한다.
     */
    private List<WageVerificationCause> buildVerificationCauses(long overtimeMinutes,
                                                                long nightMinutes,
                                                                Long differenceAmount,
                                                                boolean checkRequired) {
        return buildCauseDescriptions(overtimeMinutes, nightMinutes, differenceAmount, checkRequired).stream()
                .map(cause -> new WageVerificationCause(cause.code(), cause.title(), cause.detail()))
                .toList();
    }

    private List<CauseDescription> buildCauseDescriptions(long overtimeMinutes,
                                                          long nightMinutes,
                                                          Long differenceAmount,
                                                          boolean issueDetected) {
        List<CauseDescription> causes = new ArrayList<>();

        if (overtimeMinutes > 0) {
            causes.add(new CauseDescription(
                    "OVERTIME_INCLUDED",
                    "연장 근무가 추정에 포함됐습니다",
                    "하루 8시간을 초과한 reflected 근무가 참고용 추정 급여에 반영됐습니다."
            ));
        }
        if (nightMinutes > 0) {
            causes.add(new CauseDescription(
                    "NIGHT_SHIFT_INCLUDED",
                    "야간 근무가 추정에 포함됐습니다",
                    "22:00-06:00 구간의 reflected 근무가 참고용 추정 급여에 반영됐습니다."
            ));
        }
        if (issueDetected && differenceAmount != null) {
            causes.add(new CauseDescription(
                    "DIFFERENCE_OVER_THRESHOLD",
                    "확인 필요한 차이가 감지됐습니다",
                    differenceAmount >= 0
                            ? "참고용 추정 급여가 실제 입금액보다 임계값 이상 크게 계산돼 근거 확인이 필요합니다."
                            : "실제 입금액과 참고용 추정 급여 사이에 임계값 이상 차이가 있어 근거 확인이 필요합니다."
            ));
        }

        return List.copyOf(causes);
    }

    private Lane1EstimateBreakdown calculateLane1EstimateBreakdown(CurrentContractResponse contract,
                                                                   WorkProofMonthlySummaryContractResponse summary) {
        return new Lane1EstimateBreakdown(
                prorateAmount(
                        summary.integrity().verifiedMinutes(),
                        contract.normalizedHourlyWage(),
                        BigDecimal.ONE,
                        LANE1_ESTIMATE_ROUNDING_MODE
                ),
                prorateAmount(
                        summary.overtimeMinutes(),
                        contract.normalizedHourlyWage(),
                        BigDecimal.valueOf(0.5),
                        LANE1_ESTIMATE_ROUNDING_MODE
                ),
                prorateAmount(
                        summary.nightMinutes(),
                        contract.normalizedHourlyWage(),
                        BigDecimal.valueOf(0.5),
                        LANE1_ESTIMATE_ROUNDING_MODE
                )
        );
    }

    private record CauseDescription(
            String code,
            String title,
            String detail
    ) {
    }

    private record Lane1EstimateBreakdown(
            long baseEstimate,
            long overtimePremium,
            long nightPremium
    ) {
        private long estimatedTotal() {
            return baseEstimate + overtimePremium + nightPremium;
        }

        private WageEstimateSnapshot toSnapshot() {
            return new WageEstimateSnapshot(
                    baseEstimate,
                    overtimePremium,
                    nightPremium,
                    estimatedTotal()
            );
        }
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

    public record ThresholdSnapshot(
            long absoluteWon,
            BigDecimal relativePercent,
            boolean deductionRelaxed
    ) {
    }

    public record WageVerificationCause(
            String code,
            String title,
            String detail
    ) {
    }

    public record WageVerificationSnapshot(
            WageEstimateSnapshot estimate,
            long differenceAmount,
            BigDecimal differenceRate,
            ThresholdSnapshot threshold,
            WageVerificationStatus status,
            WageVerificationResolutionStage resolutionStage,
            List<WageVerificationCause> possibleCauses
    ) {
    }
}
