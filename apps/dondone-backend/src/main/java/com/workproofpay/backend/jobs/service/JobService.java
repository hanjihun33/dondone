package com.workproofpay.backend.jobs.service;

import com.workproofpay.backend.jobs.model.Job;
import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.repo.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    @Transactional
    public void enqueue(JobType jobType, String referenceId, LocalDateTime runAt) {
        String activeKey = Job.buildActiveKey(jobType, referenceId);
        try {
            jobRepository.saveAndFlush(Job.queue(jobType, referenceId, runAt));
        } catch (DataIntegrityViolationException e) {
            if (jobRepository.existsByActiveKey(activeKey)) {
                return;
            }
            throw e;
        }
    }
}
