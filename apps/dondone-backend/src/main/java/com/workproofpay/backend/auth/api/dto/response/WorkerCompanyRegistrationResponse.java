package com.workproofpay.backend.auth.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record WorkerCompanyRegistrationResponse(
        @Schema(description = "Joined company ID", example = "1")
        Long companyId,
        @Schema(description = "Joined company name", example = "Acme Logistics")
        String companyName,
        @Schema(description = "Joined company code", example = "DNSEOUL01")
        String companyCode,
        @Schema(description = "Joined workplace ID", example = "10")
        Long workplaceId,
        @Schema(description = "Joined workplace name", example = "Seoul Hub")
        String workplaceName,
        @Schema(description = "Membership status", example = "ACTIVE")
        String membershipStatus,
        @Schema(description = "Membership effective from", example = "2026-03-23")
        LocalDate effectiveFrom
) {
}
