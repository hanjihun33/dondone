package com.workproofpay.backend.vault.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
        name = "vault_positions",
        indexes = {
                @Index(name = "uk_vault_positions_user", columnList = "user_id", unique = true),
                @Index(name = "idx_vault_positions_status_updated_at", columnList = "status, updated_at DESC, id DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VaultPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "wallet_address", nullable = false, length = 42)
    private String walletAddress;

    @Column(name = "asset_symbol", nullable = false, length = 32)
    private String assetSymbol;

    @Column(name = "asset_address", nullable = false, length = 128)
    private String assetAddress;

    @Column(name = "vault_address", nullable = false, length = 128)
    private String vaultAddress;

    @Column(name = "network", nullable = false, length = 32)
    private String network;

    @Column(name = "share_balance", nullable = false, precision = 38, scale = 0)
    private BigInteger shareBalance;

    @Column(name = "principal_amount_atomic", nullable = false, precision = 38, scale = 0)
    private BigInteger principalAmountAtomic;

    @Column(name = "accrued_yield_atomic", nullable = false, precision = 38, scale = 0)
    private BigInteger accruedYieldAtomic;

    @Column(name = "apy_bps", nullable = false)
    private int apyBps;

    @Column(name = "last_accrued_at")
    private LocalDateTime lastAccruedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VaultPositionStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private VaultPosition(
            Long userId,
            String walletAddress,
            String assetSymbol,
            String assetAddress,
            String vaultAddress,
            String network,
            int apyBps
    ) {
        this.userId = userId;
        this.walletAddress = walletAddress;
        this.assetSymbol = assetSymbol;
        this.assetAddress = assetAddress;
        this.vaultAddress = vaultAddress;
        this.network = network;
        this.shareBalance = BigInteger.ZERO;
        this.principalAmountAtomic = BigInteger.ZERO;
        this.accruedYieldAtomic = BigInteger.ZERO;
        this.apyBps = apyBps;
        this.status = VaultPositionStatus.ACTIVE;
        this.lastAccruedAt = LocalDateTime.now();
    }

    public static VaultPosition create(
            Long userId,
            String walletAddress,
            String assetSymbol,
            String assetAddress,
            String vaultAddress,
            String network,
            int apyBps
    ) {
        return new VaultPosition(userId, walletAddress, assetSymbol, assetAddress, vaultAddress, network, apyBps);
    }

    public void applyDeposit(BigInteger amountAtomic, BigInteger shareDelta, LocalDateTime appliedAt) {
        requireActive();
        this.principalAmountAtomic = principalAmountAtomic.add(amountAtomic);
        this.shareBalance = shareBalance.add(shareDelta);
        this.lastAccruedAt = appliedAt;
    }

    public void applyWithdraw(BigInteger amountAtomic, BigInteger shareDelta, LocalDateTime appliedAt) {
        requireActive();
        if (principalAmountAtomic.compareTo(amountAtomic) < 0 || shareBalance.compareTo(shareDelta) < 0) {
            throw new IllegalStateException("vault withdrawal exceeds current position");
        }
        this.principalAmountAtomic = principalAmountAtomic.subtract(amountAtomic);
        this.shareBalance = shareBalance.subtract(shareDelta);
        this.lastAccruedAt = appliedAt;
    }

    public void accrue(BigInteger yieldAmountAtomic, LocalDateTime accruedAt) {
        requireActive();
        if (yieldAmountAtomic.signum() > 0) {
            this.accruedYieldAtomic = accruedYieldAtomic.add(yieldAmountAtomic);
        }
        this.lastAccruedAt = accruedAt;
    }

    private void requireActive() {
        if (status != VaultPositionStatus.ACTIVE) {
            throw new IllegalStateException("vault position is not active");
        }
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
