package com.workproofpay.remittance.dto;

public record DemoSeedResponse(
        String seedName,
        Long userId,
        String walletAddress,
        int recipientCount,
        int transferCount
) {}
