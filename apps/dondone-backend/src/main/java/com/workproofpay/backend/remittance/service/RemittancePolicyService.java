package com.workproofpay.backend.remittance.service;

import com.workproofpay.backend.remittance.adapter.ChainBalanceSnapshot;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.Recipient;
import com.workproofpay.backend.remittance.model.RemittancePolicyCode;
import com.workproofpay.backend.remittance.model.TransferStatus;
import com.workproofpay.backend.remittance.model.UserWallet;
import com.workproofpay.backend.remittance.repo.TransferRepository;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RemittancePolicyService {

    private final RecipientService recipientService;
    private final WalletService walletService;
    private final TransferRepository transferRepository;
    private final RemittanceProperties properties;
    private final RemittanceMetrics remittanceMetrics;

    @Transactional(readOnly = true)
    public RemittancePolicyDecision evaluate(
            Long userId,
            String recipientId,
            long amountAtomic,
            boolean highAmountConfirmed,
            boolean recentRecipientConfirmed
    ) {
        Timer.Sample sample = remittanceMetrics.start();
        PolicyEvaluationTrace trace = new PolicyEvaluationTrace();

        try {
            Recipient recipient = recipientService.getRequiredRecipient(userId, recipientId);
            UserWallet wallet = walletService.getRequiredWallet(userId);
            ChainBalanceSnapshot balanceSnapshot = walletService.getBalances(wallet.getWalletAddress());

            if (!recipient.isAllowed()) {
                return blocked(trace, RemittancePolicyCode.RECIPIENT_NOT_ALLOWED, recipient, wallet, balanceSnapshot, false);
            }

            if (recipient.getWalletAddress().equalsIgnoreCase(wallet.getWalletAddress())) {
                return blocked(trace, RemittancePolicyCode.SELF_TRANSFER_NOT_ALLOWED, recipient, wallet, balanceSnapshot, false);
            }

            boolean recentConfirmationRequired = recipient.getUpdatedAt()
                    .plusSeconds(properties.getPolicy().getRecentRecipientWindowSeconds())
                    .isAfter(LocalDateTime.now());
            if (recentConfirmationRequired && !recentRecipientConfirmed) {
                return blocked(
                        trace,
                        RemittancePolicyCode.RECENT_RECIPIENT_CONFIRMATION_REQUIRED,
                        recipient,
                        wallet,
                        balanceSnapshot,
                        true
                );
            }

            if (amountAtomic > properties.getPolicy().getHighAmountThresholdAtomic() && !highAmountConfirmed) {
                return blocked(
                        trace,
                        RemittancePolicyCode.HIGH_AMOUNT_CONFIRMATION_REQUIRED,
                        recipient,
                        wallet,
                        balanceSnapshot,
                        recentConfirmationRequired
                );
            }

            boolean hasActiveTransfer = transferRepository.existsByUserIdAndStatusIn(
                    userId,
                    List.of(TransferStatus.REQUESTED, TransferStatus.SIGNED, TransferStatus.BROADCASTED)
            );
            if (hasActiveTransfer) {
                return blocked(
                        trace,
                        RemittancePolicyCode.TRANSFER_ALREADY_IN_PROGRESS,
                        recipient,
                        wallet,
                        balanceSnapshot,
                        recentConfirmationRequired
                );
            }

            BigInteger requiredGasCostWei = walletService.estimateTransferGasCostWei();
            if (balanceSnapshot.tokenBalanceAtomic().compareTo(BigInteger.valueOf(amountAtomic)) < 0
                    || balanceSnapshot.nativeBalanceWei().compareTo(requiredGasCostWei) < 0) {
                return blocked(
                        trace,
                        RemittancePolicyCode.INSUFFICIENT_WALLET_BALANCE,
                        recipient,
                        wallet,
                        balanceSnapshot,
                        recentConfirmationRequired
                );
            }

            return allowed(trace, recipient, wallet, balanceSnapshot, recentConfirmationRequired);
        } finally {
            remittanceMetrics.recordPolicyEvaluate(sample, trace.outcome(), trace.policyCode());
        }
    }

    private RemittancePolicyDecision blocked(
            PolicyEvaluationTrace trace,
            RemittancePolicyCode policyCode,
            Recipient recipient,
            UserWallet wallet,
            ChainBalanceSnapshot balanceSnapshot,
            boolean recentRecipientConfirmationRequired
    ) {
        trace.markBlocked(policyCode);
        return new RemittancePolicyDecision(
                false,
                policyCode,
                recipient,
                wallet,
                balanceSnapshot,
                recentRecipientConfirmationRequired
        );
    }

    private RemittancePolicyDecision allowed(
            PolicyEvaluationTrace trace,
            Recipient recipient,
            UserWallet wallet,
            ChainBalanceSnapshot balanceSnapshot,
            boolean recentRecipientConfirmationRequired
    ) {
        trace.markAllowed();
        return new RemittancePolicyDecision(
                true,
                null,
                recipient,
                wallet,
                balanceSnapshot,
                recentRecipientConfirmationRequired
        );
    }

    private static final class PolicyEvaluationTrace {

        private String outcome = "error";
        private String policyCode;

        private void markBlocked(RemittancePolicyCode policyCode) {
            this.outcome = "blocked";
            this.policyCode = policyCode.name().toLowerCase();
        }

        private void markAllowed() {
            this.outcome = "allowed";
            this.policyCode = null;
        }

        private String outcome() {
            return outcome;
        }

        private String policyCode() {
            return policyCode;
        }
    }
}
