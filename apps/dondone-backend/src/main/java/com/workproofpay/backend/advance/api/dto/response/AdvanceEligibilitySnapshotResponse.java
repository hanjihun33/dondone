package com.workproofpay.backend.advance.api.dto.response;

import java.math.BigDecimal;

public record AdvanceEligibilitySnapshotResponse(
        String assetSymbol,
        Integer assetDecimals,
        BigDecimal exchangeRateSnapshot,
        Long availableAmountAtomic,
        Long availableDisplayKrwAmount,
        Long maxCapAmountAtomic,
        Long maxCapDisplayKrwAmount,
        BigDecimal policyRate,
        Integer reflectedWorkDays,
        Long reflectedWorkMinutes,
        Integer needsReviewRecordCount
) {
}
