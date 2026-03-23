package com.workproofpay.backend.vault.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateVaultTransactionRequest(
        @NotNull(message = "amountAtomic is required")
        @Positive(message = "amountAtomic must be greater than 0")
        Long amountAtomic
) {
}
