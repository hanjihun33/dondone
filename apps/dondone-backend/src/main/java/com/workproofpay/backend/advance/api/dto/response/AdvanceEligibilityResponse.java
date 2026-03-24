package com.workproofpay.backend.advance.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdvanceEligibilityResponse(
        Long workplaceId,
        String assetSymbol,
        Integer assetDecimals,
        java.math.BigDecimal exchangeRateSnapshot,
        Long reflectedEarnedAmountAtomic,
        Long reflectedEarnedDisplayKrwAmount,
        Long alreadyAdvancedAmountAtomic,
        Long alreadyAdvancedDisplayKrwAmount,
        Long availableAmountAtomic,
        Long availableDisplayKrwAmount,
        Long maxCapAmountAtomic,
        Long maxCapDisplayKrwAmount,
        BigDecimal policyRate,
        String repaymentTier,
        Integer reflectedWorkDays,
        Long reflectedWorkMinutes,
        Long verifiedMinutes,
        Long pendingMinutes,
        Integer needsReviewRecordCount,
        Integer pendingRecordCount,
        List<String> blockReasonCodes,
        List<String> noticeReasonCodes,
        Long nextTierRemainingMinutes,
        Long estimatedFeeAmountAtomic,
        Long estimatedFeeDisplayKrwAmount,
        LocalDate settlementDueDate,
        LocalDate estimatedRepaymentDate,
        String disclaimer
) {
}
