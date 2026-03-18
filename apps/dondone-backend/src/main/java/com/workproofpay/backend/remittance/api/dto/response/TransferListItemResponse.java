package com.workproofpay.backend.remittance.api.dto.response;

import java.time.LocalDateTime;

public record TransferListItemResponse(
        String transferId,
        String status,
        String assetSymbol,
        Long amountAtomic,
        String recipientId,
        String recipientAlias,
        String recipientAddress,
        String txHash,
        LocalDateTime updatedAt
) {
}
