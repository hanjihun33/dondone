package com.workproofpay.backend.auth.api.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String email,
        String name
) {
}
