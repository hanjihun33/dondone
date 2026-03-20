package com.workproofpay.backend.remittance.service;

import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.remittance.adapter.ChainBalanceSnapshot;
import com.workproofpay.backend.remittance.adapter.RemittanceBlockchainGateway;
import com.workproofpay.backend.remittance.api.dto.response.WalletBalanceResponse;
import com.workproofpay.backend.remittance.api.dto.response.WalletResponse;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.UserWallet;
import com.workproofpay.backend.remittance.model.WalletFundingStatus;
import com.workproofpay.backend.remittance.repo.UserWalletRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.math.BigInteger;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserWalletRepository userWalletRepository;
    private final UserRepository userRepository;
    private final WalletCryptoService walletCryptoService;
    private final RemittanceBlockchainGateway blockchainGateway;
    private final RemittanceProperties properties;
    private final RemittanceMetrics remittanceMetrics;

    public WalletCreateResult createWalletIfAbsent(Long userId) {
        UserWallet existing = userWalletRepository.findById(userId).orElse(null);
        if (existing != null) {
            if (existing.getFundingStatus() == WalletFundingStatus.FUNDED) {
                return new WalletCreateResult(false, toResponse(existing));
            }
            if (hasSeedFunding(existing)) {
                return new WalletCreateResult(false, toResponse(markWalletFunded(existing.getUserId())));
            }
            if (existing.getFundingStatus() == WalletFundingStatus.PENDING) {
                return new WalletCreateResult(false, toResponse(existing));
            }
        }

        WalletRecordResult walletResult = existing == null
                ? createWalletRecord(userId)
                : new WalletRecordResult(markWalletFundingPending(existing.getUserId()), false);

        return new WalletCreateResult(
                walletResult.created(),
                toResponse(ensureWalletFunded(walletResult.wallet(), "Failed to create and fund wallet"))
        );
    }

    public WalletResponse recoverWalletFunding(Long userId) {
        UserWallet wallet = getRequiredWallet(userId);
        if (wallet.getFundingStatus() == WalletFundingStatus.FUNDED) {
            return toResponse(wallet);
        }
        if (hasSeedFunding(wallet)) {
            return toResponse(markWalletFunded(userId));
        }
        if (wallet.getFundingStatus() == WalletFundingStatus.PENDING && !isFundingPendingStale(wallet)) {
            throw new ApiException(
                    ErrorCode.RECOVERY_ACTION_NOT_ALLOWED,
                    "Wallet funding is still pending"
            );
        }

        UserWallet pendingWallet = markWalletFundingPending(userId);
        return toResponse(ensureWalletFunded(pendingWallet, "Failed to recover wallet funding"));
    }

    @Transactional
    public WalletRecordResult createWalletRecord(Long userId) {
        UserWallet existing = userWalletRepository.findById(userId).orElse(null);
        if (existing != null) {
            return new WalletRecordResult(existing, false);
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        try {
            ECKeyPair keyPair = Keys.createEcKeyPair();
            String privateKey = toHexPrivateKey(keyPair.getPrivateKey());
            String walletAddress = Credentials.create(privateKey).getAddress().toLowerCase();
            return new WalletRecordResult(userWalletRepository.saveAndFlush(
                    UserWallet.create(userId, walletAddress, walletCryptoService.encrypt(privateKey))
            ), true);
        } catch (ApiException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            return new WalletRecordResult(getRequiredWallet(userId), false);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.WALLET_FUNDING_FAILED, "Failed to create wallet");
        }
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId) {
        return toResponse(getRequiredWallet(userId));
    }

    @Transactional(readOnly = true)
    public WalletBalanceResponse getWalletBalance(Long userId) {
        UserWallet wallet = getRequiredWallet(userId);
        ChainBalanceSnapshot balanceSnapshot = blockchainGateway.getBalances(wallet.getWalletAddress());
        return toBalanceResponse(wallet.getWalletAddress(), balanceSnapshot);
    }

    @Transactional(readOnly = true)
    public UserWallet getRequiredWallet(Long userId) {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "success";
        try {
            return userWalletRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.WALLET_NOT_FOUND));
        } catch (RuntimeException e) {
            outcome = "error";
            throw e;
        } finally {
            remittanceMetrics.recordWalletLookup(sample, "read", outcome);
        }
    }

    @Transactional
    public UserWallet getRequiredWalletForUpdate(Long userId) {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "success";
        try {
            return userWalletRepository.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.WALLET_NOT_FOUND));
        } catch (RuntimeException e) {
            outcome = "error";
            throw e;
        } finally {
            remittanceMetrics.recordWalletLookup(sample, "for_update", outcome);
        }
    }

    @Transactional
    public UserWallet getOrCreateWallet(Long userId) {
        createWalletIfAbsent(userId);
        return getRequiredWallet(userId);
    }

    @Transactional(readOnly = true)
    public String getDecryptedPrivateKey(Long userId) {
        return walletCryptoService.decrypt(getRequiredWallet(userId).getEncryptedPrivateKey());
    }

    @Transactional(readOnly = true)
    public ChainBalanceSnapshot getBalances(String walletAddress) {
        return blockchainGateway.getBalances(walletAddress);
    }

    @Transactional(readOnly = true)
    public BigInteger estimateTransferGasCostWei() {
        return blockchainGateway.estimateTokenTransferGasCostWei();
    }

    protected UserWallet markWalletFunded(Long userId) {
        UserWallet wallet = getRequiredWallet(userId);
        wallet.markFunded();
        return userWalletRepository.save(wallet);
    }

    protected UserWallet markWalletFundingPending(Long userId) {
        UserWallet wallet = getRequiredWallet(userId);
        wallet.markFundingPending();
        return userWalletRepository.save(wallet);
    }

    protected void markWalletFundingFailed(Long userId, String failureReason) {
        UserWallet wallet = getRequiredWallet(userId);
        wallet.markFundingFailed(failureReason);
        userWalletRepository.save(wallet);
    }

    public WalletBalanceResponse toBalanceResponse(String walletAddress, ChainBalanceSnapshot balanceSnapshot) {
        return new WalletBalanceResponse(
                walletAddress,
                properties.getPolicy().getAssetSymbol(),
                properties.getPolicy().getAssetDecimals(),
                balanceSnapshot.tokenBalanceAtomic().toString(),
                balanceSnapshot.nativeBalanceWei().toString()
        );
    }

    private WalletResponse toResponse(UserWallet wallet) {
        return new WalletResponse(
                wallet.getUserId(),
                wallet.getWalletAddress(),
                wallet.getFundingStatus(),
                wallet.getFundingFailureReason(),
                wallet.getFundedAt(),
                wallet.getCreatedAt()
        );
    }

    private String toHexPrivateKey(BigInteger privateKey) {
        return String.format("%064x", privateKey);
    }

    private String summarizeFundingFailure(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() > 300 ? normalized.substring(0, 300) : normalized;
    }

    private boolean hasSeedFunding(UserWallet wallet) {
        return calculateFundingShortfall(wallet.getWalletAddress()).isZero();
    }

    private boolean isFundingPendingStale(UserWallet wallet) {
        return Duration.between(wallet.getUpdatedAt(), java.time.LocalDateTime.now()).getSeconds()
                >= properties.getWallet().getFundingPendingStaleSeconds();
    }

    private UserWallet ensureWalletFunded(UserWallet wallet, String failureMessage) {
        try {
            FundingShortfall fundingShortfall = calculateFundingShortfall(wallet.getWalletAddress());
            if (!fundingShortfall.isZero()) {
                blockchainGateway.fundWallet(
                        wallet.getWalletAddress(),
                        fundingShortfall.tokenAmountAtomic(),
                        fundingShortfall.nativeAmountWei()
                );
            }
            return markWalletFunded(wallet.getUserId());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            markWalletFundingFailed(wallet.getUserId(), summarizeFundingFailure(e));
            throw new ApiException(ErrorCode.WALLET_FUNDING_FAILED, failureMessage);
        }
    }

    public record WalletRecordResult(UserWallet wallet, boolean created) {
    }

    public record WalletCreateResult(boolean created, WalletResponse response) {
    }

    private FundingShortfall calculateFundingShortfall(String walletAddress) {
        ChainBalanceSnapshot balanceSnapshot = blockchainGateway.getBalances(walletAddress);
        BigInteger requiredTokenAmountAtomic = BigInteger.valueOf(properties.getTreasury().getInitialTokenAmountAtomic());
        BigInteger requiredNativeAmountWei = new BigInteger(properties.getTreasury().getInitialNativeAmountWei());
        BigInteger missingTokenAmountAtomic = requiredTokenAmountAtomic.subtract(balanceSnapshot.tokenBalanceAtomic()).max(BigInteger.ZERO);
        BigInteger missingNativeAmountWei = requiredNativeAmountWei.subtract(balanceSnapshot.nativeBalanceWei()).max(BigInteger.ZERO);
        return new FundingShortfall(missingTokenAmountAtomic, missingNativeAmountWei);
    }

    private record FundingShortfall(
            BigInteger tokenAmountAtomic,
            BigInteger nativeAmountWei
    ) {
        private boolean isZero() {
            return tokenAmountAtomic.signum() == 0 && nativeAmountWei.signum() == 0;
        }
    }
}
