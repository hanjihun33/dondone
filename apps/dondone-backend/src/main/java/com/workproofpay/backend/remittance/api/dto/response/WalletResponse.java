package com.workproofpay.backend.remittance.api.dto.response;

import com.workproofpay.backend.remittance.model.WalletFundingStatus;

import java.time.LocalDateTime;

public record WalletResponse(
        Long userId,
        String walletAddress,
        WalletFundingStatus fundingStatus,
        String fundingFailureReason,
        LocalDateTime fundedAt,
        LocalDateTime createdAt
) {
}
