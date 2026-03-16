package com.workproofpay.backend.advance.service;

import com.workproofpay.backend.advance.api.dto.response.AdvanceRequestResponse;

public record AdvanceCreateResult(
        AdvanceRequestResponse response,
        boolean replayed
) {
}
