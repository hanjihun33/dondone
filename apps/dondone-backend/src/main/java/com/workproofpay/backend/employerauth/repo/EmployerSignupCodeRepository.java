package com.workproofpay.backend.employerauth.repo;

import com.workproofpay.backend.employerauth.model.EmployerSignupCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployerSignupCodeRepository extends JpaRepository<EmployerSignupCode, Long> {
    Optional<EmployerSignupCode> findByCodeHash(String codeHash);

    List<EmployerSignupCode> findByCompanyIdAndDefaultWorkplaceId(Long companyId, Long defaultWorkplaceId);
}
