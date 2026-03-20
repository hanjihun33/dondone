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
                @Index(name = "idx_jobs_reference_kind_id_type", columnList = "reference_kind, reference_id, job_type"),
                @Index(name = "uk_jobs_active_key", columnList = "active_key", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job {

    private static final int ACTIVE_KEY_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 40)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_kind", nullable = false, length = 40)
    private JobReferenceKind referenceKind;

    @Column(name = "reference_id", nullable = false, length = 64)
    private String referenceId;

    @Column(name = "active_key", length = ACTIVE_KEY_MAX_LENGTH)
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

    private Job(JobReferenceKind referenceKind, JobType jobType, String referenceId, LocalDateTime runAt) {
        this.referenceKind = referenceKind;
        this.jobType = jobType;
        this.referenceId = referenceId;
        this.activeKey = buildActiveKey(referenceKind, jobType, referenceId);
        this.status = JobStatus.QUEUED;
        this.attemptCount = 0;
        this.runAt = runAt;
    }

    public static Job queue(JobReferenceKind referenceKind, JobType jobType, String referenceId, LocalDateTime runAt) {
        return new Job(referenceKind, jobType, referenceId, runAt);
    }

    public static Job queue(JobType jobType, String referenceId, LocalDateTime runAt) {
        return queue(jobType.getReferenceKind(), jobType, referenceId, runAt);
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

    public static String buildActiveKey(JobReferenceKind referenceKind, JobType jobType, String referenceId) {
        String activeKey = referenceKind.name() + ":" + jobType.name() + ":" + referenceId;
        if (activeKey.length() > ACTIVE_KEY_MAX_LENGTH) {
            throw new IllegalArgumentException("Job active key exceeds max length");
        }
        return activeKey;
    }

    public static String buildActiveKey(JobType jobType, String referenceId) {
        return buildActiveKey(jobType.getReferenceKind(), jobType, referenceId);
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
