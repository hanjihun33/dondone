package com.workproofpay.backend.jobs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "jobs",
        indexes = {
                @Index(name = "idx_jobs_status_run_at", columnList = "status, run_at, id"),
                @Index(name = "idx_jobs_reference_type", columnList = "reference_id, job_type"),
                @Index(name = "uk_jobs_active_key", columnList = "active_key", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 40)
    private JobType jobType;

    @Column(name = "reference_id", nullable = false, length = 64)
    private String referenceId;

    @Column(name = "active_key", length = 128)
    private String activeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "run_at", nullable = false)
    private LocalDateTime runAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Job(JobType jobType, String referenceId, LocalDateTime runAt) {
        this.jobType = jobType;
        this.referenceId = referenceId;
        this.activeKey = buildActiveKey(jobType, referenceId);
        this.status = JobStatus.QUEUED;
        this.attemptCount = 0;
        this.runAt = runAt;
    }

    public static Job queue(JobType jobType, String referenceId, LocalDateTime runAt) {
        return new Job(jobType, referenceId, runAt);
    }

    public void markRunning() {
        this.status = JobStatus.RUNNING;
        this.attemptCount += 1;
    }

    public void markDone() {
        this.status = JobStatus.DONE;
        this.activeKey = null;
        this.lastError = null;
    }

    public void requeue(LocalDateTime nextRunAt, String lastError) {
        this.status = JobStatus.QUEUED;
        this.runAt = nextRunAt;
        this.lastError = sanitizeError(lastError);
    }

    public void markFailed(String lastError) {
        this.status = JobStatus.FAILED;
        this.activeKey = null;
        this.lastError = sanitizeError(lastError);
    }

    public static String buildActiveKey(JobType jobType, String referenceId) {
        return jobType.name() + ":" + referenceId;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private String sanitizeError(String lastError) {
        if (lastError == null || lastError.isBlank()) {
            return null;
        }
        String normalized = lastError.replaceAll("\\s+", " ").trim();
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }
}
