package com.workproofpay.backend.advance.service;

import com.workproofpay.backend.advance.api.dto.response.AdvanceEligibilityResponse;
import com.workproofpay.backend.advance.model.AdvanceBlockReasonCode;
import com.workproofpay.backend.advance.model.AdvanceFeeType;
import com.workproofpay.backend.advance.model.AdvancePolicy;
import com.workproofpay.backend.advance.model.AdvanceRepaymentTier;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryContractResponse;
import com.workproofpay.backend.workproof.model.WorkContract;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Component
public class AdvancePolicyEngine {

    static final int STANDARD_WORKDAY_MINUTES = 480;

    public AdvanceEligibilityResponse evaluate(
            AdvancePolicy policy,
            Long workplaceId,
            WorkContract contract,
            WorkProofMonthlySummaryContractResponse summary,
            long alreadyAdvancedAmountAtomic,
            long alreadyAdvancedDisplayKrwAmount,
            boolean hasOutstandingAdvance,
            LocalDate today,
            YearMonth targetMonth
    ) {
        int reflectedWorkDays = summary.financeReadiness().advanceEligibleWorkDays();
        long reflectedWorkMinutes = summary.totalWorkMinutes();
        long verifiedMinutes = summary.integrity().verifiedMinutes();
        long pendingMinutes = summary.integrity().pendingMinutes();
        int needsReviewRecordCount = summary.reflection().needsReviewRecordCount();
        AdvanceRepaymentTier tier = AdvanceRepaymentTier.fromReflectedDays(reflectedWorkDays);

        long verifiedAmount = contract.getNormalizedHourlyWage()
                .multiply(BigDecimal.valueOf(verifiedMinutes))
                .divide(BigDecimal.valueOf(60), 0, RoundingMode.DOWN)
                .longValue();
        long reflectedEarnedAmountAtomic = toAtomic(policy, verifiedAmount);
        long baseLimit = BigDecimal.valueOf(verifiedAmount)
                .multiply(tier.ratio())
                .setScale(0, RoundingMode.DOWN)
                .longValue();

        int daysUntilRepayment = daysUntilRepayment(policy, today, targetMonth);
        boolean isRepaymentDate = daysUntilRepayment == 0;
        long paydayCapKrw = isRepaymentDate
                ? 0L
                : daysUntilRepayment <= policy.getReducedCapDaysBeforePayday()
                ? policy.getNearPaydayMaxCapDisplayKrwAmount()
                : policy.getMaxCapDisplayKrwAmount();

        List<String> blockReasonCodes = new ArrayList<>();
        List<String> noticeReasonCodes = new ArrayList<>();
        boolean hardBlocked = false;
        if (reflectedWorkDays == 0 || verifiedMinutes == 0) {
            blockReasonCodes.add(AdvanceBlockReasonCode.INSUFFICIENT_VERIFIED_WORK.name());
            hardBlocked = true;
        }
        if (hasOutstandingAdvance) {
            blockReasonCodes.add(AdvanceBlockReasonCode.EXISTING_OUTSTANDING_ADVANCE.name());
            hardBlocked = true;
        }
        if (isRepaymentDate) {
            blockReasonCodes.add(AdvanceBlockReasonCode.ADVANCE_WINDOW_CLOSED_TODAY.name());
            hardBlocked = true;
        }
        if (needsReviewRecordCount > 0 || pendingMinutes > 0) {
            noticeReasonCodes.add(AdvanceBlockReasonCode.PENDING_WORKPROOF_REVIEW.name());
        }

        long maxAvailableBeforeUsageDisplayKrwAmount = Math.min(Math.min(baseLimit, tier.capAmount()), Math.min(paydayCapKrw, policy.getMaxCapDisplayKrwAmount()));
        long availableDisplayKrwAmount = hardBlocked
                ? 0L
                : Math.max(0L, maxAvailableBeforeUsageDisplayKrwAmount - alreadyAdvancedDisplayKrwAmount);
        long maxCapDisplayKrwAmount = policy.getMaxCapDisplayKrwAmount();
        long estimatedFeeDisplayKrwAmount = estimateFeeDisplayKrwAmount(policy, availableDisplayKrwAmount);

        Integer nextTierDays = tier.nextMinimumDays();
        AdvanceRepaymentTier nextTier = nextTierDays == null
                ? null
                : AdvanceRepaymentTier.fromReflectedDays(nextTierDays);
        int remainingWorkDaysToNextTier = nextTierDays == null
                ? 0
                : Math.max(0, nextTierDays - reflectedWorkDays);
        long nextTierRemainingMinutes = nextTierDays == null
                ? 0L
                : Math.max(0L, (long) nextTierDays * STANDARD_WORKDAY_MINUTES - reflectedWorkMinutes);
        BigDecimal progressToNextTier = nextTierDays == null
                ? BigDecimal.ONE
                : BigDecimal.valueOf(reflectedWorkDays)
                .divide(BigDecimal.valueOf(nextTierDays), 4, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .min(BigDecimal.ONE);
        long nextTierExpectedCapDisplayKrw = nextTier == null
                ? maxAvailableBeforeUsageDisplayKrwAmount
                : Math.min(
                Math.min(nextTier.capAmount(), paydayCapKrw),
                policy.getMaxCapDisplayKrwAmount()
        );

        return new AdvanceEligibilityResponse(
                workplaceId,
                policy.getAssetSymbol(),
                policy.getAssetDecimals(),
                policy.getReferenceKrwPerAsset(),
                reflectedEarnedAmountAtomic,
                verifiedAmount,
                alreadyAdvancedAmountAtomic,
                alreadyAdvancedDisplayKrwAmount,
                toAtomic(policy, availableDisplayKrwAmount),
                availableDisplayKrwAmount,
                toAtomic(policy, maxCapDisplayKrwAmount),
                maxCapDisplayKrwAmount,
                tier.ratio(),
                tier.code(),
                nextTier != null ? nextTier.code() : null,
                progressToNextTier,
                remainingWorkDaysToNextTier,
                nextTierExpectedCapDisplayKrw,
                tier.code(),
                reflectedWorkDays,
                reflectedWorkMinutes,
                verifiedMinutes,
                pendingMinutes,
                needsReviewRecordCount,
                needsReviewRecordCount,
                List.copyOf(blockReasonCodes),
                List.copyOf(noticeReasonCodes),
                nextTierRemainingMinutes,
                toAtomic(policy, estimatedFeeDisplayKrwAmount),
                estimatedFeeDisplayKrwAmount,
                repaymentDate(policy, today, targetMonth),
                repaymentDate(policy, today, targetMonth),
                policy.getDisclaimer()
        );
    }

    public YearMonth resolveTargetMonth(AdvancePolicy policy, LocalDate today, YearMonth latestWorkedMonth) {
        YearMonth cycleMonth = YearMonth.from(today);
        if (today.getDayOfMonth() > policy.getPaydayDay()) {
            cycleMonth = cycleMonth.plusMonths(1);
        }
        if (latestWorkedMonth == null || latestWorkedMonth.isBefore(cycleMonth)) {
            return cycleMonth;
        }
        return latestWorkedMonth;
    }

    public boolean isHardBlocked(AdvanceEligibilityResponse response) {
        return response.blockReasonCodes().stream().anyMatch(code ->
                code.equals(AdvanceBlockReasonCode.INSUFFICIENT_VERIFIED_WORK.name())
                        || code.equals(AdvanceBlockReasonCode.EXISTING_OUTSTANDING_ADVANCE.name())
                        || code.equals(AdvanceBlockReasonCode.ADVANCE_WINDOW_CLOSED_TODAY.name())
        );
    }

    private int daysUntilRepayment(AdvancePolicy policy, LocalDate today, YearMonth targetMonth) {
        LocalDate repaymentDate = repaymentDate(policy, today, targetMonth);
        return (int) (repaymentDate.toEpochDay() - today.toEpochDay());
    }

    private LocalDate repaymentDate(AdvancePolicy policy, LocalDate today, YearMonth targetMonth) {
        YearMonth repaymentMonth = targetMonth.isBefore(YearMonth.from(today)) ? YearMonth.from(today) : targetMonth;
        return repaymentMonth.atDay(Math.min(policy.getPaydayDay(), repaymentMonth.lengthOfMonth()));
    }

    private long estimateFeeDisplayKrwAmount(AdvancePolicy policy, long availableDisplayKrwAmount) {
        if (availableDisplayKrwAmount <= 0) {
            return 0L;
        }
        if (policy.getFeeType() == AdvanceFeeType.FLAT) {
            return policy.getFlatFeeDisplayKrwAmount();
        }
        return 0L;
    }

    private long toAtomic(AdvancePolicy policy, long referenceKrw) {
        if (referenceKrw <= 0) {
            return 0L;
        }
        return BigDecimal.valueOf(referenceKrw)
                .multiply(BigDecimal.TEN.pow(policy.getAssetDecimals()))
                .divide(policy.getReferenceKrwPerAsset(), 0, RoundingMode.DOWN)
                .longValue();
    }
}
