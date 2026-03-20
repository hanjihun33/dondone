package com.workproofpay.backend.remittance.api.dto.response;

import java.time.LocalDateTime;

public record RemittanceOpsJobItemResponse(
        Long jobId,
        String referenceKind,
        String jobType,
        String status,
        String referenceId,
        int attemptCount,
        String lastError,
        LocalDateTime updatedAt
) {
}
