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
        String outcome = "error";
        String policyCode = null;

        try {
            Recipient recipient = recipientService.getRequiredRecipient(userId, recipientId);
            UserWallet wallet = walletService.getRequiredWallet(userId);
            ChainBalanceSnapshot balanceSnapshot = walletService.getBalances(wallet.getWalletAddress());

            if (!recipient.isAllowed()) {
                policyCode = RemittancePolicyCode.RECIPIENT_NOT_ALLOWED.name().toLowerCase();
                outcome = "blocked";
                return blocked(RemittancePolicyCode.RECIPIENT_NOT_ALLOWED, recipient, wallet, balanceSnapshot, false);
            }

            if (recipient.getWalletAddress().equalsIgnoreCase(wallet.getWalletAddress())) {
                policyCode = RemittancePolicyCode.SELF_TRANSFER_NOT_ALLOWED.name().toLowerCase();
                outcome = "blocked";
                return blocked(RemittancePolicyCode.SELF_TRANSFER_NOT_ALLOWED, recipient, wallet, balanceSnapshot, false);
            }

            boolean recentConfirmationRequired = recipient.getUpdatedAt()
                    .plusSeconds(properties.getPolicy().getRecentRecipientWindowSeconds())
                    .isAfter(LocalDateTime.now());
            if (recentConfirmationRequired && !recentRecipientConfirmed) {
                policyCode = RemittancePolicyCode.RECENT_RECIPIENT_CONFIRMATION_REQUIRED.name().toLowerCase();
                outcome = "blocked";
                return blocked(
                        RemittancePolicyCode.RECENT_RECIPIENT_CONFIRMATION_REQUIRED,
                        recipient,
                        wallet,
                        balanceSnapshot,
                        true
                );
            }

            if (amountAtomic > properties.getPolicy().getHighAmountThresholdAtomic() && !highAmountConfirmed) {
                policyCode = RemittancePolicyCode.HIGH_AMOUNT_CONFIRMATION_REQUIRED.name().toLowerCase();
                outcome = "blocked";
                return blocked(
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
                policyCode = RemittancePolicyCode.TRANSFER_ALREADY_IN_PROGRESS.name().toLowerCase();
                outcome = "blocked";
                return blocked(
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
                policyCode = RemittancePolicyCode.INSUFFICIENT_WALLET_BALANCE.name().toLowerCase();
                outcome = "blocked";
                return blocked(
                        RemittancePolicyCode.INSUFFICIENT_WALLET_BALANCE,
                        recipient,
                        wallet,
                        balanceSnapshot,
                        recentConfirmationRequired
                );
            }

            outcome = "allowed";
            return new RemittancePolicyDecision(true, null, recipient, wallet, balanceSnapshot, recentConfirmationRequired);
        } finally {
            remittanceMetrics.recordPolicyEvaluate(sample, outcome, policyCode);
        }
    }

    private RemittancePolicyDecision blocked(
            RemittancePolicyCode policyCode,
            Recipient recipient,
            UserWallet wallet,
            ChainBalanceSnapshot balanceSnapshot,
            boolean recentRecipientConfirmationRequired
    ) {
        return new RemittancePolicyDecision(
                false,
                policyCode,
                recipient,
                wallet,
                balanceSnapshot,
                recentRecipientConfirmationRequired
        );
    }
}
