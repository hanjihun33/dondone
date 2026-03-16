package com.workproofpay.backend.workproof.repo;

import com.workproofpay.backend.workproof.model.WorkProof;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkProofRepository extends JpaRepository<WorkProof, Long> {

    List<WorkProof> findByUserIdOrderByWorkDateDescClockInAtDesc(Long userId);

    List<WorkProof> findByUserIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(Long userId, LocalDate startDate, LocalDate endDate);

    List<WorkProof> findByUserIdAndWorkplaceIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(Long userId, Long workplaceId, LocalDate startDate, LocalDate endDate);

    Optional<WorkProof> findFirstByUserIdAndWorkplaceIdOrderByWorkDateDescClockInAtDesc(Long userId, Long workplaceId);

    Optional<WorkProof> findByIdAndUserId(Long id, Long userId);

    Optional<WorkProof> findFirstByUserIdAndClockOutAtIsNullOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndWorkDate(Long userId, LocalDate workDate);
}
