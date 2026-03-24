package com.workproofpay.backend.advance.repo;

import com.workproofpay.backend.advance.model.AdvancePayout;
import com.workproofpay.backend.advance.model.AdvancePayoutStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdvancePayoutRepository extends JpaRepository<AdvancePayout, String> {
    Optional<AdvancePayout> findByAdvancePayoutIdAndUserId(String advancePayoutId, Long userId);
    Optional<AdvancePayout> findByAdvanceRequestId(Long advanceRequestId);
    Optional<AdvancePayout> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
    List<AdvancePayout> findByUserIdOrderByCreatedAtDescAdvancePayoutIdDesc(Long userId, Pageable pageable);
    List<AdvancePayout> findByStatusInOrderByUpdatedAtDescAdvancePayoutIdDesc(Collection<AdvancePayoutStatus> statuses, Pageable pageable);
    List<AdvancePayout> findByStatusInAndUpdatedAtBeforeOrderByUpdatedAtDescAdvancePayoutIdDesc(
            Collection<AdvancePayoutStatus> statuses,
            LocalDateTime updatedAt,
            Pageable pageable
    );
    boolean existsByAdvanceRequestId(Long advanceRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payout from AdvancePayout payout where payout.advancePayoutId = :advancePayoutId")
    Optional<AdvancePayout> findByIdForUpdate(String advancePayoutId);
}
