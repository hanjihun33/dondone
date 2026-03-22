package com.workproofpay.backend.admin.api.dto.response;

import java.time.LocalDateTime;

public record AdminEmployerCompanyEmployerSummaryResponse(
        Long employerProfileId,
        Long accountId,
        String displayName,
        String email,
        String profileStatus,
        Long defaultWorkplaceId,
        String defaultWorkplaceName,
        boolean workplaceSettingsConfigured,
        LocalDateTime joinedAt
) {
}
