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
@Table(name = "recipients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipient {

    @Id
    @Column(name = "recipient_id", nullable = false, length = 64)
    private String recipientId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "alias", nullable = false, length = 100)
    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation", nullable = false, length = 20)
    private RecipientRelation relation;

    @Column(name = "wallet_address", nullable = false, length = 42)
    private String walletAddress;

    @Column(name = "allowed", nullable = false)
    private boolean allowed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Recipient(String recipientId, Long userId, String alias, RecipientRelation relation, String walletAddress, boolean allowed) {
        this.recipientId = recipientId;
        this.userId = userId;
        this.alias = alias;
        this.relation = relation;
        this.walletAddress = walletAddress;
        this.allowed = allowed;
    }

    public static Recipient create(String recipientId, Long userId, String alias, RecipientRelation relation, String walletAddress, boolean allowed) {
        return new Recipient(recipientId, userId, alias, relation, walletAddress, allowed);
    }

    public void update(String alias, RecipientRelation relation, String walletAddress, boolean allowed) {
        this.alias = alias;
        this.relation = relation;
        this.walletAddress = walletAddress;
        this.allowed = allowed;
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
