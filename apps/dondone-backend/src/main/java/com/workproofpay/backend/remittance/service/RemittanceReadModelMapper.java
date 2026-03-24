package com.workproofpay.backend.remittance.service;

import com.workproofpay.backend.remittance.api.dto.response.TransferDetailResponse;
import com.workproofpay.backend.remittance.api.dto.response.TransferListItemResponse;
import com.workproofpay.backend.remittance.api.dto.response.WalletLedgerDirection;
import com.workproofpay.backend.remittance.api.dto.response.WalletLedgerEntryType;
import com.workproofpay.backend.remittance.api.dto.response.WalletLedgerItemResponse;
import com.workproofpay.backend.remittance.model.Transfer;

import java.time.LocalDateTime;

final class RemittanceReadModelMapper {

    private static final String NETWORK_FEE_ASSET_SYMBOL = "ETH";

    private RemittanceReadModelMapper() {
    }

    static TransferListItemResponse toTransferListItemResponse(Transfer transfer, Long userId) {
        return new TransferListItemResponse(
                transfer.getTransferId(),
                resolveTransferDirection(transfer, userId),
                transfer.getStatus().name(),
                transfer.getAssetSymbol(),
                transfer.getAmountAtomic(),
                transfer.getSenderAddress(),
                transfer.getUser().getName(),
                transfer.getRecipientId(),
                transfer.getRecipientAliasSnapshot(),
                transfer.getRecipientAddress(),
                transfer.getTxHash(),
                transfer.getNetworkFeeWei(),
                NETWORK_FEE_ASSET_SYMBOL,
                transfer.getUpdatedAt()
        );
    }

    static TransferDetailResponse toTransferDetailResponse(Transfer transfer, Long userId) {
        return new TransferDetailResponse(
                transfer.getTransferId(),
                resolveTransferDirection(transfer, userId),
                transfer.getStatus().name(),
                transfer.getAssetSymbol(),
                transfer.getAmountAtomic(),
                transfer.getSenderAddress(),
                transfer.getUser().getName(),
                transfer.getRecipientId(),
                transfer.getRecipientAliasSnapshot(),
                transfer.getRecipientAddress(),
                transfer.getTxHash(),
                transfer.getNetworkFeeWei(),
                NETWORK_FEE_ASSET_SYMBOL,
                transfer.getFailureCode() == null ? null : transfer.getFailureCode().name(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt()
        );
    }

    static WalletLedgerItemResponse toLedgerTransferEntry(Transfer transfer, Long userId) {
        boolean outbound = userId.equals(transfer.getUserId());
        return new WalletLedgerItemResponse(
                transfer.getTransferId(),
                WalletLedgerEntryType.REMITTANCE_TRANSFER,
                outbound ? WalletLedgerDirection.OUTBOUND : WalletLedgerDirection.INBOUND,
                transfer.getStatus().name(),
                transfer.getAssetSymbol(),
                transfer.getAmountAtomic(),
                transfer.getTxHash(),
                occurredAt(transfer.getUpdatedAt(), transfer.getCreatedAt()),
                outbound ? transfer.getRecipientAliasSnapshot() : defaultIfBlank(transfer.getUser().getName(), transfer.getSenderAddress()),
                null
        );
    }

    private static String resolveTransferDirection(Transfer transfer, Long userId) {
        return userId.equals(transfer.getUserId()) ? "EXPENSE" : "INCOME";
    }

    private static LocalDateTime occurredAt(LocalDateTime updatedAt, LocalDateTime createdAt) {
        return updatedAt != null ? updatedAt : createdAt;
    }

    private static String defaultIfBlank(String preferred, String fallback) {
        if (preferred == null || preferred.isBlank()) {
            return fallback;
        }
        return preferred;
    }
}
