package com.workproofpay.backend.remittance.api.dto.response;

import java.time.LocalDateTime;

public record RemittanceOpsTransferItemResponse(
        String transferId,
        Long userId,
        String status,
        String failureCode,
        String recipientAddress,
        String txHash,
        LocalDateTime updatedAt
) {
}
