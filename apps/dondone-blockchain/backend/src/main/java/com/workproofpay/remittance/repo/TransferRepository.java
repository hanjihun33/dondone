package com.workproofpay.remittance.repo;

import com.workproofpay.remittance.domain.TransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferRepository extends JpaRepository<TransferEntity, String> {
    Optional<TransferEntity> findByTransferIdAndUserId(String transferId, String userId);
    Optional<TransferEntity> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
}
