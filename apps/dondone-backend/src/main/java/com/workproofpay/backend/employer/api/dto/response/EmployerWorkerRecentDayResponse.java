package com.workproofpay.backend.employer.api.dto.response;

import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordStatus;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofReflectionStatus;

import java.time.LocalDate;

public record EmployerWorkerRecentDayResponse(
        LocalDate date,
        WorkProofRecordStatus recordStatus,
        WorkProofReflectionStatus reflectionStatus,
        EmployerWorkerAttendanceStatus attendanceStatus,
        Long workedMinutes
) {
}
