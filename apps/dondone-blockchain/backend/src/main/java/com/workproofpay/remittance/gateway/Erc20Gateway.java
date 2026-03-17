package com.workproofpay.remittance.gateway;

import java.util.Optional;

public interface Erc20Gateway {
    String submitTransfer(String tokenAddress, String toAddress, long amountAtomic, String senderPrivateKey);
    Optional<TxReceiptResult> getReceipt(String txHash);
}
