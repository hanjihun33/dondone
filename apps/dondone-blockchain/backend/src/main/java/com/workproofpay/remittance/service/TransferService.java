package com.workproofpay.remittance.service;

import com.workproofpay.remittance.config.AppProperties;
import com.workproofpay.remittance.domain.*;
import com.workproofpay.remittance.dto.*;
import com.workproofpay.remittance.repo.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class TransferService {
    private final TransferRepository transferRepository;
    private final RecipientService recipientService;
    private final JobService jobService;
    private final AppProperties appProperties;
    private final UserWalletService userWalletService;

    public TransferService(
            TransferRepository transferRepository,
            RecipientService recipientService,
            JobService jobService,
            AppProperties appProperties,
            UserWalletService userWalletService
    ) {
        this.transferRepository = transferRepository;
        this.recipientService = recipientService;
        this.jobService = jobService;
        this.appProperties = appProperties;
        this.userWalletService = userWalletService;
    }

    @Transactional(readOnly = true)
    public TransferPrecheckResponse precheck(String userId, TransferPrecheckRequest request) {
        PolicyDecision policy = evaluatePolicy(userId, request.recipientId(), request.amount(), request.highAmountConfirmed());
        return new TransferPrecheckResponse(
                policy.allowed,
                policy.policyCode,
                policy.waitSeconds,
                String.valueOf(appProperties.getRemittance().getHighAmountThreshold())
        );
    }

    @Transactional
    public CreateTransferResponse createTransfer(
            String userId,
            String senderAddress,
            String idempotencyKey,
            CreateTransferRequest request
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(400, "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key is required", null);
        }

        TransferEntity existing = transferRepository
                .findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            return toCreateResponse(existing, null, null);
        }

        PolicyDecision policy = evaluatePolicy(userId, request.recipientId(), request.amount(), request.highAmountConfirmed());
        if (!policy.allowed) {
            throw new ApiException(
                    409,
                    "POLICY_BLOCKED",
                    "Transfer is blocked by policy",
                    Map.of(
                            "policyCode", policy.policyCode,
                            "waitSeconds", policy.waitSeconds
                    )
            );
        }

        RecipientEntity recipient = recipientService.getRequiredRecipient(userId, request.recipientId());
        var wallet = userWalletService.createWalletIfAbsent(userId);
        Instant now = Instant.now();

        TransferEntity transfer = new TransferEntity();
        transfer.setTransferId(generateTransferId());
        transfer.setUserId(userId);
        transfer.setRecipientId(request.recipientId());
        transfer.setAsset(request.asset());
        transfer.setAmountAtomic(request.amount());
        transfer.setSenderAddress(wallet.walletAddress());
        transfer.setRecipientAddress(recipient.getWalletAddress());
        transfer.setStatus(TransferStatus.REQUESTED);
        transfer.setTxHash(null);
        transfer.setFailureCode(null);
        transfer.setIdempotencyKey(idempotencyKey);
        transfer.setHighAmountConfirmed(request.highAmountConfirmed());
        transfer.setCreatedAt(now);
        transfer.setUpdatedAt(now);

        transferRepository.save(transfer);
        jobService.enqueue(transfer.getTransferId(), JobType.SUBMIT_TRANSFER, now);

        return toCreateResponse(transfer, null, null);
    }

    @Transactional(readOnly = true)
    public TransferDetailResponse getTransfer(String userId, String transferId) {
        TransferEntity transfer = transferRepository.findByTransferIdAndUserId(transferId, userId)
                .orElseThrow(() -> new ApiException(404, "TRANSFER_NOT_FOUND", "Transfer not found", null));
        return toDetailResponse(transfer);
    }

    @Transactional(readOnly = true)
    public ReceiptLinkResponse issueReceiptLink(String userId, String transferId) {
        TransferEntity transfer = transferRepository.findByTransferIdAndUserId(transferId, userId)
                .orElseThrow(() -> new ApiException(404, "TRANSFER_NOT_FOUND", "Transfer not found", null));

        Instant expiresAt = Instant.now().plusSeconds(appProperties.getRemittance().getReceiptLinkTtlSeconds());
        String token = UUID.randomUUID().toString().replace("-", "");
        String url = "/api/v1/remittance/transfers/" + transfer.getTransferId() + "/receipt.pdf?token=" + token;
        return new ReceiptLinkResponse(transfer.getTransferId(), url, expiresAt);
    }

    @Transactional(readOnly = true)
    public String renderReceiptText(String userId, String transferId) {
        TransferEntity transfer = transferRepository.findByTransferIdAndUserId(transferId, userId)
                .orElseThrow(() -> new ApiException(404, "TRANSFER_NOT_FOUND", "Transfer not found", null));
        return "TRANSFER RECEIPT\n"
                + "transferId=" + transfer.getTransferId() + "\n"
                + "status=" + transfer.getStatus() + "\n"
                + "asset=" + transfer.getAsset() + "\n"
                + "amount=" + transfer.getAmountAtomic() + "\n"
                + "sender=" + transfer.getSenderAddress() + "\n"
                + "recipient=" + transfer.getRecipientAddress() + "\n"
                + "txHash=" + transfer.getTxHash() + "\n"
                + "updatedAt=" + transfer.getUpdatedAt() + "\n";
    }

    private PolicyDecision evaluatePolicy(String userId, String recipientId, long amount, boolean highAmountConfirmed) {
        RecipientEntity recipient = recipientService.getRequiredRecipient(userId, recipientId);
        if (!recipient.isAllowed()) {
            return new PolicyDecision(false, PolicyCode.RECIPIENT_NOT_ALLOWED.name(), 0);
        }

        Instant cooldownEndsAt = recipient.getUpdatedAt().plusSeconds(appProperties.getRemittance().getCooldownSeconds());
        Instant now = Instant.now();
        if (now.isBefore(cooldownEndsAt)) {
            long waitSeconds = Math.max(0, cooldownEndsAt.getEpochSecond() - now.getEpochSecond());
            return new PolicyDecision(false, PolicyCode.COOLDOWN_ACTIVE.name(), waitSeconds);
        }

        if (amount > appProperties.getRemittance().getHighAmountThreshold() && !highAmountConfirmed) {
            return new PolicyDecision(false, PolicyCode.HIGH_AMOUNT_CONFIRM_REQUIRED.name(), 0);
        }

        return new PolicyDecision(true, null, 0);
    }

    private CreateTransferResponse toCreateResponse(TransferEntity t, String policyCode, Instant cooldownEndsAt) {
        return new CreateTransferResponse(
                t.getTransferId(),
                t.getStatus().name(),
                new PolicyResponse(policyCode, cooldownEndsAt),
                t.getCreatedAt()
        );
    }

    private TransferDetailResponse toDetailResponse(TransferEntity t) {
        return new TransferDetailResponse(
                t.getTransferId(),
                t.getStatus().name(),
                t.getAsset(),
                String.valueOf(t.getAmountAtomic()),
                t.getSenderAddress(),
                t.getRecipientAddress(),
                t.getTxHash(),
                t.getFailureCode(),
                t.getUpdatedAt()
        );
    }

    private String generateTransferId() {
        return "tr_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private record PolicyDecision(boolean allowed, String policyCode, long waitSeconds) {}
}
