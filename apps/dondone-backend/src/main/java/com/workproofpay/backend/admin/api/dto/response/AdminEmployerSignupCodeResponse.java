package com.workproofpay.backend.admin.api.dto.response;

import java.time.LocalDateTime;

public record AdminEmployerSignupCodeResponse(
        Long companyId,
        String companyName,
        Long defaultWorkplaceId,
        String defaultWorkplaceName,
        String employerSignupCode,
        LocalDateTime issuedAt
) {
}
