package com.workproofpay.remittance.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "transfers", schema = "test", indexes = {
        @Index(name = "idx_transfers_user_idem", columnList = "user_id,idempotency_key", unique = true)
})
public class TransferEntity {
    @Id
    @Column(name = "transfer_id", nullable = false, length = 64)
    private String transferId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recipient_id", nullable = false, length = 64)
    private String recipientId;

    @Column(name = "asset", nullable = false, length = 32)
    private String asset;

    @Column(name = "amount_atomic", nullable = false)
    private long amountAtomic;

    @Column(name = "sender_address", nullable = false, length = 128)
    private String senderAddress;

    @Column(name = "recipient_address", nullable = false, length = 128)
    private String recipientAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private TransferStatus status;

    @Column(name = "tx_hash", length = 128)
    private String txHash;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "high_amount_confirmed", nullable = false)
    private boolean highAmountConfirmed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public long getAmountAtomic() {
        return amountAtomic;
    }

    public void setAmountAtomic(long amountAtomic) {
        this.amountAtomic = amountAtomic;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public String getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public boolean isHighAmountConfirmed() {
        return highAmountConfirmed;
    }

    public void setHighAmountConfirmed(boolean highAmountConfirmed) {
        this.highAmountConfirmed = highAmountConfirmed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
