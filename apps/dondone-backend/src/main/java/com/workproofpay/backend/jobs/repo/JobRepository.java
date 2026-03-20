package com.workproofpay.backend.jobs.repo;

import com.workproofpay.backend.jobs.model.Job;
import com.workproofpay.backend.jobs.model.JobReferenceKind;
import com.workproofpay.backend.jobs.model.JobStatus;
import com.workproofpay.backend.jobs.model.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findTop20ByReferenceKindAndStatusAndRunAtLessThanEqualOrderByIdAsc(
            JobReferenceKind referenceKind,
            JobStatus status,
            LocalDateTime runAt
    );
    boolean existsByReferenceKindAndReferenceIdAndJobTypeAndStatusIn(
            JobReferenceKind referenceKind,
            String referenceId,
            JobType jobType,
            Collection<JobStatus> statuses
    );
    boolean existsByActiveKey(String activeKey);
    List<Job> findByReferenceKindAndStatusInOrderByUpdatedAtDescIdDesc(
            JobReferenceKind referenceKind,
            Collection<JobStatus> statuses,
            Pageable pageable
    );
    long countByStatus(JobStatus status);
    long countByReferenceKindAndStatus(JobReferenceKind referenceKind, JobStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Job j where j.id = :id")
    Optional<Job> findByIdForUpdate(Long id);
}
