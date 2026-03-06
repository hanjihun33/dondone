package com.workproofpay.backend.auth.api.dto.response;

public record MeResponse(
        Long userId,
        String email,
        String name,
        String role
) {
}
