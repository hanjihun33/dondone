package com.workproofpay.backend.remittance.api.dto.response;

import java.util.List;

public record RecipientListResponse(
        List<RecipientItemResponse> recipients
) {
}
