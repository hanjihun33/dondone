package com.workproofpay.backend.advance.service;

import com.workproofpay.backend.advance.api.dto.response.AdvanceEligibilityResponse;
import com.workproofpay.backend.advance.model.AdvanceBlockReasonCode;
import com.workproofpay.backend.advance.model.AdvanceRepaymentTier;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryContractResponse;
import com.workproofpay.backend.workproof.model.WorkContract;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Component
public class AdvancePolicyEngine {

    static final int DEFAULT_PAYDAY_DAY = 25;
    static final int STANDARD_WORKDAY_MINUTES = 480;
    static final long DEMO_MAX_CAP = 500_000L;
    static final long REDUCED_PAYDAY_CAP = 50_000L;
    static final long FLAT_FEE_AMOUNT = 5_000L;
    static final String ADVANCE_DISCLAIMER = "미리받기 금액은 반영된 근무 기록 기준의 데모 시뮬레이션입니다. 실제 금융 서비스 제공을 의미하지 않습니다.";

    public AdvanceEligibilityResponse evaluate(
            Long workplaceId,
            WorkContract contract,
            WorkProofMonthlySummaryContractResponse summary,
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
        long baseLimit = BigDecimal.valueOf(verifiedAmount)
                .multiply(tier.ratio())
                .setScale(0, RoundingMode.DOWN)
                .longValue();

        int daysUntilRepayment = daysUntilRepayment(today, targetMonth);
        boolean isRepaymentDate = daysUntilRepayment == 0;
        long paydayCap = isRepaymentDate ? 0L : daysUntilRepayment <= 7 ? REDUCED_PAYDAY_CAP : DEMO_MAX_CAP;

        List<String> blockReasonCodes = new ArrayList<>();
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
            blockReasonCodes.add(AdvanceBlockReasonCode.PAYDAY_TOO_CLOSE.name());
            hardBlocked = true;
        }
        if (needsReviewRecordCount > 0 || pendingMinutes > 0) {
            blockReasonCodes.add(AdvanceBlockReasonCode.PENDING_WORKPROOF_REVIEW.name());
        }

        long availableAmount = hardBlocked
                ? 0L
                : Math.min(Math.min(baseLimit, tier.capAmount()), Math.min(paydayCap, DEMO_MAX_CAP));

        Integer nextTierDays = tier.nextMinimumDays();
        long nextTierRemainingMinutes = nextTierDays == null
                ? 0L
                : Math.max(0L, (long) nextTierDays * STANDARD_WORKDAY_MINUTES - reflectedWorkMinutes);

        return new AdvanceEligibilityResponse(
                workplaceId,
                availableAmount,
                DEMO_MAX_CAP,
                tier.ratio(),
                tier.code(),
                reflectedWorkDays,
                reflectedWorkMinutes,
                verifiedMinutes,
                pendingMinutes,
                needsReviewRecordCount,
                List.copyOf(blockReasonCodes),
                nextTierRemainingMinutes,
                availableAmount > 0 ? FLAT_FEE_AMOUNT : 0L,
                repaymentDate(today, targetMonth),
                ADVANCE_DISCLAIMER
        );
    }

    public YearMonth resolveTargetMonth(LocalDate today, YearMonth latestWorkedMonth) {
        YearMonth cycleMonth = YearMonth.from(today);
        if (today.getDayOfMonth() > DEFAULT_PAYDAY_DAY) {
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
                        || code.equals(AdvanceBlockReasonCode.PAYDAY_TOO_CLOSE.name())
        );
    }

    private int daysUntilRepayment(LocalDate today, YearMonth targetMonth) {
        LocalDate repaymentDate = repaymentDate(today, targetMonth);
        return (int) (repaymentDate.toEpochDay() - today.toEpochDay());
    }

    private LocalDate repaymentDate(LocalDate today, YearMonth targetMonth) {
        YearMonth repaymentMonth = targetMonth.isBefore(YearMonth.from(today)) ? YearMonth.from(today) : targetMonth;
        return repaymentMonth.atDay(Math.min(DEFAULT_PAYDAY_DAY, repaymentMonth.lengthOfMonth()));
    }
}
