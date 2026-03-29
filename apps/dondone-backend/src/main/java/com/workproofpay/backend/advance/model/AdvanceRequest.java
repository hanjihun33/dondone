package com.workproofpay.backend.advance.model;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import com.workproofpay.backend.workproof.model.WorkContract;
import com.workproofpay.backend.workproof.model.Workplace;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "advance_requests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_advance_requests_user_idempotency", columnNames = {"user_id", "idempotency_key"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdvanceRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workplace_id", nullable = false)
    private Workplace workplace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private WorkContract contract;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "asset_symbol", nullable = false, length = 20)
    private String assetSymbol;

    @Column(name = "asset_decimals", nullable = false)
    private Integer assetDecimals;

    @Column(name = "reference_exchange_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal exchangeRateSnapshot;

    @Column(name = "requested_amount_atomic", nullable = false)
    private Long requestedAmountAtomic;

    @Column(name = "requested_reference_krw", nullable = false)
    private Long requestedDisplayKrwAmount;

    @Column(name = "approved_amount_atomic")
    private Long approvedAmountAtomic;

    @Column(name = "approved_reference_krw")
    private Long approvedDisplayKrwAmount;

    @Column(name = "fee_amount_atomic", nullable = false)
    private Long feeAmountAtomic;

    @Column(name = "fee_reference_krw", nullable = false)
    private Long feeDisplayKrwAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdvanceRequestStatus status;

    @Column(name = "repayment_due_date", nullable = false)
    private LocalDate repaymentDueDate;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_by_account_id")
    private Long reviewedByAccountId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "snapshot_available_amount_atomic", nullable = false)
    private Long snapshotAvailableAmountAtomic;

    @Column(name = "snapshot_available_reference_krw", nullable = false)
    private Long snapshotAvailableDisplayKrwAmount;

    @Column(name = "snapshot_max_cap_amount_atomic", nullable = false)
    private Long snapshotMaxCapAmountAtomic;

    @Column(name = "snapshot_max_cap_reference_krw", nullable = false)
    private Long snapshotMaxCapDisplayKrwAmount;

    @Column(name = "snapshot_policy_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal snapshotPolicyRate;

    @Column(name = "snapshot_reflected_work_days", nullable = false)
    private Integer snapshotReflectedWorkDays;

    @Column(name = "snapshot_reflected_work_minutes", nullable = false)
    private Long snapshotReflectedWorkMinutes;

    @Column(name = "snapshot_needs_review_record_count", nullable = false)
    private Integer snapshotNeedsReviewRecordCount;

    private AdvanceRequest(
            User user,
            Workplace workplace,
            WorkContract contract,
            String yearMonth,
            String idempotencyKey,
            String assetSymbol,
            Integer assetDecimals,
            BigDecimal exchangeRateSnapshot,
            Long requestedAmountAtomic,
            Long requestedDisplayKrwAmount,
            Long approvedAmountAtomic,
            Long approvedDisplayKrwAmount,
            Long feeAmountAtomic,
            Long feeDisplayKrwAmount,
            AdvanceRequestStatus status,
            LocalDate repaymentDueDate,
            LocalDateTime requestedAt,
            Long reviewedByAccountId,
            LocalDateTime reviewedAt,
            Long snapshotAvailableAmountAtomic,
            Long snapshotAvailableDisplayKrwAmount,
            Long snapshotMaxCapAmountAtomic,
            Long snapshotMaxCapDisplayKrwAmount,
            BigDecimal snapshotPolicyRate,
            Integer snapshotReflectedWorkDays,
            Long snapshotReflectedWorkMinutes,
            Integer snapshotNeedsReviewRecordCount
    ) {
        this.user = user;
        this.workplace = workplace;
        this.contract = contract;
        this.yearMonth = yearMonth;
        this.idempotencyKey = idempotencyKey;
        this.assetSymbol = assetSymbol;
        this.assetDecimals = assetDecimals;
        this.exchangeRateSnapshot = exchangeRateSnapshot;
        this.requestedAmountAtomic = requestedAmountAtomic;
        this.requestedDisplayKrwAmount = requestedDisplayKrwAmount;
        this.approvedAmountAtomic = approvedAmountAtomic;
        this.approvedDisplayKrwAmount = approvedDisplayKrwAmount;
        this.feeAmountAtomic = feeAmountAtomic;
        this.feeDisplayKrwAmount = feeDisplayKrwAmount;
        this.status = status;
        this.repaymentDueDate = repaymentDueDate;
        this.requestedAt = requestedAt;
        this.reviewedByAccountId = reviewedByAccountId;
        this.reviewedAt = reviewedAt;
        this.snapshotAvailableAmountAtomic = snapshotAvailableAmountAtomic;
        this.snapshotAvailableDisplayKrwAmount = snapshotAvailableDisplayKrwAmount;
        this.snapshotMaxCapAmountAtomic = snapshotMaxCapAmountAtomic;
        this.snapshotMaxCapDisplayKrwAmount = snapshotMaxCapDisplayKrwAmount;
        this.snapshotPolicyRate = snapshotPolicyRate;
        this.snapshotReflectedWorkDays = snapshotReflectedWorkDays;
        this.snapshotReflectedWorkMinutes = snapshotReflectedWorkMinutes;
        this.snapshotNeedsReviewRecordCount = snapshotNeedsReviewRecordCount;
    }

    public static AdvanceRequest submit(
            User user,
            Workplace workplace,
            WorkContract contract,
            String yearMonth,
            String idempotencyKey,
            String assetSymbol,
            Integer assetDecimals,
            BigDecimal exchangeRateSnapshot,
            Long requestedAmountAtomic,
            Long requestedDisplayKrwAmount,
            Long feeAmountAtomic,
            Long feeDisplayKrwAmount,
            LocalDate repaymentDueDate,
            LocalDateTime requestedAt,
            Long snapshotAvailableAmountAtomic,
            Long snapshotAvailableDisplayKrwAmount,
            Long snapshotMaxCapAmountAtomic,
            Long snapshotMaxCapDisplayKrwAmount,
            BigDecimal snapshotPolicyRate,
            Integer snapshotReflectedWorkDays,
            Long snapshotReflectedWorkMinutes,
            Integer snapshotNeedsReviewRecordCount
    ) {
        return new AdvanceRequest(
                user,
                workplace,
                contract,
                yearMonth,
                idempotencyKey,
                assetSymbol,
                assetDecimals,
                exchangeRateSnapshot,
                requestedAmountAtomic,
                requestedDisplayKrwAmount,
                null,
                null,
                feeAmountAtomic,
                feeDisplayKrwAmount,
                AdvanceRequestStatus.SUBMITTED,
                repaymentDueDate,
                requestedAt,
                null,
                null,
                snapshotAvailableAmountAtomic,
                snapshotAvailableDisplayKrwAmount,
                snapshotMaxCapAmountAtomic,
                snapshotMaxCapDisplayKrwAmount,
                snapshotPolicyRate,
                snapshotReflectedWorkDays,
                snapshotReflectedWorkMinutes,
                snapshotNeedsReviewRecordCount
        );
    }

    public void approve(Long reviewedByAccountId) {
        ensureSubmitted();
        this.status = AdvanceRequestStatus.APPROVED;
        this.approvedAmountAtomic = this.requestedAmountAtomic;
        this.approvedDisplayKrwAmount = this.requestedDisplayKrwAmount;
        this.reviewedByAccountId = reviewedByAccountId;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(Long reviewedByAccountId) {
        ensureSubmitted();
        this.status = AdvanceRequestStatus.REJECTED;
        this.approvedAmountAtomic = null;
        this.approvedDisplayKrwAmount = null;
        this.reviewedByAccountId = reviewedByAccountId;
        this.reviewedAt = LocalDateTime.now();
    }

    public boolean isSubmitted() {
        return status == AdvanceRequestStatus.SUBMITTED;
    }

    public boolean matches(String candidateKey, Long workplaceId, Long requestedAmountAtomic, LocalDateTime requestedAt) {
        return idempotencyKey.equals(candidateKey)
                && workplace.getId().equals(workplaceId)
                && this.requestedAmountAtomic.equals(requestedAmountAtomic)
                && this.requestedAt.equals(requestedAt);
    }

    private void ensureSubmitted() {
        if (status != AdvanceRequestStatus.SUBMITTED) {
            throw new IllegalStateException("Advance request is not in SUBMITTED status.");
        }
    }
}
