package com.workproofpay.backend.correction.model;

import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import com.workproofpay.backend.workproof.model.WorkProof;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "correction_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CorrectionRequest extends BaseTimeEntity {

    private static final CorrectionRequestReasonCode DEFAULT_REASON_CODE = CorrectionRequestReasonCode.OTHER;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_proof_id", nullable = false)
    private WorkProof workProof;

    @Column(name = "requested_by_account_id", nullable = false)
    private Long requestedByAccountId;

    @Column(name = "worker_account_id", nullable = false)
    private Long workerAccountId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "workplace_id", nullable = false)
    private Long workplaceId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "original_clock_in_at", nullable = false)
    private LocalDateTime originalClockInAt;

    @Column(name = "original_clock_out_at", nullable = false)
    private LocalDateTime originalClockOutAt;

    @Column(name = "requested_clock_in_at", nullable = false)
    private LocalDateTime requestedClockInAt;

    @Column(name = "requested_clock_out_at", nullable = false)
    private LocalDateTime requestedClockOutAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 50)
    private CorrectionRequestReasonCode reasonCode;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "request_memo", length = 500)
    private String requestMemo;

    @Column(name = "attachment_count", nullable = false)
    private int attachmentCount;

    @Column(name = "attachment_metadata_json", columnDefinition = "TEXT")
    private String attachmentMetadataJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CorrectionRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_reason_code", length = 50)
    private CorrectionReviewReasonCode reviewReasonCode;

    @Column(name = "decision_by_account_id")
    private Long decisionByAccountId;

    @Column(name = "decision_at")
    private LocalDateTime decisionAt;

    @Column(name = "decision_memo", length = 500)
    private String decisionMemo;

    @Column(name = "reject_reason_code", length = 100)
    private String rejectReasonCode;

    private CorrectionRequest(WorkProof workProof,
                              Long requestedByAccountId,
                              Long workerAccountId,
                              Long companyId,
                              Long workplaceId,
                              LocalDate workDate,
                              LocalDateTime originalClockInAt,
                              LocalDateTime originalClockOutAt,
                              LocalDateTime requestedClockInAt,
                              LocalDateTime requestedClockOutAt,
                              CorrectionRequestReasonCode reasonCode,
                              String reason,
                              String requestMemo,
                              int attachmentCount,
                              String attachmentMetadataJson,
                              CorrectionRequestStatus status,
                              CorrectionReviewReasonCode reviewReasonCode) {
        this.workProof = workProof;
        this.requestedByAccountId = requestedByAccountId;
        this.workerAccountId = workerAccountId;
        this.companyId = companyId;
        this.workplaceId = workplaceId;
        this.workDate = workDate;
        this.originalClockInAt = originalClockInAt;
        this.originalClockOutAt = originalClockOutAt;
        this.requestedClockInAt = requestedClockInAt;
        this.requestedClockOutAt = requestedClockOutAt;
        this.reasonCode = reasonCode;
        this.reason = reason;
        this.requestMemo = requestMemo;
        this.attachmentCount = attachmentCount;
        this.attachmentMetadataJson = attachmentMetadataJson;
        this.status = status;
        this.reviewReasonCode = reviewReasonCode;
    }

    public static CorrectionRequest create(WorkProof workProof,
                                           Long requestedByAccountId,
                                           Long workerAccountId,
                                           Long companyId,
                                           Long workplaceId,
                                           LocalDate workDate,
                                           LocalDateTime originalClockInAt,
                                           LocalDateTime originalClockOutAt,
                                           LocalDateTime requestedClockInAt,
                                           LocalDateTime requestedClockOutAt,
                                           CorrectionRequestReasonCode reasonCode,
                                           String reason,
                                           String requestMemo,
                                           int attachmentCount,
                                           String attachmentMetadataJson) {
        return new CorrectionRequest(
                workProof,
                requestedByAccountId,
                workerAccountId,
                companyId,
                workplaceId,
                workDate,
                originalClockInAt,
                originalClockOutAt,
                requestedClockInAt,
                requestedClockOutAt,
                reasonCode,
                reason,
                requestMemo,
                attachmentCount,
                attachmentMetadataJson,
                CorrectionRequestStatus.PENDING,
                null
        );
    }

    public static CorrectionRequest create(WorkProof workProof,
                                           Long requestedByAccountId,
                                           Long workerAccountId,
                                           Long companyId,
                                           Long workplaceId,
                                           LocalDate workDate,
                                           LocalDateTime originalClockInAt,
                                           LocalDateTime originalClockOutAt,
                                           LocalDateTime requestedClockInAt,
                                           LocalDateTime requestedClockOutAt,
                                           String reason,
                                           String requestMemo,
                                           int attachmentCount,
                                           String attachmentMetadataJson) {
        return create(
                workProof,
                requestedByAccountId,
                workerAccountId,
                companyId,
                workplaceId,
                workDate,
                originalClockInAt,
                originalClockOutAt,
                requestedClockInAt,
                requestedClockOutAt,
                CorrectionRequestReasonCode.OTHER,
                reason,
                requestMemo,
                attachmentCount,
                attachmentMetadataJson
        );
    }

    public static CorrectionRequest createAutoApproved(WorkProof workProof,
                                                       Long requestedByAccountId,
                                                       Long workerAccountId,
                                                       Long companyId,
                                                       Long workplaceId,
                                                       LocalDate workDate,
                                                       LocalDateTime originalClockInAt,
                                                       LocalDateTime originalClockOutAt,
                                                       LocalDateTime requestedClockInAt,
                                                       LocalDateTime requestedClockOutAt,
                                                       CorrectionRequestReasonCode reasonCode,
                                                       String reason,
                                                       String requestMemo,
                                                       int attachmentCount,
                                                       String attachmentMetadataJson,
                                                       String decisionMemo,
                                                       LocalDateTime decisionAt) {
        CorrectionRequest request = new CorrectionRequest(
                workProof,
                requestedByAccountId,
                workerAccountId,
                companyId,
                workplaceId,
                workDate,
                originalClockInAt,
                originalClockOutAt,
                requestedClockInAt,
                requestedClockOutAt,
                reasonCode,
                reason,
                requestMemo,
                attachmentCount,
                attachmentMetadataJson,
                CorrectionRequestStatus.PENDING,
                null
        );
        request.approve(null, decisionMemo, decisionAt);
        return request;
    }

    public boolean isPending() {
        return status == CorrectionRequestStatus.PENDING;
    }

    public void markNeedsReview(CorrectionReviewReasonCode reviewReasonCode) {
        this.reviewReasonCode = reviewReasonCode;
    }

    public void approve(Long decisionByAccountId, String decisionMemo, LocalDateTime decisionAt) {
        this.status = CorrectionRequestStatus.APPROVED;
        this.decisionByAccountId = decisionByAccountId;
        this.decisionMemo = decisionMemo;
        this.decisionAt = decisionAt;
        this.rejectReasonCode = null;
        this.reviewReasonCode = null;
    }

    public void reject(Long decisionByAccountId, String decisionMemo, String rejectReasonCode, LocalDateTime decisionAt) {
        this.status = CorrectionRequestStatus.REJECTED;
        this.decisionByAccountId = decisionByAccountId;
        this.decisionMemo = decisionMemo;
        this.rejectReasonCode = rejectReasonCode;
        this.decisionAt = decisionAt;
    }

    public CorrectionRequestReasonCode getReasonCode() {
        return reasonCode == null ? DEFAULT_REASON_CODE : reasonCode;
    }
}
