package com.workproofpay.backend.remittance.repo;

import com.workproofpay.backend.remittance.model.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecipientRepository extends JpaRepository<Recipient, String> {
    List<Recipient> findByUserIdOrderByUpdatedAtDescRecipientIdDesc(Long userId);
    List<Recipient> findByUserIdAndRecipientIdIn(Long userId, Collection<String> recipientIds);
    Optional<Recipient> findByRecipientIdAndUserId(String recipientId, Long userId);
    boolean existsByUserIdAndWalletAddress(Long userId, String walletAddress);
    boolean existsByUserIdAndWalletAddressAndRecipientIdNot(Long userId, String walletAddress, String recipientId);
}
