package com.workproofpay.remittance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TransferPrecheckRequest(
        @NotBlank String recipientId,
        @Positive long amount,
        @NotBlank String asset,
        boolean highAmountConfirmed
) {}
