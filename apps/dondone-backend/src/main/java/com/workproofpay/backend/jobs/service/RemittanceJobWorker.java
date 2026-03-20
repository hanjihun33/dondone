package com.workproofpay.backend.jobs.service;

import com.workproofpay.backend.jobs.model.Job;
import com.workproofpay.backend.jobs.model.JobReferenceKind;
import com.workproofpay.backend.jobs.model.JobStatus;
import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.repo.JobRepository;
import com.workproofpay.backend.remittance.adapter.ChainReceiptResult;
import com.workproofpay.backend.remittance.adapter.PreparedTokenTransfer;
import com.workproofpay.backend.remittance.adapter.RemittanceBlockchainGateway;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.Transfer;
import com.workproofpay.backend.remittance.model.TransferFailureCode;
import com.workproofpay.backend.remittance.model.TransferStatus;
import com.workproofpay.backend.remittance.repo.TransferRepository;
import com.workproofpay.backend.remittance.service.WalletCryptoService;
import com.workproofpay.backend.remittance.service.WalletService;
import com.workproofpay.backend.remittance.service.RemittanceMetrics;
import io.micrometer.core.instrument.Timer;
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
public class RemittanceJobWorker {

    private static final List<JobStatus> ACTIVE_JOB_STATUSES = List.of(JobStatus.QUEUED, JobStatus.RUNNING);

    private final JobRepository jobRepository;
    private final TransferRepository transferRepository;
    private final WalletService walletService;
    private final WalletCryptoService walletCryptoService;
    private final RemittanceBlockchainGateway blockchainGateway;
    private final JobService jobService;
    private final RemittanceProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final RemittanceMetrics remittanceMetrics;

    @Scheduled(fixedDelayString = "${remittance.worker.poll-interval-ms:2000}")
    public void run() {
        List<Long> jobIds = transactionTemplate.execute(status ->
                jobRepository.findTop20ByReferenceKindAndStatusAndRunAtLessThanEqualOrderByIdAsc(
                                JobReferenceKind.TRANSFER,
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
                case SUBMIT_TRANSFER -> handleSubmit(claimedJob);
                case POLL_TRANSFER_RECEIPT -> handlePoll(claimedJob);
            }
        } catch (Exception e) {
            handleFailure(claimedJob.jobId(), e);
        }
    }

    private ClaimedJob claim(Long jobId) {
        return transactionTemplate.execute(status -> {
            LocalDateTime now = LocalDateTime.now();
            Job job = jobRepository.findByIdForUpdate(jobId).orElse(null);
            if (job == null || job.getStatus() != JobStatus.QUEUED || job.getRunAt().isAfter(now)) {
                return null;
            }
            job.markRunning();
            return new ClaimedJob(job.getId(), job.getJobType(), job.getReferenceId(), job.getRunAt(), now);
        });
    }

    private void handleSubmit(ClaimedJob claimedJob) {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "error";

        try {
            PendingSubmission submission = transactionTemplate.execute(status -> prepareSubmission(claimedJob.jobId()));
            if (submission == null) {
                markJobDone(claimedJob.jobId());
                outcome = "skipped";
                return;
            }

            blockchainGateway.broadcastSignedTransaction(submission.signedTransaction());
            transactionTemplate.executeWithoutResult(status -> completeSubmit(claimedJob.jobId(), submission.transferId()));
            outcome = "broadcasted";
        } finally {
            remittanceMetrics.recordWorkerJob(sample, JobType.SUBMIT_TRANSFER.name().toLowerCase(), outcome);
        }
    }

    private PendingSubmission prepareSubmission(Long jobId) {
        Transfer transfer = requiredTransfer(requiredJob(jobId).getReferenceId());
        if (transfer.getStatus() == TransferStatus.REQUESTED) {
            String senderPrivateKey = walletService.getDecryptedPrivateKey(transfer.getUserId());
            PreparedTokenTransfer prepared = blockchainGateway.prepareTokenTransfer(
                    senderPrivateKey,
                    transfer.getRecipientAddress(),
                    BigInteger.valueOf(transfer.getAmountAtomic())
            );
            transfer.markSigned(prepared.txHash(), walletCryptoService.encrypt(prepared.signedTransaction()));
            return new PendingSubmission(transfer.getTransferId(), prepared.signedTransaction(), transfer.getCreatedAt());
        }

        if (transfer.getStatus() == TransferStatus.SIGNED
                && transfer.getSignedTransaction() != null
                && transfer.getTxHash() != null) {
            return new PendingSubmission(
                    transfer.getTransferId(),
                    decryptSignedTransaction(transfer.getSignedTransaction()),
                    transfer.getCreatedAt()
            );
        }

        return null;
    }

    private void completeSubmit(Long jobId, String transferId) {
        Transfer transfer = requiredTransfer(transferId);
        if (transfer.getStatus() == TransferStatus.SIGNED) {
            transfer.markBroadcasted();
        }
        enqueuePollJobIfAbsent(transferId);
        requiredJob(jobId).markDone();
    }

    private void handlePoll(ClaimedJob claimedJob) {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "error";

        try {
            PollTarget pollTarget = transactionTemplate.execute(status -> {
                Transfer transfer = requiredTransfer(requiredJob(claimedJob.jobId()).getReferenceId());
                if (transfer.getStatus() != TransferStatus.BROADCASTED || transfer.getTxHash() == null) {
                    return null;
                }
                return new PollTarget(transfer.getTransferId(), transfer.getTxHash(), transfer.getCreatedAt());
            });

            if (pollTarget == null) {
                markJobDone(claimedJob.jobId());
                outcome = "skipped";
                return;
            }

            Optional<ChainReceiptResult> receiptResult = blockchainGateway.getReceipt(pollTarget.txHash());
            outcome = transactionTemplate.execute(status -> applyPollResult(claimedJob.jobId(), pollTarget.transferId(), receiptResult));
        } finally {
            remittanceMetrics.recordWorkerJob(sample, JobType.POLL_TRANSFER_RECEIPT.name().toLowerCase(), outcome);
        }
    }

    private String applyPollResult(Long jobId, String transferId, Optional<ChainReceiptResult> receiptResult) {
        Job job = requiredJob(jobId);
        Transfer transfer = requiredTransfer(transferId);
        if (transfer.getStatus() != TransferStatus.BROADCASTED || transfer.getTxHash() == null) {
            job.markDone();
            return "skipped";
        }

        if (receiptResult.isEmpty()) {
            if (isReceiptTimedOut(transfer) && !blockchainGateway.isTransactionKnown(transfer.getTxHash())) {
                transfer.markTimedOut(TransferFailureCode.NETWORK_ERROR);
                job.markDone();
                return "timed_out_network_error";
            }
            if (isReceiptTimedOut(transfer)) {
                job.requeue(nextPollAt(), "receipt delayed for known transaction");
                return "requeued_known_transaction";
            }
            job.requeue(nextPollAt(), null);
            return "requeued_waiting_receipt";
        }

        if (receiptResult.get().success()) {
            transfer.markConfirmed();
            job.markDone();
            return "confirmed";
        }

        transfer.markFailed(receiptResult.get().failureCode() == null ? TransferFailureCode.UNKNOWN : receiptResult.get().failureCode());
        job.markDone();
        return "failed_" + (receiptResult.get().failureCode() == null
                ? TransferFailureCode.UNKNOWN.name()
                : receiptResult.get().failureCode().name()).toLowerCase();
    }

    private void handleFailure(Long jobId, Exception e) {
        transactionTemplate.executeWithoutResult(status -> {
            Job job = requiredJob(jobId);
            if (job.getStatus() != JobStatus.RUNNING) {
                return;
            }

            if (job.getAttemptCount() > properties.getPolicy().getMaxAutoRetryCount()) {
                if (job.getJobType() == JobType.SUBMIT_TRANSFER && keepBroadcastedTransferTrackable(job.getReferenceId())) {
                    job.markFailed(e.getMessage());
                    return;
                }
                markTransferFailed(job.getReferenceId(), TransferFailureCode.UNKNOWN);
                job.markFailed(e.getMessage());
                return;
            }

            job.requeue(LocalDateTime.now().plusSeconds(3L), e.getMessage());
        });
    }

    private boolean keepBroadcastedTransferTrackable(String transferId) {
        Transfer transfer = transferRepository.findById(transferId).orElse(null);
        if (transfer == null || transfer.getStatus() != TransferStatus.BROADCASTED || transfer.getTxHash() == null) {
            return false;
        }
        enqueuePollJobIfAbsent(transferId);
        return true;
    }

    private void enqueuePollJobIfAbsent(String transferId) {
        if (jobRepository.existsByReferenceKindAndReferenceIdAndJobTypeAndStatusIn(
                JobReferenceKind.TRANSFER,
                transferId,
                JobType.POLL_TRANSFER_RECEIPT,
                ACTIVE_JOB_STATUSES
        )) {
            return;
        }
        jobService.enqueue(JobReferenceKind.TRANSFER, JobType.POLL_TRANSFER_RECEIPT, transferId, nextPollAt());
    }

    private void markJobDone(Long jobId) {
        transactionTemplate.executeWithoutResult(status -> {
            Job job = requiredJob(jobId);
            if (job.getStatus() != JobStatus.DONE) {
                job.markDone();
            }
        });
    }

    private void markTransferFailed(String transferId, TransferFailureCode failureCode) {
        Transfer transfer = transferRepository.findById(transferId).orElse(null);
        if (transfer == null) {
            return;
        }
        if (transfer.getStatus() == TransferStatus.REQUESTED
                || transfer.getStatus() == TransferStatus.SIGNED
                || transfer.getStatus() == TransferStatus.BROADCASTED) {
            transfer.markFailed(failureCode);
        }
    }

    private Job requiredJob(Long jobId) {
        return jobRepository.findById(jobId).orElseThrow();
    }

    private Transfer requiredTransfer(String transferId) {
        return transferRepository.findById(transferId).orElseThrow();
    }

    private LocalDateTime nextPollAt() {
        return LocalDateTime.now().plusSeconds(properties.getWorker().getReceiptPollDelaySeconds());
    }

    private boolean isReceiptTimedOut(Transfer transfer) {
        return Duration.between(transfer.getUpdatedAt(), LocalDateTime.now()).getSeconds()
                >= properties.getWorker().getReceiptTimeoutSeconds();
    }

    private String decryptSignedTransaction(String encryptedOrLegacySignedTransaction) {
        try {
            return walletCryptoService.decrypt(encryptedOrLegacySignedTransaction);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return encryptedOrLegacySignedTransaction;
        }
    }

    private record PendingSubmission(
            String transferId,
            String signedTransaction,
            LocalDateTime createdAt
    ) {
    }

    private record PollTarget(
            String transferId,
            String txHash,
            LocalDateTime createdAt
    ) {
    }

    private record ClaimedJob(
            Long jobId,
            JobType jobType,
            String referenceId,
            LocalDateTime runAt,
            LocalDateTime claimedAt
    ) {
    }
}
