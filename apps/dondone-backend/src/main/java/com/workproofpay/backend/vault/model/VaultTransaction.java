package com.workproofpay.backend.vault.model;

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

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "vault_transactions",
        indexes = {
                @Index(name = "idx_vault_transactions_user_created_at", columnList = "user_id, created_at DESC, vault_transaction_id DESC"),
                @Index(name = "uk_vault_transactions_user_idempotency", columnList = "user_id, idempotency_key", unique = true),
                @Index(name = "idx_vault_transactions_status_updated_at", columnList = "status, updated_at DESC, vault_transaction_id DESC"),
                @Index(name = "idx_vault_transactions_user_status", columnList = "user_id, status"),
                @Index(name = "uk_vault_transactions_tx_hash", columnList = "tx_hash", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VaultTransaction {

    @Id
    @Column(name = "vault_transaction_id", nullable = false, length = 64)
    private String vaultTransactionId;

    @Column(name = "position_id", nullable = false)
    private Long positionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false, length = 20)
    private VaultTransactionType txType;

    @Column(name = "amount_atomic", nullable = false, precision = 38, scale = 0)
    private BigInteger amountAtomic;

    @Column(name = "wallet_address", nullable = false, length = 42)
    private String walletAddress;

    @Column(name = "vault_address", nullable = false, length = 128)
    private String vaultAddress;

    @Column(name = "asset_symbol", nullable = false, length = 32)
    private String assetSymbol;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VaultTransactionStatus status;

    @Column(name = "share_delta", precision = 38, scale = 0)
    private BigInteger shareDelta;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "signed_transaction", columnDefinition = "TEXT")
    private String signedTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_code", length = 40)
    private VaultFailureCode failureCode;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private VaultTransaction(
            String vaultTransactionId,
            Long positionId,
            Long userId,
            VaultTransactionType txType,
            BigInteger amountAtomic,
            String walletAddress,
            String vaultAddress,
            String assetSymbol,
            String idempotencyKey
    ) {
        this.vaultTransactionId = vaultTransactionId;
        this.positionId = positionId;
        this.userId = userId;
        this.txType = txType;
        this.amountAtomic = amountAtomic;
        this.walletAddress = walletAddress;
        this.vaultAddress = vaultAddress;
        this.assetSymbol = assetSymbol;
        this.idempotencyKey = idempotencyKey;
        this.status = VaultTransactionStatus.REQUESTED;
    }

    public static VaultTransaction request(
            String vaultTransactionId,
            Long positionId,
            Long userId,
            VaultTransactionType txType,
            BigInteger amountAtomic,
            String walletAddress,
            String vaultAddress,
            String assetSymbol,
            String idempotencyKey
    ) {
        return new VaultTransaction(
                vaultTransactionId,
                positionId,
                userId,
                txType,
                amountAtomic,
                walletAddress,
                vaultAddress,
                assetSymbol,
                idempotencyKey
        );
    }

    public boolean matchesReplay(VaultTransactionType txType, BigInteger amountAtomic) {
        return this.txType == txType && this.amountAtomic.compareTo(amountAtomic) == 0;
    }

    public void markSigned(String txHash, String signedTransaction, BigInteger shareDelta) {
        requireStatus(VaultTransactionStatus.REQUESTED);
        this.status = VaultTransactionStatus.SIGNED;
        this.txHash = txHash;
        this.signedTransaction = signedTransaction;
        this.shareDelta = shareDelta;
        this.failureCode = null;
    }

    public void markBroadcasted() {
        requireStatus(VaultTransactionStatus.SIGNED);
        this.status = VaultTransactionStatus.BROADCASTED;
        this.failureCode = null;
    }

    public void markConfirmed() {
        requireStatus(VaultTransactionStatus.BROADCASTED);
        this.status = VaultTransactionStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.signedTransaction = null;
        this.failureCode = null;
    }

    public void markFailed(VaultFailureCode failureCode) {
        requireStatus(VaultTransactionStatus.REQUESTED, VaultTransactionStatus.SIGNED, VaultTransactionStatus.BROADCASTED);
        this.status = VaultTransactionStatus.FAILED;
        this.signedTransaction = null;
        this.failureCode = failureCode;
    }

    public void markTimedOut(VaultFailureCode failureCode) {
        requireStatus(VaultTransactionStatus.BROADCASTED);
        this.status = VaultTransactionStatus.TIMED_OUT;
        this.signedTransaction = null;
        this.failureCode = failureCode;
    }

    private void requireStatus(VaultTransactionStatus... expectedStatuses) {
        for (VaultTransactionStatus expectedStatus : expectedStatuses) {
            if (this.status == expectedStatus) {
                return;
            }
        }
        throw new IllegalStateException("Invalid vault transaction state transition from " + status);
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
