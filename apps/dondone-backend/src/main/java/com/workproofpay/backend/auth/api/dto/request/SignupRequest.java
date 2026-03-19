package com.workproofpay.backend.auth.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record SignupRequest(
        @Schema(description = "User email address", example = "worker@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,

        @Schema(description = "Password with at least 8 characters", example = "qweqwe123")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @Schema(description = "Display name", example = "Test User")
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be 100 characters or less")
        String name,

        @Schema(description = "Korean mobile phone number", example = "010-1234-5678")
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9-]{10,13}$", message = "Phone number format is invalid")
        String phoneNumber
) {
}
