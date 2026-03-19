package com.workproofpay.backend.employer.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record EmployerAttendanceBoardRowResponse(
        @Schema(description = "Worker account ID", example = "21")
        Long workerId,
        @Schema(description = "Worker display name", example = "김민수")
        String name,
        @Schema(description = "Worker role or job title when a canonical profile source exists", nullable = true, example = "UI/UX 디자이너")
        String role,
        @Schema(description = "Worker avatar URL when a canonical profile source exists", nullable = true)
        String avatarUrl,
        @Schema(description = "Seven day cells for the requested week")
        List<EmployerAttendanceBoardDayResponse> days
) {
}
