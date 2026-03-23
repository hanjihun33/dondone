package com.workproofpay.backend.vault.repo;

import com.workproofpay.backend.vault.model.VaultTransaction;
import com.workproofpay.backend.vault.model.VaultTransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VaultTransactionRepository extends JpaRepository<VaultTransaction, String> {
    Optional<VaultTransaction> findByVaultTransactionIdAndUserId(String vaultTransactionId, Long userId);
    Optional<VaultTransaction> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
    List<VaultTransaction> findByUserIdOrderByCreatedAtDescVaultTransactionIdDesc(Long userId, Pageable pageable);
    boolean existsByUserIdAndStatusIn(Long userId, Collection<VaultTransactionStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select vt from VaultTransaction vt where vt.vaultTransactionId = :vaultTransactionId")
    Optional<VaultTransaction> findByIdForUpdate(String vaultTransactionId);
}
