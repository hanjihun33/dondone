package com.workproofpay.backend.vault.adapter;

import java.math.BigInteger;

public record VaultChainState(
        BigInteger walletTokenBalanceAtomic,
        BigInteger walletNativeBalanceWei,
        BigInteger vaultShareBalance
) {
}
