package com.workproofpay.backend.remittance;

import com.workproofpay.backend.jobs.model.Job;
import com.workproofpay.backend.jobs.model.JobStatus;
import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.model.JobReferenceKind;
import com.workproofpay.backend.jobs.repo.JobRepository;
import com.workproofpay.backend.jobs.service.JobService;
import com.workproofpay.backend.remittance.api.dto.response.RemittanceAdminActionResponse;
import com.workproofpay.backend.remittance.api.dto.response.RemittanceOpsJobListResponse;
import com.workproofpay.backend.remittance.api.dto.response.RemittanceOpsSummaryResponse;
import com.workproofpay.backend.remittance.api.dto.response.WalletResponse;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.Transfer;
import com.workproofpay.backend.remittance.model.TransferFailureCode;
import com.workproofpay.backend.remittance.model.RecipientRelation;
import com.workproofpay.backend.remittance.model.TransferStatus;
import com.workproofpay.backend.remittance.model.WalletFundingStatus;
import com.workproofpay.backend.remittance.model.UserWallet;
import com.workproofpay.backend.remittance.repo.TransferRepository;
import com.workproofpay.backend.remittance.repo.UserWalletRepository;
import com.workproofpay.backend.remittance.service.RemittanceOpsService;
import com.workproofpay.backend.remittance.service.WalletService;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemittanceOpsServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserWalletRepository userWalletRepository;

    @Mock
    private JobService jobService;

    @Mock
    private WalletService walletService;

    private RemittanceOpsService remittanceOpsService;

    @BeforeEach
    void setUp() {
        remittanceOpsService = new RemittanceOpsService(
                transferRepository,
                jobRepository,
                userWalletRepository,
                jobService,
                walletService,
                new RemittanceProperties()
        );
    }

    @Test
    void retriesTimedOutTransferByResettingToRequestedAndQueueingSubmitJob() {
        Transfer transfer = timedOutTransfer();
        when(transferRepository.findById(transfer.getTransferId())).thenReturn(Optional.of(transfer));
        when(jobRepository.existsByReferenceKindAndReferenceIdAndJobTypeAndStatusIn(
                eq(JobReferenceKind.TRANSFER),
                eq(transfer.getTransferId()),
                eq(JobType.SUBMIT_TRANSFER),
                any()
        ))
                .thenReturn(false);

        RemittanceAdminActionResponse response = remittanceOpsService.retryTransfer(transfer.getTransferId());

        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.REQUESTED);
        assertThat(response.status()).isEqualTo("REQUESTED");
        verify(jobService).enqueue(eq(JobReferenceKind.TRANSFER), eq(JobType.SUBMIT_TRANSFER), eq(transfer.getTransferId()), any(LocalDateTime.class));
    }

    @Test
    void retriesBroadcastedTransferByQueueingPollOnly() {
        Transfer transfer = broadcastedTransfer();
        when(transferRepository.findById(transfer.getTransferId())).thenReturn(Optional.of(transfer));
        when(jobRepository.existsByReferenceKindAndReferenceIdAndJobTypeAndStatusIn(
                eq(JobReferenceKind.TRANSFER),
                eq(transfer.getTransferId()),
                eq(JobType.POLL_TRANSFER_RECEIPT),
                any()
        ))
                .thenReturn(false);

        RemittanceAdminActionResponse response = remittanceOpsService.retryTransfer(transfer.getTransferId());

        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.BROADCASTED);
        assertThat(response.status()).isEqualTo("BROADCASTED");
        verify(jobService).enqueue(eq(JobReferenceKind.TRANSFER), eq(JobType.POLL_TRANSFER_RECEIPT), eq(transfer.getTransferId()), any(LocalDateTime.class));
        verify(jobService, never()).enqueue(eq(JobReferenceKind.TRANSFER), eq(JobType.SUBMIT_TRANSFER), eq(transfer.getTransferId()), any(LocalDateTime.class));
    }

    @Test
    void rejectsRetryForConfirmedTransfer() {
        Transfer transfer = requestedTransfer();
        transfer.markSigned("0xabc", "signed-tx");
        transfer.markBroadcasted();
        transfer.markConfirmed();
        when(transferRepository.findById(transfer.getTransferId())).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> remittanceOpsService.retryTransfer(transfer.getTransferId()))
                .isInstanceOf(ApiException.class)
                .satisfies(throwable -> assertThat(((ApiException) throwable).getErrorCode())
                        .isEqualTo(ErrorCode.RECOVERY_ACTION_NOT_ALLOWED));
    }

    @Test
    void retriesFailedWalletFundingOnlyForFailedWallets() {
        UserWallet wallet = UserWallet.create(5L, "0x1234567890123456789012345678901234567890", "encrypted");
        wallet.onCreate();
        wallet.markFundingFailed("rpc timeout");
        when(walletService.getRequiredWallet(5L)).thenReturn(wallet);
        when(walletService.recoverWalletFunding(5L)).thenReturn(
                new WalletResponse(5L, wallet.getWalletAddress(), WalletFundingStatus.FUNDED, null, LocalDateTime.now(), wallet.getCreatedAt())
        );

        WalletResponse response = remittanceOpsService.retryWalletFunding(5L);

        assertThat(response.fundingStatus()).isEqualTo(WalletFundingStatus.FUNDED);
    }

    @Test
    void getJobsFiltersByTransferReferenceKind() {
        Job job = Job.queue(JobType.SUBMIT_TRANSFER, "tr_jobs", LocalDateTime.now());
        job.onCreate();

        when(jobRepository.findByReferenceKindAndStatusInOrderByUpdatedAtDescIdDesc(
                eq(JobReferenceKind.TRANSFER),
                eq(List.of(JobStatus.FAILED)),
                any()
        )).thenReturn(List.of(job));

        RemittanceOpsJobListResponse response = remittanceOpsService.getJobs(null, null);

        assertThat(response.jobs()).hasSize(1);
        assertThat(response.jobs().get(0).referenceKind()).isEqualTo("TRANSFER");
    }

    @Test
    void getSummaryCountsOnlyTransferReferenceKindJobs() {
        Job failedJob = Job.queue(JobType.SUBMIT_TRANSFER, "tr_summary", LocalDateTime.now());
        failedJob.onCreate();
        failedJob.markFailed("chain timeout");

        for (TransferStatus status : TransferStatus.values()) {
            when(transferRepository.countByStatus(status)).thenReturn(0L);
        }
        for (WalletFundingStatus status : WalletFundingStatus.values()) {
            when(userWalletRepository.countByFundingStatus(status)).thenReturn(0L);
        }
        for (JobStatus status : JobStatus.values()) {
            when(jobRepository.countByReferenceKindAndStatus(JobReferenceKind.TRANSFER, status)).thenReturn(0L);
        }
        when(jobRepository.countByReferenceKindAndStatus(JobReferenceKind.TRANSFER, JobStatus.FAILED)).thenReturn(1L);
        when(jobRepository.findByReferenceKindAndStatusInOrderByUpdatedAtDescIdDesc(
                eq(JobReferenceKind.TRANSFER),
                eq(List.of(JobStatus.FAILED)),
                any()
        )).thenReturn(List.of(failedJob));

        RemittanceOpsSummaryResponse response = remittanceOpsService.getSummary();

        assertThat(response.jobCounts()).containsEntry(JobStatus.FAILED.name(), 1L);
        assertThat(response.recentFailureReasons()).contains("chain timeout");
    }

    private Transfer broadcastedTransfer() {
        Transfer transfer = requestedTransfer();
        transfer.markSigned("0xabc", "signed-tx");
        transfer.markBroadcasted();
        return transfer;
    }

    private Transfer timedOutTransfer() {
        Transfer transfer = broadcastedTransfer();
        transfer.markTimedOut(TransferFailureCode.NETWORK_ERROR);
        return transfer;
    }

    private Transfer requestedTransfer() {
        Transfer transfer = Transfer.request(
                "tr_ops",
                1L,
                "rcp_ops",
                "dUSDC",
                50_000_000L,
                "0x1111111111111111111111111111111111111111",
                "0x2222222222222222222222222222222222222222",
                "Ops Recipient",
                RecipientRelation.FAMILY,
                null,
                "idem-ops",
                false,
                true
        );
        transfer.onCreate();
        return transfer;
    }
}
