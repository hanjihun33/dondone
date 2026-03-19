package com.workproofpay.backend.remittance.api.dto.response;

public record RemittanceAdminActionResponse(
        String action,
        String targetId,
        String status,
        String message
) {
}
