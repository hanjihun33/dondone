package com.workproofpay.remittance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_wallets")
public class UserWalletEntity {
    @Id
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "wallet_address", nullable = false, length = 128, unique = true)
    private String walletAddress;

    @Column(name = "encrypted_private_key", nullable = false, length = 512)
    private String encryptedPrivateKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getEncryptedPrivateKey() {
        return encryptedPrivateKey;
    }

    public void setEncryptedPrivateKey(String encryptedPrivateKey) {
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
