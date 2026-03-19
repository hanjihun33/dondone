package com.workproofpay.backend.employer.api.dto.response;

import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordStatus;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofReflectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record EmployerAttendanceBoardDayResponse(
        @Schema(description = "Calendar date for this cell")
        LocalDate date,
        @Schema(description = "Latest scoped workproof record status for the date when a record exists", nullable = true)
        WorkProofRecordStatus recordStatus,
        @Schema(description = "Latest scoped workproof reflection status for the date when a record exists", nullable = true)
        WorkProofReflectionStatus reflectionStatus,
        @Schema(description = "Employer read-model attendance status for the date")
        EmployerWorkerAttendanceStatus attendanceStatus,
        @Schema(description = "Worked minutes when the scoped record is completed or reviewable", nullable = true, example = "480")
        Long workedMinutes
) {
}
