package com.workproofpay.backend.remittance.service;

import com.workproofpay.backend.advance.model.AdvancePayout;
import com.workproofpay.backend.advance.model.AdvancePayoutStatus;
import com.workproofpay.backend.advance.repo.AdvancePayoutRepository;
import com.workproofpay.backend.remittance.api.dto.response.WalletLedgerDirection;
import com.workproofpay.backend.remittance.api.dto.response.WalletLedgerEntryType;
import com.workproofpay.backend.remittance.api.dto.response.WalletLedgerItemResponse;
import com.workproofpay.backend.remittance.api.dto.response.WalletLedgerResponse;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.Transfer;
import com.workproofpay.backend.remittance.repo.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WalletLedgerService {

    private static final int MAX_LIMIT = 100;
    private static final String ADVANCE_PAYOUT_LABEL = "미리받기 지급";

    private final TransferRepository transferRepository;
    private final AdvancePayoutRepository advancePayoutRepository;
    private final RemittanceProperties properties;

    @Transactional(readOnly = true)
    public WalletLedgerResponse getLedger(Long userId, Integer requestedLimit) {
        int limit = resolveLimit(requestedLimit);
        PageRequest pageable = PageRequest.of(0, limit);

        List<WalletLedgerItemResponse> transferEntries = transferRepository
                .findByUserIdOrRecipientTargetUserIdSnapshotOrderByCreatedAtDescTransferIdDesc(userId, userId, pageable)
                .stream()
                .map(transfer -> toTransferEntry(transfer, userId))
                .toList();

        List<WalletLedgerItemResponse> advancePayoutEntries = advancePayoutRepository
                .findByUserIdAndStatusOrderByUpdatedAtDescAdvancePayoutIdDesc(userId, AdvancePayoutStatus.CONFIRMED, pageable)
                .stream()
                .map(this::toAdvancePayoutEntry)
                .toList();

        List<WalletLedgerItemResponse> entries = java.util.stream.Stream.concat(
                        transferEntries.stream(),
                        advancePayoutEntries.stream()
                )
                .sorted(Comparator
                        .comparing(WalletLedgerItemResponse::occurredAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(WalletLedgerItemResponse::entryId, Comparator.reverseOrder()))
                .limit(limit)
                .toList();

        return new WalletLedgerResponse(entries);
    }

    private int resolveLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return properties.getPolicy().getDefaultListLimit();
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private WalletLedgerItemResponse toTransferEntry(Transfer transfer, Long userId) {
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

    private WalletLedgerItemResponse toAdvancePayoutEntry(AdvancePayout payout) {
        return new WalletLedgerItemResponse(
                payout.getAdvancePayoutId(),
                WalletLedgerEntryType.ADVANCE_PAYOUT,
                WalletLedgerDirection.INBOUND,
                payout.getStatus().name(),
                payout.getAssetSymbol(),
                payout.getAmountAtomic(),
                payout.getTxHash(),
                occurredAt(payout.getUpdatedAt(), payout.getCreatedAt()),
                ADVANCE_PAYOUT_LABEL,
                formatAdvanceMemo(payout)
        );
    }

    private LocalDateTime occurredAt(LocalDateTime updatedAt, LocalDateTime createdAt) {
        return updatedAt != null ? updatedAt : createdAt;
    }

    private String formatAdvanceMemo(AdvancePayout payout) {
        if (payout.getAdvanceRequest() == null || payout.getAdvanceRequest().getApprovedDisplayKrwAmount() == null) {
            return null;
        }
        return "약 ₩" + NumberFormat.getNumberInstance(Locale.KOREA)
                .format(payout.getAdvanceRequest().getApprovedDisplayKrwAmount()) + " 상당";
    }

    private String defaultIfBlank(String preferred, String fallback) {
        if (preferred == null || preferred.isBlank()) {
            return fallback;
        }
        return preferred;
    }
}
