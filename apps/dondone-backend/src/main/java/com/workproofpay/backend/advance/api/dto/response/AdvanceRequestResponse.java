package com.workproofpay.backend.advance.api.dto.response;

import java.time.LocalDate;

public record AdvanceRequestResponse(
        Long requestId,
        String status,
        Long approvedAmount,
        Long feeAmount,
        LocalDate repaymentDueDate,
        AdvanceEligibilitySnapshotResponse eligibilitySnapshot
) {
}
