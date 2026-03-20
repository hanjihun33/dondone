package com.workproofpay.backend.vault.service;

import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.service.JobService;
import com.workproofpay.backend.remittance.model.UserWallet;
import com.workproofpay.backend.remittance.service.WalletService;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.vault.adapter.VaultBlockchainGateway;
import com.workproofpay.backend.vault.adapter.VaultChainState;
import com.workproofpay.backend.vault.api.dto.request.CreateVaultTransactionRequest;
import com.workproofpay.backend.vault.api.dto.response.CreateVaultTransactionResponse;
import com.workproofpay.backend.vault.api.dto.response.VaultSummaryResponse;
import com.workproofpay.backend.vault.api.dto.response.VaultTransactionDetailResponse;
import com.workproofpay.backend.vault.api.dto.response.VaultTransactionItemResponse;
import com.workproofpay.backend.vault.api.dto.response.VaultTransactionListResponse;
import com.workproofpay.backend.vault.config.VaultProperties;
import com.workproofpay.backend.vault.model.VaultPosition;
import com.workproofpay.backend.vault.model.VaultTransaction;
import com.workproofpay.backend.vault.model.VaultTransactionStatus;
import com.workproofpay.backend.vault.model.VaultTransactionType;
import com.workproofpay.backend.vault.repo.VaultPositionRepository;
import com.workproofpay.backend.vault.repo.VaultTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VaultService {

    private final VaultPositionRepository vaultPositionRepository;
    private final VaultTransactionRepository vaultTransactionRepository;
    private final WalletService walletService;
    private final VaultBlockchainGateway vaultBlockchainGateway;
    private final VaultYieldService vaultYieldService;
    private final JobService jobService;
    private final VaultProperties properties;

    @Transactional
    public VaultSummaryResponse getSummary(Long userId) {
        UserWallet wallet = walletService.getRequiredWallet(userId);
        VaultPosition position = vaultPositionRepository.findByUserId(userId).orElse(null);
        if (position != null) {
            vaultYieldService.accrueIfNeeded(position);
        }
        VaultChainState chainState = vaultBlockchainGateway.getState(wallet.getWalletAddress());

        return new VaultSummaryResponse(
                wallet.getWalletAddress(),
                requireVaultAddress(),
                properties.getChain().getNetwork(),
                properties.getPolicy().getAssetSymbol(),
                properties.getPolicy().getAssetDecimals(),
                position == null ? "0" : position.getPrincipalAmountAtomic().toString(),
                position == null ? "0" : position.getAccruedYieldAtomic().toString(),
                chainState.walletTokenBalanceAtomic().toString(),
                calculateAvailableToStoreAmount(chainState).toString(),
                chainState.vaultShareBalance().toString(),
                vaultYieldService.preview(position),
                properties.getPolicy().getDisclaimer()
        );
    }

    @Transactional
    public VaultCreateResult createDeposit(Long userId, String idempotencyKey, CreateVaultTransactionRequest request) {
        return createTransaction(userId, idempotencyKey, request, VaultTransactionType.DEPOSIT);
    }

    @Transactional
    public VaultCreateResult createWithdrawal(Long userId, String idempotencyKey, CreateVaultTransactionRequest request) {
        return createTransaction(userId, idempotencyKey, request, VaultTransactionType.WITHDRAW);
    }

    @Transactional(readOnly = true)
    public VaultTransactionListResponse getTransactions(Long userId, Integer requestedLimit) {
        int limit = requestedLimit == null ? properties.getPolicy().getDefaultListLimit() : Math.min(requestedLimit, 100);
        List<VaultTransaction> transactions = vaultTransactionRepository.findByUserIdOrderByCreatedAtDescVaultTransactionIdDesc(userId, PageRequest.of(0, limit));
        return new VaultTransactionListResponse(
                transactions.stream()
                        .map(transaction -> new VaultTransactionItemResponse(
                                transaction.getVaultTransactionId(),
                                transaction.getTxType().name(),
                                transaction.getStatus().name(),
                                transaction.getAmountAtomic().toString(),
                                transaction.getShareDelta() == null ? null : transaction.getShareDelta().toString(),
                                transaction.getTxHash(),
                                transaction.getFailureCode() == null ? null : transaction.getFailureCode().name(),
                                transaction.getUpdatedAt()
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public VaultTransactionDetailResponse getTransaction(Long userId, String vaultTransactionId) {
        VaultTransaction transaction = vaultTransactionRepository.findByVaultTransactionIdAndUserId(vaultTransactionId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.VAULT_TRANSACTION_NOT_FOUND));
        return new VaultTransactionDetailResponse(
                transaction.getVaultTransactionId(),
                transaction.getTxType().name(),
                transaction.getStatus().name(),
                transaction.getWalletAddress(),
                transaction.getVaultAddress(),
                transaction.getAssetSymbol(),
                transaction.getAmountAtomic().toString(),
                transaction.getShareDelta() == null ? null : transaction.getShareDelta().toString(),
                transaction.getTxHash(),
                transaction.getFailureCode() == null ? null : transaction.getFailureCode().name(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                transaction.getConfirmedAt()
        );
    }

    @Transactional
    public VaultPosition getOrCreatePositionForUpdate(Long userId, String walletAddress) {
        VaultPosition existing = vaultPositionRepository.findByUserIdForUpdate(userId).orElse(null);
        if (existing != null) {
            return existing;
        }
        VaultPosition created = VaultPosition.create(
                userId,
                walletAddress,
                properties.getPolicy().getAssetSymbol(),
                requireTokenAddress(),
                requireVaultAddress(),
                properties.getChain().getNetwork(),
                properties.getPolicy().getApyBps()
        );
        try {
            return vaultPositionRepository.saveAndFlush(created);
        } catch (DataIntegrityViolationException e) {
            return vaultPositionRepository.findByUserIdForUpdate(userId).orElseThrow(() -> e);
        }
    }

    private VaultCreateResult createTransaction(
            Long userId,
            String idempotencyKey,
            CreateVaultTransactionRequest request,
            VaultTransactionType txType
    ) {
        requireConfig();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
        if (request.amountAtomic() == null || request.amountAtomic() <= 0) {
            throw new ApiException(ErrorCode.INVALID_VAULT_AMOUNT);
        }

        UserWallet wallet = walletService.getRequiredWalletForUpdate(userId);
        VaultPosition position = getOrCreatePositionForUpdate(userId, wallet.getWalletAddress());
        vaultYieldService.accrueIfNeeded(position);

        BigInteger amountAtomic = BigInteger.valueOf(request.amountAtomic());
        VaultTransaction existing = vaultTransactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).orElse(null);
        if (existing != null) {
            return replay(existing, txType, amountAtomic);
        }
        if (vaultTransactionRepository.existsByUserIdAndStatusIn(userId, List.of(
                VaultTransactionStatus.REQUESTED,
                VaultTransactionStatus.SIGNED,
                VaultTransactionStatus.BROADCASTED
        ))) {
            throw new ApiException(ErrorCode.VAULT_TRANSACTION_ALREADY_IN_PROGRESS);
        }

        VaultChainState chainState = vaultBlockchainGateway.getState(wallet.getWalletAddress());
        if (txType == VaultTransactionType.DEPOSIT) {
            if (chainState.walletTokenBalanceAtomic().compareTo(amountAtomic) < 0 || chainState.walletNativeBalanceWei().signum() <= 0) {
                throw new ApiException(ErrorCode.VAULT_INSUFFICIENT_AVAILABLE_BALANCE);
            }
        } else {
            if (position.getPrincipalAmountAtomic().compareTo(amountAtomic) < 0) {
                throw new ApiException(ErrorCode.VAULT_INSUFFICIENT_STORED_BALANCE);
            }
            BigInteger requiredShares = previewWithdrawShares(amountAtomic);
            if (chainState.vaultShareBalance().compareTo(requiredShares) < 0) {
                throw new ApiException(ErrorCode.VAULT_INSUFFICIENT_STORED_BALANCE);
            }
        }

        VaultTransaction transaction = VaultTransaction.request(
                generateTransactionId(),
                position.getId(),
                userId,
                txType,
                amountAtomic,
                wallet.getWalletAddress(),
                requireVaultAddress(),
                properties.getPolicy().getAssetSymbol(),
                idempotencyKey
        );
        try {
            vaultTransactionRepository.saveAndFlush(transaction);
            jobService.enqueue(JobType.SUBMIT_VAULT_TRANSACTION, transaction.getVaultTransactionId(), LocalDateTime.now());
        } catch (DataIntegrityViolationException e) {
            VaultTransaction conflicted = vaultTransactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                    .orElseThrow(() -> e);
            return replay(conflicted, txType, amountAtomic);
        }
        return new VaultCreateResult(false, toCreateResponse(transaction));
    }

    private VaultCreateResult replay(VaultTransaction existing, VaultTransactionType txType, BigInteger amountAtomic) {
        if (!existing.matchesReplay(txType, amountAtomic)) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD);
        }
        return new VaultCreateResult(true, toCreateResponse(existing));
    }

    private CreateVaultTransactionResponse toCreateResponse(VaultTransaction transaction) {
        return new CreateVaultTransactionResponse(
                transaction.getVaultTransactionId(),
                transaction.getTxType().name(),
                transaction.getStatus().name(),
                "/api/vault/transactions/" + transaction.getVaultTransactionId(),
                transaction.getCreatedAt()
        );
    }

    private String generateTransactionId() {
        return "vtx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private BigInteger calculateAvailableToStoreAmount(VaultChainState chainState) {
        if (chainState.walletNativeBalanceWei().signum() <= 0) {
            return BigInteger.ZERO;
        }
        return chainState.walletTokenBalanceAtomic();
    }

    private BigInteger previewWithdrawShares(BigInteger amountAtomic) {
        try {
            return vaultBlockchainGateway.previewWithdraw(amountAtomic);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.VAULT_INSUFFICIENT_STORED_BALANCE);
        }
    }

    private void requireConfig() {
        if (properties.getChain().getTokenAddress() == null || properties.getChain().getTokenAddress().isBlank()
                || properties.getChain().getVaultAddress() == null || properties.getChain().getVaultAddress().isBlank()) {
            throw new ApiException(ErrorCode.VAULT_CONFIG_MISSING);
        }
    }

    private String requireVaultAddress() {
        requireConfig();
        return properties.getChain().getVaultAddress();
    }

    private String requireTokenAddress() {
        requireConfig();
        return properties.getChain().getTokenAddress();
    }
}
