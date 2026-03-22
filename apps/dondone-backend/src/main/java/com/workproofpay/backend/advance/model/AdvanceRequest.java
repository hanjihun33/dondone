package com.workproofpay.backend.advance.model;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.shared.persistence.BaseCreatedEntity;
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
public class AdvanceRequest extends BaseCreatedEntity {

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

    @Column(name = "requested_amount", nullable = false)
    private Long requestedAmount;

    @Column(name = "approved_amount")
    private Long approvedAmount;

    @Column(name = "fee_amount", nullable = false)
    private Long feeAmount;

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

    @Column(name = "snapshot_available_amount", nullable = false)
    private Long snapshotAvailableAmount;

    @Column(name = "snapshot_max_cap", nullable = false)
    private Long snapshotMaxCap;

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
            Long requestedAmount,
            Long approvedAmount,
            Long feeAmount,
            AdvanceRequestStatus status,
            LocalDate repaymentDueDate,
            LocalDateTime requestedAt,
            Long reviewedByAccountId,
            LocalDateTime reviewedAt,
            Long snapshotAvailableAmount,
            Long snapshotMaxCap,
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
        this.requestedAmount = requestedAmount;
        this.approvedAmount = approvedAmount;
        this.feeAmount = feeAmount;
        this.status = status;
        this.repaymentDueDate = repaymentDueDate;
        this.requestedAt = requestedAt;
        this.reviewedByAccountId = reviewedByAccountId;
        this.reviewedAt = reviewedAt;
        this.snapshotAvailableAmount = snapshotAvailableAmount;
        this.snapshotMaxCap = snapshotMaxCap;
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
            Long requestedAmount,
            Long feeAmount,
            LocalDate repaymentDueDate,
            LocalDateTime requestedAt,
            Long snapshotAvailableAmount,
            Long snapshotMaxCap,
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
                requestedAmount,
                null,
                feeAmount,
                AdvanceRequestStatus.SUBMITTED,
                repaymentDueDate,
                requestedAt,
                null,
                null,
                snapshotAvailableAmount,
                snapshotMaxCap,
                snapshotPolicyRate,
                snapshotReflectedWorkDays,
                snapshotReflectedWorkMinutes,
                snapshotNeedsReviewRecordCount
        );
    }

    public void approve(Long reviewedByAccountId) {
        ensureSubmitted();
        this.status = AdvanceRequestStatus.APPROVED;
        this.approvedAmount = this.requestedAmount;
        this.reviewedByAccountId = reviewedByAccountId;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(Long reviewedByAccountId) {
        ensureSubmitted();
        this.status = AdvanceRequestStatus.REJECTED;
        this.approvedAmount = null;
        this.reviewedByAccountId = reviewedByAccountId;
        this.reviewedAt = LocalDateTime.now();
    }

    public boolean isSubmitted() {
        return status == AdvanceRequestStatus.SUBMITTED;
    }

    public boolean matches(String candidateKey, Long workplaceId, Long requestedAmount, LocalDateTime requestedAt) {
        return idempotencyKey.equals(candidateKey)
                && workplace.getId().equals(workplaceId)
                && this.requestedAmount.equals(requestedAmount)
                && this.requestedAt.equals(requestedAt);
    }

    private void ensureSubmitted() {
        if (status != AdvanceRequestStatus.SUBMITTED) {
            throw new IllegalStateException("Advance request is not in SUBMITTED status.");
        }
    }
}
