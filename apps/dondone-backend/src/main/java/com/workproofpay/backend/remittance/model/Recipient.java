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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "recipients",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recipients_user_wallet", columnNames = {"user_id", "wallet_address"})
        },
        indexes = {
                @Index(name = "idx_recipients_user_updated_at", columnList = "user_id, updated_at DESC, recipient_id DESC"),
                @Index(name = "idx_recipients_target_user_id", columnList = "target_user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipient {

    @Id
    @Column(name = "recipient_id", nullable = false, length = 64)
    private String recipientId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_recipients_user"))
    private User user;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_recipients_target_user"))
    private User targetUser;

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

    private Recipient(
            String recipientId,
            Long userId,
            Long targetUserId,
            String alias,
            RecipientRelation relation,
            String walletAddress,
            boolean allowed
    ) {
        this.recipientId = recipientId;
        this.userId = userId;
        this.targetUserId = targetUserId;
        this.alias = alias;
        this.relation = relation;
        this.walletAddress = walletAddress;
        this.allowed = allowed;
    }

    public static Recipient create(
            String recipientId,
            Long userId,
            Long targetUserId,
            String alias,
            RecipientRelation relation,
            String walletAddress,
            boolean allowed
    ) {
        return new Recipient(recipientId, userId, targetUserId, alias, relation, walletAddress, allowed);
    }

    public void update(String alias, RecipientRelation relation, String walletAddress, Long targetUserId, boolean allowed) {
        this.alias = alias;
        this.relation = relation;
        this.walletAddress = walletAddress;
        this.targetUserId = targetUserId;
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
