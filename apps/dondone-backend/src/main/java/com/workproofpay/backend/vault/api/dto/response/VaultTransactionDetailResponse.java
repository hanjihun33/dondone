package com.workproofpay.backend.vault.api.dto.response;

import java.time.LocalDateTime;

public record VaultTransactionDetailResponse(
        String requestId,
        String txType,
        String status,
        String walletAddress,
        String vaultAddress,
        String assetSymbol,
        String amountAtomic,
        String shareDelta,
        String txHash,
        String failureCode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime confirmedAt
) {
}
