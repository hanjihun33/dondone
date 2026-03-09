package com.workproofpay.backend.workproof.repo;

import com.workproofpay.backend.workproof.model.WorkProof;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkProofRepository extends JpaRepository<WorkProof, Long> {

    List<WorkProof> findByUserIdOrderByWorkDateDescClockInAtDesc(Long userId);

    List<WorkProof> findByUserIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(Long userId, LocalDate startDate, LocalDate endDate);

    Optional<WorkProof> findByIdAndUserId(Long id, Long userId);
}
