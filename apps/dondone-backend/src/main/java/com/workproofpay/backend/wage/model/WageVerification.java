package com.workproofpay.backend.wage.model;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Wage verification은 worker self-check 결과를 snapshot으로 고정해,
 * 이후 Documents/Claim가 같은 근거를 다시 읽을 수 있게 하는 aggregate다.
 */
@Entity
@Table(name = "wage_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WageVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "year_month", nullable = false, length = 7)
    private String month;

    @Column(name = "workplace_id", nullable = false)
    private Long workplaceId;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_unit", nullable = false, length = 20)
    private WorkProofPayUnit payUnit;

    @Column(name = "base_pay_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal basePayAmount;

    @Column(name = "daily_work_minutes")
    private Integer dailyWorkMinutes;

    @Column(name = "monthly_work_minutes")
    private Integer monthlyWorkMinutes;

    @Column(name = "normalized_hourly_wage", nullable = false, precision = 19, scale = 2)
    private BigDecimal normalizedHourlyWage;

    @Column(name = "work_day_count", nullable = false)
    private int workDayCount;

    @Column(name = "verified_work_minutes", nullable = false)
    private long verifiedWorkMinutes;

    @Column(name = "overtime_minutes", nullable = false)
    private long overtimeMinutes;

    @Column(name = "night_minutes", nullable = false)
    private long nightMinutes;

    @Column(name = "modified_record_count", nullable = false)
    private int modifiedRecordCount;

    @Column(name = "excluded_pending_record_count", nullable = false)
    private int excludedPendingRecordCount;

    @Column(name = "actual_deposit_amount", nullable = false)
    private long actualDepositAmount;

    @Column(name = "deductions_known", nullable = false)
    private boolean deductionsKnown;

    @Column(length = 500)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WageVerificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_stage", nullable = false, length = 50)
    private WageVerificationResolutionStage resolutionStage;

    @Column(name = "base_estimate", nullable = false)
    private long baseEstimate;

    @Column(name = "overtime_premium", nullable = false)
    private long overtimePremium;

    @Column(name = "night_premium", nullable = false)
    private long nightPremium;

    @Column(name = "estimated_total", nullable = false)
    private long estimatedTotal;

    @Column(name = "difference_amount", nullable = false)
    private long differenceAmount;

    @Column(name = "difference_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal differenceRate;

    @Column(name = "threshold_absolute_won", nullable = false)
    private long thresholdAbsoluteWon;

    @Column(name = "threshold_relative_percent", nullable = false, precision = 10, scale = 4)
    private BigDecimal thresholdRelativePercent;

    @Column(name = "threshold_deduction_relaxed", nullable = false)
    private boolean thresholdDeductionRelaxed;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "wage_verification_record_ids",
            joinColumns = @JoinColumn(name = "verification_id")
    )
    @OrderColumn(name = "record_order")
    @Column(name = "record_id", nullable = false)
    private List<Long> evidenceRecordIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "wage_verification_possible_causes",
            joinColumns = @JoinColumn(name = "verification_id")
    )
    @OrderColumn(name = "cause_order")
    private List<WageVerificationPossibleCauseSnapshot> possibleCauses = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private WageVerification(User user, WageVerificationDraft draft) {
        this.user = user;
        this.month = draft.month();
        this.workplaceId = draft.workplaceId();
        this.contractId = draft.contractId();
        this.payUnit = draft.payUnit();
        this.basePayAmount = draft.basePayAmount();
        this.dailyWorkMinutes = draft.dailyWorkMinutes();
        this.monthlyWorkMinutes = draft.monthlyWorkMinutes();
        this.normalizedHourlyWage = draft.normalizedHourlyWage();
        this.workDayCount = draft.workDayCount();
        this.verifiedWorkMinutes = draft.verifiedWorkMinutes();
        this.overtimeMinutes = draft.overtimeMinutes();
        this.nightMinutes = draft.nightMinutes();
        this.modifiedRecordCount = draft.modifiedRecordCount();
        this.excludedPendingRecordCount = draft.excludedPendingRecordCount();
        this.actualDepositAmount = draft.actualDepositAmount();
        this.deductionsKnown = draft.deductionsKnown();
        this.memo = draft.memo();
        this.status = draft.status();
        this.resolutionStage = draft.resolutionStage();
        this.baseEstimate = draft.baseEstimate();
        this.overtimePremium = draft.overtimePremium();
        this.nightPremium = draft.nightPremium();
        this.estimatedTotal = draft.estimatedTotal();
        this.differenceAmount = draft.differenceAmount();
        this.differenceRate = draft.differenceRate();
        this.thresholdAbsoluteWon = draft.thresholdAbsoluteWon();
        this.thresholdRelativePercent = draft.thresholdRelativePercent();
        this.thresholdDeductionRelaxed = draft.thresholdDeductionRelaxed();
        this.evidenceRecordIds = new ArrayList<>(draft.evidenceRecordIds());
        this.possibleCauses = new ArrayList<>(draft.possibleCauses());
    }

    public static WageVerification record(User user, WageVerificationDraft draft) {
        return new WageVerification(user, draft);
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
