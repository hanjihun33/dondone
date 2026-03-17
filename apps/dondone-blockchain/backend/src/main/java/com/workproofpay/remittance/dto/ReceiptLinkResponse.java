package com.workproofpay.remittance.dto;

import java.time.Instant;

public record ReceiptLinkResponse(
        String transferId,
        String downloadUrl,
        Instant expiresAt
) {}
