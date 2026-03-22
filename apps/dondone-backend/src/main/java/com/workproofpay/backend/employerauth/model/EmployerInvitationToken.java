package com.workproofpay.backend.employerauth.model;

import com.workproofpay.backend.auth.model.UserRole;
import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "employer_invitation_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employer_invitation_tokens_token_hash", columnNames = "token_hash")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployerInvitationToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "invitee_email", nullable = false, length = 255)
    private String inviteeEmail;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "default_workplace_id", nullable = false)
    private Long defaultWorkplaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "invited_by_account_id")
    private Long invitedByAccountId;

    private EmployerInvitationToken(String token,
                                    String inviteeEmail,
                                    Long companyId,
                                    Long defaultWorkplaceId,
                                    UserRole role,
                                    LocalDateTime expiresAt,
                                    Long invitedByAccountId) {
        this.tokenHash = hash(token);
        this.inviteeEmail = inviteeEmail;
        this.companyId = companyId;
        this.defaultWorkplaceId = defaultWorkplaceId;
        this.role = role;
        this.expiresAt = expiresAt;
        this.invitedByAccountId = invitedByAccountId;
    }

    public static EmployerInvitationToken create(String token,
                                                 String inviteeEmail,
                                                 Long companyId,
                                                 Long defaultWorkplaceId,
                                                 LocalDateTime expiresAt,
                                                 Long invitedByAccountId) {
        return new EmployerInvitationToken(
                token,
                inviteeEmail,
                companyId,
                defaultWorkplaceId,
                UserRole.EMPLOYER,
                expiresAt,
                invitedByAccountId
        );
    }

    public boolean isUsableAt(LocalDateTime now) {
        return usedAt == null
                && revokedAt == null
                && !expiresAt.isBefore(now)
                && role == UserRole.EMPLOYER;
    }

    public boolean matchesInviteeEmail(String email) {
        return inviteeEmail.equalsIgnoreCase(email);
    }

    public void markUsed(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is required", e);
        }
    }
}
