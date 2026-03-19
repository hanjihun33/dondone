package com.workproofpay.backend.remittance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "remittance")
public class RemittanceProperties {

    private final Policy policy = new Policy();
    private final Worker worker = new Worker();
    private final Chain chain = new Chain();
    private final Wallet wallet = new Wallet();
    private final Treasury treasury = new Treasury();

    public Policy getPolicy() {
        return policy;
    }

    public Worker getWorker() {
        return worker;
    }

    public Chain getChain() {
        return chain;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Treasury getTreasury() {
        return treasury;
    }

    public static class Policy {
        private long highAmountThresholdAtomic = 100_000_000L;
        private long recentRecipientWindowSeconds = 86_400L;
        private int maxAutoRetryCount = 2;
        private int defaultListLimit = 20;
        private String assetSymbol = "dUSDC";
        private int assetDecimals = 6;

        public long getHighAmountThresholdAtomic() {
            return highAmountThresholdAtomic;
        }

        public void setHighAmountThresholdAtomic(long highAmountThresholdAtomic) {
            this.highAmountThresholdAtomic = highAmountThresholdAtomic;
        }

        public long getRecentRecipientWindowSeconds() {
            return recentRecipientWindowSeconds;
        }

        public void setRecentRecipientWindowSeconds(long recentRecipientWindowSeconds) {
            this.recentRecipientWindowSeconds = recentRecipientWindowSeconds;
        }

        public int getMaxAutoRetryCount() {
            return maxAutoRetryCount;
        }

        public void setMaxAutoRetryCount(int maxAutoRetryCount) {
            this.maxAutoRetryCount = maxAutoRetryCount;
        }

        public int getDefaultListLimit() {
            return defaultListLimit;
        }

        public void setDefaultListLimit(int defaultListLimit) {
            this.defaultListLimit = defaultListLimit;
        }

        public String getAssetSymbol() {
            return assetSymbol;
        }

        public void setAssetSymbol(String assetSymbol) {
            this.assetSymbol = assetSymbol;
        }

        public int getAssetDecimals() {
            return assetDecimals;
        }

        public void setAssetDecimals(int assetDecimals) {
            this.assetDecimals = assetDecimals;
        }
    }

    public static class Worker {
        private long pollIntervalMs = 2_000L;
        private long receiptPollDelaySeconds = 2L;
        private long receiptTimeoutSeconds = 300L;

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public long getReceiptPollDelaySeconds() {
            return receiptPollDelaySeconds;
        }

        public void setReceiptPollDelaySeconds(long receiptPollDelaySeconds) {
            this.receiptPollDelaySeconds = receiptPollDelaySeconds;
        }

        public long getReceiptTimeoutSeconds() {
            return receiptTimeoutSeconds;
        }

        public void setReceiptTimeoutSeconds(long receiptTimeoutSeconds) {
            this.receiptTimeoutSeconds = receiptTimeoutSeconds;
        }
    }

    public static class Chain {
        private String mode = "sepolia";
        private String rpcUrl = "";
        private long chainId = 11155111L;
        private String tokenAddress = "";
        private long tokenGasLimit = 120_000L;
        private long nativeTransferGasLimit = 21_000L;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getRpcUrl() {
            return rpcUrl;
        }

        public void setRpcUrl(String rpcUrl) {
            this.rpcUrl = rpcUrl;
        }

        public long getChainId() {
            return chainId;
        }

        public void setChainId(long chainId) {
            this.chainId = chainId;
        }

        public String getTokenAddress() {
            return tokenAddress;
        }

        public void setTokenAddress(String tokenAddress) {
            this.tokenAddress = tokenAddress;
        }

        public long getTokenGasLimit() {
            return tokenGasLimit;
        }

        public void setTokenGasLimit(long tokenGasLimit) {
            this.tokenGasLimit = tokenGasLimit;
        }

        public long getNativeTransferGasLimit() {
            return nativeTransferGasLimit;
        }

        public void setNativeTransferGasLimit(long nativeTransferGasLimit) {
            this.nativeTransferGasLimit = nativeTransferGasLimit;
        }
    }

    public static class Wallet {
        private String encryptionKey = "";
        private long fundingReceiptTimeoutSeconds = 60L;
        private long fundingPendingStaleSeconds = 300L;

        public String getEncryptionKey() {
            return encryptionKey;
        }

        public void setEncryptionKey(String encryptionKey) {
            this.encryptionKey = encryptionKey;
        }

        public long getFundingReceiptTimeoutSeconds() {
            return fundingReceiptTimeoutSeconds;
        }

        public void setFundingReceiptTimeoutSeconds(long fundingReceiptTimeoutSeconds) {
            this.fundingReceiptTimeoutSeconds = fundingReceiptTimeoutSeconds;
        }

        public long getFundingPendingStaleSeconds() {
            return fundingPendingStaleSeconds;
        }

        public void setFundingPendingStaleSeconds(long fundingPendingStaleSeconds) {
            this.fundingPendingStaleSeconds = fundingPendingStaleSeconds;
        }
    }

    public static class Treasury {
        private String privateKey = "";
        private long initialTokenAmountAtomic = 500_000_000L;
        private String initialNativeAmountWei = "10000000000000000";

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public long getInitialTokenAmountAtomic() {
            return initialTokenAmountAtomic;
        }

        public void setInitialTokenAmountAtomic(long initialTokenAmountAtomic) {
            this.initialTokenAmountAtomic = initialTokenAmountAtomic;
        }

        public String getInitialNativeAmountWei() {
            return initialNativeAmountWei;
        }

        public void setInitialNativeAmountWei(String initialNativeAmountWei) {
            this.initialNativeAmountWei = initialNativeAmountWei;
        }
    }
}
