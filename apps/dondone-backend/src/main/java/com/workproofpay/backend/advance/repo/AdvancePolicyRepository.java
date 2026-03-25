package com.workproofpay.backend.advance.repo;

import com.workproofpay.backend.advance.model.AdvancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdvancePolicyRepository extends JpaRepository<AdvancePolicy, Long> {
    Optional<AdvancePolicy> findFirstByOrderByIdAsc();
}
