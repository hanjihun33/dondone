package com.workproofpay.backend.remittance.adapter;

import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.TransferFailureCode;
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

    private final RemittanceProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, BigInteger> tokenBalances = new ConcurrentHashMap<>();
    private final Map<String, BigInteger> nativeBalances = new ConcurrentHashMap<>();
    private final Map<String, DemoTxState> txStates = new ConcurrentHashMap<>();

    public DemoRemittanceBlockchainGateway(RemittanceProperties properties) {
        this.properties = properties;
    }

    @Override
    public ChainBalanceSnapshot getBalances(String walletAddress) {
        return new ChainBalanceSnapshot(
                tokenBalances.getOrDefault(walletAddress, BigInteger.ZERO),
                nativeBalances.getOrDefault(walletAddress, BigInteger.ZERO)
        );
    }

    @Override
    public void fundWallet(String walletAddress) {
        tokenBalances.merge(walletAddress, BigInteger.valueOf(properties.getTreasury().getInitialTokenAmountAtomic()), BigInteger::add);
        nativeBalances.merge(walletAddress, new BigInteger(properties.getTreasury().getInitialNativeAmountWei()), BigInteger::add);
    }

    @Override
    public PreparedTokenTransfer prepareTokenTransfer(String senderPrivateKey, String toAddress, BigInteger amountAtomic) {
        String senderAddress = Credentials.create(senderPrivateKey).getAddress();
        BigInteger senderToken = tokenBalances.getOrDefault(senderAddress, BigInteger.ZERO);
        BigInteger senderNative = nativeBalances.getOrDefault(senderAddress, BigInteger.ZERO);
        if (senderToken.compareTo(amountAtomic) < 0 || senderNative.signum() <= 0) {
            throw new IllegalStateException("insufficient demo balance");
        }

        String txHash = randomTxHash();
        boolean fail = amountAtomic.mod(BigInteger.valueOf(17L)).signum() == 0;
        txStates.put(txHash, new DemoTxState(txHash, senderAddress, toAddress, amountAtomic, Instant.now(), fail, false));
        String signedTransaction = encodeSignedTransaction(txHash, senderAddress, toAddress, amountAtomic);
        return new PreparedTokenTransfer(txHash, signedTransaction);
    }

    @Override
    public void broadcastSignedTransaction(String signedTransaction) {
        DemoPreparedTransfer preparedTransfer = decodeSignedTransaction(signedTransaction);
        DemoTxState state = txStates.get(preparedTransfer.txHash());
        if (state == null || state.broadcasted()) {
            return;
        }

        if (!state.fail()) {
            BigInteger senderToken = tokenBalances.getOrDefault(state.senderAddress(), BigInteger.ZERO);
            BigInteger senderNative = nativeBalances.getOrDefault(state.senderAddress(), BigInteger.ZERO);
            if (senderToken.compareTo(state.amountAtomic()) < 0 || senderNative.signum() <= 0) {
                throw new IllegalStateException("insufficient demo balance");
            }
            tokenBalances.put(state.senderAddress(), senderToken.subtract(state.amountAtomic()));
            nativeBalances.put(state.senderAddress(), senderNative.subtract(BigInteger.ONE));
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
    }

    @Override
    public Optional<ChainReceiptResult> getReceipt(String txHash) {
        DemoTxState state = txStates.get(txHash);
        if (state == null || !state.broadcasted()) {
            return Optional.empty();
        }
        if (Instant.now().isBefore(state.createdAt().plusSeconds(properties.getWorker().getReceiptPollDelaySeconds()))) {
            return Optional.empty();
        }
        if (state.fail()) {
            return Optional.of(new ChainReceiptResult(false, TransferFailureCode.NETWORK_ERROR));
        }
        return Optional.of(new ChainReceiptResult(true, null));
    }

    @Override
    public boolean isTransactionKnown(String txHash) {
        return txStates.containsKey(txHash);
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
