package com.workproofpay.remittance.repo;

import com.workproofpay.remittance.domain.RecipientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipientRepository extends JpaRepository<RecipientEntity, String> {
    List<RecipientEntity> findByUserIdOrderByUpdatedAtDesc(String userId);
    Optional<RecipientEntity> findByRecipientIdAndUserId(String recipientId, String userId);
}
