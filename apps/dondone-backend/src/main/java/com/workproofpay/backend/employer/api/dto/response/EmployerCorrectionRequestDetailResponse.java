package com.workproofpay.backend.employer.api.dto.response;

import com.workproofpay.backend.correction.model.CorrectionRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployerCorrectionRequestDetailResponse(
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
        String requestMemo,
        int attachmentCount,
        LocalDateTime requestedAt,
        CorrectionRequestStatus status,
        Long decisionByAccountId,
        String decisionByName,
        LocalDateTime decisionAt,
        String decisionMemo,
        String rejectReasonCode
) {
}
