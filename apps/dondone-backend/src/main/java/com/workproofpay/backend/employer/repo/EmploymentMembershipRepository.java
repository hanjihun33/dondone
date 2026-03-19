package com.workproofpay.backend.employer.repo;

import com.workproofpay.backend.employer.model.EmploymentMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmploymentMembershipRepository extends JpaRepository<EmploymentMembership, Long> {

    List<EmploymentMembership> findByCompanyIdAndWorkplaceId(Long companyId, Long workplaceId);
}
