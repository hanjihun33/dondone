package com.workproofpay.backend.wage.api.dto.response;

import com.workproofpay.backend.wage.model.WageDeposit;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WageDepositResponse(
        @Schema(description = "Wage deposit record ID", example = "1")
        Long id,
        @Schema(description = "Recorded wage month in YYYY-MM format", example = "2026-03")
        String yearMonth,
        @Schema(description = "Recorded deposit date", example = "2026-03-25")
        LocalDate depositDate,
        @Schema(description = "Worker self-reported actual received deposit amount in KRW", example = "1740000")
        Long actualDepositAmount,
        @Schema(description = "Whether deductions were known when the deposit was recorded", example = "false")
        boolean deductionsKnown,
        @Schema(description = "Optional worker note for follow-up confirmation", example = "Overtime may not be fully reflected.")
        String note,
        @Schema(description = "Record creation timestamp")
        LocalDateTime createdAt,
        @Schema(description = "Record update timestamp")
        LocalDateTime updatedAt
) {
    public static WageDepositResponse from(WageDeposit deposit) {
        return new WageDepositResponse(
                deposit.getId(),
                deposit.getYearMonth(),
                deposit.getDepositDate(),
                deposit.getActualDepositAmount(),
                deposit.isDeductionsKnown(),
                deposit.getNote(),
                deposit.getCreatedAt(),
                deposit.getUpdatedAt()
        );
    }
}
