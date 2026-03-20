package com.workproofpay.backend.admin.api.dto.response;

import java.util.List;

public record AdminEmployerCompanyEmployersResponse(
        Long companyId,
        String companyName,
        List<AdminEmployerCompanyEmployerSummaryResponse> employers
) {
}
