package com.workproofpay.backend.remittance.api.dto.response;

public record WalletBalanceResponse(
        String walletAddress,
        String assetSymbol,
        int assetDecimals,
        String tokenBalanceAtomic,
        String nativeBalanceWei
) {
}
