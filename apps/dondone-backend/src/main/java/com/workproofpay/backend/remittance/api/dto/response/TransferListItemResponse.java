package com.workproofpay.backend.remittance.api.dto.response;

import java.time.LocalDateTime;

public record TransferListItemResponse(
        String transferId,
        String direction,
        String status,
        String assetSymbol,
        Long amountAtomic,
        String senderAddress,
        String senderName,
        String recipientId,
        String recipientAlias,
        String recipientAddress,
        String txHash,
        String networkFeeWei,
        String networkFeeAssetSymbol,
        LocalDateTime updatedAt
) {
}
