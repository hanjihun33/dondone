package com.workproofpay.backend.employer.model;

import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "employment_memberships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmploymentMembership extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_account_id", nullable = false)
    private Long workerAccountId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "workplace_id", nullable = false)
    private Long workplaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmploymentMembershipStatus status;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    private EmploymentMembership(Long workerAccountId,
                                 Long companyId,
                                 Long workplaceId,
                                 EmploymentMembershipStatus status,
                                 LocalDate effectiveFrom,
                                 LocalDate effectiveTo) {
        this.workerAccountId = workerAccountId;
        this.companyId = companyId;
        this.workplaceId = workplaceId;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public static EmploymentMembership create(Long workerAccountId,
                                              Long companyId,
                                              Long workplaceId,
                                              LocalDate effectiveFrom) {
        return new EmploymentMembership(
                workerAccountId,
                companyId,
                workplaceId,
                EmploymentMembershipStatus.ACTIVE,
                effectiveFrom,
                null
        );
    }

    public boolean isActiveOn(LocalDate date) {
        if (status != EmploymentMembershipStatus.ACTIVE) {
            return false;
        }
        if (effectiveFrom != null && date.isBefore(effectiveFrom)) {
            return false;
        }
        return effectiveTo == null || !date.isAfter(effectiveTo);
    }

    public boolean matchesScope(Long companyId, Long workplaceId) {
        return Objects.equals(this.companyId, companyId)
                && Objects.equals(this.workplaceId, workplaceId);
    }
}
