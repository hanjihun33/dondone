package com.workproofpay.remittance.repo;

import com.workproofpay.remittance.domain.TransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferRepository extends JpaRepository<TransferEntity, String> {
    Optional<TransferEntity> findByTransferIdAndUserId(String transferId, Long userId);
    Optional<TransferEntity> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}
