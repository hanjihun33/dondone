package com.workproofpay.backend.auth.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
        @Schema(description = "User email address", example = "test@gmail.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,

        @Schema(description = "User password", example = "qweqwe123")
        @NotBlank(message = "Password is required")
        String password
) {
}
