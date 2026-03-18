package com.workproofpay.remittance.service;

public record WalletCreatedEvent(
        Long userId,
        String walletAddress
) {
}
