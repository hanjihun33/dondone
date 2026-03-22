package com.workproofpay.backend.employer.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record EmployerDashboardSummaryResponse(
        @Schema(description = "Active workers currently scoped to the employer default workplace", example = "12")
        long activeWorkerCount,
        @Schema(description = "Workers with an open workproof for today", example = "4")
        long workingCount,
        @Schema(description = "Workers whose latest scoped workproof for today is completed", example = "6")
        long completedCount,
        @Schema(description = "Workers whose latest scoped workproof for today needs review", example = "1")
        long needsReviewCount,
        @Schema(description = "Workers with no scoped workproof for today", example = "1")
        long noRecordCount,
        @Schema(description = "Date used to calculate the summary")
        LocalDate asOf
) {
}
