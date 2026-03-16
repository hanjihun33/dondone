package com.workproofpay.backend.workproof.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ListWorkProofRecordsQuery(
        @NotBlank(message = "month is required")
        @Size(min = 7, max = 7, message = "month must be exactly 7 characters")
        @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "month must follow YYYY-MM")
        String month,

        @NotNull(message = "workplaceId is required")
        @Positive(message = "workplaceId must be greater than 0")
        Long workplaceId
) {
}
