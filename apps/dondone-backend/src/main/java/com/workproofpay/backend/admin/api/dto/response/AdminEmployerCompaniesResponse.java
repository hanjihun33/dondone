package com.workproofpay.backend.admin.api.dto.response;

import java.util.List;

public record AdminEmployerCompaniesResponse(
        List<AdminEmployerCompanySummaryResponse> companies
) {
}
