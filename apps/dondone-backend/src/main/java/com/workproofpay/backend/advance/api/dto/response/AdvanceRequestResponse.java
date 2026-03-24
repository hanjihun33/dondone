package com.workproofpay.backend.advance.api.dto.response;

import java.time.LocalDate;

public record AdvanceRequestResponse(
        Long requestId,
        String assetSymbol,
        Integer assetDecimals,
        java.math.BigDecimal referenceExchangeRate,
        String status,
        Long approvedAmountAtomic,
        Long approvedReferenceKrw,
        Long feeAmountAtomic,
        Long feeReferenceKrw,
        LocalDate repaymentDueDate,
        AdvanceEligibilitySnapshotResponse eligibilitySnapshot
) {
}
