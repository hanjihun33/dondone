package com.workproofpay.backend.advance.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record CreateAdvanceRequest(
        @NotNull(message = "workplaceId is required")
        @Positive(message = "workplaceId must be greater than 0")
        Long workplaceId,

        @NotNull(message = "requestedAmount is required")
        @Positive(message = "requestedAmount must be greater than 0")
        Long requestedAmount,

        @NotNull(message = "requestedAt is required")
        LocalDateTime requestedAt
) {
}
