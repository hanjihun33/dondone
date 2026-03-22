package com.workproofpay.backend.admin.api.dto.response;

import java.time.LocalDateTime;

public record AdminEmployerCompanyCreatedResponse(
        Long companyId,
        String companyName,
        String companyCode,
        Long defaultWorkplaceId,
        String defaultWorkplaceName,
        String address,
        String detailAddress,
        Double latitude,
        Double longitude,
        Integer allowedRadiusMeters,
        boolean workplaceSettingsConfigured,
        boolean hasJoinedEmployer,
        long employerCount,
        LocalDateTime latestEmployerJoinedAt,
        String employerSignupCode,
        LocalDateTime employerSignupCodeIssuedAt,
        LocalDateTime createdAt
) {
}
