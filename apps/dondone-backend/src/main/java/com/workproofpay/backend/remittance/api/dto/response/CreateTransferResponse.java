package com.workproofpay.backend.remittance.api.dto.response;

import java.time.LocalDateTime;

public record CreateTransferResponse(
        String transferId,
        String status,
        String assetSymbol,
        Long amountAtomic,
        String recipientId,
        LocalDateTime createdAt
) {
}
