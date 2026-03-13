package com.workproofpay.backend.wage.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateWageDepositRequest(
        @Schema(description = "Target wage month in YYYY-MM format", example = "2026-03")
        @NotBlank(message = "yearMonth is required")
        String yearMonth,

        @Schema(description = "Deposit date inside the target month", example = "2026-03-25")
        @NotNull(message = "depositDate is required")
        LocalDate depositDate,

        @Schema(
                description = "Worker self-reported actual received deposit amount in KRW",
                example = "1740000"
        )
        @NotNull(message = "actualDepositAmount is required")
        @Min(value = 0, message = "actualDepositAmount must be 0 or greater")
        Long actualDepositAmount,

        @Schema(
                description = "Whether deductions are known when the worker records the actual deposit",
                example = "false"
        )
        @NotNull(message = "deductionsKnown is required")
        Boolean deductionsKnown,

        @Schema(
                description = "Optional worker note for follow-up confirmation or explanation",
                example = "Overtime may not be fully reflected."
        )
        @Size(max = 500, message = "note must be 500 characters or less")
        String note
) {
}
