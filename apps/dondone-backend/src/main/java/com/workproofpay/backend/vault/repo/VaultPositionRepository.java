package com.workproofpay.backend.vault.repo;

import com.workproofpay.backend.vault.model.VaultPosition;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface VaultPositionRepository extends JpaRepository<VaultPosition, Long> {
    @Query("select p from VaultPosition p where p.userId = :userId")
    Optional<VaultPosition> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from VaultPosition p where p.userId = :userId")
    Optional<VaultPosition> findByUserIdForUpdate(Long userId);
}
