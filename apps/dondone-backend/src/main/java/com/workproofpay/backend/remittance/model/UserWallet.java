package com.workproofpay.backend.remittance.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_wallets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserWallet {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "wallet_address", nullable = false, unique = true, length = 42)
    private String walletAddress;

    @Column(name = "encrypted_private_key", nullable = false, length = 1024)
    private String encryptedPrivateKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "funding_status", nullable = false, length = 20)
    private WalletFundingStatus fundingStatus;

    @Column(name = "funding_failure_reason", length = 300)
    private String fundingFailureReason;

    @Column(name = "funded_at")
    private LocalDateTime fundedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private UserWallet(Long userId, String walletAddress, String encryptedPrivateKey) {
        this.userId = userId;
        this.walletAddress = walletAddress;
        this.encryptedPrivateKey = encryptedPrivateKey;
        this.fundingStatus = WalletFundingStatus.PENDING;
    }

    public static UserWallet create(Long userId, String walletAddress, String encryptedPrivateKey) {
        return new UserWallet(userId, walletAddress, encryptedPrivateKey);
    }

    public void markFundingPending() {
        this.fundingStatus = WalletFundingStatus.PENDING;
        this.fundingFailureReason = null;
        this.fundedAt = null;
    }

    public void markFunded() {
        this.fundingStatus = WalletFundingStatus.FUNDED;
        this.fundingFailureReason = null;
        this.fundedAt = LocalDateTime.now();
    }

    public void markFundingFailed(String failureReason) {
        this.fundingStatus = WalletFundingStatus.FAILED;
        this.fundingFailureReason = failureReason;
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
