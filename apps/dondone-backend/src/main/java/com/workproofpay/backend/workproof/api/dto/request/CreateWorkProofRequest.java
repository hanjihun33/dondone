package com.workproofpay.backend.workproof.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreateWorkProofRequest(
        @NotNull(message = "workDate is required")
        LocalDate workDate,

        @NotNull(message = "clockInAt is required")
        LocalDateTime clockInAt,

        LocalDateTime clockOutAt,

        @NotNull(message = "deviceClockInAt is required")
        LocalDateTime deviceClockInAt,

        LocalDateTime deviceClockOutAt,

        @NotNull(message = "clockInLatitude is required")
        Double clockInLatitude,

        @NotNull(message = "clockInLongitude is required")
        Double clockInLongitude,

        Double clockOutLatitude,

        Double clockOutLongitude,

        @Size(max = 500, message = "memo must be 500 characters or less")
        String memo,

        @Size(max = 500, message = "editReason must be 500 characters or less")
        String editReason,

        @Min(value = 0, message = "attachmentCount must be 0 or greater")
        @Max(value = 20, message = "attachmentCount must be 20 or less")
        Integer attachmentCount
) {
}
