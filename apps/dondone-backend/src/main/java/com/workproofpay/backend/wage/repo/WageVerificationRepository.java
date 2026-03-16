package com.workproofpay.backend.wage.repo;

import com.workproofpay.backend.wage.model.WageVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WageVerificationRepository extends JpaRepository<WageVerification, Long> {

    Optional<WageVerification> findByIdAndUserId(Long id, Long userId);
}
