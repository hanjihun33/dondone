package com.workproofpay.backend.vault.api.dto.response;

import java.time.LocalDateTime;

public record VaultTransactionItemResponse(
        String requestId,
        String txType,
        String status,
        String amountAtomic,
        String shareDelta,
        String txHash,
        String failureCode,
        LocalDateTime updatedAt
) {
}
