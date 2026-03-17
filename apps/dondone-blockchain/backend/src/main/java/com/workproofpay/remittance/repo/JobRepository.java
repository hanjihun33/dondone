package com.workproofpay.remittance.repo;

import com.workproofpay.remittance.domain.JobEntity;
import com.workproofpay.remittance.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface JobRepository extends JpaRepository<JobEntity, Long> {
    List<JobEntity> findTop20ByStatusAndRunAtLessThanEqualOrderByIdAsc(JobStatus status, Instant runAt);
}
