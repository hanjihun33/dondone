package com.workproofpay.backend.shared.api;

public record ValidationErrorDetail(
        String field,
        String message
) {
}
