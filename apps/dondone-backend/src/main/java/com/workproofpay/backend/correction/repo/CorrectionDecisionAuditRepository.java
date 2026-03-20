package com.workproofpay.backend.correction.repo;

import com.workproofpay.backend.correction.model.CorrectionDecisionAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorrectionDecisionAuditRepository extends JpaRepository<CorrectionDecisionAudit, Long> {

    List<CorrectionDecisionAudit> findByCorrectionRequestIdOrderByCreatedAtDesc(Long correctionRequestId);
}
