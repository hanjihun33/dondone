package com.workproofpay.remittance.dto;

import java.time.Instant;

public record UserWalletResponse(
        Long userId,
        String walletAddress,
        Instant createdAt
) {}
