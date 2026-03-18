package com.workproofpay.backend.remittance;

import com.workproofpay.backend.jobs.model.Job;
import com.workproofpay.backend.jobs.model.JobStatus;
import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.repo.JobRepository;
import com.workproofpay.backend.jobs.service.JobService;
import com.workproofpay.backend.jobs.service.RemittanceJobWorker;
import com.workproofpay.backend.remittance.adapter.RemittanceBlockchainGateway;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.Transfer;
import com.workproofpay.backend.remittance.model.TransferFailureCode;
import com.workproofpay.backend.remittance.model.TransferStatus;
import com.workproofpay.backend.remittance.repo.TransferRepository;
import com.workproofpay.backend.remittance.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemittanceJobWorkerTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private RemittanceBlockchainGateway blockchainGateway;

    @Mock
    private JobService jobService;

    @Mock
    private TransactionTemplate transactionTemplate;

    private RemittanceJobWorker worker;

    @BeforeEach
    void setUp() {
        RemittanceProperties properties = new RemittanceProperties();
        properties.getWorker().setReceiptPollDelaySeconds(0L);
        properties.getWorker().setReceiptTimeoutSeconds(5L);

        worker = new RemittanceJobWorker(
                jobRepository,
                transferRepository,
                walletService,
                blockchainGateway,
                jobService,
                properties,
                transactionTemplate
        );

        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
        doAnswer(invocation -> {
            ((Consumer<TransactionStatus>) invocation.getArgument(0)).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void marksSubmittedTransferFailedWhenReceiptTimesOutAndChainDoesNotKnowTx() {
        Transfer transfer = Transfer.request(
                "tr_timeout",
                1L,
                "rcp_1",
                "dUSDC",
                50_000_000L,
                "0x1111111111111111111111111111111111111111",
                "0x2222222222222222222222222222222222222222",
                "idem-timeout",
                false,
                true
        );
        transfer.onCreate();
        transfer.markSigned("0xdeadbeef", "demo:signed");
        transfer.markBroadcasted();
        transfer.onUpdate();
        ReflectionTestUtils.setField(transfer, "updatedAt", LocalDateTime.now().minusSeconds(10));

        Job job = Job.queue(JobType.POLL_TRANSFER_RECEIPT, transfer.getTransferId(), LocalDateTime.now());
        job.onCreate();
        ReflectionTestUtils.setField(job, "id", 1L);

        when(jobRepository.findTop20ByStatusAndRunAtLessThanEqualOrderByIdAsc(any(), any()))
                .thenReturn(List.of(job));
        when(jobRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(job));
        when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(transferRepository.findById(anyString())).thenReturn(Optional.of(transfer));
        when(blockchainGateway.getReceipt(transfer.getTxHash())).thenReturn(Optional.empty());
        when(blockchainGateway.isTransactionKnown(transfer.getTxHash())).thenReturn(false);

        worker.run();

        assertThat(job.getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.TIMED_OUT);
        assertThat(transfer.getFailureCode()).isEqualTo(TransferFailureCode.NETWORK_ERROR);
        assertThat(transfer.getSignedTransaction()).isNull();
    }

    @Test
    void keepsBroadcastedTransferTrackableWhenKnownTxReceiptIsDelayed() {
        Transfer transfer = Transfer.request(
                "tr_known",
                1L,
                "rcp_1",
                "dUSDC",
                50_000_000L,
                "0x1111111111111111111111111111111111111111",
                "0x2222222222222222222222222222222222222222",
                "idem-known",
                false,
                true
        );
        transfer.onCreate();
        transfer.markSigned("0xfeedbeef", "demo:signed");
        transfer.markBroadcasted();
        transfer.onUpdate();
        ReflectionTestUtils.setField(transfer, "updatedAt", LocalDateTime.now().minusSeconds(10));

        Job job = Job.queue(JobType.POLL_TRANSFER_RECEIPT, transfer.getTransferId(), LocalDateTime.now());
        job.onCreate();
        ReflectionTestUtils.setField(job, "id", 2L);

        when(jobRepository.findTop20ByStatusAndRunAtLessThanEqualOrderByIdAsc(any(), any()))
                .thenReturn(List.of(job));
        when(jobRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(job));
        when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(transferRepository.findById(anyString())).thenReturn(Optional.of(transfer));
        when(blockchainGateway.getReceipt(transfer.getTxHash())).thenReturn(Optional.empty());
        when(blockchainGateway.isTransactionKnown(transfer.getTxHash())).thenReturn(true);

        worker.run();

        assertThat(job.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.BROADCASTED);
    }
}
