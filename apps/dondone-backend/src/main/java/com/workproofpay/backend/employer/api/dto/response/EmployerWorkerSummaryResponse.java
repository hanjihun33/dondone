package com.workproofpay.backend.employer.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordStatus;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofReflectionStatus;

import java.time.LocalDate;

public record EmployerWorkerSummaryResponse(
        @Schema(description = "Worker account ID", example = "21")
        Long workerId,
        @Schema(description = "Worker employee code when a canonical profile source exists", nullable = true, example = "52936567")
        String employeeCode,
        @Schema(description = "Worker display name", example = "김민수")
        String name,
        @Schema(description = "Worker team when a canonical profile source exists", nullable = true, example = "디자인팀")
        String team,
        @Schema(description = "Worker role or job title when a canonical profile source exists", nullable = true, example = "UI/UX 디자이너")
        String role,
        @Schema(description = "Worker email", example = "worker1@acme.test")
        String email,
        @Schema(description = "Worker phone when a canonical profile source exists", nullable = true, example = "010-1234-5678")
        String phone,
        @Schema(description = "Worker avatar URL when a canonical profile source exists", nullable = true)
        String avatarUrl,
        @Schema(description = "Latest scoped workproof record status for today when a record exists", nullable = true)
        WorkProofRecordStatus recordStatus,
        @Schema(description = "Latest scoped workproof reflection status for today when a record exists", nullable = true)
        WorkProofReflectionStatus reflectionStatus,
        @Schema(description = "Scoped attendance status derived from today's workproof")
        EmployerWorkerAttendanceStatus attendanceStatus,
        @Schema(description = "Latest scoped work date included in the read-model", nullable = true)
        LocalDate latestWorkDate
) {
}
