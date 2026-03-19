package com.workproofpay.backend.remittance.service;

import com.workproofpay.backend.remittance.api.dto.response.CreateTransferResponse;

public record TransferCreateResult(
        boolean replayed,
        CreateTransferResponse response
) {
}
