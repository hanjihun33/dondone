package com.workproofpay.remittance.service;

import com.workproofpay.remittance.domain.JobEntity;
import com.workproofpay.remittance.domain.JobStatus;
import com.workproofpay.remittance.domain.JobType;
import com.workproofpay.remittance.repo.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class JobService {
    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public void enqueue(String transferId, JobType jobType, Instant runAt) {
        JobEntity job = new JobEntity();
        job.setTransferId(transferId);
        job.setJobType(jobType);
        job.setStatus(JobStatus.QUEUED);
        job.setAttemptCount(0);
        job.setRunAt(runAt);
        jobRepository.save(job);
    }
}
