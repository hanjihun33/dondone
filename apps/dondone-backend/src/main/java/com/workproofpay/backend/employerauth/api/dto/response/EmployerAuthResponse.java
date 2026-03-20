package com.workproofpay.backend.employerauth.api.dto.response;

import com.workproofpay.backend.employer.service.EmployerAccessScope;
import io.swagger.v3.oas.annotations.media.Schema;

public record EmployerAuthResponse(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature")
        String accessToken,
        @Schema(description = "Access token expiration in seconds", example = "86400")
        long expiresIn,
        @Schema(description = "Employer profile ID", example = "10")
        Long employerId,
        @Schema(description = "Connected company ID", example = "1")
        Long companyId,
        @Schema(description = "Connected company name", example = "Acme Logistics")
        String companyName,
        @Schema(description = "Default workplace ID", example = "5")
        Long defaultWorkplaceId,
        @Schema(description = "Default workplace name", example = "Seoul Hub")
        String defaultWorkplaceName
) {
    public static EmployerAuthResponse of(String accessToken, long expiresIn, EmployerAccessScope scope) {
        return new EmployerAuthResponse(
                accessToken,
                expiresIn,
                scope.employerId(),
                scope.companyId(),
                scope.companyName(),
                scope.defaultWorkplaceId(),
                scope.defaultWorkplaceName()
        );
    }
}
