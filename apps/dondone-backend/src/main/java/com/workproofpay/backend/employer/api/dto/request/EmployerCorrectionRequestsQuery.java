package com.workproofpay.backend.employer.api.dto.request;

import com.workproofpay.backend.correction.model.CorrectionRequestStatus;

import java.util.List;

public record EmployerCorrectionRequestsQuery(
        String query,
        List<CorrectionRequestStatus> statuses,
        Integer page,
        Integer size
) {
}
