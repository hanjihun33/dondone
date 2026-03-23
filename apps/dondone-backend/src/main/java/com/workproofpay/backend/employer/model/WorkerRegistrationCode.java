package com.workproofpay.backend.employer.model;

import com.workproofpay.backend.employerauth.model.EmployerInvitationToken;
import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "worker_registration_codes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_worker_registration_codes_code_hash", columnNames = "code_hash")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkerRegistrationCode extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "encrypted_code", nullable = false, length = 1024)
    private String encryptedCode;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "workplace_id", nullable = false)
    private Long workplaceId;

    @Column(name = "issued_by_account_id", nullable = false)
    private Long issuedByAccountId;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    private WorkerRegistrationCode(String code,
                                   String encryptedCode,
                                   Long companyId,
                                   Long workplaceId,
                                   Long issuedByAccountId) {
        this.codeHash = EmployerInvitationToken.hash(code);
        this.encryptedCode = encryptedCode;
        this.companyId = companyId;
        this.workplaceId = workplaceId;
        this.issuedByAccountId = issuedByAccountId;
    }

    public static WorkerRegistrationCode create(String code,
                                                String encryptedCode,
                                                Long companyId,
                                                Long workplaceId,
                                                Long issuedByAccountId) {
        return new WorkerRegistrationCode(code, encryptedCode, companyId, workplaceId, issuedByAccountId);
    }

    public boolean isUsable() {
        return revokedAt == null;
    }

    public void revoke(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
