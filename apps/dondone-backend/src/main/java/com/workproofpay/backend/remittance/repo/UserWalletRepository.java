package com.workproofpay.backend.remittance.repo;

import com.workproofpay.backend.remittance.model.UserWallet;
import com.workproofpay.backend.remittance.model.WalletFundingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from UserWallet w where w.userId = :userId")
    Optional<UserWallet> findByUserIdForUpdate(Long userId);

    long countByFundingStatus(WalletFundingStatus fundingStatus);
}
