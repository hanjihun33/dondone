package com.workproofpay.backend.jobs.service;

import com.workproofpay.backend.advance.model.AdvancePayout;
import com.workproofpay.backend.advance.model.AdvancePayoutStatus;
import com.workproofpay.backend.advance.repo.AdvancePayoutRepository;
import com.workproofpay.backend.jobs.model.Job;
import com.workproofpay.backend.jobs.model.JobReferenceKind;
import com.workproofpay.backend.jobs.model.JobStatus;
import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.repo.JobRepository;
import com.workproofpay.backend.remittance.adapter.PreparedTokenTransfer;
import com.workproofpay.backend.remittance.adapter.RemittanceBlockchainGateway;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.service.WalletCryptoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class AdvancePayoutJobWorker {

    private final JobRepository jobRepository;
    private final AdvancePayoutRepository advancePayoutRepository;
    private final RemittanceBlockchainGateway blockchainGateway;
    private final WalletCryptoService walletCryptoService;
    private final RemittanceProperties properties;
    private final TransactionTemplate transactionTemplate;

    public AdvancePayoutJobWorker(
            JobRepository jobRepository,
            AdvancePayoutRepository advancePayoutRepository,
            RemittanceBlockchainGateway blockchainGateway,
            WalletCryptoService walletCryptoService,
            RemittanceProperties properties,
            TransactionTemplate transactionTemplate
    ) {
        this.jobRepository = jobRepository;
        this.advancePayoutRepository = advancePayoutRepository;
        this.blockchainGateway = blockchainGateway;
        this.walletCryptoService = walletCryptoService;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
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
            if (claimedJob.jobType() == JobType.SUBMIT_ADVANCE_PAYOUT) {
                handleSubmit(claimedJob);
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
        requiredJob(jobId).markDone();
    }

    private void handleFailure(Long jobId, Exception e) {
        transactionTemplate.executeWithoutResult(status -> {
            Job job = requiredJob(jobId);
            if (job.getStatus() != JobStatus.RUNNING) {
                return;
            }

            if (job.getAttemptCount() > properties.getPolicy().getMaxAutoRetryCount()) {
                markPayoutFailed(job.getReferenceId(), e.getMessage());
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
}
