package com.workproofpay.backend.jobs.service;

import com.workproofpay.backend.advance.model.AdvancePayout;
import com.workproofpay.backend.advance.model.AdvancePayoutStatus;
import com.workproofpay.backend.advance.repo.AdvancePayoutRepository;
import com.workproofpay.backend.jobs.model.Job;
import com.workproofpay.backend.jobs.model.JobReferenceKind;
import com.workproofpay.backend.jobs.model.JobStatus;
import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.repo.JobRepository;
import com.workproofpay.backend.remittance.adapter.ChainReceiptLookupException;
import com.workproofpay.backend.remittance.adapter.ChainReceiptResult;
import com.workproofpay.backend.remittance.adapter.PreparedTokenTransfer;
import com.workproofpay.backend.remittance.adapter.RemittanceBlockchainGateway;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.TransferFailureCode;
import com.workproofpay.backend.remittance.service.WalletCryptoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class AdvancePayoutJobWorker {

    private static final List<JobStatus> ACTIVE_JOB_STATUSES = List.of(JobStatus.QUEUED, JobStatus.RUNNING);

    private final JobRepository jobRepository;
    private final AdvancePayoutRepository advancePayoutRepository;
    private final RemittanceBlockchainGateway blockchainGateway;
    private final WalletCryptoService walletCryptoService;
    private final RemittanceProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final JobService jobService;

    public AdvancePayoutJobWorker(
            JobRepository jobRepository,
            AdvancePayoutRepository advancePayoutRepository,
            RemittanceBlockchainGateway blockchainGateway,
            WalletCryptoService walletCryptoService,
            RemittanceProperties properties,
            TransactionTemplate transactionTemplate,
            JobService jobService
    ) {
        this.jobRepository = jobRepository;
        this.advancePayoutRepository = advancePayoutRepository;
        this.blockchainGateway = blockchainGateway;
        this.walletCryptoService = walletCryptoService;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
        this.jobService = jobService;
    }

    @Scheduled(fixedDelayString = "${remittance.worker.poll-interval-ms:2000}")
    public void run() {
        List<Long> jobIds = transactionTemplate.execute(status ->
                jobRepository.findTop20ByReferenceKindAndStatusAndRunAtLessThanEqualOrderByIdAsc(
                                JobReferenceKind.ADVANCE_PAYOUT,
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
                case SUBMIT_ADVANCE_PAYOUT -> handleSubmit(claimedJob);
                case POLL_ADVANCE_PAYOUT_RECEIPT -> handlePoll(claimedJob);
            }
        } catch (Exception e) {
            handleFailure(claimedJob.jobId(), claimedJob.jobType(), claimedJob.referenceId(), e);
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

        blockchainGateway.broadcastSignedTransaction(submission.signedTransaction());
        transactionTemplate.executeWithoutResult(status -> completeSubmit(claimedJob.jobId(), submission.advancePayoutId()));
    }

    private PendingSubmission prepareSubmission(String advancePayoutId) {
        AdvancePayout payout = requiredPayout(advancePayoutId);
        if (payout.getStatus() == AdvancePayoutStatus.REQUESTED) {
            PreparedTokenTransfer prepared = blockchainGateway.prepareTokenTransfer(
                    requireTreasuryPrivateKey(),
                    payout.getWalletAddress(),
                    BigInteger.valueOf(payout.getAmountAtomic())
            );
            payout.markSigned(
                    prepared.txHash(),
                    walletCryptoService.encrypt(prepared.signedTransaction())
            );
            return new PendingSubmission(payout.getAdvancePayoutId(), prepared.signedTransaction());
        }

        if (payout.getStatus() == AdvancePayoutStatus.SIGNED
                && payout.getSignedTransaction() != null
                && payout.getTxHash() != null) {
            return new PendingSubmission(
                    payout.getAdvancePayoutId(),
                    decryptSignedTransaction(payout.getSignedTransaction())
            );
        }

        return null;
    }

    private void completeSubmit(Long jobId, String advancePayoutId) {
        AdvancePayout payout = requiredPayout(advancePayoutId);
        if (payout.getStatus() == AdvancePayoutStatus.SIGNED) {
            payout.markBroadcasted();
        }
        enqueuePollJobIfAbsent(advancePayoutId);
        requiredJob(jobId).markDone();
    }

    private void handlePoll(ClaimedJob claimedJob) {
        try {
            PollTarget pollTarget = transactionTemplate.execute(status -> {
                AdvancePayout payout = requiredPayout(claimedJob.referenceId());
                if (payout.getStatus() != AdvancePayoutStatus.BROADCASTED || payout.getTxHash() == null) {
                    return null;
                }
                return new PollTarget(payout.getAdvancePayoutId(), payout.getTxHash());
            });

            if (pollTarget == null) {
                markJobDone(claimedJob.jobId());
                return;
            }

            Optional<ChainReceiptResult> receiptResult = blockchainGateway.getReceipt(pollTarget.txHash());
            transactionTemplate.executeWithoutResult(status -> applyPollResult(claimedJob.jobId(), pollTarget.advancePayoutId(), receiptResult));
        } catch (ChainReceiptLookupException e) {
            transactionTemplate.executeWithoutResult(status -> requeueClaimedJob(claimedJob.jobId(), nextPollAt(), e.getMessage()));
        }
    }

    private void applyPollResult(Long jobId, String advancePayoutId, Optional<ChainReceiptResult> receiptResult) {
        Job job = requiredJob(jobId);
        AdvancePayout payout = requiredPayout(advancePayoutId);
        if (payout.getStatus() != AdvancePayoutStatus.BROADCASTED || payout.getTxHash() == null) {
            job.markDone();
            return;
        }

        if (receiptResult.isEmpty()) {
            if (isReceiptTimedOut(payout) && !blockchainGateway.isTransactionKnown(payout.getTxHash())) {
                payout.markTimedOut(TransferFailureCode.NETWORK_ERROR.name());
                job.markDone();
                return;
            }
            if (isReceiptTimedOut(payout)) {
                job.requeue(nextPollAt(), "receipt delayed for known transaction");
                return;
            }
            job.requeue(nextPollAt(), null);
            return;
        }

        if (receiptResult.get().success()) {
            payout.markConfirmed();
            job.markDone();
            return;
        }

        payout.markFailed(receiptFailureReason(receiptResult.get()));
        job.markDone();
    }

    private void handleFailure(Long jobId, JobType jobType, String referenceId, Exception e) {
        transactionTemplate.executeWithoutResult(status -> {
            Job job = requiredJob(jobId);
            if (job.getStatus() != JobStatus.RUNNING) {
                return;
            }

            if (job.getAttemptCount() > properties.getPolicy().getMaxAutoRetryCount()) {
                if (jobType == JobType.SUBMIT_ADVANCE_PAYOUT && keepBroadcastedPayoutTrackable(referenceId)) {
                    job.markFailed(e.getMessage());
                    return;
                }
                markPayoutFailed(referenceId, e.getMessage());
                job.markFailed(e.getMessage());
                return;
            }

            job.requeue(LocalDateTime.now().plusSeconds(3L), e.getMessage());
        });
    }

    private void markJobDone(Long jobId) {
        transactionTemplate.executeWithoutResult(status -> {
            Job job = requiredJob(jobId);
            if (job.getStatus() != JobStatus.DONE) {
                job.markDone();
            }
        });
    }

    private void markPayoutFailed(String advancePayoutId, String failureReason) {
        AdvancePayout payout = advancePayoutRepository.findById(advancePayoutId).orElse(null);
        if (payout == null) {
            return;
        }
        if (payout.getStatus() == AdvancePayoutStatus.REQUESTED
                || payout.getStatus() == AdvancePayoutStatus.SIGNED
                || payout.getStatus() == AdvancePayoutStatus.BROADCASTED) {
            payout.markFailed(failureReason);
        }
    }

    private boolean keepBroadcastedPayoutTrackable(String advancePayoutId) {
        AdvancePayout payout = advancePayoutRepository.findById(advancePayoutId).orElse(null);
        if (payout == null || payout.getStatus() != AdvancePayoutStatus.BROADCASTED || payout.getTxHash() == null) {
            return false;
        }
        enqueuePollJobIfAbsent(advancePayoutId);
        return true;
    }

    private void enqueuePollJobIfAbsent(String advancePayoutId) {
        if (jobRepository.existsByReferenceKindAndReferenceIdAndJobTypeAndStatusIn(
                JobReferenceKind.ADVANCE_PAYOUT,
                advancePayoutId,
                JobType.POLL_ADVANCE_PAYOUT_RECEIPT,
                ACTIVE_JOB_STATUSES
        )) {
            return;
        }
        jobService.enqueue(JobReferenceKind.ADVANCE_PAYOUT, JobType.POLL_ADVANCE_PAYOUT_RECEIPT, advancePayoutId, nextPollAt());
    }

    private void requeueClaimedJob(Long jobId, LocalDateTime nextRunAt, String lastError) {
        Job job = requiredJob(jobId);
        if (job.getStatus() == JobStatus.RUNNING) {
            job.requeue(nextRunAt, lastError);
        }
    }

    private AdvancePayout requiredPayout(String advancePayoutId) {
        return advancePayoutRepository.findByIdForUpdate(advancePayoutId).orElseThrow();
    }

    private Job requiredJob(Long jobId) {
        return jobRepository.findById(jobId).orElseThrow();
    }

    private String decryptSignedTransaction(String encryptedSignedTransaction) {
        try {
            return walletCryptoService.decrypt(encryptedSignedTransaction);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return encryptedSignedTransaction;
        }
    }

    private String requireTreasuryPrivateKey() {
        String privateKey = properties.getTreasury().getPrivateKey();
        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException("REMITTANCE_TREASURY_PRIVATE_KEY is required");
        }
        return privateKey;
    }

    private LocalDateTime nextPollAt() {
        return LocalDateTime.now().plusSeconds(properties.getWorker().getReceiptPollDelaySeconds());
    }

    private boolean isReceiptTimedOut(AdvancePayout payout) {
        return Duration.between(payout.getUpdatedAt(), LocalDateTime.now()).getSeconds()
                >= properties.getWorker().getReceiptTimeoutSeconds();
    }

    private String receiptFailureReason(ChainReceiptResult receiptResult) {
        return receiptResult.failureCode() == null ? TransferFailureCode.UNKNOWN.name() : receiptResult.failureCode().name();
    }

    private record PendingSubmission(
            String advancePayoutId,
            String signedTransaction
    ) {
    }

    private record ClaimedJob(
            Long jobId,
            JobType jobType,
            String referenceId
    ) {
    }

    private record PollTarget(
            String advancePayoutId,
            String txHash
    ) {
    }
}
