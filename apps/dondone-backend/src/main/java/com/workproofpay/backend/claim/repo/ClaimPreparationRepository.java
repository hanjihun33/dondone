package com.workproofpay.backend.claim.repo;

import com.workproofpay.backend.claim.model.ClaimPreparation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClaimPreparationRepository extends JpaRepository<ClaimPreparation, Long> {

    Optional<ClaimPreparation> findByIdAndUserId(Long id, Long userId);

    Optional<ClaimPreparation> findFirstByUserIdAndWageVerificationIdOrderByCreatedAtDesc(Long userId, Long wageVerificationId);
}
