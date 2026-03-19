package com.workproofpay.backend.employer.api.dto.response;

import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordStatus;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofReflectionStatus;

import java.time.LocalDate;
import java.util.List;

public record EmployerWorkerDetailResponse(
        Long workerId,
        String employeeCode,
        String name,
        String team,
        String role,
        String email,
        String phone,
        String avatarUrl,
        LocalDate membershipEffectiveFrom,
        LocalDate membershipEffectiveTo,
        WorkProofRecordStatus recordStatus,
        WorkProofReflectionStatus reflectionStatus,
        EmployerWorkerAttendanceStatus attendanceStatus,
        LocalDate latestWorkDate,
        EmployerWorkerLatestRecordResponse latestRecord,
        List<EmployerWorkerRecentDayResponse> recentDays
) {
}
