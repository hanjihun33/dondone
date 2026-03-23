package com.workproofpay.backend.employer.model;

import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "companies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "company_code", nullable = false, unique = true, length = 50)
    private String companyCode;

    @Column(name = "default_workplace_id")
    private Long defaultWorkplaceId;

    @Column(name = "scheduled_clock_in_time", nullable = false)
    private LocalTime scheduledClockInTime;

    @Column(name = "scheduled_clock_out_time", nullable = false)
    private LocalTime scheduledClockOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "overtime_rounding_unit", nullable = false, length = 20)
    private AttendanceOvertimeRoundingUnit overtimeRoundingUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyStatus status;

    private Company(String name,
                    String companyCode,
                    Long defaultWorkplaceId,
                    LocalTime scheduledClockInTime,
                    LocalTime scheduledClockOutTime,
                    AttendanceOvertimeRoundingUnit overtimeRoundingUnit,
                    CompanyStatus status) {
        this.name = name;
        this.companyCode = companyCode;
        this.defaultWorkplaceId = defaultWorkplaceId;
        this.scheduledClockInTime = scheduledClockInTime;
        this.scheduledClockOutTime = scheduledClockOutTime;
        this.overtimeRoundingUnit = overtimeRoundingUnit;
        this.status = status;
    }

    public static Company create(String name, String companyCode) {
        return new Company(
                name,
                companyCode,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                AttendanceOvertimeRoundingUnit.FIFTEEN_MINUTES,
                CompanyStatus.ACTIVE
        );
    }

    public void bindDefaultWorkplace(Long defaultWorkplaceId) {
        this.defaultWorkplaceId = defaultWorkplaceId;
    }

    public void updateAttendancePolicy(LocalTime scheduledClockInTime,
                                       LocalTime scheduledClockOutTime,
                                       AttendanceOvertimeRoundingUnit overtimeRoundingUnit) {
        this.scheduledClockInTime = scheduledClockInTime;
        this.scheduledClockOutTime = scheduledClockOutTime;
        this.overtimeRoundingUnit = overtimeRoundingUnit;
    }
}
