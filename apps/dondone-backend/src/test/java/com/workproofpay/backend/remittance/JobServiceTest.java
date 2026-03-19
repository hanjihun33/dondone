package com.workproofpay.backend.remittance;

import com.workproofpay.backend.jobs.model.Job;
import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.repo.JobRepository;
import com.workproofpay.backend.jobs.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobService(jobRepository);
    }

    @Test
    void ignoresDuplicateActiveJobInsertWhenActiveKeyAlreadyExists() {
        when(jobRepository.saveAndFlush(any(Job.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate active key"));
        when(jobRepository.existsByActiveKey("SUBMIT_TRANSFER:tr_1"))
                .thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                jobService.enqueue(JobType.SUBMIT_TRANSFER, "tr_1", LocalDateTime.now())
        );

        verify(jobRepository).existsByActiveKey("SUBMIT_TRANSFER:tr_1");
    }

    @Test
    void rethrowsUnexpectedIntegrityViolationWhenActiveKeyIsMissing() {
        when(jobRepository.saveAndFlush(any(Job.class)))
                .thenThrow(new DataIntegrityViolationException("unexpected"));
        when(jobRepository.existsByActiveKey(eq("SUBMIT_TRANSFER:tr_2")))
                .thenReturn(false);

        assertThatThrownBy(() ->
                jobService.enqueue(JobType.SUBMIT_TRANSFER, "tr_2", LocalDateTime.now())
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
