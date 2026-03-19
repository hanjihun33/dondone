package com.workproofpay.backend.remittance.adapter;

import java.math.BigInteger;
import java.util.Optional;

public interface RemittanceBlockchainGateway {
    ChainBalanceSnapshot getBalances(String walletAddress);
    void fundWallet(String walletAddress, BigInteger tokenAmountAtomic, BigInteger nativeAmountWei);
    BigInteger estimateTokenTransferGasCostWei();
    PreparedTokenTransfer prepareTokenTransfer(String senderPrivateKey, String toAddress, BigInteger amountAtomic);
    void broadcastSignedTransaction(String signedTransaction);
    boolean isTransactionKnown(String txHash);
    Optional<ChainReceiptResult> getReceipt(String txHash);
}
