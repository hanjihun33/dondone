package com.workproofpay.backend.documents.model;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 실제 렌더링 job이 없어도 request anchor와 idempotency를 먼저 고정하기 위한 문서 생성 요청 엔티티다.
 */
@Entity
@Table(
        name = "document_generation_requests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_generation_requests_user_type_key",
                        columnNames = {"user_id", "document_type", "idempotency_key"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentGenerationRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Column(name = "wage_verification_id", nullable = false)
    private Long wageVerificationId;

    @Column(name = "year_month", nullable = false, length = 7)
    private String month;

    @Column(name = "workplace_id", nullable = false)
    private Long workplaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", nullable = false, length = 10)
    private DocumentFileFormat outputFormat;

    @Column(name = "include_attachments", nullable = false)
    private boolean includeAttachments;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentGenerationStatus status;

    private DocumentGenerationRequest(User user,
                                      DocumentType documentType,
                                      Long wageVerificationId,
                                      String month,
                                      Long workplaceId,
                                      DocumentFileFormat outputFormat,
                                      boolean includeAttachments,
                                      String idempotencyKey) {
        this.user = user;
        this.documentType = documentType;
        this.wageVerificationId = wageVerificationId;
        this.month = month;
        this.workplaceId = workplaceId;
        this.outputFormat = outputFormat;
        this.includeAttachments = includeAttachments;
        this.idempotencyKey = idempotencyKey;
        this.status = DocumentGenerationStatus.QUEUED;
    }

    public static DocumentGenerationRequest queueProofPack(User user,
                                                           Long wageVerificationId,
                                                           String month,
                                                           Long workplaceId,
                                                           String idempotencyKey) {
        return new DocumentGenerationRequest(
                user,
                DocumentType.PROOF_PACK,
                wageVerificationId,
                month,
                workplaceId,
                DocumentFileFormat.PDF,
                false,
                idempotencyKey
        );
    }

    public static DocumentGenerationRequest queueClaimKit(User user,
                                                          Long wageVerificationId,
                                                          String month,
                                                          Long workplaceId,
                                                          DocumentFileFormat outputFormat,
                                                          boolean includeAttachments,
                                                          String idempotencyKey) {
        return new DocumentGenerationRequest(
                user,
                DocumentType.CLAIM_KIT,
                wageVerificationId,
                month,
                workplaceId,
                outputFormat,
                includeAttachments,
                idempotencyKey
        );
    }

    public void markRunning() {
        this.status = DocumentGenerationStatus.RUNNING;
    }

    public void markReady() {
        this.status = DocumentGenerationStatus.READY;
    }

    public void markFailed() {
        this.status = DocumentGenerationStatus.FAILED;
    }

    @PrePersist
    public void onCreate() {
        if (this.requestId == null) {
            this.requestId = UUID.randomUUID().toString();
        }
    }
}
