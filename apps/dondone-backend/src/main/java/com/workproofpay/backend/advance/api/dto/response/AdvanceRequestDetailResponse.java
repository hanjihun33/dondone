package com.workproofpay.backend.advance.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdvanceRequestDetailResponse(
        Long requestId,
        Long workplaceId,
        String assetSymbol,
        Integer assetDecimals,
        java.math.BigDecimal referenceExchangeRate,
        Long requestedAmountAtomic,
        Long requestedReferenceKrw,
        Long approvedAmountAtomic,
        Long approvedReferenceKrw,
        Long feeAmountAtomic,
        Long feeReferenceKrw,
        String status,
        LocalDate repaymentDueDate,
        AdvanceEligibilitySnapshotResponse eligibilitySnapshot,
        LocalDateTime createdAt
) {
}
