package com.workproofpay.backend.auth.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record MeResponse(
        @Schema(description = "Authenticated user ID", example = "1")
        Long userId,
        @Schema(description = "Authenticated user email", example = "test@gmail.com")
        String email,
        @Schema(description = "Authenticated user display name", example = "Test User")
        String name,
        @Schema(description = "Authenticated user role", example = "USER")
        String role
) {
}
