package com.workproofpay.remittance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Remittance remittance = new Remittance();
    private Worker worker = new Worker();
    private Chain chain = new Chain();
    private Wallet wallet = new Wallet();

    public Remittance getRemittance() {
        return remittance;
    }

    public void setRemittance(Remittance remittance) {
        this.remittance = remittance;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public Chain getChain() {
        return chain;
    }

    public void setChain(Chain chain) {
        this.chain = chain;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public static class Remittance {
        private long highAmountThreshold = 100000;
        private long cooldownSeconds = 86400;
        private long receiptLinkTtlSeconds = 900;

        public long getHighAmountThreshold() {
            return highAmountThreshold;
        }

        public void setHighAmountThreshold(long highAmountThreshold) {
            this.highAmountThreshold = highAmountThreshold;
        }

        public long getCooldownSeconds() {
            return cooldownSeconds;
        }

        public void setCooldownSeconds(long cooldownSeconds) {
            this.cooldownSeconds = cooldownSeconds;
        }

        public long getReceiptLinkTtlSeconds() {
            return receiptLinkTtlSeconds;
        }

        public void setReceiptLinkTtlSeconds(long receiptLinkTtlSeconds) {
            this.receiptLinkTtlSeconds = receiptLinkTtlSeconds;
        }
    }

    public static class Worker {
        private long pollIntervalMs = 2000;
        private long receiptConfirmDelaySeconds = 5;

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public long getReceiptConfirmDelaySeconds() {
            return receiptConfirmDelaySeconds;
        }

        public void setReceiptConfirmDelaySeconds(long receiptConfirmDelaySeconds) {
            this.receiptConfirmDelaySeconds = receiptConfirmDelaySeconds;
        }
    }

    public static class Chain {
        private String mode = "demo";
        private String rpcUrl = "";
        private long chainId = 11155111;
        private String tokenAddress = "";
        private long gasLimit = 120000;

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

        public long getGasLimit() {
            return gasLimit;
        }

        public void setGasLimit(long gasLimit) {
            this.gasLimit = gasLimit;
        }
    }

    public static class Wallet {
        private String encryptionKey = "";

        public String getEncryptionKey() {
            return encryptionKey;
        }

        public void setEncryptionKey(String encryptionKey) {
            this.encryptionKey = encryptionKey;
        }
    }
}
