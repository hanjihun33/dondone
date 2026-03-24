package com.workproofpay.backend.advance.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdvanceRequestListItemResponse(
        Long requestId,
        Long workplaceId,
        String assetSymbol,
        Integer assetDecimals,
        java.math.BigDecimal referenceExchangeRate,
        Long requestedAmountAtomic,
        Long requestedReferenceKrw,
        Long approvedAmountAtomic,
        Long approvedReferenceKrw,
        String status,
        LocalDate repaymentDueDate,
        LocalDateTime requestedAt
) {
}
