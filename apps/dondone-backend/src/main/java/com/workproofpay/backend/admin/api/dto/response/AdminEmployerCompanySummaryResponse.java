package com.workproofpay.backend.admin.api.dto.response;

import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.workproof.model.Workplace;

import java.time.LocalDateTime;

public record AdminEmployerCompanySummaryResponse(
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
        boolean hasActiveEmployerSignupCode,
        LocalDateTime latestEmployerSignupCodeIssuedAt,
        LocalDateTime createdAt
) {
    public static AdminEmployerCompanySummaryResponse of(Company company,
                                                         Workplace workplace,
                                                         boolean workplaceSettingsConfigured,
                                                         boolean hasJoinedEmployer,
                                                         long employerCount,
                                                         LocalDateTime latestEmployerJoinedAt,
                                                         boolean hasActiveEmployerSignupCode,
                                                         LocalDateTime latestEmployerSignupCodeIssuedAt) {
        return new AdminEmployerCompanySummaryResponse(
                company.getId(),
                company.getName(),
                company.getCompanyCode(),
                workplace.getId(),
                workplace.getName(),
                workplace.getAddress(),
                workplace.resolveDetailAddress(),
                workplace.getLatitude(),
                workplace.getLongitude(),
                workplace.getAllowedRadiusMeters(),
                workplaceSettingsConfigured,
                hasJoinedEmployer,
                employerCount,
                latestEmployerJoinedAt,
                hasActiveEmployerSignupCode,
                latestEmployerSignupCodeIssuedAt,
                company.getCreatedAt()
        );
    }
}
