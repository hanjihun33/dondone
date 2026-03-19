package com.workproofpay.backend.remittance.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "transfers",
        indexes = {
                @Index(name = "idx_transfers_user_created_at", columnList = "user_id, created_at DESC, transfer_id DESC"),
                @Index(name = "uk_transfers_user_idempotency", columnList = "user_id, idempotency_key", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer {

    @Id
    @Column(name = "transfer_id", nullable = false, length = 64)
    private String transferId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recipient_id", nullable = false, length = 64)
    private String recipientId;

    @Column(name = "asset_symbol", nullable = false, length = 20)
    private String assetSymbol;

    @Column(name = "amount_atomic", nullable = false)
    private Long amountAtomic;

    @Column(name = "sender_address", nullable = false, length = 42)
    private String senderAddress;

    @Column(name = "recipient_address", nullable = false, length = 42)
    private String recipientAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "high_amount_confirmed", nullable = false)
    private boolean highAmountConfirmed;

    @Column(name = "recent_recipient_confirmed", nullable = false)
    private boolean recentRecipientConfirmed;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "signed_transaction", columnDefinition = "TEXT")
    private String signedTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_code", length = 40)
    private TransferFailureCode failureCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Transfer(
            String transferId,
            Long userId,
            String recipientId,
            String assetSymbol,
            Long amountAtomic,
            String senderAddress,
            String recipientAddress,
            String idempotencyKey,
            boolean highAmountConfirmed,
            boolean recentRecipientConfirmed
    ) {
        this.transferId = transferId;
        this.userId = userId;
        this.recipientId = recipientId;
        this.assetSymbol = assetSymbol;
        this.amountAtomic = amountAtomic;
        this.senderAddress = senderAddress;
        this.recipientAddress = recipientAddress;
        this.status = TransferStatus.REQUESTED;
        this.idempotencyKey = idempotencyKey;
        this.highAmountConfirmed = highAmountConfirmed;
        this.recentRecipientConfirmed = recentRecipientConfirmed;
    }

    public static Transfer request(
            String transferId,
            Long userId,
            String recipientId,
            String assetSymbol,
            Long amountAtomic,
            String senderAddress,
            String recipientAddress,
            String idempotencyKey,
            boolean highAmountConfirmed,
            boolean recentRecipientConfirmed
    ) {
        return new Transfer(
                transferId,
                userId,
                recipientId,
                assetSymbol,
                amountAtomic,
                senderAddress,
                recipientAddress,
                idempotencyKey,
                highAmountConfirmed,
                recentRecipientConfirmed
        );
    }

    public boolean matchesCreateRequest(
            String recipientId,
            Long amountAtomic,
            boolean highAmountConfirmed,
            boolean recentRecipientConfirmed
    ) {
        return this.recipientId.equals(recipientId)
                && this.amountAtomic.equals(amountAtomic)
                && this.highAmountConfirmed == highAmountConfirmed
                && this.recentRecipientConfirmed == recentRecipientConfirmed;
    }

    public void markSigned(String txHash, String encryptedSignedTransaction) {
        requireStatus(TransferStatus.REQUESTED);
        this.status = TransferStatus.SIGNED;
        this.txHash = txHash;
        this.signedTransaction = encryptedSignedTransaction;
        this.failureCode = null;
    }

    public void markBroadcasted() {
        requireStatus(TransferStatus.SIGNED);
        this.status = TransferStatus.BROADCASTED;
        this.failureCode = null;
    }

    public void markConfirmed() {
        requireStatus(TransferStatus.BROADCASTED);
        this.status = TransferStatus.CONFIRMED;
        this.signedTransaction = null;
        this.failureCode = null;
    }

    public void markFailed(TransferFailureCode failureCode) {
        requireStatus(TransferStatus.REQUESTED, TransferStatus.SIGNED, TransferStatus.BROADCASTED);
        this.status = TransferStatus.FAILED;
        this.signedTransaction = null;
        this.failureCode = failureCode;
    }

    public void markTimedOut(TransferFailureCode failureCode) {
        requireStatus(TransferStatus.BROADCASTED);
        this.status = TransferStatus.TIMED_OUT;
        this.signedTransaction = null;
        this.failureCode = failureCode;
    }

    public void resetForRetry() {
        requireStatus(TransferStatus.FAILED, TransferStatus.TIMED_OUT);
        this.status = TransferStatus.REQUESTED;
        this.txHash = null;
        this.signedTransaction = null;
        this.failureCode = null;
    }

    public boolean isActive() {
        return status == TransferStatus.REQUESTED
                || status == TransferStatus.SIGNED
                || status == TransferStatus.BROADCASTED;
    }

    private void requireStatus(TransferStatus... expectedStatuses) {
        for (TransferStatus expectedStatus : expectedStatuses) {
            if (this.status == expectedStatus) {
                return;
            }
        }
        throw new IllegalStateException("Invalid transfer state transition from " + this.status);
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
