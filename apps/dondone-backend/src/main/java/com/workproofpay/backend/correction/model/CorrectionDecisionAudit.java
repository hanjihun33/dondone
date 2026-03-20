package com.workproofpay.backend.correction.model;

import com.workproofpay.backend.shared.persistence.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "correction_decision_audits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CorrectionDecisionAudit extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "correction_request_id", nullable = false)
    private CorrectionRequest correctionRequest;

    @Column(name = "actor_account_id", nullable = false)
    private Long actorAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "before_status", nullable = false, length = 20)
    private CorrectionRequestStatus beforeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "after_status", nullable = false, length = 20)
    private CorrectionRequestStatus afterStatus;

    @Column(name = "decision_memo", length = 500)
    private String decisionMemo;

    @Column(name = "reject_reason_code", length = 100)
    private String rejectReasonCode;

    private CorrectionDecisionAudit(CorrectionRequest correctionRequest,
                                    Long actorAccountId,
                                    CorrectionRequestStatus beforeStatus,
                                    CorrectionRequestStatus afterStatus,
                                    String decisionMemo,
                                    String rejectReasonCode) {
        this.correctionRequest = correctionRequest;
        this.actorAccountId = actorAccountId;
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.decisionMemo = decisionMemo;
        this.rejectReasonCode = rejectReasonCode;
    }

    public static CorrectionDecisionAudit record(CorrectionRequest correctionRequest,
                                                 Long actorAccountId,
                                                 CorrectionRequestStatus beforeStatus,
                                                 CorrectionRequestStatus afterStatus,
                                                 String decisionMemo,
                                                 String rejectReasonCode) {
        return new CorrectionDecisionAudit(
                correctionRequest,
                actorAccountId,
                beforeStatus,
                afterStatus,
                decisionMemo,
                rejectReasonCode
        );
    }
}
