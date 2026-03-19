package com.workproofpay.backend.jobs.service;

import com.workproofpay.backend.jobs.model.Job;
import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.repo.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    @Transactional
    public void enqueue(JobType jobType, String referenceId, LocalDateTime runAt) {
        jobRepository.save(Job.queue(jobType, referenceId, runAt));
    }
}
