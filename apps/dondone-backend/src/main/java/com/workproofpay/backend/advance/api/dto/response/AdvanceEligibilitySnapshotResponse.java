package com.workproofpay.backend.advance.api.dto.response;

import java.math.BigDecimal;

public record AdvanceEligibilitySnapshotResponse(
        String assetSymbol,
        Integer assetDecimals,
        BigDecimal referenceExchangeRate,
        Long availableAmountAtomic,
        Long availableReferenceKrw,
        Long maxCapAmountAtomic,
        Long maxCapReferenceKrw,
        BigDecimal policyRate,
        Integer reflectedWorkDays,
        Long reflectedWorkMinutes,
        Integer needsReviewRecordCount
) {
}
