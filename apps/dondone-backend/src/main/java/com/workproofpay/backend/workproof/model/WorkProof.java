package com.workproofpay.backend.workproof.model;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
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
/**
 * 기존 CRUD형 근무 기록과 lane 1 출퇴근 흐름을 함께 담는 근무 증거 엔티티다.
 */
public class WorkProof extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workplace_id")
    private Workplace workplace;

    @Column(name = "workplace_name_snapshot", length = 100)
    private String workplaceNameSnapshot;

    @Column(name = "workplace_address_snapshot", length = 255)
    private String workplaceAddressSnapshot;

    @Column(name = "workplace_map_label_snapshot", length = 100)
    private String workplaceMapLabelSnapshot;

    @Column(name = "workplace_latitude_snapshot")
    private Double workplaceLatitudeSnapshot;

    @Column(name = "workplace_longitude_snapshot")
    private Double workplaceLongitudeSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private WorkContract contract;

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

    @Column(name = "clock_in_location_label", length = 100)
    private String clockInLocationLabel;

    @Column(name = "clock_out_location_label", length = 100)
    private String clockOutLocationLabel;

    @Column(name = "clock_out_outside_allowed_radius")
    // 반경 밖 퇴근 여부를 남겨 review 사유를 상세/요약 응답에서 다시 설명한다.
    private Boolean clockOutOutsideAllowedRadius;

    @Column(length = 500)
    private String memo;

    @Column(name = "edit_reason", length = 500)
    private String editReason;

    @Column(name = "attachment_count", nullable = false)
    private int attachmentCount;

    @Column(name = "attachment_metadata_json", columnDefinition = "TEXT")
    private String attachmentMetadataJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "financial_status", nullable = false, length = 20)
    private WorkProofFinancialStatus financialStatus;

    private WorkProof(User user,
                      Workplace workplace,
                      String workplaceNameSnapshot,
                      String workplaceAddressSnapshot,
                      String workplaceMapLabelSnapshot,
                      Double workplaceLatitudeSnapshot,
                      Double workplaceLongitudeSnapshot,
                      WorkContract contract,
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
                      String clockInLocationLabel,
                      String clockOutLocationLabel,
                      Boolean clockOutOutsideAllowedRadius,
                      String memo,
                      String editReason,
                      int attachmentCount,
                      String attachmentMetadataJson,
                      WorkProofFinancialStatus financialStatus) {
        this.user = user;
        this.workplace = workplace;
        this.workplaceNameSnapshot = workplaceNameSnapshot;
        this.workplaceAddressSnapshot = workplaceAddressSnapshot;
        this.workplaceMapLabelSnapshot = workplaceMapLabelSnapshot;
        this.workplaceLatitudeSnapshot = workplaceLatitudeSnapshot;
        this.workplaceLongitudeSnapshot = workplaceLongitudeSnapshot;
        this.contract = contract;
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
        this.clockInLocationLabel = clockInLocationLabel;
        this.clockOutLocationLabel = clockOutLocationLabel;
        this.clockOutOutsideAllowedRadius = clockOutOutsideAllowedRadius;
        this.memo = memo;
        this.editReason = editReason;
        this.attachmentCount = attachmentCount;
        this.attachmentMetadataJson = attachmentMetadataJson;
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
                null,
                null,
                null,
                null,
                null,
                null,
                null,
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
                null,
                null,
                null,
                memo,
                editReason,
                attachmentCount == null ? 0 : attachmentCount,
                null,
                clockOutAt == null ? WorkProofFinancialStatus.PENDING : WorkProofFinancialStatus.REFLECTED
        );
    }

    public static WorkProof checkIn(User user,
                                    Workplace workplace,
                                    WorkContract contract,
                                    LocalDateTime deviceAt,
                                    LocalDateTime serverAt,
                                    Double latitude,
                                    Double longitude,
                                    String locationLabel) {
        return new WorkProof(
                user,
                workplace,
                workplace.getName(),
                workplace.getAddress(),
                workplace.getMapLabel(),
                workplace.getLatitude(),
                workplace.getLongitude(),
                contract,
                deviceAt.toLocalDate(),
                deviceAt,
                null,
                deviceAt,
                null,
                serverAt,
                null,
                latitude,
                longitude,
                null,
                null,
                locationLabel,
                null,
                null,
                null,
                null,
                0,
                null,
                WorkProofFinancialStatus.PENDING
        );
    }

    public boolean isReflected() {
        return financialStatus == WorkProofFinancialStatus.REFLECTED && clockOutAt != null;
    }

    public boolean isNeedsReview() {
        return financialStatus == WorkProofFinancialStatus.NEEDS_REVIEW && clockOutAt != null;
    }

    public boolean isClockOutOutsideAllowedRadius() {
        return Boolean.TRUE.equals(clockOutOutsideAllowedRadius);
    }

    public boolean isEdited() {
        return (editReason != null && !editReason.isBlank()) || attachmentCount > 0;
    }

    public boolean isCheckedIn() {
        return clockOutAt == null;
    }

    public long workedMinutes() {
        if (clockOutAt == null) {
            return 0L;
        }
        return Duration.between(clockInAt, clockOutAt).toMinutes();
    }

    public void updateTimes(LocalDateTime clockInAt,
                            LocalDateTime clockOutAt,
                            String editReason,
                            String memo,
                            int attachmentCount,
                            String attachmentMetadataJson) {
        // 일반 수정만으로 위치 이슈를 해소했다고 볼 수 없어서 별도 승인 흐름 전까지 review 상태를 유지한다.
        boolean keepNeedsReview = isClockOutOutsideAllowedRadius();
        this.clockInAt = clockInAt;
        this.clockOutAt = clockOutAt;
        this.editReason = editReason;
        this.memo = memo != null ? memo : this.memo;
        this.attachmentCount = attachmentCount;
        this.attachmentMetadataJson = attachmentMetadataJson;
        this.clockOutOutsideAllowedRadius = keepNeedsReview ? Boolean.TRUE : Boolean.FALSE;
        this.financialStatus = keepNeedsReview
                ? WorkProofFinancialStatus.NEEDS_REVIEW
                : WorkProofFinancialStatus.REFLECTED;
    }

    public void completeCheckOut(LocalDateTime deviceAt,
                                 LocalDateTime serverAt,
                                 Double latitude,
                                 Double longitude,
                                 String locationLabel,
                                 boolean outsideAllowedRadius) {
        this.clockOutAt = deviceAt;
        this.deviceClockOutAt = deviceAt;
        this.serverClockOutAt = serverAt;
        this.clockOutLatitude = latitude;
        this.clockOutLongitude = longitude;
        this.clockOutLocationLabel = locationLabel;
        this.clockOutOutsideAllowedRadius = outsideAllowedRadius;
        this.financialStatus = outsideAllowedRadius
                ? WorkProofFinancialStatus.NEEDS_REVIEW
                : WorkProofFinancialStatus.REFLECTED;
    }

    public String resolveWorkplaceName() {
        if (workplaceNameSnapshot != null) {
            return workplaceNameSnapshot;
        }
        return workplace == null ? null : workplace.getName();
    }

    public String resolveWorkplaceAddress() {
        if (workplaceAddressSnapshot != null) {
            return workplaceAddressSnapshot;
        }
        return workplace == null ? null : workplace.getAddress();
    }

    public String resolveWorkplaceMapLabel() {
        if (workplaceMapLabelSnapshot != null) {
            return workplaceMapLabelSnapshot;
        }
        return workplace == null ? null : workplace.getMapLabel();
    }

    public Double resolveWorkplaceLatitude() {
        if (workplaceLatitudeSnapshot != null) {
            return workplaceLatitudeSnapshot;
        }
        return workplace == null ? null : workplace.getLatitude();
    }

    public Double resolveWorkplaceLongitude() {
        if (workplaceLongitudeSnapshot != null) {
            return workplaceLongitudeSnapshot;
        }
        return workplace == null ? null : workplace.getLongitude();
    }
}
