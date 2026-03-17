package com.workproofpay.remittance.dto;

import java.time.Instant;

public record UserWalletResponse(
        String userId,
        String walletAddress,
        Instant createdAt
) {}
