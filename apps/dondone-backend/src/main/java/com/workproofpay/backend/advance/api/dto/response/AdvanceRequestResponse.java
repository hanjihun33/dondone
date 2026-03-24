package com.workproofpay.backend.advance.api.dto.response;

import java.time.LocalDate;

public record AdvanceRequestResponse(
        Long requestId,
        String assetSymbol,
        Integer assetDecimals,
        java.math.BigDecimal exchangeRateSnapshot,
        String status,
        Long approvedAmountAtomic,
        Long approvedDisplayKrwAmount,
        Long feeAmountAtomic,
        Long feeDisplayKrwAmount,
        LocalDate repaymentDueDate,
        AdvanceEligibilitySnapshotResponse eligibilitySnapshot
) {
}
