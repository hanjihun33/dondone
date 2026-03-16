package com.workproofpay.backend.wage.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Wage verification은 근로자가 실제 받은 돈을 확인해 reference-only estimate와 비교를 시작하는 입력이다.
 * P0에서는 worker self-check만 먼저 열고, payslip parsing이나 employer workflow는 포함하지 않는다.
 */
public record CreateWageVerificationRequest(
        @Schema(description = "Target wage month in YYYY-MM format", example = "2026-03")
        @NotBlank(message = "month is required")
        String month,

        @Schema(description = "Owned workplace ID used for this verification", example = "1")
        @NotNull(message = "workplaceId is required")
        @Min(value = 1, message = "workplaceId must be greater than 0")
        Long workplaceId,

        @Schema(description = "Worker-confirmed actual deposit amount in KRW", example = "1740000")
        @NotNull(message = "actualDepositAmount is required")
        @Min(value = 0, message = "actualDepositAmount must be 0 or greater")
        Long actualDepositAmount,

        @Schema(description = "Whether the worker knows deductions for the current month", example = "false")
        @NotNull(message = "deductionsKnown is required")
        Boolean deductionsKnown,

        @Schema(description = "Optional worker memo captured at verification time", example = "I need to check overtime with my employer.")
        @Size(max = 500, message = "memo must be 500 characters or less")
        String memo
) {
}
