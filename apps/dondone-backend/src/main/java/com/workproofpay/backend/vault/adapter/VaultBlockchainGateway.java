package com.workproofpay.backend.vault.adapter;

import java.math.BigInteger;
import java.util.Optional;

public interface VaultBlockchainGateway {
    VaultChainState getState(String walletAddress);
    BigInteger previewDeposit(BigInteger assets);
    BigInteger previewWithdraw(BigInteger assets);
    void approveAssetIfNeeded(String senderPrivateKey, BigInteger amountAtomic);
    PreparedVaultTransaction prepareDeposit(String senderPrivateKey, BigInteger amountAtomic, String receiverAddress);
    PreparedVaultTransaction prepareWithdraw(String senderPrivateKey, BigInteger amountAtomic, String receiverAddress, String ownerAddress);
    void broadcastSignedTransaction(String signedTransaction);
    Optional<VaultReceiptResult> getReceipt(String txHash);
    boolean isTransactionKnown(String txHash);
}
