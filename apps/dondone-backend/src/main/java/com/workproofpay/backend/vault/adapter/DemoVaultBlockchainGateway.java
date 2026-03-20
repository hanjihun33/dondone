package com.workproofpay.backend.vault.adapter;

import com.workproofpay.backend.remittance.adapter.DemoRemittanceBlockchainGateway;
import com.workproofpay.backend.remittance.service.WalletService;
import com.workproofpay.backend.vault.config.VaultProperties;
import com.workproofpay.backend.vault.model.VaultFailureCode;
import com.workproofpay.backend.vault.model.VaultPosition;
import com.workproofpay.backend.vault.repo.VaultPositionRepository;
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
import java.util.concurrent.atomic.AtomicReference;

@Component
@ConditionalOnProperty(name = "vault.chain.mode", havingValue = "demo", matchIfMissing = true)
public class DemoVaultBlockchainGateway implements VaultBlockchainGateway {

    private final DemoRemittanceBlockchainGateway remittanceBlockchainGateway;
    private final WalletService walletService;
    private final VaultProperties properties;
    private final VaultPositionRepository vaultPositionRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, BigInteger> shareBalances = new ConcurrentHashMap<>();
    private final Map<String, DemoVaultTxState> txStates = new ConcurrentHashMap<>();
    private final AtomicReference<BigInteger> totalShares = new AtomicReference<>(BigInteger.ZERO);
    private final AtomicReference<BigInteger> totalAssets = new AtomicReference<>(BigInteger.ZERO);

    public DemoVaultBlockchainGateway(
            DemoRemittanceBlockchainGateway remittanceBlockchainGateway,
            WalletService walletService,
            VaultProperties properties,
            VaultPositionRepository vaultPositionRepository
    ) {
        this.remittanceBlockchainGateway = remittanceBlockchainGateway;
        this.walletService = walletService;
        this.properties = properties;
        this.vaultPositionRepository = vaultPositionRepository;
    }

    @Override
    public VaultChainState getState(String walletAddress) {
        synchronizeFromPersistedPositions();
        var balances = walletService.getBalances(walletAddress);
        return new VaultChainState(
                balances.tokenBalanceAtomic(),
                balances.nativeBalanceWei(),
                shareBalances.getOrDefault(walletAddress, BigInteger.ZERO)
        );
    }

    @Override
    public BigInteger previewDeposit(BigInteger assets) {
        if (assets == null || assets.signum() <= 0) {
            throw new IllegalArgumentException("assets must be greater than 0");
        }
        synchronizeFromPersistedPositions();
        BigInteger shares = totalShares.get();
        BigInteger assetsInVault = totalAssets.get();
        if (shares.signum() == 0 || assetsInVault.signum() == 0) {
            return assets;
        }
        return assets.multiply(shares).divide(assetsInVault);
    }

    @Override
    public BigInteger previewWithdraw(BigInteger assets) {
        if (assets == null || assets.signum() <= 0) {
            throw new IllegalArgumentException("assets must be greater than 0");
        }
        synchronizeFromPersistedPositions();
        BigInteger shares = totalShares.get();
        BigInteger assetsInVault = totalAssets.get();
        if (shares.signum() == 0 || assetsInVault.signum() == 0) {
            throw new IllegalArgumentException("vault has no liquidity");
        }
        BigInteger requiredShares = assets.multiply(shares).divide(assetsInVault);
        if (requiredShares.multiply(assetsInVault).divide(shares).compareTo(assets) < 0) {
            requiredShares = requiredShares.add(BigInteger.ONE);
        }
        return requiredShares;
    }

    @Override
    public void approveAssetIfNeeded(String senderPrivateKey, BigInteger amountAtomic) {
        if (senderPrivateKey == null || senderPrivateKey.isBlank() || amountAtomic == null || amountAtomic.signum() <= 0) {
            throw new IllegalStateException("approve failed");
        }
    }

    @Override
    public PreparedVaultTransaction prepareDeposit(String senderPrivateKey, BigInteger amountAtomic, String receiverAddress) {
        String senderAddress = Credentials.create(senderPrivateKey).getAddress();
        BigInteger shareDelta = previewDeposit(amountAtomic);
        String txHash = randomTxHash();
        txStates.put(txHash, new DemoVaultTxState(
                txHash,
                VaultAction.DEPOSIT,
                senderAddress,
                receiverAddress,
                amountAtomic,
                shareDelta,
                Instant.now(),
                false,
                false
        ));
        return new PreparedVaultTransaction(txHash, encodeSignedTransaction(txHash), shareDelta);
    }

    @Override
    public PreparedVaultTransaction prepareWithdraw(String senderPrivateKey, BigInteger amountAtomic, String receiverAddress, String ownerAddress) {
        String senderAddress = Credentials.create(senderPrivateKey).getAddress();
        BigInteger shareDelta = previewWithdraw(amountAtomic);
        String txHash = randomTxHash();
        txStates.put(txHash, new DemoVaultTxState(
                txHash,
                VaultAction.WITHDRAW,
                senderAddress,
                receiverAddress,
                amountAtomic,
                shareDelta,
                Instant.now(),
                false,
                false
        ));
        return new PreparedVaultTransaction(txHash, encodeSignedTransaction(txHash), shareDelta);
    }

    @Override
    public void broadcastSignedTransaction(String signedTransaction) {
        DemoVaultTxState state = txStates.get(decodeSignedTransaction(signedTransaction));
        if (state == null || state.broadcasted()) {
            return;
        }
        synchronized (this) {
            synchronizeFromPersistedPositions();
            if (state.action() == VaultAction.DEPOSIT) {
                remittanceBlockchainGateway.debitToken(state.senderAddress(), state.amountAtomic());
                remittanceBlockchainGateway.consumeNativeGas(state.senderAddress());
                totalAssets.updateAndGet(current -> current.add(state.amountAtomic()));
                totalShares.updateAndGet(current -> current.add(state.shareDelta()));
                shareBalances.merge(state.receiverAddress(), state.shareDelta(), BigInteger::add);
            } else {
                BigInteger currentShares = shareBalances.getOrDefault(state.senderAddress(), BigInteger.ZERO);
                if (currentShares.compareTo(state.shareDelta()) < 0 || totalAssets.get().compareTo(state.amountAtomic()) < 0) {
                    throw new IllegalStateException("insufficient vault balance");
                }
                remittanceBlockchainGateway.consumeNativeGas(state.senderAddress());
                shareBalances.put(state.senderAddress(), currentShares.subtract(state.shareDelta()));
                totalAssets.updateAndGet(current -> current.subtract(state.amountAtomic()));
                totalShares.updateAndGet(current -> current.subtract(state.shareDelta()));
                remittanceBlockchainGateway.creditToken(state.receiverAddress(), state.amountAtomic());
            }
        }
        txStates.put(state.txHash(), state.withBroadcasted(true));
    }

    @Override
    public Optional<VaultReceiptResult> getReceipt(String txHash) {
        DemoVaultTxState state = txStates.get(txHash);
        if (state == null || !state.broadcasted()) {
            return Optional.empty();
        }
        if (Instant.now().isBefore(state.createdAt().plusSeconds(properties.getWorker().getReceiptPollDelaySeconds()))) {
            return Optional.empty();
        }
        return Optional.of(new VaultReceiptResult(true, null));
    }

    @Override
    public boolean isTransactionKnown(String txHash) {
        return txStates.containsKey(txHash);
    }

    public void resetState() {
        synchronized (this) {
            shareBalances.clear();
            txStates.clear();
            totalShares.set(BigInteger.ZERO);
            totalAssets.set(BigInteger.ZERO);
        }
    }

    private String encodeSignedTransaction(String txHash) {
        return "vault-demo:" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(txHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void synchronizeFromPersistedPositions() {
        synchronized (this) {
            shareBalances.clear();
            BigInteger recalculatedTotalShares = BigInteger.ZERO;
            BigInteger recalculatedTotalAssets = BigInteger.ZERO;
            for (VaultPosition position : vaultPositionRepository.findAll()) {
                shareBalances.put(position.getWalletAddress(), position.getShareBalance());
                recalculatedTotalShares = recalculatedTotalShares.add(position.getShareBalance());
                recalculatedTotalAssets = recalculatedTotalAssets.add(position.getPrincipalAmountAtomic());
            }
            totalShares.set(recalculatedTotalShares);
            totalAssets.set(recalculatedTotalAssets);
        }
    }

    private String decodeSignedTransaction(String signedTransaction) {
        if (signedTransaction == null || !signedTransaction.startsWith("vault-demo:")) {
            throw new IllegalArgumentException("invalid demo signed transaction");
        }
        String encodedPayload = signedTransaction.substring("vault-demo:".length());
        return new String(Base64.getUrlDecoder().decode(encodedPayload), java.nio.charset.StandardCharsets.UTF_8);
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

    private enum VaultAction {
        DEPOSIT,
        WITHDRAW
    }

    private record DemoVaultTxState(
            String txHash,
            VaultAction action,
            String senderAddress,
            String receiverAddress,
            BigInteger amountAtomic,
            BigInteger shareDelta,
            Instant createdAt,
            boolean fail,
            boolean broadcasted
    ) {
        private DemoVaultTxState withBroadcasted(boolean nextBroadcasted) {
            return new DemoVaultTxState(
                    txHash,
                    action,
                    senderAddress,
                    receiverAddress,
                    amountAtomic,
                    shareDelta,
                    createdAt,
                    fail,
                    nextBroadcasted
            );
        }
    }
}
