package com.workproofpay.backend.workproof.model;

import com.workproofpay.backend.auth.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_proofs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkProof {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "clock_in_at", nullable = false)
    private LocalDateTime clockInAt;

    @Column(name = "clock_out_at")
    private LocalDateTime clockOutAt;

    @Column(name = "device_clock_in_at", nullable = false)
    private LocalDateTime deviceClockInAt;

    @Column(name = "device_clock_out_at")
    private LocalDateTime deviceClockOutAt;

    @Column(name = "server_clock_in_at", nullable = false)
    private LocalDateTime serverClockInAt;

    @Column(name = "server_clock_out_at")
    private LocalDateTime serverClockOutAt;

    @Column(name = "clock_in_latitude", nullable = false)
    private Double clockInLatitude;

    @Column(name = "clock_in_longitude", nullable = false)
    private Double clockInLongitude;

    @Column(name = "clock_out_latitude")
    private Double clockOutLatitude;

    @Column(name = "clock_out_longitude")
    private Double clockOutLongitude;

    @Column(length = 500)
    private String memo;

    @Column(name = "edit_reason", length = 500)
    private String editReason;

    @Column(name = "attachment_count", nullable = false)
    private int attachmentCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "financial_status", nullable = false, length = 20)
    private WorkProofFinancialStatus financialStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private WorkProof(User user,
                      LocalDate workDate,
                      LocalDateTime clockInAt,
                      LocalDateTime clockOutAt,
                      LocalDateTime deviceClockInAt,
                      LocalDateTime deviceClockOutAt,
                      LocalDateTime serverClockInAt,
                      LocalDateTime serverClockOutAt,
                      Double clockInLatitude,
                      Double clockInLongitude,
                      Double clockOutLatitude,
                      Double clockOutLongitude,
                      String memo,
                      String editReason,
                      int attachmentCount,
                      WorkProofFinancialStatus financialStatus) {
        this.user = user;
        this.workDate = workDate;
        this.clockInAt = clockInAt;
        this.clockOutAt = clockOutAt;
        this.deviceClockInAt = deviceClockInAt;
        this.deviceClockOutAt = deviceClockOutAt;
        this.serverClockInAt = serverClockInAt;
        this.serverClockOutAt = serverClockOutAt;
        this.clockInLatitude = clockInLatitude;
        this.clockInLongitude = clockInLongitude;
        this.clockOutLatitude = clockOutLatitude;
        this.clockOutLongitude = clockOutLongitude;
        this.memo = memo;
        this.editReason = editReason;
        this.attachmentCount = attachmentCount;
        this.financialStatus = financialStatus;
    }

    public static WorkProof record(User user,
                                   LocalDate workDate,
                                   LocalDateTime clockInAt,
                                   LocalDateTime clockOutAt,
                                   LocalDateTime deviceClockInAt,
                                   LocalDateTime deviceClockOutAt,
                                   LocalDateTime serverRecordedAt,
                                   Double clockInLatitude,
                                   Double clockInLongitude,
                                   Double clockOutLatitude,
                                   Double clockOutLongitude,
                                   String memo,
                                   String editReason,
                                   Integer attachmentCount) {
        return new WorkProof(
                user,
                workDate,
                clockInAt,
                clockOutAt,
                deviceClockInAt,
                deviceClockOutAt,
                serverRecordedAt,
                clockOutAt == null ? null : serverRecordedAt,
                clockInLatitude,
                clockInLongitude,
                clockOutLatitude,
                clockOutLongitude,
                memo,
                editReason,
                attachmentCount == null ? 0 : attachmentCount,
                clockOutAt == null ? WorkProofFinancialStatus.PENDING : WorkProofFinancialStatus.REFLECTED
        );
    }

    public boolean isReflected() {
        return financialStatus == WorkProofFinancialStatus.REFLECTED && clockOutAt != null;
    }

    public boolean isEdited() {
        return (editReason != null && !editReason.isBlank()) || attachmentCount > 0;
    }

    public long workedMinutes() {
        if (clockOutAt == null) {
            return 0L;
        }
        return Duration.between(clockInAt, clockOutAt).toMinutes();
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
}
