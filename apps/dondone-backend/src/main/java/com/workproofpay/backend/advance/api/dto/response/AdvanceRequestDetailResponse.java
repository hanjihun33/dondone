package com.workproofpay.backend.advance.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdvanceRequestDetailResponse(
        Long requestId,
        Long workplaceId,
        Long requestedAmount,
        Long approvedAmount,
        Long feeAmount,
        String status,
        LocalDate repaymentDueDate,
        AdvanceEligibilitySnapshotResponse eligibilitySnapshot,
        LocalDateTime createdAt
) {
}
