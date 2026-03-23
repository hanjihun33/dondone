package com.workproofpay.backend.auth.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateCompanyCodeRequest(
        @Schema(description = "Current company code", example = "DN-SEOUL-4101")
        @NotBlank(message = "Company code is required")
        @Pattern(regexp = "^[A-Za-z0-9-]{6,50}$", message = "Company code format is invalid")
        String companyCode
) {
}
