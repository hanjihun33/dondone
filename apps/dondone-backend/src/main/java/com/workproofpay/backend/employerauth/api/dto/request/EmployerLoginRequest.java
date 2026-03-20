package com.workproofpay.backend.employerauth.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmployerLoginRequest(
        @Schema(description = "Employer email", example = "manager@acme.test")
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,

        @Schema(description = "Employer password", example = "qweqwe123")
        @NotBlank(message = "Password is required")
        String password
) {
}
