package com.workproofpay.backend.vault.api.dto.response;

public record VaultInterestPreviewResponse(
        String dailyEstimatedYieldAtomic,
        String monthlyEstimatedYieldAtomic,
        String yearlyEstimatedYieldAtomic,
        int apyBps
) {
}
