package com.workproofpay.backend.auth.api.dto.response;

import com.workproofpay.backend.auth.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record MeResponse(
        @Schema(description = "Authenticated user ID", example = "1")
        Long userId,
        @Schema(description = "Authenticated user email", example = "test@gmail.com")
        String email,
        @Schema(description = "Authenticated user display name", example = "Test User")
        String name,
        @Schema(description = "Authenticated user phone number", example = "01012345678")
        String phoneNumber,
        @Schema(description = "Authenticated user company code", example = "DONDONE2026")
        String companyCode,
        @Schema(description = "Authenticated user company name when a current worker membership exists", example = "Acme Logistics")
        String companyName,
        @Schema(description = "Authenticated user role", example = "USER")
        String role
) {
    public static MeResponse from(User user, String companyCode, String companyName) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhoneNumber(),
                companyCode,
                companyName,
                user.getRole().name()
        );
    }
}
