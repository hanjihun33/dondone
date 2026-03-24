package com.workproofpay.backend.advance.model;

import com.workproofpay.backend.auth.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "advance_payouts",
        indexes = {
                @Index(name = "idx_advance_payouts_user_created_at", columnList = "user_id, created_at DESC, advance_payout_id DESC"),
                @Index(name = "uk_advance_payouts_advance_request", columnList = "advance_request_id", unique = true),
                @Index(name = "uk_advance_payouts_user_idempotency", columnList = "user_id, idempotency_key", unique = true),
                @Index(name = "idx_advance_payouts_status_updated_at", columnList = "status, updated_at DESC, advance_payout_id DESC"),
                @Index(name = "idx_advance_payouts_user_status", columnList = "user_id, status"),
                @Index(name = "uk_advance_payouts_tx_hash", columnList = "tx_hash", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdvancePayout {

    @Id
    @Column(name = "advance_payout_id", nullable = false, length = 64)
    private String advancePayoutId;

    @Column(name = "advance_request_id", nullable = false)
    private Long advanceRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "advance_request_id",
            nullable = false,
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_advance_payouts_advance_request")
    )
    private AdvanceRequest advanceRequest;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_advance_payouts_user")
    )
    private User user;

    @Column(name = "wallet_address", nullable = false, length = 42)
    private String walletAddress;

    @Column(name = "amount_atomic", nullable = false)
    private Long amountAtomic;

    @Column(name = "asset_symbol", nullable = false, length = 32)
    private String assetSymbol;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AdvancePayoutStatus status;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private AdvancePayout(
            String advancePayoutId,
            Long advanceRequestId,
            Long userId,
            String walletAddress,
            Long amountAtomic,
            String assetSymbol,
            String idempotencyKey
    ) {
        this.advancePayoutId = advancePayoutId;
        this.advanceRequestId = advanceRequestId;
        this.userId = userId;
        this.walletAddress = walletAddress;
        this.amountAtomic = amountAtomic;
        this.assetSymbol = assetSymbol;
        this.idempotencyKey = idempotencyKey;
        this.status = AdvancePayoutStatus.REQUESTED;
    }

    public static AdvancePayout request(
            String advancePayoutId,
            Long advanceRequestId,
            Long userId,
            String walletAddress,
            Long amountAtomic,
            String assetSymbol,
            String idempotencyKey
    ) {
        return new AdvancePayout(
                advancePayoutId,
                advanceRequestId,
                userId,
                walletAddress,
                amountAtomic,
                assetSymbol,
                idempotencyKey
        );
    }

    public boolean matchesReplay(Long advanceRequestId, String walletAddress, Long amountAtomic, String assetSymbol) {
        return this.advanceRequestId.equals(advanceRequestId)
                && this.walletAddress.equals(walletAddress)
                && this.amountAtomic.equals(amountAtomic)
                && this.assetSymbol.equals(assetSymbol);
    }

    public void markSigned(String txHash) {
        requireStatus(AdvancePayoutStatus.REQUESTED);
        this.status = AdvancePayoutStatus.SIGNED;
        this.txHash = txHash;
        this.failureReason = null;
    }

    public void markBroadcasted() {
        requireStatus(AdvancePayoutStatus.SIGNED);
        this.status = AdvancePayoutStatus.BROADCASTED;
        this.failureReason = null;
    }

    public void markConfirmed() {
        requireStatus(AdvancePayoutStatus.BROADCASTED);
        this.status = AdvancePayoutStatus.CONFIRMED;
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        requireStatus(AdvancePayoutStatus.REQUESTED, AdvancePayoutStatus.SIGNED, AdvancePayoutStatus.BROADCASTED);
        this.status = AdvancePayoutStatus.FAILED;
        this.failureReason = sanitizeFailureReason(failureReason);
    }

    public void markTimedOut(String failureReason) {
        requireStatus(AdvancePayoutStatus.BROADCASTED);
        this.status = AdvancePayoutStatus.TIMED_OUT;
        this.failureReason = sanitizeFailureReason(failureReason);
    }

    public void resetForRetry() {
        requireStatus(AdvancePayoutStatus.FAILED, AdvancePayoutStatus.TIMED_OUT);
        this.status = AdvancePayoutStatus.REQUESTED;
        this.txHash = null;
        this.failureReason = null;
    }

    public boolean isActive() {
        return status == AdvancePayoutStatus.REQUESTED
                || status == AdvancePayoutStatus.SIGNED
                || status == AdvancePayoutStatus.BROADCASTED;
    }

    private void requireStatus(AdvancePayoutStatus... expectedStatuses) {
        for (AdvancePayoutStatus expectedStatus : expectedStatuses) {
            if (this.status == expectedStatus) {
                return;
            }
        }
        throw new IllegalStateException("Invalid advance payout state transition from " + status);
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

    private String sanitizeFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return null;
        }
        String normalized = failureReason.replaceAll("\\s+", " ").trim();
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }
}
