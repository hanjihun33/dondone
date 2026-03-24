package com.workproofpay.backend.advance.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdvanceEligibilityResponse(
        Long workplaceId,
        String assetSymbol,
        Integer assetDecimals,
        java.math.BigDecimal referenceExchangeRate,
        Long availableAmountAtomic,
        Long availableReferenceKrw,
        Long maxCapAmountAtomic,
        Long maxCapReferenceKrw,
        BigDecimal policyRate,
        String repaymentTier,
        Integer reflectedWorkDays,
        Long reflectedWorkMinutes,
        Long verifiedMinutes,
        Long pendingMinutes,
        Integer needsReviewRecordCount,
        List<String> blockReasonCodes,
        List<String> noticeReasonCodes,
        Long nextTierRemainingMinutes,
        Long estimatedFeeAmountAtomic,
        Long estimatedFeeReferenceKrw,
        LocalDate estimatedRepaymentDate,
        String disclaimer
) {
}
