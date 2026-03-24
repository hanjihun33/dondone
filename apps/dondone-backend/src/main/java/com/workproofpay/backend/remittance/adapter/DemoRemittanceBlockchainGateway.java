package com.workproofpay.backend.remittance.adapter;

import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.TransferFailureCode;
import com.workproofpay.backend.remittance.service.RemittanceMetrics;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "remittance.chain.mode", havingValue = "demo")
public class DemoRemittanceBlockchainGateway implements RemittanceBlockchainGateway {

    private static final BigInteger DEMO_NETWORK_FEE_WEI = BigInteger.valueOf(21_000L)
            .multiply(BigInteger.valueOf(1_000_000_000L));

    private final RemittanceProperties properties;
    private final RemittanceMetrics remittanceMetrics;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, BigInteger> tokenBalances = new ConcurrentHashMap<>();
    private final Map<String, BigInteger> nativeBalances = new ConcurrentHashMap<>();
    private final Map<String, DemoTxState> txStates = new ConcurrentHashMap<>();

    public DemoRemittanceBlockchainGateway(RemittanceProperties properties, RemittanceMetrics remittanceMetrics) {
        this.properties = properties;
        this.remittanceMetrics = remittanceMetrics;
    }

    @Override
    public ChainBalanceSnapshot getBalances(String walletAddress) {
        Timer.Sample sample = remittanceMetrics.start();
        try {
            return new ChainBalanceSnapshot(
                    tokenBalances.getOrDefault(walletAddress, BigInteger.ZERO),
                    nativeBalances.getOrDefault(walletAddress, BigInteger.ZERO)
            );
        } finally {
            remittanceMetrics.recordChainOperation(sample, properties.getChain().getMode(), "get_balances", "success");
        }
    }

    @Override
    public void fundWallet(String walletAddress, BigInteger tokenAmountAtomic, BigInteger nativeAmountWei) {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "success";
        try {
            if (tokenAmountAtomic.signum() > 0) {
                tokenBalances.merge(walletAddress, tokenAmountAtomic, BigInteger::add);
            }
            if (nativeAmountWei.signum() > 0) {
                nativeBalances.merge(walletAddress, nativeAmountWei, BigInteger::add);
            }
        } catch (RuntimeException e) {
            outcome = "error";
            throw e;
        } finally {
            remittanceMetrics.recordChainOperation(sample, properties.getChain().getMode(), "fund_wallet", outcome);
        }
    }

    public void debitToken(String walletAddress, BigInteger amountAtomic) {
        BigInteger current = tokenBalances.getOrDefault(walletAddress, BigInteger.ZERO);
        if (current.compareTo(amountAtomic) < 0) {
            throw new IllegalStateException("insufficient demo token balance");
        }
        tokenBalances.put(walletAddress, current.subtract(amountAtomic));
    }

    public void creditToken(String walletAddress, BigInteger amountAtomic) {
        tokenBalances.merge(walletAddress, amountAtomic, BigInteger::add);
    }

    public void consumeNativeGas(String walletAddress) {
        BigInteger current = nativeBalances.getOrDefault(walletAddress, BigInteger.ZERO);
        if (current.signum() <= 0) {
            throw new IllegalStateException("insufficient demo native balance");
        }
        nativeBalances.put(walletAddress, current.subtract(BigInteger.ONE));
    }

    @Override
    public BigInteger estimateTokenTransferGasCostWei() {
        Timer.Sample sample = remittanceMetrics.start();
        try {
            return BigInteger.ONE;
        } finally {
            remittanceMetrics.recordChainOperation(sample, properties.getChain().getMode(), "estimate_transfer_gas", "success");
        }
    }

    @Override
    public PreparedTokenTransfer prepareTokenTransfer(String senderPrivateKey, String toAddress, BigInteger amountAtomic) {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "success";
        try {
            String senderAddress = Credentials.create(senderPrivateKey).getAddress();
            BigInteger senderToken = tokenBalances.getOrDefault(senderAddress, BigInteger.ZERO);
            BigInteger senderNative = nativeBalances.getOrDefault(senderAddress, BigInteger.ZERO);
            if (senderToken.compareTo(amountAtomic) < 0 || senderNative.signum() <= 0) {
                outcome = "insufficient_balance";
                throw new IllegalStateException("insufficient demo balance");
            }

            String txHash = randomTxHash();
            boolean fail = amountAtomic.mod(BigInteger.valueOf(17L)).signum() == 0;
            txStates.put(txHash, new DemoTxState(txHash, senderAddress, toAddress, amountAtomic, Instant.now(), fail, false));
            String signedTransaction = encodeSignedTransaction(txHash, senderAddress, toAddress, amountAtomic);
            return new PreparedTokenTransfer(txHash, signedTransaction);
        } catch (RuntimeException e) {
            if ("success".equals(outcome)) {
                outcome = "error";
            }
            throw e;
        } finally {
            remittanceMetrics.recordChainOperation(sample, properties.getChain().getMode(), "prepare_transfer", outcome);
        }
    }

    @Override
    public void broadcastSignedTransaction(String signedTransaction) {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "success";
        try {
            DemoPreparedTransfer preparedTransfer = decodeSignedTransaction(signedTransaction);
            DemoTxState state = txStates.get(preparedTransfer.txHash());
            if (state == null || state.broadcasted()) {
                outcome = "noop";
                return;
            }

            if (!state.fail()) {
                BigInteger senderToken = tokenBalances.getOrDefault(state.senderAddress(), BigInteger.ZERO);
                BigInteger senderNative = nativeBalances.getOrDefault(state.senderAddress(), BigInteger.ZERO);
                if (senderToken.compareTo(state.amountAtomic()) < 0 || senderNative.signum() <= 0) {
                    outcome = "insufficient_balance";
                    throw new IllegalStateException("insufficient demo balance");
                }
                tokenBalances.put(state.senderAddress(), senderToken.subtract(state.amountAtomic()));
                nativeBalances.put(state.senderAddress(), senderNative.subtract(DEMO_NETWORK_FEE_WEI));
                tokenBalances.merge(state.recipientAddress(), state.amountAtomic(), BigInteger::add);
            }

            txStates.put(
                    state.txHash(),
                    new DemoTxState(
                            state.txHash(),
                            state.senderAddress(),
                            state.recipientAddress(),
                            state.amountAtomic(),
                            state.createdAt(),
                            state.fail(),
                            true
                    )
            );
        } catch (RuntimeException e) {
            if ("success".equals(outcome)) {
                outcome = "error";
            }
            throw e;
        } finally {
            remittanceMetrics.recordChainOperation(sample, properties.getChain().getMode(), "broadcast_signed_transaction", outcome);
        }
    }

    @Override
    public Optional<ChainReceiptResult> getReceipt(String txHash) {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "empty";
        try {
            DemoTxState state = txStates.get(txHash);
            if (state == null || !state.broadcasted()) {
                return Optional.empty();
            }
            if (Instant.now().isBefore(state.createdAt().plusSeconds(properties.getWorker().getReceiptPollDelaySeconds()))) {
                return Optional.empty();
            }
            if (state.fail()) {
                outcome = "failed";
                return Optional.of(new ChainReceiptResult(false, TransferFailureCode.NETWORK_ERROR, DEMO_NETWORK_FEE_WEI.toString()));
            }
            outcome = "success";
            return Optional.of(new ChainReceiptResult(true, null, DEMO_NETWORK_FEE_WEI.toString()));
        } finally {
            remittanceMetrics.recordChainOperation(sample, properties.getChain().getMode(), "get_receipt", outcome);
        }
    }

    @Override
    public boolean isTransactionKnown(String txHash) {
        Timer.Sample sample = remittanceMetrics.start();
        try {
            return txStates.containsKey(txHash);
        } finally {
            remittanceMetrics.recordChainOperation(sample, properties.getChain().getMode(), "is_transaction_known", "success");
        }
    }

    private String encodeSignedTransaction(String txHash, String senderAddress, String toAddress, BigInteger amountAtomic) {
        String payload = String.join("|", txHash, senderAddress, toAddress, amountAtomic.toString());
        return "demo:" + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private DemoPreparedTransfer decodeSignedTransaction(String signedTransaction) {
        if (signedTransaction == null || !signedTransaction.startsWith("demo:")) {
            throw new IllegalArgumentException("invalid demo signed transaction");
        }
        String encodedPayload = signedTransaction.substring("demo:".length());
        String payload = new String(
                Base64.getUrlDecoder().decode(encodedPayload),
                java.nio.charset.StandardCharsets.UTF_8
        );
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("invalid demo signed transaction");
        }
        return new DemoPreparedTransfer(parts[0], parts[1], parts[2], new BigInteger(parts[3]));
    }

    private String randomTxHash() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        StringBuilder builder = new StringBuilder("0x");
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private record DemoPreparedTransfer(
            String txHash,
            String senderAddress,
            String recipientAddress,
            BigInteger amountAtomic
    ) {
    }

    private record DemoTxState(
            String txHash,
            String senderAddress,
            String recipientAddress,
            BigInteger amountAtomic,
            Instant createdAt,
            boolean fail,
            boolean broadcasted
    ) {
    }
}
