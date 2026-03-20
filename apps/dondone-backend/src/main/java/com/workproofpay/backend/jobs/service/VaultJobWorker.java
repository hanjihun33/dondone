package com.workproofpay.backend.jobs.service;

import com.workproofpay.backend.jobs.model.Job;
import com.workproofpay.backend.jobs.model.JobReferenceKind;
import com.workproofpay.backend.jobs.model.JobStatus;
import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.repo.JobRepository;
import com.workproofpay.backend.remittance.service.WalletService;
import com.workproofpay.backend.vault.adapter.PreparedVaultTransaction;
import com.workproofpay.backend.vault.adapter.VaultBlockchainGateway;
import com.workproofpay.backend.vault.adapter.VaultReceiptResult;
import com.workproofpay.backend.vault.config.VaultProperties;
import com.workproofpay.backend.vault.model.VaultFailureCode;
import com.workproofpay.backend.vault.model.VaultPosition;
import com.workproofpay.backend.vault.model.VaultTransaction;
import com.workproofpay.backend.vault.model.VaultTransactionStatus;
import com.workproofpay.backend.vault.model.VaultTransactionType;
import com.workproofpay.backend.vault.repo.VaultPositionRepository;
import com.workproofpay.backend.vault.repo.VaultTransactionRepository;
import com.workproofpay.backend.vault.service.VaultYieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VaultJobWorker {

    private static final List<JobStatus> ACTIVE_JOB_STATUSES = List.of(JobStatus.QUEUED, JobStatus.RUNNING);

    private final JobRepository jobRepository;
    private final VaultTransactionRepository vaultTransactionRepository;
    private final VaultPositionRepository vaultPositionRepository;
    private final WalletService walletService;
    private final VaultBlockchainGateway vaultBlockchainGateway;
    private final VaultYieldService vaultYieldService;
    private final JobService jobService;
    private final VaultProperties properties;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelayString = "${vault.worker.poll-interval-ms:2000}")
    public void run() {
        List<Long> jobIds = transactionTemplate.execute(status ->
                jobRepository.findTop20ByReferenceKindAndStatusAndRunAtLessThanEqualOrderByIdAsc(
                                JobReferenceKind.VAULT,
                                JobStatus.QUEUED,
                                LocalDateTime.now()
                        )
                        .stream()
                        .map(Job::getId)
                        .toList()
        );
        if (jobIds == null) {
            return;
        }
        for (Long jobId : jobIds) {
            process(jobId);
        }
    }

    private void process(Long jobId) {
        ClaimedJob claimedJob = claim(jobId);
        if (claimedJob == null) {
            return;
        }
        try {
            switch (claimedJob.jobType()) {
                case SUBMIT_VAULT_TRANSACTION -> handleSubmit(claimedJob);
                case POLL_VAULT_TRANSACTION_RECEIPT -> handlePoll(claimedJob);
                default -> {
                }
            }
        } catch (Exception e) {
            handleFailure(claimedJob.jobId(), e);
        }
    }

    private ClaimedJob claim(Long jobId) {
        return transactionTemplate.execute(status -> {
            Job job = jobRepository.findByIdForUpdate(jobId).orElse(null);
            if (job == null || job.getStatus() != JobStatus.QUEUED || job.getRunAt().isAfter(LocalDateTime.now())) {
                return null;
            }
            job.markRunning();
            return new ClaimedJob(job.getId(), job.getJobType(), job.getReferenceId());
        });
    }

    private void handleSubmit(ClaimedJob claimedJob) {
        PendingSubmission submission = transactionTemplate.execute(status -> prepareSubmission(claimedJob.referenceId()));
        if (submission == null) {
            markJobDone(claimedJob.jobId());
            return;
        }
        vaultBlockchainGateway.broadcastSignedTransaction(submission.signedTransaction());
        transactionTemplate.executeWithoutResult(status -> completeSubmit(claimedJob.jobId(), submission.vaultTransactionId()));
    }

    private PendingSubmission prepareSubmission(String vaultTransactionId) {
        VaultTransaction transaction = requiredTransaction(vaultTransactionId);
        if (transaction.getStatus() == VaultTransactionStatus.REQUESTED) {
            String senderPrivateKey = walletService.getDecryptedPrivateKey(transaction.getUserId());
            PreparedVaultTransaction prepared;
            if (transaction.getTxType() == VaultTransactionType.DEPOSIT) {
                vaultBlockchainGateway.approveAssetIfNeeded(senderPrivateKey, transaction.getAmountAtomic());
                prepared = vaultBlockchainGateway.prepareDeposit(senderPrivateKey, transaction.getAmountAtomic(), transaction.getWalletAddress());
            } else {
                prepared = vaultBlockchainGateway.prepareWithdraw(
                        senderPrivateKey,
                        transaction.getAmountAtomic(),
                        transaction.getWalletAddress(),
                        transaction.getWalletAddress()
                );
            }
            transaction.markSigned(prepared.txHash(), prepared.signedTransaction(), prepared.shareDelta());
            return new PendingSubmission(transaction.getVaultTransactionId(), prepared.signedTransaction());
        }

        if (transaction.getStatus() == VaultTransactionStatus.SIGNED
                && transaction.getSignedTransaction() != null
                && transaction.getTxHash() != null) {
            return new PendingSubmission(transaction.getVaultTransactionId(), transaction.getSignedTransaction());
        }
        return null;
    }

    private void completeSubmit(Long jobId, String vaultTransactionId) {
        VaultTransaction transaction = requiredTransaction(vaultTransactionId);
        if (transaction.getStatus() == VaultTransactionStatus.SIGNED) {
            transaction.markBroadcasted();
        }
        enqueuePollJobIfAbsent(vaultTransactionId);
        requiredJob(jobId).markDone();
    }

    private void handlePoll(ClaimedJob claimedJob) {
        PollTarget pollTarget = transactionTemplate.execute(status -> {
            VaultTransaction transaction = requiredTransaction(claimedJob.referenceId());
            if (transaction.getStatus() != VaultTransactionStatus.BROADCASTED || transaction.getTxHash() == null) {
                return null;
            }
            return new PollTarget(transaction.getVaultTransactionId(), transaction.getTxHash());
        });

        if (pollTarget == null) {
            markJobDone(claimedJob.jobId());
            return;
        }

        Optional<VaultReceiptResult> receiptResult = vaultBlockchainGateway.getReceipt(pollTarget.txHash());
        transactionTemplate.executeWithoutResult(status -> applyPollResult(claimedJob.jobId(), pollTarget.vaultTransactionId(), receiptResult));
    }

    private void applyPollResult(Long jobId, String vaultTransactionId, Optional<VaultReceiptResult> receiptResult) {
        Job job = requiredJob(jobId);
        VaultTransaction transaction = requiredTransaction(vaultTransactionId);
        if (transaction.getStatus() != VaultTransactionStatus.BROADCASTED || transaction.getTxHash() == null) {
            job.markDone();
            return;
        }

        if (receiptResult.isEmpty()) {
            if (isReceiptTimedOut(transaction) && !vaultBlockchainGateway.isTransactionKnown(transaction.getTxHash())) {
                transaction.markTimedOut(VaultFailureCode.NETWORK_ERROR);
                job.markDone();
                return;
            }
            if (isReceiptTimedOut(transaction)) {
                job.requeue(nextPollAt(), "vault receipt delayed for known transaction");
                return;
            }
            job.requeue(nextPollAt(), null);
            return;
        }

        if (receiptResult.get().success()) {
            applyConfirmedTransaction(transaction);
            transaction.markConfirmed();
        } else {
            transaction.markFailed(receiptResult.get().failureCode() == null ? VaultFailureCode.UNKNOWN : receiptResult.get().failureCode());
        }
        job.markDone();
    }

    private void applyConfirmedTransaction(VaultTransaction transaction) {
        VaultPosition position = vaultPositionRepository.findById(transaction.getPositionId()).orElseThrow();
        vaultYieldService.accrueIfNeeded(position);
        if (transaction.getTxType() == VaultTransactionType.DEPOSIT) {
            position.applyDeposit(transaction.getAmountAtomic(), requiredShareDelta(transaction), LocalDateTime.now());
            return;
        }
        position.applyWithdraw(transaction.getAmountAtomic(), requiredShareDelta(transaction), LocalDateTime.now());
    }

    private void handleFailure(Long jobId, Exception e) {
        transactionTemplate.executeWithoutResult(status -> {
            Job job = requiredJob(jobId);
            if (job.getStatus() != JobStatus.RUNNING) {
                return;
            }
            if (job.getAttemptCount() > 2) {
                markTransactionFailed(job.getReferenceId(), summarizeFailureCode(e));
                job.markFailed(e.getMessage());
                return;
            }
            job.requeue(LocalDateTime.now().plusSeconds(3L), e.getMessage());
        });
    }

    private void enqueuePollJobIfAbsent(String vaultTransactionId) {
        if (jobRepository.existsByReferenceKindAndReferenceIdAndJobTypeAndStatusIn(
                JobReferenceKind.VAULT,
                vaultTransactionId,
                JobType.POLL_VAULT_TRANSACTION_RECEIPT,
                ACTIVE_JOB_STATUSES
        )) {
            return;
        }
        jobService.enqueue(JobReferenceKind.VAULT, JobType.POLL_VAULT_TRANSACTION_RECEIPT, vaultTransactionId, nextPollAt());
    }

    private void markTransactionFailed(String vaultTransactionId, VaultFailureCode failureCode) {
        VaultTransaction transaction = vaultTransactionRepository.findById(vaultTransactionId).orElse(null);
        if (transaction == null) {
            return;
        }
        if (transaction.getStatus() == VaultTransactionStatus.REQUESTED
                || transaction.getStatus() == VaultTransactionStatus.SIGNED
                || transaction.getStatus() == VaultTransactionStatus.BROADCASTED) {
            transaction.markFailed(failureCode);
        }
    }

    private void markJobDone(Long jobId) {
        transactionTemplate.executeWithoutResult(status -> {
            Job job = requiredJob(jobId);
            if (job.getStatus() != JobStatus.DONE) {
                job.markDone();
            }
        });
    }

    private VaultFailureCode summarizeFailureCode(Exception exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
        if (message.contains("approve")) {
            return VaultFailureCode.ALLOWANCE_FAILED;
        }
        if (message.contains("insufficient")) {
            return VaultFailureCode.INSUFFICIENT_BALANCE;
        }
        return VaultFailureCode.UNKNOWN;
    }

    private BigInteger requiredShareDelta(VaultTransaction transaction) {
        if (transaction.getShareDelta() == null) {
            throw new IllegalStateException("vault transaction share delta is missing");
        }
        return transaction.getShareDelta();
    }

    private Job requiredJob(Long jobId) {
        return jobRepository.findById(jobId).orElseThrow();
    }

    private VaultTransaction requiredTransaction(String vaultTransactionId) {
        return vaultTransactionRepository.findByIdForUpdate(vaultTransactionId).orElseThrow();
    }

    private LocalDateTime nextPollAt() {
        return LocalDateTime.now().plusSeconds(properties.getWorker().getReceiptPollDelaySeconds());
    }

    private boolean isReceiptTimedOut(VaultTransaction transaction) {
        return Duration.between(transaction.getUpdatedAt(), LocalDateTime.now()).getSeconds()
                >= properties.getWorker().getReceiptTimeoutSeconds();
    }

    private record PendingSubmission(
            String vaultTransactionId,
            String signedTransaction
    ) {
    }

    private record PollTarget(
            String vaultTransactionId,
            String txHash
    ) {
    }

    private record ClaimedJob(
            Long jobId,
            JobType jobType,
            String referenceId
    ) {
    }
}
