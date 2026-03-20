package com.workproofpay.backend.employer.api.dto.response;

import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordStatus;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofReflectionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployerWorkerLatestRecordResponse(
        LocalDate workDate,
        LocalDateTime clockInAt,
        LocalDateTime clockOutAt,
        WorkProofRecordStatus recordStatus,
        WorkProofReflectionStatus reflectionStatus,
        EmployerWorkerAttendanceStatus attendanceStatus,
        Long workedMinutes,
        Boolean needsReview,
        Boolean clockOutOutsideAllowedRadius,
        Boolean edited,
        String workplaceName,
        String workplaceAddress,
        String workplaceMapLabel,
        String clockInLocationLabel,
        String clockOutLocationLabel
) {
}
