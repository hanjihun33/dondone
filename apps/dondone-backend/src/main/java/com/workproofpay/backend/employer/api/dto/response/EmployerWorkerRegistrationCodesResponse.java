package com.workproofpay.backend.employer.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record EmployerWorkerRegistrationCodesResponse(
        @Schema(description = "Scoped company ID", example = "1")
        Long companyId,
        @Schema(description = "Scoped company name", example = "Acme Logistics")
        String companyName,
        @Schema(description = "Scoped workplace ID", example = "5")
        Long workplaceId,
        @Schema(description = "Scoped workplace name", example = "Seoul Hub")
        String workplaceName,
        @Schema(description = "Worker registration codes")
        List<EmployerWorkerRegistrationCodeResponse> codes
) {
}
