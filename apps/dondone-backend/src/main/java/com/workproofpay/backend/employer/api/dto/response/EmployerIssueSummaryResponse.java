package com.workproofpay.backend.employer.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployerIssueSummaryResponse(
        EmployerIssueItemType itemType,
        EmployerIssueStatus issueStatus,
        Long requestId,
        Long workProofId,
        Long workerId,
        String workerName,
        String workerEmail,
        String role,
        LocalDate workDate,
        LocalDateTime clockInAt,
        LocalDateTime clockOutAt,
        LocalDateTime requestedClockInAt,
        LocalDateTime requestedClockOutAt,
        String reason,
        String reviewReasonCode,
        LocalDateTime raisedAt
) {
}
