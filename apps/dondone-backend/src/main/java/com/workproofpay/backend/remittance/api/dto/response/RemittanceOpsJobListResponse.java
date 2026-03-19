package com.workproofpay.backend.remittance.api.dto.response;

import java.util.List;

public record RemittanceOpsJobListResponse(
        List<RemittanceOpsJobItemResponse> jobs
) {
}
