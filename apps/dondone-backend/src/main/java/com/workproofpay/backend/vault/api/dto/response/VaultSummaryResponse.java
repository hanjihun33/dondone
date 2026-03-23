package com.workproofpay.backend.vault.api.dto.response;

public record VaultSummaryResponse(
        String walletAddress,
        String vaultAddress,
        String network,
        String assetSymbol,
        int assetDecimals,
        String storedAmountAtomic,
        String accruedYieldAtomic,
        String walletTokenBalanceAtomic,
        String availableToStoreAmountAtomic,
        String shareBalance,
        VaultInterestPreviewResponse interestPreview,
        String disclaimer
) {
}
