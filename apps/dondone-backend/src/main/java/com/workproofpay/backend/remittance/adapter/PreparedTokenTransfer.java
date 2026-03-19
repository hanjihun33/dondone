package com.workproofpay.backend.remittance.adapter;

public record PreparedTokenTransfer(
        String txHash,
        String signedTransaction
) {
}
