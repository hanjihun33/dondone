package com.workproofpay.backend.vault.adapter;

import com.workproofpay.backend.vault.model.VaultFailureCode;

public record VaultReceiptResult(
        boolean success,
        VaultFailureCode failureCode
) {
}
