package com.workproofpay.backend.correction.repo;

import com.workproofpay.backend.correction.model.CorrectionRequest;
import com.workproofpay.backend.correction.model.CorrectionRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorrectionRequestRepository extends JpaRepository<CorrectionRequest, Long> {

    List<CorrectionRequest> findByCompanyIdAndWorkplaceIdOrderByCreatedAtDescIdDesc(Long companyId, Long workplaceId);

    boolean existsByWorkProofIdAndStatus(Long workProofId, CorrectionRequestStatus status);
}
