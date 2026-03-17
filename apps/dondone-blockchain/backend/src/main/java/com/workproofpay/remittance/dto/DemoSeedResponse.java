package com.workproofpay.remittance.dto;

public record DemoSeedResponse(
        String seedName,
        String userId,
        String walletAddress,
        int recipientCount,
        int transferCount
) {}
