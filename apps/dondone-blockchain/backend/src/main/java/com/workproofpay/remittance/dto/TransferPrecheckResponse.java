package com.workproofpay.remittance.dto;

public record TransferPrecheckResponse(
        boolean allowed,
        String policyCode,
        long waitSeconds,
        String highAmountThreshold
) {}
