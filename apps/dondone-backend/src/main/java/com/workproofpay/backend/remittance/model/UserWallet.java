package com.workproofpay.backend.remittance.model;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_wallets",
        indexes = {
                @Index(name = "idx_user_wallets_funding_status_updated_at", columnList = "funding_status, updated_at DESC, user_id DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserWallet implements Persistable<Long> {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_user_wallets_user"))
    private User user;

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

    @Transient
    private boolean isNew = true;

    private UserWallet(Long userId, String walletAddress, String encryptedPrivateKey) {
        this.userId = userId;
        this.walletAddress = walletAddress;
        this.encryptedPrivateKey = encryptedPrivateKey;
        this.fundingStatus = WalletFundingStatus.PENDING;
    }

    public static UserWallet create(Long userId, String walletAddress, String encryptedPrivateKey) {
        return new UserWallet(userId, walletAddress, encryptedPrivateKey);
    }

    @Override
    public Long getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNew;
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

    @PostPersist
    @PostLoad
    public void markNotNew() {
        this.isNew = false;
    }
}
