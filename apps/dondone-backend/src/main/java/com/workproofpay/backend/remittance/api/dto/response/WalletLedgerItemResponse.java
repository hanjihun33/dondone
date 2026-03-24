package com.workproofpay.backend.remittance.api.dto.response;

import java.time.LocalDateTime;

public record WalletLedgerItemResponse(
        String entryId,
        WalletLedgerEntryType entryType,
        WalletLedgerDirection direction,
        String status,
        String assetSymbol,
        Long amountAtomic,
        String txHash,
        LocalDateTime occurredAt,
        String counterpartyLabel,
        String memo
) {
}
