package com.workproofpay.backend.vault.api.dto.response;

import java.util.List;

public record VaultTransactionListResponse(
        List<VaultTransactionItemResponse> transactions
) {
}
