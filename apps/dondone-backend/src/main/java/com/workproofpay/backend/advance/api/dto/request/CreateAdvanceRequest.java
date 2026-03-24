package com.workproofpay.backend.advance.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record CreateAdvanceRequest(
        @NotNull(message = "workplaceId is required")
        @Positive(message = "workplaceId must be greater than 0")
        Long workplaceId,

        @NotNull(message = "requestedAmountAtomic is required")
        @Positive(message = "requestedAmountAtomic must be greater than 0")
        Long requestedAmountAtomic,

        @NotNull(message = "requestedAt is required")
        LocalDateTime requestedAt
) {
}
