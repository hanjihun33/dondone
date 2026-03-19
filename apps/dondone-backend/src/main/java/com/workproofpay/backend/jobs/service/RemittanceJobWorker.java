package com.workproofpay.backend.jobs.service;

import com.workproofpay.backend.jobs.model.Job;
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
import com.workproofpay.backend.remittance.service.WalletService;
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
    private final RemittanceBlockchainGateway blockchainGateway;
    private final JobService jobService;
    private final RemittanceProperties properties;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelayString = "${remittance.worker.poll-interval-ms:2000}")
    public void run() {
        List<Long> jobIds = transactionTemplate.execute(status ->
                jobRepository.findTop20ByStatusAndRunAtLessThanEqualOrderByIdAsc(JobStatus.QUEUED, LocalDateTime.now())
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
        JobType jobType = claim(jobId);
        if (jobType == null) {
            return;
        }

        try {
            switch (jobType) {
                case SUBMIT_TRANSFER -> handleSubmit(jobId);
                case POLL_TRANSFER_RECEIPT -> handlePoll(jobId);
            }
        } catch (Exception e) {
            handleFailure(jobId, e);
        }
    }

    private JobType claim(Long jobId) {
        return transactionTemplate.execute(status -> {
            Job job = jobRepository.findByIdForUpdate(jobId).orElse(null);
            if (job == null || job.getStatus() != JobStatus.QUEUED || job.getRunAt().isAfter(LocalDateTime.now())) {
                return null;
            }
            job.markRunning();
            return job.getJobType();
        });
    }

    private void handleSubmit(Long jobId) {
        PendingSubmission submission = transactionTemplate.execute(status -> prepareSubmission(jobId));
        if (submission == null) {
            markJobDone(jobId);
            return;
        }

        blockchainGateway.broadcastSignedTransaction(submission.signedTransaction());
        transactionTemplate.executeWithoutResult(status -> completeSubmit(jobId, submission.transferId()));
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
            transfer.markSigned(prepared.txHash(), prepared.signedTransaction());
            return new PendingSubmission(transfer.getTransferId(), prepared.signedTransaction());
        }

        if (transfer.getStatus() == TransferStatus.SIGNED
                && transfer.getSignedTransaction() != null
                && transfer.getTxHash() != null) {
            return new PendingSubmission(transfer.getTransferId(), transfer.getSignedTransaction());
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

    private void handlePoll(Long jobId) {
        PollTarget pollTarget = transactionTemplate.execute(status -> {
            Transfer transfer = requiredTransfer(requiredJob(jobId).getReferenceId());
            if (transfer.getStatus() != TransferStatus.BROADCASTED || transfer.getTxHash() == null) {
                return null;
            }
            return new PollTarget(transfer.getTransferId(), transfer.getTxHash());
        });

        if (pollTarget == null) {
            markJobDone(jobId);
            return;
        }

        Optional<ChainReceiptResult> receiptResult = blockchainGateway.getReceipt(pollTarget.txHash());
        transactionTemplate.executeWithoutResult(status -> applyPollResult(jobId, pollTarget.transferId(), receiptResult));
    }

    private void applyPollResult(Long jobId, String transferId, Optional<ChainReceiptResult> receiptResult) {
        Job job = requiredJob(jobId);
        Transfer transfer = requiredTransfer(transferId);
        if (transfer.getStatus() != TransferStatus.BROADCASTED || transfer.getTxHash() == null) {
            job.markDone();
            return;
        }

        if (receiptResult.isEmpty()) {
            if (isReceiptTimedOut(transfer) && !blockchainGateway.isTransactionKnown(transfer.getTxHash())) {
                transfer.markTimedOut(TransferFailureCode.NETWORK_ERROR);
                job.markDone();
                return;
            }
            if (isReceiptTimedOut(transfer)) {
                job.requeue(nextPollAt(), "receipt delayed for known transaction");
                return;
            }
            job.requeue(nextPollAt(), null);
            return;
        }

        if (receiptResult.get().success()) {
            transfer.markConfirmed();
        } else {
            transfer.markFailed(receiptResult.get().failureCode() == null ? TransferFailureCode.UNKNOWN : receiptResult.get().failureCode());
        }
        job.markDone();
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
        if (jobRepository.existsByReferenceIdAndJobTypeAndStatusIn(
                transferId,
                JobType.POLL_TRANSFER_RECEIPT,
                ACTIVE_JOB_STATUSES
        )) {
            return;
        }
        jobService.enqueue(JobType.POLL_TRANSFER_RECEIPT, transferId, nextPollAt());
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

    private record PendingSubmission(
            String transferId,
            String signedTransaction
    ) {
    }

    private record PollTarget(
            String transferId,
            String txHash
    ) {
    }
}
