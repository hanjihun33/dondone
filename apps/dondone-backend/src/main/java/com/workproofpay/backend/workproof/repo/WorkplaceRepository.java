package com.workproofpay.backend.workproof.repo;

import com.workproofpay.backend.workproof.model.Workplace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkplaceRepository extends JpaRepository<Workplace, Long> {

    List<Workplace> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Workplace> findByIdAndUserId(Long id, Long userId);
}
