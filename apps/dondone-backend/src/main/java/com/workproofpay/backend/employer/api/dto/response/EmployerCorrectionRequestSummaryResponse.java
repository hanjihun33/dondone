package com.workproofpay.backend.employer.api.dto.response;

import com.workproofpay.backend.correction.model.CorrectionRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployerCorrectionRequestSummaryResponse(
        Long requestId,
        Long workProofId,
        Long workerId,
        String workerName,
        String workerEmail,
        String role,
        LocalDate workDate,
        LocalDateTime originalClockInAt,
        LocalDateTime originalClockOutAt,
        LocalDateTime requestedClockInAt,
        LocalDateTime requestedClockOutAt,
        String reason,
        LocalDateTime requestedAt,
        CorrectionRequestStatus status
) {
}
