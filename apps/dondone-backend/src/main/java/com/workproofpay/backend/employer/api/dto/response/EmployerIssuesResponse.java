package com.workproofpay.backend.employer.api.dto.response;

import java.util.List;

public record EmployerIssuesResponse(
        List<EmployerIssueSummaryResponse> issues,
        int page,
        int size,
        int totalElements,
        int totalPages
) {
}
