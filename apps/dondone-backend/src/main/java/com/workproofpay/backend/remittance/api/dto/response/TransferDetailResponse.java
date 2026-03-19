package com.workproofpay.backend.remittance.api.dto.response;

import java.time.LocalDateTime;

public record TransferDetailResponse(
        String transferId,
        String status,
        String assetSymbol,
        Long amountAtomic,
        String senderAddress,
        String recipientId,
        String recipientAlias,
        String recipientAddress,
        String txHash,
        String failureCode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
