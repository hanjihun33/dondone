package com.workproofpay.remittance.dto;

import java.time.Instant;

public record TransferDetailResponse(
        String transferId,
        String status,
        String asset,
        String amount,
        String senderAddress,
        String recipientAddress,
        String txHash,
        String failureCode,
        Instant updatedAt
) {}
