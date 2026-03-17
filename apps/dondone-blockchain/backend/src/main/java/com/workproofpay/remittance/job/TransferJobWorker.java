package com.workproofpay.remittance.job;

import com.workproofpay.remittance.config.AppProperties;
import com.workproofpay.remittance.domain.*;
import com.workproofpay.remittance.gateway.Erc20Gateway;
import com.workproofpay.remittance.gateway.TxReceiptResult;
import com.workproofpay.remittance.repo.JobRepository;
import com.workproofpay.remittance.repo.TransferRepository;
import com.workproofpay.remittance.service.UserWalletService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class TransferJobWorker {
    private final JobRepository jobRepository;
    private final TransferRepository transferRepository;
    private final Erc20Gateway erc20Gateway;
    private final AppProperties appProperties;
    private final UserWalletService userWalletService;

    public TransferJobWorker(
            JobRepository jobRepository,
            TransferRepository transferRepository,
            Erc20Gateway erc20Gateway,
            AppProperties appProperties,
            UserWalletService userWalletService
    ) {
        this.jobRepository = jobRepository;
        this.transferRepository = transferRepository;
        this.erc20Gateway = erc20Gateway;
        this.appProperties = appProperties;
        this.userWalletService = userWalletService;
    }

    @Scheduled(fixedDelayString = "${app.worker.poll-interval-ms:2000}")
    @Transactional
    public void run() {
        List<JobEntity> jobs = jobRepository.findTop20ByStatusAndRunAtLessThanEqualOrderByIdAsc(
                JobStatus.QUEUED,
                Instant.now()
        );
        for (JobEntity job : jobs) {
            process(job);
        }
    }

    private void process(JobEntity job) {
        job.setStatus(JobStatus.RUNNING);
        job.setAttemptCount(job.getAttemptCount() + 1);

        try {
            switch (job.getJobType()) {
                case SUBMIT_TRANSFER -> handleSubmit(job);
                case POLL_TRANSFER_RECEIPT -> handlePoll(job);
                case RENDER_TRANSFER_RECEIPT -> handleRender(job);
            }
            job.setStatus(JobStatus.DONE);
            job.setLastError(null);
        } catch (Exception e) {
            job.setStatus(JobStatus.QUEUED);
            job.setRunAt(Instant.now().plusSeconds(3));
            job.setLastError(e.getMessage());
            if (job.getAttemptCount() >= 10) {
                job.setStatus(JobStatus.FAILED);
            }
        }
    }

    private void handleSubmit(JobEntity job) {
        TransferEntity transfer = transferRepository.findById(job.getTransferId()).orElseThrow();
        if (transfer.getStatus() != TransferStatus.REQUESTED) {
            return;
        }

        String senderPrivateKey = userWalletService.getDecryptedPrivateKey(transfer.getUserId());
        String txHash = erc20Gateway.submitTransfer(
                "demo-token",
                transfer.getRecipientAddress(),
                transfer.getAmountAtomic(),
                senderPrivateKey
        );
        transfer.setStatus(TransferStatus.SUBMITTED);
        transfer.setTxHash(txHash);
        transfer.setUpdatedAt(Instant.now());

        enqueue(job.getTransferId(), JobType.POLL_TRANSFER_RECEIPT, Instant.now().plusSeconds(2));
    }

    private void handlePoll(JobEntity job) {
        TransferEntity transfer = transferRepository.findById(job.getTransferId()).orElseThrow();
        if (transfer.getStatus() != TransferStatus.SUBMITTED) {
            return;
        }

        Optional<TxReceiptResult> receipt = erc20Gateway.getReceipt(transfer.getTxHash());
        if (receipt.isEmpty()) {
            enqueue(job.getTransferId(), JobType.POLL_TRANSFER_RECEIPT, Instant.now().plusSeconds(2));
            return;
        }

        TxReceiptResult r = receipt.get();
        if (r.success()) {
            transfer.setStatus(TransferStatus.CONFIRMED);
            transfer.setFailureCode(null);
            transfer.setUpdatedAt(Instant.now());
            enqueue(job.getTransferId(), JobType.RENDER_TRANSFER_RECEIPT, Instant.now());
        } else {
            transfer.setStatus(TransferStatus.FAILED);
            transfer.setFailureCode(r.failureCode() == null ? FailureCode.UNKNOWN.name() : r.failureCode());
            transfer.setUpdatedAt(Instant.now());
        }
    }

    private void handleRender(JobEntity job) {
        TransferEntity transfer = transferRepository.findById(job.getTransferId()).orElseThrow();
        if (transfer.getStatus() != TransferStatus.CONFIRMED) {
            return;
        }
        transfer.setUpdatedAt(Instant.now());
    }

    private void enqueue(String transferId, JobType type, Instant runAt) {
        JobEntity next = new JobEntity();
        next.setTransferId(transferId);
        next.setJobType(type);
        next.setStatus(JobStatus.QUEUED);
        next.setAttemptCount(0);
        next.setRunAt(runAt);
        jobRepository.save(next);
    }
}
