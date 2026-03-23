package com.workproofpay.backend.vault.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vault")
public class VaultProperties {

    private final Policy policy = new Policy();
    private final Worker worker = new Worker();
    private final Chain chain = new Chain();

    public Policy getPolicy() {
        return policy;
    }

    public Worker getWorker() {
        return worker;
    }

    public Chain getChain() {
        return chain;
    }

    public static class Policy {
        private String assetSymbol = "dUSDC";
        private int assetDecimals = 6;
        private int apyBps = 500;
        private int defaultListLimit = 20;
        private String disclaimer = "Vault yield numbers are demo estimates on testnet and do not guarantee real profit.";

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

        public int getApyBps() {
            return apyBps;
        }

        public void setApyBps(int apyBps) {
            this.apyBps = apyBps;
        }

        public int getDefaultListLimit() {
            return defaultListLimit;
        }

        public void setDefaultListLimit(int defaultListLimit) {
            this.defaultListLimit = defaultListLimit;
        }

        public String getDisclaimer() {
            return disclaimer;
        }

        public void setDisclaimer(String disclaimer) {
            this.disclaimer = disclaimer;
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
        private String mode = "demo";
        private String rpcUrl = "";
        private long chainId = 11155111L;
        private String tokenAddress = "";
        private String vaultAddress = "";
        private long tokenApproveGasLimit = 100_000L;
        private long vaultGasLimit = 200_000L;
        private long approvalReceiptTimeoutSeconds = 60L;
        private String network = "sepolia";

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

        public String getVaultAddress() {
            return vaultAddress;
        }

        public void setVaultAddress(String vaultAddress) {
            this.vaultAddress = vaultAddress;
        }

        public long getTokenApproveGasLimit() {
            return tokenApproveGasLimit;
        }

        public void setTokenApproveGasLimit(long tokenApproveGasLimit) {
            this.tokenApproveGasLimit = tokenApproveGasLimit;
        }

        public long getVaultGasLimit() {
            return vaultGasLimit;
        }

        public void setVaultGasLimit(long vaultGasLimit) {
            this.vaultGasLimit = vaultGasLimit;
        }

        public long getApprovalReceiptTimeoutSeconds() {
            return approvalReceiptTimeoutSeconds;
        }

        public void setApprovalReceiptTimeoutSeconds(long approvalReceiptTimeoutSeconds) {
            this.approvalReceiptTimeoutSeconds = approvalReceiptTimeoutSeconds;
        }

        public String getNetwork() {
            return network;
        }

        public void setNetwork(String network) {
            this.network = network;
        }
    }
}
