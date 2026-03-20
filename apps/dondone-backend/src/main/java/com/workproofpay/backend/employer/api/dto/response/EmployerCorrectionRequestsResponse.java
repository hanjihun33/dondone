package com.workproofpay.backend.employer.api.dto.response;

import java.util.List;

public record EmployerCorrectionRequestsResponse(
        List<EmployerCorrectionRequestSummaryResponse> requests,
        int page,
        int size,
        int totalElements,
        int totalPages
) {
}
