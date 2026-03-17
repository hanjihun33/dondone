package com.workproofpay.remittance.dto;

import java.time.Instant;

public record CreateTransferResponse(
        String transferId,
        String status,
        PolicyResponse policy,
        Instant createdAt
) {}
