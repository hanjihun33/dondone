package com.workproofpay.backend.advance.api.dto.response;

import java.util.List;

public record AdvanceRequestListResponse(
        String month,
        List<AdvanceRequestListItemResponse> requests
) {
}
