package com.workproofpay.backend.employerauth.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployerSignupRequest(
        @Schema(description = "Company code for employer signup", example = "EMPLOYER-SEOUL-2026")
        @NotBlank(message = "Company code is required")
        String companyCode,

        @Schema(description = "Employer display name", example = "Acme HR")
        @NotBlank(message = "Display name is required")
        @Size(max = 100, message = "Display name must be 100 characters or less")
        String displayName,

        @Schema(description = "Employer email", example = "manager@acme.test")
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,

        @Schema(description = "Password with at least 8 characters", example = "qweqwe123")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
