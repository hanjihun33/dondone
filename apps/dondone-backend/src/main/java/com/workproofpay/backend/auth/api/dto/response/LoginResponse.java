package com.workproofpay.backend.auth.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature")
        String accessToken,
        @Schema(description = "Authentication scheme", example = "Bearer")
        String tokenType,
        @Schema(description = "Access token expiration in milliseconds", example = "86400000")
        long expiresIn,
        @Schema(description = "Authenticated user ID", example = "1")
        Long userId,
        @Schema(description = "Authenticated user email", example = "test@gmail.com")
        String email,
        @Schema(description = "Authenticated user display name", example = "Test User")
        String name
) {
}
