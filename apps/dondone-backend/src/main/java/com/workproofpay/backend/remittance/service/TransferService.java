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
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
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

    private final TransferRepository transferRepository;
    private final WalletService walletService;
    private final RemittancePolicyService policyService;
    private final JobService jobService;
    private final RemittanceProperties properties;
    private final RemittanceMetrics remittanceMetrics;

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
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "error";
        boolean replayed = false;

        try {
            validateIdempotencyKey(idempotencyKey);
            walletService.getRequiredWalletForUpdate(userId);

            Transfer existing = findExistingTransfer(userId, idempotencyKey);
            if (existing != null) {
                replayed = true;
                outcome = "replayed";
                return replay(existing, request);
            }

            RemittancePolicyDecision decision = evaluateCreateDecision(userId, request);
            if (!decision.allowed()) {
                outcome = "blocked";
                throw policyViolation(decision);
            }

            Transfer transfer = buildRequestedTransfer(userId, idempotencyKey, request, decision);
            saveTransferAndEnqueue(transfer);
            outcome = "created";
            return new TransferCreateResult(false, toCreateResponse(transfer));
        } catch (DataIntegrityViolationException e) {
            Transfer conflicted = findExistingTransfer(userId, idempotencyKey);
            if (conflicted == null) {
                throw e;
            }
            replayed = true;
            outcome = "replayed_after_conflict";
            return replay(conflicted, request);
        } catch (ApiException e) {
            if (e.getErrorCode() == ErrorCode.IDEMPOTENCY_KEY_REQUIRED) {
                outcome = "invalid_idempotency_key";
            }
            throw e;
        } finally {
            remittanceMetrics.recordTransferCreate(sample, outcome, replayed);
        }
    }

    @Transactional(readOnly = true)
    public TransferListResponse getTransfers(Long userId, Integer requestedLimit) {
        int limit = requestedLimit == null ? properties.getPolicy().getDefaultListLimit() : Math.min(requestedLimit, 100);
        var transfers = transferRepository.findByUserIdOrRecipientTargetUserIdSnapshotOrderByCreatedAtDescTransferIdDesc(
                userId,
                userId,
                PageRequest.of(0, limit)
        );
        if (transfers.isEmpty()) {
            return new TransferListResponse(java.util.List.of());
        }
        return new TransferListResponse(
                transfers.stream()
                        .map(transfer -> RemittanceReadModelMapper.toTransferListItemResponse(transfer, userId))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public TransferDetailResponse getTransfer(Long userId, String transferId) {
        Transfer transfer = transferRepository.findAccessibleTransferByTransferId(transferId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.TRANSFER_NOT_FOUND));

        return RemittanceReadModelMapper.toTransferDetailResponse(transfer, userId);
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

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
    }

    private Transfer findExistingTransfer(Long userId, String idempotencyKey) {
        return transferRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).orElse(null);
    }

    private RemittancePolicyDecision evaluateCreateDecision(Long userId, CreateTransferRequest request) {
        return policyService.evaluate(
                userId,
                request.recipientId(),
                request.amountAtomic(),
                request.highAmountConfirmed(),
                request.recentRecipientConfirmed()
        );
    }

    private Transfer buildRequestedTransfer(
            Long userId,
            String idempotencyKey,
            CreateTransferRequest request,
            RemittancePolicyDecision decision
    ) {
        return Transfer.request(
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
    }

    private void saveTransferAndEnqueue(Transfer transfer) {
        transferRepository.saveAndFlush(transfer);
        jobService.enqueue(JobReferenceKind.TRANSFER, JobType.SUBMIT_TRANSFER, transfer.getTransferId(), LocalDateTime.now());
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
