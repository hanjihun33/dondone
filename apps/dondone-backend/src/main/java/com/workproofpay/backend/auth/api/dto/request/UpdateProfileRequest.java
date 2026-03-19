package com.workproofpay.backend.auth.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
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
