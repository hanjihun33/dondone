package com.workproofpay.backend.employer.repo;

import com.workproofpay.backend.employer.model.EmploymentMembership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentMembershipRepository extends JpaRepository<EmploymentMembership, Long> {
}
