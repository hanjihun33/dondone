package com.workproofpay.backend.advance.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdvanceRequestListItemResponse(
        Long requestId,
        Long workplaceId,
        Long requestedAmount,
        Long approvedAmount,
        String status,
        LocalDate repaymentDueDate,
        LocalDateTime requestedAt
) {
}
