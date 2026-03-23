package com.workproofpay.backend.vault.api.dto.response;

import java.time.LocalDateTime;

public record CreateVaultTransactionResponse(
        String requestId,
        String txType,
        String status,
        String detailPath,
        LocalDateTime createdAt
) {
}
