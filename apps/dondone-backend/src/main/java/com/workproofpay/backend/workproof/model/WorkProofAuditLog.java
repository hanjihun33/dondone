package com.workproofpay.backend.workproof.model;

import com.workproofpay.backend.shared.persistence.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_proof_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkProofAuditLog extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_proof_id", nullable = false)
    private WorkProof workProof;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "before_clock_in_at", nullable = false)
    private LocalDateTime beforeClockInAt;

    @Column(name = "before_clock_out_at", nullable = false)
    private LocalDateTime beforeClockOutAt;

    @Column(name = "after_clock_in_at", nullable = false)
    private LocalDateTime afterClockInAt;

    @Column(name = "after_clock_out_at", nullable = false)
    private LocalDateTime afterClockOutAt;

    @Column(name = "before_edit_reason", length = 500)
    private String beforeEditReason;

    @Column(name = "after_edit_reason", nullable = false, length = 500)
    private String afterEditReason;

    @Column(name = "before_memo", length = 500)
    private String beforeMemo;

    @Column(name = "after_memo", length = 500)
    private String afterMemo;

    @Column(name = "before_attachment_count", nullable = false)
    private int beforeAttachmentCount;

    @Column(name = "after_attachment_count", nullable = false)
    private int afterAttachmentCount;

    @Column(name = "before_attachment_metadata_json", columnDefinition = "TEXT")
    private String beforeAttachmentMetadataJson;

    @Column(name = "after_attachment_metadata_json", columnDefinition = "TEXT")
    private String afterAttachmentMetadataJson;

    private WorkProofAuditLog(WorkProof workProof,
                              Long actorUserId,
                              LocalDateTime beforeClockInAt,
                              LocalDateTime beforeClockOutAt,
                              LocalDateTime afterClockInAt,
                              LocalDateTime afterClockOutAt,
                              String beforeEditReason,
                              String afterEditReason,
                              String beforeMemo,
                              String afterMemo,
                              int beforeAttachmentCount,
                              int afterAttachmentCount,
                              String beforeAttachmentMetadataJson,
                              String afterAttachmentMetadataJson) {
        this.workProof = workProof;
        this.actorUserId = actorUserId;
        this.beforeClockInAt = beforeClockInAt;
        this.beforeClockOutAt = beforeClockOutAt;
        this.afterClockInAt = afterClockInAt;
        this.afterClockOutAt = afterClockOutAt;
        this.beforeEditReason = beforeEditReason;
        this.afterEditReason = afterEditReason;
        this.beforeMemo = beforeMemo;
        this.afterMemo = afterMemo;
        this.beforeAttachmentCount = beforeAttachmentCount;
        this.afterAttachmentCount = afterAttachmentCount;
        this.beforeAttachmentMetadataJson = beforeAttachmentMetadataJson;
        this.afterAttachmentMetadataJson = afterAttachmentMetadataJson;
    }

    public static WorkProofAuditLog record(WorkProof workProof,
                                           Long actorUserId,
                                           LocalDateTime beforeClockInAt,
                                           LocalDateTime beforeClockOutAt,
                                           LocalDateTime afterClockInAt,
                                           LocalDateTime afterClockOutAt,
                                           String beforeEditReason,
                                           String afterEditReason,
                                           String beforeMemo,
                                           String afterMemo,
                                           int beforeAttachmentCount,
                                           int afterAttachmentCount,
                                           String beforeAttachmentMetadataJson,
                                           String afterAttachmentMetadataJson) {
        return new WorkProofAuditLog(
                workProof,
                actorUserId,
                beforeClockInAt,
                beforeClockOutAt,
                afterClockInAt,
                afterClockOutAt,
                beforeEditReason,
                afterEditReason,
                beforeMemo,
                afterMemo,
                beforeAttachmentCount,
                afterAttachmentCount,
                beforeAttachmentMetadataJson,
                afterAttachmentMetadataJson
        );
    }
}
