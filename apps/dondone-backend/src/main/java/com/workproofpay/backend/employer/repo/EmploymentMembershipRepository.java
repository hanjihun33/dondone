package com.workproofpay.backend.employer.repo;

import com.workproofpay.backend.employer.model.EmploymentMembership;
import com.workproofpay.backend.employer.model.EmploymentMembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmploymentMembershipRepository extends JpaRepository<EmploymentMembership, Long> {

    List<EmploymentMembership> findByCompanyIdAndWorkplaceId(Long companyId, Long workplaceId);

    @Query("""
            select membership
            from EmploymentMembership membership
            where membership.companyId = :companyId
              and membership.workplaceId = :workplaceId
              and membership.status = :status
              and membership.effectiveFrom <= :targetDate
              and (membership.effectiveTo is null or membership.effectiveTo >= :targetDate)
            order by membership.createdAt desc, membership.id desc
            """)
    List<EmploymentMembership> findActiveByScope(
            @Param("companyId") Long companyId,
            @Param("workplaceId") Long workplaceId,
            @Param("status") EmploymentMembershipStatus status,
            @Param("targetDate") LocalDate targetDate
    );

    @Query("""
            select membership
            from EmploymentMembership membership
            where membership.companyId = :companyId
              and membership.workplaceId = :workplaceId
              and membership.status = :status
              and membership.effectiveFrom <= :endDate
              and (membership.effectiveTo is null or membership.effectiveTo >= :startDate)
            order by membership.createdAt desc, membership.id desc
            """)
    List<EmploymentMembership> findOverlappingByScope(
            @Param("companyId") Long companyId,
            @Param("workplaceId") Long workplaceId,
            @Param("status") EmploymentMembershipStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
