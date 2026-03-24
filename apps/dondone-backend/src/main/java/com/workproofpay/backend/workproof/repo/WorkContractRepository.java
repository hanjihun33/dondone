package com.workproofpay.backend.workproof.repo;

import com.workproofpay.backend.workproof.model.WorkContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkContractRepository extends JpaRepository<WorkContract, Long> {

    List<WorkContract> findByWorkplaceId(Long workplaceId);

    boolean existsByWorkplaceIdAndEffectiveToIsNull(Long workplaceId);

    Optional<WorkContract> findFirstByWorkplaceIdAndEffectiveToIsNullOrderByEffectiveFromDesc(Long workplaceId);

    boolean existsByWorkplaceIdAndWorkplaceUserIdAndEffectiveToIsNull(Long workplaceId, Long userId);

    Optional<WorkContract> findFirstByWorkplaceIdAndWorkplaceUserIdAndEffectiveToIsNullOrderByEffectiveFromDesc(Long workplaceId, Long userId);
}
