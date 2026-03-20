package com.workproofpay.backend.remittance.service;

import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.model.JobReferenceKind;
import com.workproofpay.backend.jobs.service.JobService;
import com.workproofpay.backend.remittance.api.dto.request.CreateTransferRequest;
import com.workproofpay.backend.remittance.api.dto.request.TransferPrecheckRequest;
import com.workproofpay.backend.remittance.api.dto.response.CreateTransferResponse;
import com.workproofpay.backend.remittance.api.dto.response.TransferDetailResponse;
import com.workproofpay.backend.remittance.api.dto.response.TransferListItemResponse;
import com.workproofpay.backend.remittance.api.dto.response.TransferListResponse;
import com.workproofpay.backend.remittance.api.dto.response.TransferPrecheckResponse;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.RemittancePolicyCode;
import com.workproofpay.backend.remittance.model.Transfer;
import com.workproofpay.backend.remittance.repo.TransferRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transferRepository;
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
        long startNs = System.nanoTime();
        long walletLockMs = 0L;
        long idempotencyLookupMs = 0L;
        long policyMs = 0L;
        long saveMs = 0L;
        long enqueueMs = 0L;
        String outcome = "ERROR";
        String transferId = null;
        boolean replayed = false;

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            outcome = "FAILED_IDEMPOTENCY_KEY_REQUIRED";
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        try {
            long stepStartNs = System.nanoTime();
            walletService.getRequiredWalletForUpdate(userId);
            walletLockMs = elapsedMillis(stepStartNs);

            stepStartNs = System.nanoTime();
            Transfer existing = transferRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).orElse(null);
            idempotencyLookupMs = elapsedMillis(stepStartNs);
            if (existing != null) {
                TransferCreateResult replayResult = replay(existing, request);
                transferId = existing.getTransferId();
                replayed = true;
                outcome = "REPLAYED";
                return replayResult;
            }

            stepStartNs = System.nanoTime();
            RemittancePolicyDecision decision = policyService.evaluate(
                    userId,
                    request.recipientId(),
                    request.amountAtomic(),
                    request.highAmountConfirmed(),
                    request.recentRecipientConfirmed()
            );
            policyMs = elapsedMillis(stepStartNs);
            if (!decision.allowed()) {
                outcome = "BLOCKED_" + decision.policyCode().name();
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
                    decision.recipient().getAlias(),
                    decision.recipient().getRelation(),
                    decision.recipient().getTargetUserId(),
                    idempotencyKey,
                    request.highAmountConfirmed(),
                    request.recentRecipientConfirmed()
            );

            stepStartNs = System.nanoTime();
            transferRepository.saveAndFlush(transfer);
            saveMs = elapsedMillis(stepStartNs);

            stepStartNs = System.nanoTime();
            jobService.enqueue(JobReferenceKind.TRANSFER, JobType.SUBMIT_TRANSFER, transfer.getTransferId(), LocalDateTime.now());
            enqueueMs = elapsedMillis(stepStartNs);
            transferId = transfer.getTransferId();
            outcome = "CREATED";
            return new TransferCreateResult(false, toCreateResponse(transfer));
        } catch (DataIntegrityViolationException e) {
            Transfer conflicted = transferRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                    .orElseThrow(() -> e);
            TransferCreateResult replayResult = replay(conflicted, request);
            transferId = conflicted.getTransferId();
            replayed = true;
            outcome = "REPLAY_AFTER_CONFLICT";
            return replayResult;
        } finally {
            logCreateTransferPerf(
                    outcome,
                    userId,
                    request.recipientId(),
                    request.amountAtomic(),
                    transferId,
                    replayed,
                    walletLockMs,
                    idempotencyLookupMs,
                    policyMs,
                    saveMs,
                    enqueueMs,
                    elapsedMillis(startNs)
            );
        }
    }

    @Transactional(readOnly = true)
    public TransferListResponse getTransfers(Long userId, Integer requestedLimit) {
        int limit = requestedLimit == null ? properties.getPolicy().getDefaultListLimit() : Math.min(requestedLimit, 100);
        var transfers = transferRepository.findByUserIdOrderByCreatedAtDescTransferIdDesc(userId, PageRequest.of(0, limit));
        if (transfers.isEmpty()) {
            return new TransferListResponse(java.util.List.of());
        }
        return new TransferListResponse(
                transfers.stream()
                        .map(transfer -> new TransferListItemResponse(
                                transfer.getTransferId(),
                                transfer.getStatus().name(),
                                transfer.getAssetSymbol(),
                                transfer.getAmountAtomic(),
                                transfer.getRecipientId(),
                                transfer.getRecipientAliasSnapshot(),
                                transfer.getRecipientAddress(),
                                transfer.getTxHash(),
                                transfer.getUpdatedAt()
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public TransferDetailResponse getTransfer(Long userId, String transferId) {
        Transfer transfer = transferRepository.findByTransferIdAndUserId(transferId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.TRANSFER_NOT_FOUND));

        return new TransferDetailResponse(
                transfer.getTransferId(),
                transfer.getStatus().name(),
                transfer.getAssetSymbol(),
                transfer.getAmountAtomic(),
                transfer.getSenderAddress(),
                transfer.getRecipientId(),
                transfer.getRecipientAliasSnapshot(),
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

    private void logCreateTransferPerf(
            String outcome,
            Long userId,
            String recipientId,
            long amountAtomic,
            String transferId,
            boolean replayed,
            long walletLockMs,
            long idempotencyLookupMs,
            long policyMs,
            long saveMs,
            long enqueueMs,
            long totalMs
    ) {
        if (!properties.getObservability().isPerfLogEnabled()) {
            return;
        }
        log.info(
                "remittance_perf event=create_transfer outcome={} replayed={} user_id={} recipient_id={} amount_atomic={} transfer_id={} total_ms={} wallet_lock_ms={} idempotency_lookup_ms={} policy_ms={} save_ms={} enqueue_ms={}",
                outcome,
                replayed,
                userId,
                recipientId,
                amountAtomic,
                transferId,
                totalMs,
                walletLockMs,
                idempotencyLookupMs,
                policyMs,
                saveMs,
                enqueueMs
        );
    }

    private long elapsedMillis(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }
}
