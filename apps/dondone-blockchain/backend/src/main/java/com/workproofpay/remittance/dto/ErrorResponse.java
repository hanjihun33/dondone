package com.workproofpay.remittance.dto;

import java.util.Map;

public record ErrorResponse(ErrorBody error) {
    public static record ErrorBody(
            String code,
            String message,
            String requestId,
            Map<String, Object> details
    ) {}
}
