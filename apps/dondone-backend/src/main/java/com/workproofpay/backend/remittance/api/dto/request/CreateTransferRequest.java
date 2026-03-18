package com.workproofpay.backend.remittance.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTransferRequest(
        @NotBlank(message = "recipientId is required")
        String recipientId,
        @NotNull(message = "amountAtomic is required")
        @Positive(message = "amountAtomic must be greater than 0")
        Long amountAtomic,
        boolean highAmountConfirmed,
        boolean recentRecipientConfirmed
) {
}
