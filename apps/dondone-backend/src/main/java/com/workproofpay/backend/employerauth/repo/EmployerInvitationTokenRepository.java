package com.workproofpay.backend.employerauth.repo;

import com.workproofpay.backend.employerauth.model.EmployerInvitationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployerInvitationTokenRepository extends JpaRepository<EmployerInvitationToken, Long> {
    Optional<EmployerInvitationToken> findByTokenHash(String tokenHash);
}
