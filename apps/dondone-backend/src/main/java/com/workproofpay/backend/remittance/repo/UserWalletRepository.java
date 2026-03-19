package com.workproofpay.backend.remittance.repo;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.remittance.model.UserWallet;
import com.workproofpay.backend.remittance.model.WalletFundingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {
    Optional<UserWallet> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from UserWallet w where w.userId = :userId")
    Optional<UserWallet> findByUserIdForUpdate(Long userId);

    @Query("""
            select u.id as userId,
                   u.name as displayName,
                   u.phoneNumber as phoneNumber,
                   w.walletAddress as walletAddress
            from UserWallet w
            join User u on u.id = w.userId
            where u.phoneNumber = :phoneNumber
              and u.id <> :currentUserId
            """)
    java.util.List<RecipientSearchCandidateProjection> findRecipientCandidatesByPhoneNumber(
            String phoneNumber,
            Long currentUserId
    );

    long countByFundingStatus(WalletFundingStatus fundingStatus);

    interface RecipientSearchCandidateProjection {
        Long getUserId();
        String getDisplayName();
        String getPhoneNumber();
        String getWalletAddress();
    }
}
