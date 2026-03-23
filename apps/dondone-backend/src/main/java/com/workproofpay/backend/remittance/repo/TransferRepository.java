package com.workproofpay.backend.remittance.repo;

import com.workproofpay.backend.remittance.model.Transfer;
import com.workproofpay.backend.remittance.model.TransferStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, String> {
    Optional<Transfer> findByTransferIdAndUserId(String transferId, Long userId);
    @Query("""
            select transfer
            from Transfer transfer
            where transfer.transferId = :transferId
              and (transfer.userId = :userId or transfer.recipientTargetUserIdSnapshot = :userId)
            """)
    Optional<Transfer> findAccessibleTransferByTransferId(
            @Param("transferId") String transferId,
            @Param("userId") Long userId
    );
    Optional<Transfer> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
    List<Transfer> findByUserIdOrderByCreatedAtDescTransferIdDesc(Long userId, Pageable pageable);
    List<Transfer> findByUserIdOrRecipientTargetUserIdSnapshotOrderByCreatedAtDescTransferIdDesc(
            Long userId,
            Long recipientTargetUserIdSnapshot,
            Pageable pageable
    );
    List<Transfer> findByStatusInOrderByUpdatedAtDescTransferIdDesc(Collection<TransferStatus> statuses, Pageable pageable);
    List<Transfer> findByStatusInAndUpdatedAtBeforeOrderByUpdatedAtDescTransferIdDesc(
            Collection<TransferStatus> statuses,
            java.time.LocalDateTime updatedAt,
            Pageable pageable
    );
    boolean existsByUserIdAndStatusIn(Long userId, Collection<TransferStatus> statuses);
    long countByStatus(TransferStatus status);
}
