package com.workproofpay.backend.advance.repo;

import com.workproofpay.backend.advance.model.AdvanceRequest;
import com.workproofpay.backend.advance.model.AdvanceRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdvanceRequestRepository extends JpaRepository<AdvanceRequest, Long> {

    Optional<AdvanceRequest> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    boolean existsByUserIdAndWorkplaceIdAndYearMonthAndStatus(
            Long userId,
            Long workplaceId,
            String yearMonth,
            AdvanceRequestStatus status
    );

    boolean existsByUserIdAndWorkplaceIdAndYearMonthAndStatusIn(
            Long userId,
            Long workplaceId,
            String yearMonth,
            List<AdvanceRequestStatus> statuses
    );

    List<AdvanceRequest> findByUserIdAndWorkplaceIdAndYearMonthOrderByRequestedAtDescCreatedAtDesc(
            Long userId,
            Long workplaceId,
            String yearMonth
    );

    List<AdvanceRequest> findByUserIdAndYearMonthOrderByRequestedAtDescCreatedAtDesc(Long userId, String yearMonth);

    Optional<AdvanceRequest> findByIdAndUserId(Long id, Long userId);

    List<AdvanceRequest> findAllByOrderByRequestedAtDescCreatedAtDesc();
}
