package com.workproofpay.backend.employer.repo;

import com.workproofpay.backend.employer.model.WorkerRegistrationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkerRegistrationCodeRepository extends JpaRepository<WorkerRegistrationCode, Long> {

    Optional<WorkerRegistrationCode> findByCodeHash(String codeHash);

    List<WorkerRegistrationCode> findByCompanyIdAndWorkplaceIdOrderByCreatedAtDescIdDesc(Long companyId, Long workplaceId);

    Optional<WorkerRegistrationCode> findByIdAndCompanyIdAndWorkplaceId(Long id, Long companyId, Long workplaceId);
}
