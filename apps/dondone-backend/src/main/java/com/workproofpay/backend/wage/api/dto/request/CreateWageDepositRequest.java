package com.workproofpay.backend.wage.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateWageDepositRequest(
        @NotBlank(message = "yearMonth is required")
        String yearMonth,

        @NotNull(message = "depositDate is required")
        LocalDate depositDate,

        @NotNull(message = "actualDepositAmount is required")
        @Min(value = 0, message = "actualDepositAmount must be 0 or greater")
        Long actualDepositAmount,

        @NotNull(message = "deductionsKnown is required")
        Boolean deductionsKnown,

        @Size(max = 500, message = "note must be 500 characters or less")
        String note
) {
}
