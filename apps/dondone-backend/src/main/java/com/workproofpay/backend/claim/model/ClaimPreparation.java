package com.workproofpay.backend.claim.model;

import com.workproofpay.backend.auth.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Instant Claim v0는 자동 제출이 아니라 준비 결과를 저장해 다시 열어볼 수 있게 하는 엔티티다.
 */
@Entity
@Table(name = "claim_preparations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimPreparation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "wage_verification_id", nullable = false)
    private Long wageVerificationId;

    @Column(name = "claim_kit_document_id")
    private Long claimKitDocumentId;

    @Column(nullable = false, length = 20)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimPreparationTone tone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimPreparationStatus status;

    @Column(name = "summary_text", nullable = false, length = 2000)
    private String summaryText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ClaimPreparation(User user,
                             Long wageVerificationId,
                             Long claimKitDocumentId,
                             String locale,
                             ClaimPreparationTone tone,
                             String summaryText) {
        this.user = user;
        this.wageVerificationId = wageVerificationId;
        this.claimKitDocumentId = claimKitDocumentId;
        this.locale = locale;
        this.tone = tone;
        this.status = ClaimPreparationStatus.READY;
        this.summaryText = summaryText;
    }

    public static ClaimPreparation ready(User user,
                                         Long wageVerificationId,
                                         Long claimKitDocumentId,
                                         String locale,
                                         ClaimPreparationTone tone,
                                         String summaryText) {
        return new ClaimPreparation(user, wageVerificationId, claimKitDocumentId, locale, tone, summaryText);
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
