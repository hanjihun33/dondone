package com.workproofpay.backend.employer.api.dto.response;

import com.workproofpay.backend.employer.model.WorkerRegistrationCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record EmployerWorkerRegistrationCodeResponse(
        @Schema(description = "Registration code ID", example = "1")
        Long codeId,
        @Schema(description = "Worker registration code", example = "WORKER-AB12-CD34")
        String registrationCode,
        @Schema(description = "Whether this code can still be redeemed", example = "true")
        boolean active,
        @Schema(description = "Issued at", example = "2026-03-23T10:15:00")
        LocalDateTime issuedAt,
        @Schema(description = "Revoked at", example = "2026-03-23T11:00:00")
        LocalDateTime revokedAt
) {
    public static EmployerWorkerRegistrationCodeResponse of(WorkerRegistrationCode code, String rawCode) {
        return new EmployerWorkerRegistrationCodeResponse(
                code.getId(),
                rawCode,
                code.isUsable(),
                code.getCreatedAt(),
                code.getRevokedAt()
        );
    }
}
