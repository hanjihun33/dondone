package com.workproofpay.backend.employer.api.dto.response;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.employer.service.EmployerAccessScope;
import io.swagger.v3.oas.annotations.media.Schema;

public record EmployerProfileResponse(
        @Schema(description = "Employer profile ID", example = "10")
        Long employerId,
        @Schema(description = "Employer display name", example = "Acme HR")
        String displayName,
        @Schema(description = "Employer account email", example = "manager@acme.test")
        String email,
        @Schema(description = "Connected company ID", example = "1")
        Long companyId,
        @Schema(description = "Connected company name", example = "Acme Logistics")
        String companyName,
        @Schema(description = "Connected company code", example = "ACME-SEOUL")
        String companyCode,
        @Schema(description = "Default workplace ID", example = "5")
        Long defaultWorkplaceId,
        @Schema(description = "Default workplace name", example = "Seoul Hub")
        String defaultWorkplaceName,
        @Schema(description = "Employer profile status", example = "ACTIVE")
        String status
) {
    public static EmployerProfileResponse from(User user, EmployerAccessScope scope) {
        return new EmployerProfileResponse(
                scope.employerId(),
                scope.displayName(),
                user.getEmail(),
                scope.companyId(),
                scope.companyName(),
                scope.companyCode(),
                scope.defaultWorkplaceId(),
                scope.defaultWorkplaceName(),
                scope.status().name()
        );
    }
}
