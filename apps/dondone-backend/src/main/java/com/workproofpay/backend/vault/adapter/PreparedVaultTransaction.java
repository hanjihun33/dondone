package com.workproofpay.backend.vault.adapter;

import java.math.BigInteger;

public record PreparedVaultTransaction(
        String txHash,
        String signedTransaction,
        BigInteger shareDelta
) {
}
