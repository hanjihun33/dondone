package com.workproofpay.backend.vault.service;

import com.workproofpay.backend.vault.api.dto.response.CreateVaultTransactionResponse;

public record VaultCreateResult(
        boolean replayed,
        CreateVaultTransactionResponse response
) {
}
