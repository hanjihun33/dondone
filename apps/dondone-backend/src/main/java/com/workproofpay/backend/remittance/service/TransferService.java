package com.workproofpay.backend.remittance.service;

import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.service.JobService;
import com.workproofpay.backend.remittance.api.dto.request.CreateTransferRequest;
import com.workproofpay.backend.remittance.api.dto.request.TransferPrecheckRequest;
import com.workproofpay.backend.remittance.api.dto.response.CreateTransferResponse;
import com.workproofpay.backend.remittance.api.dto.response.TransferDetailResponse;
import com.workproofpay.backend.remittance.api.dto.response.TransferListItemResponse;
import com.workproofpay.backend.remittance.api.dto.response.TransferListResponse;
import com.workproofpay.backend.remittance.api.dto.response.TransferPrecheckResponse;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.Recipient;
import com.workproofpay.backend.remittance.model.RemittancePolicyCode;
import com.workproofpay.backend.remittance.model.Transfer;
import com.workproofpay.backend.remittance.repo.RecipientRepository;
import com.workproofpay.backend.remittance.repo.TransferRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final RecipientRepository recipientRepository;
    private final RecipientService recipientService;
    private final WalletService walletService;
    private final RemittancePolicyService policyService;
    private final JobService jobService;
    private final RemittanceProperties properties;

    @Transactional
    public TransferPrecheckResponse precheck(Long userId, TransferPrecheckRequest request) {
        RemittancePolicyDecision decision = policyService.evaluate(
                userId,
                request.recipientId(),
                request.amountAtomic(),
                request.highAmountConfirmed(),
                request.recentRecipientConfirmed()
        );

        return new TransferPrecheckResponse(
                decision.allowed(),
                decision.policyCode() == null ? null : decision.policyCode().name(),
                properties.getPolicy().getAssetSymbol(),
                properties.getPolicy().getHighAmountThresholdAtomic(),
                decision.recentRecipientConfirmationRequired(),
                decision.recipient().getUpdatedAt(),
                decision.wallet().getWalletAddress(),
                decision.balanceSnapshot().tokenBalanceAtomic().toString(),
                decision.balanceSnapshot().nativeBalanceWei().toString()
        );
    }

    @Transactional
    public TransferCreateResult createTransfer(Long userId, String idempotencyKey, CreateTransferRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        walletService.getRequiredWalletForUpdate(userId);

        Transfer existing = transferRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).orElse(null);
        if (existing != null) {
            return replay(existing, request);
        }

        RemittancePolicyDecision decision = policyService.evaluate(
                userId,
                request.recipientId(),
                request.amountAtomic(),
                request.highAmountConfirmed(),
                request.recentRecipientConfirmed()
        );
        if (!decision.allowed()) {
            throw policyViolation(decision);
        }

        Transfer transfer = Transfer.request(
                generateTransferId(),
                userId,
                request.recipientId(),
                properties.getPolicy().getAssetSymbol(),
                request.amountAtomic(),
                decision.wallet().getWalletAddress(),
                decision.recipient().getWalletAddress(),
                idempotencyKey,
                request.highAmountConfirmed(),
                request.recentRecipientConfirmed()
        );
        try {
            transferRepository.saveAndFlush(transfer);
            jobService.enqueue(JobType.SUBMIT_TRANSFER, transfer.getTransferId(), LocalDateTime.now());
        } catch (DataIntegrityViolationException e) {
            Transfer conflicted = transferRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                    .orElseThrow(() -> e);
            return replay(conflicted, request);
        }

        return new TransferCreateResult(false, toCreateResponse(transfer));
    }

    @Transactional(readOnly = true)
    public TransferListResponse getTransfers(Long userId, Integer requestedLimit) {
        int limit = requestedLimit == null ? properties.getPolicy().getDefaultListLimit() : Math.min(requestedLimit, 100);
        var transfers = transferRepository.findByUserIdOrderByCreatedAtDescTransferIdDesc(userId, PageRequest.of(0, limit));
        if (transfers.isEmpty()) {
            return new TransferListResponse(java.util.List.of());
        }
        Map<String, Recipient> recipientsById = recipientRepository.findByUserIdAndRecipientIdIn(
                        userId,
                        transfers.stream().map(Transfer::getRecipientId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(Recipient::getRecipientId, Function.identity()));

        return new TransferListResponse(
                transfers.stream()
                        .map(transfer -> {
                            Recipient recipient = recipientsById.get(transfer.getRecipientId());
                            return new TransferListItemResponse(
                                    transfer.getTransferId(),
                                    transfer.getStatus().name(),
                                    transfer.getAssetSymbol(),
                                    transfer.getAmountAtomic(),
                                    transfer.getRecipientId(),
                                    recipient == null ? null : recipient.getAlias(),
                                    transfer.getRecipientAddress(),
                                    transfer.getTxHash(),
                                    transfer.getUpdatedAt()
                            );
                        })
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public TransferDetailResponse getTransfer(Long userId, String transferId) {
        Transfer transfer = transferRepository.findByTransferIdAndUserId(transferId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.TRANSFER_NOT_FOUND));
        Recipient recipient = recipientService.getRequiredRecipient(userId, transfer.getRecipientId());

        return new TransferDetailResponse(
                transfer.getTransferId(),
                transfer.getStatus().name(),
                transfer.getAssetSymbol(),
                transfer.getAmountAtomic(),
                transfer.getSenderAddress(),
                transfer.getRecipientId(),
                recipient.getAlias(),
                transfer.getRecipientAddress(),
                transfer.getTxHash(),
                transfer.getFailureCode() == null ? null : transfer.getFailureCode().name(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt()
        );
    }

    private CreateTransferResponse toCreateResponse(Transfer transfer) {
        return new CreateTransferResponse(
                transfer.getTransferId(),
                transfer.getStatus().name(),
                transfer.getAssetSymbol(),
                transfer.getAmountAtomic(),
                transfer.getRecipientId(),
                transfer.getCreatedAt()
        );
    }

    private TransferCreateResult replay(Transfer existing, CreateTransferRequest request) {
        if (!existing.matchesCreateRequest(
                request.recipientId(),
                request.amountAtomic(),
                request.highAmountConfirmed(),
                request.recentRecipientConfirmed()
        )) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD);
        }
        return new TransferCreateResult(true, toCreateResponse(existing));
    }

    private String generateTransferId() {
        return "tr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private ApiException policyViolation(RemittancePolicyDecision decision) {
        ErrorCode errorCode = decision.policyCode() == null ? ErrorCode.INVALID_REQUEST : switch (decision.policyCode()) {
            case RECIPIENT_NOT_ALLOWED -> ErrorCode.RECIPIENT_NOT_ALLOWED;
            case RECENT_RECIPIENT_CONFIRMATION_REQUIRED -> ErrorCode.RECENT_RECIPIENT_CONFIRMATION_REQUIRED;
            case HIGH_AMOUNT_CONFIRMATION_REQUIRED -> ErrorCode.HIGH_AMOUNT_CONFIRMATION_REQUIRED;
            case SELF_TRANSFER_NOT_ALLOWED -> ErrorCode.SELF_TRANSFER_NOT_ALLOWED;
            case INSUFFICIENT_WALLET_BALANCE -> ErrorCode.INSUFFICIENT_WALLET_BALANCE;
            case TRANSFER_ALREADY_IN_PROGRESS -> ErrorCode.TRANSFER_ALREADY_IN_PROGRESS;
        };
        return new ApiException(
                errorCode,
                errorCode.getMessage(),
                Map.of(
                        "policyCode", decision.policyCode() == null ? null : decision.policyCode().name(),
                        "walletAddress", decision.wallet().getWalletAddress(),
                        "currentTokenBalanceAtomic", decision.balanceSnapshot().tokenBalanceAtomic().toString(),
                        "currentNativeBalanceWei", decision.balanceSnapshot().nativeBalanceWei().toString(),
                        "recentRecipientUpdatedAt", decision.recipient().getUpdatedAt()
                )
        );
    }
}
