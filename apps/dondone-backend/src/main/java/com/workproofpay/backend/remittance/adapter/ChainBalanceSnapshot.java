package com.workproofpay.backend.remittance.adapter;

import java.math.BigInteger;

public record ChainBalanceSnapshot(
        BigInteger tokenBalanceAtomic,
        BigInteger nativeBalanceWei
) {
}
