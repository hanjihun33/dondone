package com.workproofpay.backend.employer.model;

import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "employer_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employer_profiles_account_id", columnNames = "account_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployerProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "default_workplace_id", nullable = false)
    private Long defaultWorkplaceId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployerProfileStatus status;

    private EmployerProfile(Long accountId,
                            Long companyId,
                            Long defaultWorkplaceId,
                            String displayName,
                            EmployerProfileStatus status) {
        this.accountId = accountId;
        this.companyId = companyId;
        this.defaultWorkplaceId = defaultWorkplaceId;
        this.displayName = displayName;
        this.status = status;
    }

    public static EmployerProfile create(Long accountId,
                                         Long companyId,
                                         Long defaultWorkplaceId,
                                         String displayName) {
        return new EmployerProfile(
                accountId,
                companyId,
                defaultWorkplaceId,
                displayName,
                EmployerProfileStatus.ACTIVE
        );
    }
}
