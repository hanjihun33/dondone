package com.workproofpay.backend.employer.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record EmployerAttendanceBoardResponse(
        @Schema(description = "Normalized week start date")
        LocalDate weekStart,
        @Schema(description = "Normalized week end date")
        LocalDate weekEnd,
        @Schema(description = "Attendance board rows")
        List<EmployerAttendanceBoardRowResponse> rows,
        @Schema(description = "1-based current page", example = "1")
        int page,
        @Schema(description = "Requested page size", example = "20")
        int size,
        @Schema(description = "Total workers after query and status filters", example = "42")
        long totalElements,
        @Schema(description = "Total page count after query and status filters", example = "3")
        int totalPages,
        @Schema(description = "Whether another page exists", example = "true")
        boolean hasNext
) {
}
