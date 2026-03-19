package com.workproofpay.backend.remittance.api.dto.response;

import java.time.LocalDateTime;

public record TransferPrecheckResponse(
        boolean allowed,
        String policyCode,
        String assetSymbol,
        long highAmountThresholdAtomic,
        boolean recentRecipientConfirmationRequired,
        LocalDateTime recentRecipientUpdatedAt,
        String walletAddress,
        String currentTokenBalanceAtomic,
        String currentNativeBalanceWei
) {
}
