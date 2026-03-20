package com.workproofpay.backend.employer.repo;

import com.workproofpay.backend.employer.model.EmployerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployerProfileRepository extends JpaRepository<EmployerProfile, Long> {
    Optional<EmployerProfile> findByAccountId(Long accountId);

    List<EmployerProfile> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<EmployerProfile> findByCompanyIdIn(List<Long> companyIds);
}
