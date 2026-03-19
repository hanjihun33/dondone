package com.workproofpay.backend.workproof.repo;

import com.workproofpay.backend.workproof.model.WorkProofAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkProofAuditLogRepository extends JpaRepository<WorkProofAuditLog, Long> {

    long countByWorkProofId(Long workProofId);

    Optional<WorkProofAuditLog> findFirstByWorkProofIdOrderByCreatedAtDesc(Long workProofId);

    List<WorkProofAuditLog> findByWorkProofIdInOrderByCreatedAtDesc(List<Long> workProofIds);
}
