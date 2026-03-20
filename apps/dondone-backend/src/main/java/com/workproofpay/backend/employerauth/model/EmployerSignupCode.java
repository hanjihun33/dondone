package com.workproofpay.backend.employerauth.model;

import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "employer_signup_codes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employer_signup_codes_code_hash", columnNames = "code_hash")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployerSignupCode extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "default_workplace_id", nullable = false)
    private Long defaultWorkplaceId;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "issued_by_account_id")
    private Long issuedByAccountId;

    private EmployerSignupCode(String code,
                               Long companyId,
                               Long defaultWorkplaceId,
                               Long issuedByAccountId) {
        this.codeHash = EmployerInvitationToken.hash(code);
        this.companyId = companyId;
        this.defaultWorkplaceId = defaultWorkplaceId;
        this.issuedByAccountId = issuedByAccountId;
    }

    public static EmployerSignupCode create(String code,
                                            Long companyId,
                                            Long defaultWorkplaceId,
                                            Long issuedByAccountId) {
        return new EmployerSignupCode(code, companyId, defaultWorkplaceId, issuedByAccountId);
    }

    public boolean isUsable() {
        return revokedAt == null;
    }

    public void revoke(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
