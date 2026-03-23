package com.workproofpay.backend.employer.api.dto.response;

import com.workproofpay.backend.workproof.api.dto.response.WorkProofReflectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record EmployerReviewRecordConfirmResponse(
        @Schema(description = "Confirmed WorkProof ID", example = "12")
        Long workProofId,
        @Schema(description = "Updated reflection status", example = "REFLECTED")
        WorkProofReflectionStatus reflectionStatus,
        @Schema(description = "Server timestamp when the review was confirmed")
        LocalDateTime confirmedAt
) {
}
