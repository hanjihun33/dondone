package com.workproofpay.backend.vault.model;

import com.workproofpay.backend.auth.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(name = "vault_yield_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VaultYieldLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "position_id", nullable = false)
    private Long positionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_id", nullable = false, insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_vault_yield_logs_position"))
    private VaultPosition position;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_vault_yield_logs_user"))
    private User user;

    @Column(name = "from_at", nullable = false)
    private LocalDateTime fromAt;

    @Column(name = "to_at", nullable = false)
    private LocalDateTime toAt;

    @Column(name = "apy_bps", nullable = false)
    private int apyBps;

    @Column(name = "principal_amount_atomic", nullable = false, precision = 38, scale = 0)
    private BigInteger principalAmountAtomic;

    @Column(name = "yield_amount_atomic", nullable = false, precision = 38, scale = 0)
    private BigInteger yieldAmountAtomic;

    @Column(name = "calculation_basis", nullable = false, length = 32)
    private String calculationBasis;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private VaultYieldLog(
            Long positionId,
            Long userId,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            int apyBps,
            BigInteger principalAmountAtomic,
            BigInteger yieldAmountAtomic,
            String calculationBasis
    ) {
        this.positionId = positionId;
        this.userId = userId;
        this.fromAt = fromAt;
        this.toAt = toAt;
        this.apyBps = apyBps;
        this.principalAmountAtomic = principalAmountAtomic;
        this.yieldAmountAtomic = yieldAmountAtomic;
        this.calculationBasis = calculationBasis;
    }

    public static VaultYieldLog create(
            Long positionId,
            Long userId,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            int apyBps,
            BigInteger principalAmountAtomic,
            BigInteger yieldAmountAtomic,
            String calculationBasis
    ) {
        return new VaultYieldLog(positionId, userId, fromAt, toAt, apyBps, principalAmountAtomic, yieldAmountAtomic, calculationBasis);
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
