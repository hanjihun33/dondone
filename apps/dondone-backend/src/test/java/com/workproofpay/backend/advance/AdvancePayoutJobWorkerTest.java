package com.workproofpay.backend.advance;

import com.workproofpay.backend.advance.model.AdvancePayout;
import com.workproofpay.backend.advance.model.AdvancePayoutStatus;
import com.workproofpay.backend.advance.repo.AdvancePayoutRepository;
import com.workproofpay.backend.jobs.model.Job;
import com.workproofpay.backend.jobs.model.JobReferenceKind;
import com.workproofpay.backend.jobs.model.JobStatus;
import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.repo.JobRepository;
import com.workproofpay.backend.jobs.service.AdvancePayoutJobWorker;
import com.workproofpay.backend.jobs.service.JobService;
import com.workproofpay.backend.remittance.adapter.ChainReceiptResult;
import com.workproofpay.backend.remittance.adapter.PreparedTokenTransfer;
import com.workproofpay.backend.remittance.adapter.RemittanceBlockchainGateway;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.TransferFailureCode;
import com.workproofpay.backend.remittance.service.WalletCryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvancePayoutJobWorkerTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private AdvancePayoutRepository advancePayoutRepository;

    @Mock
    private RemittanceBlockchainGateway blockchainGateway;

    @Mock
    private WalletCryptoService walletCryptoService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private JobService jobService;

    private AdvancePayoutJobWorker worker;

    @BeforeEach
    void setUp() {
        RemittanceProperties properties = new RemittanceProperties();
        properties.getPolicy().setMaxAutoRetryCount(1);
        properties.getWorker().setReceiptPollDelaySeconds(0L);
        properties.getWorker().setReceiptTimeoutSeconds(5L);
        properties.getTreasury().setPrivateKey("0xtreasury");

        worker = new AdvancePayoutJobWorker(
                jobRepository,
                advancePayoutRepository,
                blockchainGateway,
                walletCryptoService,
                properties,
                transactionTemplate,
                jobService
        );

        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
        lenient().doAnswer(invocation -> {
            ((Consumer<TransactionStatus>) invocation.getArgument(0)).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        lenient().when(walletCryptoService.encrypt(anyString()))
                .thenAnswer(invocation -> "enc:" + invocation.getArgument(0, String.class));
        lenient().when(walletCryptoService.decrypt(anyString()))
                .thenAnswer(invocation -> {
                    String value = invocation.getArgument(0, String.class);
                    return value.startsWith("enc:") ? value.substring(4) : value;
                });
    }

    @Test
    void marksPayoutBroadcastedWhenSubmitSucceeds() {
        AdvancePayout payout = AdvancePayout.request(
                "ap_1234567890abcdef",
                10L,
                1L,
                "0x1111111111111111111111111111111111111111",
                50_000_000L,
                "dUSDC",
                "advance-payout:10"
        );
        payout.onCreate();

        Job job = Job.queue(JobReferenceKind.ADVANCE_PAYOUT, JobType.SUBMIT_ADVANCE_PAYOUT, payout.getAdvancePayoutId(), LocalDateTime.now());
        job.onCreate();
        org.springframework.test.util.ReflectionTestUtils.setField(job, "id", 1L);

        when(jobRepository.findTop20ByReferenceKindAndStatusAndRunAtLessThanEqualOrderByIdAsc(
                eq(JobReferenceKind.ADVANCE_PAYOUT),
                eq(JobStatus.QUEUED),
                any()
        )).thenReturn(List.of(job));
        when(jobRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(job));
        lenient().when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(advancePayoutRepository.findByIdForUpdate(payout.getAdvancePayoutId())).thenReturn(Optional.of(payout));
        when(blockchainGateway.prepareTokenTransfer(
                eq("0xtreasury"),
                eq(payout.getWalletAddress()),
                eq(java.math.BigInteger.valueOf(payout.getAmountAtomic()))
        )).thenReturn(new PreparedTokenTransfer(
                "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "signed-payout"
        ));

        worker.run();

        assertThat(payout.getStatus()).isEqualTo(AdvancePayoutStatus.BROADCASTED);
        assertThat(payout.getTxHash()).isEqualTo("0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertThat(payout.getSignedTransaction()).isEqualTo("enc:signed-payout");
        assertThat(job.getStatus()).isEqualTo(JobStatus.DONE);
    }

    @Test
    void requeuesAndEventuallyFailsPayoutWhenSubmitKeepsFailing() {
        AdvancePayout payout = AdvancePayout.request(
                "ap_retryfailed0001",
                11L,
                1L,
                "0x2222222222222222222222222222222222222222",
                51_000_000L,
                "dUSDC",
                "advance-payout:11"
        );
        payout.onCreate();

        Job job = Job.queue(JobReferenceKind.ADVANCE_PAYOUT, JobType.SUBMIT_ADVANCE_PAYOUT, payout.getAdvancePayoutId(), LocalDateTime.now());
        job.onCreate();
        org.springframework.test.util.ReflectionTestUtils.setField(job, "id", 2L);

        when(jobRepository.findTop20ByReferenceKindAndStatusAndRunAtLessThanEqualOrderByIdAsc(
                eq(JobReferenceKind.ADVANCE_PAYOUT),
                eq(JobStatus.QUEUED),
                any()
        )).thenReturn(List.of(job));
        when(jobRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(job));
        lenient().when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(advancePayoutRepository.findByIdForUpdate(payout.getAdvancePayoutId())).thenReturn(Optional.of(payout));
        lenient().when(advancePayoutRepository.findById(payout.getAdvancePayoutId())).thenReturn(Optional.of(payout));
        when(blockchainGateway.prepareTokenTransfer(
                anyString(),
                anyString(),
                any()
        )).thenThrow(new IllegalStateException("treasury unavailable"));

        worker.run();
        assertThat(job.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(payout.getStatus()).isEqualTo(AdvancePayoutStatus.REQUESTED);

        org.springframework.test.util.ReflectionTestUtils.setField(job, "runAt", LocalDateTime.now().minusSeconds(1));
        worker.run();
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(payout.getStatus()).isEqualTo(AdvancePayoutStatus.FAILED);
        assertThat(payout.getFailureReason()).isEqualTo("treasury unavailable");
    }

    @Test
    void marksPayoutConfirmedWhenReceiptSucceeds() {
        AdvancePayout payout = AdvancePayout.request(
                "ap_confirm00000001",
                12L,
                1L,
                "0x3333333333333333333333333333333333333333",
                52_000_000L,
                "dUSDC",
                "advance-payout:12"
        );
        payout.onCreate();
        payout.markSigned("0xcccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc", "signed-confirm");
        payout.markBroadcasted();
        payout.onUpdate();

        Job job = Job.queue(JobReferenceKind.ADVANCE_PAYOUT, JobType.POLL_ADVANCE_PAYOUT_RECEIPT, payout.getAdvancePayoutId(), LocalDateTime.now());
        job.onCreate();
        org.springframework.test.util.ReflectionTestUtils.setField(job, "id", 3L);

        when(jobRepository.findTop20ByReferenceKindAndStatusAndRunAtLessThanEqualOrderByIdAsc(
                eq(JobReferenceKind.ADVANCE_PAYOUT),
                eq(JobStatus.QUEUED),
                any()
        )).thenReturn(List.of(job));
        when(jobRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(job));
        lenient().when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(advancePayoutRepository.findByIdForUpdate(payout.getAdvancePayoutId())).thenReturn(Optional.of(payout));
        when(blockchainGateway.getReceipt(payout.getTxHash()))
                .thenReturn(Optional.of(new ChainReceiptResult(true, null, "21000")));

        worker.run();

        assertThat(payout.getStatus()).isEqualTo(AdvancePayoutStatus.CONFIRMED);
        assertThat(payout.getSignedTransaction()).isNull();
        assertThat(job.getStatus()).isEqualTo(JobStatus.DONE);
    }

    @Test
    void marksPayoutTimedOutWhenReceiptIsMissingAndChainDoesNotKnowTx() {
        AdvancePayout payout = AdvancePayout.request(
                "ap_timeout00000001",
                13L,
                1L,
                "0x4444444444444444444444444444444444444444",
                53_000_000L,
                "dUSDC",
                "advance-payout:13"
        );
        payout.onCreate();
        payout.markSigned("0xdddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd", "signed-timeout");
        payout.markBroadcasted();
        payout.onUpdate();
        org.springframework.test.util.ReflectionTestUtils.setField(payout, "updatedAt", LocalDateTime.now().minusSeconds(10));

        Job job = Job.queue(JobReferenceKind.ADVANCE_PAYOUT, JobType.POLL_ADVANCE_PAYOUT_RECEIPT, payout.getAdvancePayoutId(), LocalDateTime.now());
        job.onCreate();
        org.springframework.test.util.ReflectionTestUtils.setField(job, "id", 4L);

        when(jobRepository.findTop20ByReferenceKindAndStatusAndRunAtLessThanEqualOrderByIdAsc(
                eq(JobReferenceKind.ADVANCE_PAYOUT),
                eq(JobStatus.QUEUED),
                any()
        )).thenReturn(List.of(job));
        when(jobRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(job));
        lenient().when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(advancePayoutRepository.findByIdForUpdate(payout.getAdvancePayoutId())).thenReturn(Optional.of(payout));
        when(blockchainGateway.getReceipt(payout.getTxHash())).thenReturn(Optional.empty());
        when(blockchainGateway.isTransactionKnown(payout.getTxHash())).thenReturn(false);

        worker.run();

        assertThat(payout.getStatus()).isEqualTo(AdvancePayoutStatus.TIMED_OUT);
        assertThat(payout.getFailureReason()).isEqualTo("NETWORK_ERROR");
        assertThat(payout.getSignedTransaction()).isNull();
        assertThat(job.getStatus()).isEqualTo(JobStatus.DONE);
    }

    @Test
    void marksPayoutFailedWhenReceiptReportsRevert() {
        AdvancePayout payout = AdvancePayout.request(
                "ap_failed000000001",
                14L,
                1L,
                "0x5555555555555555555555555555555555555555",
                54_000_000L,
                "dUSDC",
                "advance-payout:14"
        );
        payout.onCreate();
        payout.markSigned("0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", "signed-failed");
        payout.markBroadcasted();
        payout.onUpdate();

        Job job = Job.queue(JobReferenceKind.ADVANCE_PAYOUT, JobType.POLL_ADVANCE_PAYOUT_RECEIPT, payout.getAdvancePayoutId(), LocalDateTime.now());
        job.onCreate();
        org.springframework.test.util.ReflectionTestUtils.setField(job, "id", 5L);

        when(jobRepository.findTop20ByReferenceKindAndStatusAndRunAtLessThanEqualOrderByIdAsc(
                eq(JobReferenceKind.ADVANCE_PAYOUT),
                eq(JobStatus.QUEUED),
                any()
        )).thenReturn(List.of(job));
        when(jobRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(job));
        lenient().when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(advancePayoutRepository.findByIdForUpdate(payout.getAdvancePayoutId())).thenReturn(Optional.of(payout));
        when(blockchainGateway.getReceipt(payout.getTxHash()))
                .thenReturn(Optional.of(new ChainReceiptResult(false, TransferFailureCode.CHAIN_REVERT, "21000")));

        worker.run();

        assertThat(payout.getStatus()).isEqualTo(AdvancePayoutStatus.FAILED);
        assertThat(payout.getFailureReason()).isEqualTo("CHAIN_REVERT");
        assertThat(job.getStatus()).isEqualTo(JobStatus.DONE);
    }
}
