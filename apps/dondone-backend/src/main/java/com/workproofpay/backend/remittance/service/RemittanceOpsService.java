package com.workproofpay.backend.remittance.service;

import com.workproofpay.backend.jobs.model.Job;
import com.workproofpay.backend.jobs.model.JobStatus;
import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.repo.JobRepository;
import com.workproofpay.backend.jobs.service.JobService;
import com.workproofpay.backend.remittance.api.dto.response.RemittanceAdminActionResponse;
import com.workproofpay.backend.remittance.api.dto.response.RemittanceOpsJobItemResponse;
import com.workproofpay.backend.remittance.api.dto.response.RemittanceOpsJobListResponse;
import com.workproofpay.backend.remittance.api.dto.response.RemittanceOpsSummaryResponse;
import com.workproofpay.backend.remittance.api.dto.response.RemittanceOpsTransferItemResponse;
import com.workproofpay.backend.remittance.api.dto.response.RemittanceOpsTransferListResponse;
import com.workproofpay.backend.remittance.api.dto.response.WalletResponse;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.Transfer;
import com.workproofpay.backend.remittance.model.TransferStatus;
import com.workproofpay.backend.remittance.model.WalletFundingStatus;
import com.workproofpay.backend.remittance.repo.TransferRepository;
import com.workproofpay.backend.remittance.repo.UserWalletRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RemittanceOpsService {

    private static final List<JobStatus> ACTIVE_JOB_STATUSES = List.of(JobStatus.QUEUED, JobStatus.RUNNING);

    private final TransferRepository transferRepository;
    private final JobRepository jobRepository;
    private final UserWalletRepository userWalletRepository;
    private final JobService jobService;
    private final WalletService walletService;
    private final RemittanceProperties properties;

    @Transactional(readOnly = true)
    public RemittanceOpsSummaryResponse getSummary() {
        Map<String, Long> transferCounts = Arrays.stream(TransferStatus.values())
                .collect(Collectors.toMap(Enum::name, transferRepository::countByStatus, (left, right) -> right, java.util.LinkedHashMap::new));
        Map<String, Long> walletFundingCounts = Arrays.stream(WalletFundingStatus.values())
                .collect(Collectors.toMap(Enum::name, userWalletRepository::countByFundingStatus, (left, right) -> right, java.util.LinkedHashMap::new));
        Map<String, Long> jobCounts = Arrays.stream(JobStatus.values())
                .collect(Collectors.toMap(Enum::name, jobRepository::countByStatus, (left, right) -> right, java.util.LinkedHashMap::new));

        List<String> recentFailureReasons = jobRepository.findByStatusInOrderByUpdatedAtDescIdDesc(
                        List.of(JobStatus.FAILED),
                        PageRequest.of(0, 5)
                ).stream()
                .map(Job::getLastError)
                .filter(error -> error != null && !error.isBlank())
                .toList();

        return new RemittanceOpsSummaryResponse(transferCounts, walletFundingCounts, jobCounts, recentFailureReasons);
    }

    @Transactional(readOnly = true)
    public RemittanceOpsTransferListResponse getTransfers(List<TransferStatus> requestedStatuses, boolean stuckOnly, Integer requestedLimit) {
        int limit = normalizeLimit(requestedLimit);
        List<TransferStatus> statuses = resolveTransferStatuses(requestedStatuses, stuckOnly);
        List<Transfer> transfers = stuckOnly
                ? transferRepository.findByStatusInAndUpdatedAtBeforeOrderByUpdatedAtDescTransferIdDesc(
                        statuses,
                        LocalDateTime.now().minusSeconds(properties.getWorker().getReceiptTimeoutSeconds()),
                        PageRequest.of(0, limit)
                )
                : transferRepository.findByStatusInOrderByUpdatedAtDescTransferIdDesc(statuses, PageRequest.of(0, limit));

        return new RemittanceOpsTransferListResponse(
                transfers.stream()
                        .map(this::toOpsTransferItem)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public RemittanceOpsJobListResponse getJobs(List<JobStatus> requestedStatuses, Integer requestedLimit) {
        int limit = normalizeLimit(requestedLimit);
        List<JobStatus> statuses = requestedStatuses == null || requestedStatuses.isEmpty()
                ? List.of(JobStatus.FAILED)
                : requestedStatuses;

        return new RemittanceOpsJobListResponse(
                jobRepository.findByStatusInOrderByUpdatedAtDescIdDesc(statuses, PageRequest.of(0, limit))
                        .stream()
                        .map(this::toOpsJobItem)
                        .toList()
        );
    }

    @Transactional
    public RemittanceAdminActionResponse retryTransfer(String transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ApiException(ErrorCode.TRANSFER_NOT_FOUND));

        return switch (transfer.getStatus()) {
            case REQUESTED, SIGNED -> {
                ensureSubmitJobQueued(transferId);
                yield new RemittanceAdminActionResponse("RETRY_TRANSFER", transferId, transfer.getStatus().name(), "submit worker queued");
            }
            case BROADCASTED -> {
                ensurePollJobQueued(transferId);
                yield new RemittanceAdminActionResponse("RETRY_TRANSFER", transferId, transfer.getStatus().name(), "receipt poll queued");
            }
            case FAILED, TIMED_OUT -> {
                transfer.resetForRetry();
                ensureSubmitJobQueued(transferId);
                yield new RemittanceAdminActionResponse("RETRY_TRANSFER", transferId, transfer.getStatus().name(), "transfer reset to REQUESTED and submit worker queued");
            }
            case CONFIRMED -> throw new ApiException(
                    ErrorCode.RECOVERY_ACTION_NOT_ALLOWED,
                    "Confirmed transfer cannot be retried"
            );
        };
    }

    @Transactional
    public WalletResponse retryWalletFunding(Long userId) {
        WalletFundingStatus fundingStatus = walletService.getRequiredWallet(userId).getFundingStatus();
        if (fundingStatus == WalletFundingStatus.FUNDED) {
            throw new ApiException(
                    ErrorCode.RECOVERY_ACTION_NOT_ALLOWED,
                    "Wallet funding retry is not allowed for FUNDED wallets"
            );
        }
        return walletService.recoverWalletFunding(userId);
    }

    private void ensureSubmitJobQueued(String transferId) {
        if (jobRepository.existsByReferenceIdAndJobTypeAndStatusIn(transferId, JobType.SUBMIT_TRANSFER, ACTIVE_JOB_STATUSES)) {
            return;
        }
        jobService.enqueue(JobType.SUBMIT_TRANSFER, transferId, LocalDateTime.now());
    }

    private void ensurePollJobQueued(String transferId) {
        if (jobRepository.existsByReferenceIdAndJobTypeAndStatusIn(transferId, JobType.POLL_TRANSFER_RECEIPT, ACTIVE_JOB_STATUSES)) {
            return;
        }
        jobService.enqueue(JobType.POLL_TRANSFER_RECEIPT, transferId, LocalDateTime.now());
    }

    private int normalizeLimit(Integer requestedLimit) {
        int defaultLimit = properties.getPolicy().getDefaultListLimit();
        if (requestedLimit == null) {
            return defaultLimit;
        }
        return Math.min(Math.max(requestedLimit, 1), 100);
    }

    private List<TransferStatus> resolveTransferStatuses(List<TransferStatus> requestedStatuses, boolean stuckOnly) {
        if (requestedStatuses != null && !requestedStatuses.isEmpty()) {
            return requestedStatuses;
        }
        if (stuckOnly) {
            return List.of(TransferStatus.REQUESTED, TransferStatus.SIGNED, TransferStatus.BROADCASTED);
        }
        return List.of(TransferStatus.FAILED, TransferStatus.TIMED_OUT, TransferStatus.BROADCASTED);
    }

    private RemittanceOpsTransferItemResponse toOpsTransferItem(Transfer transfer) {
        return new RemittanceOpsTransferItemResponse(
                transfer.getTransferId(),
                transfer.getUserId(),
                transfer.getStatus().name(),
                transfer.getFailureCode() == null ? null : transfer.getFailureCode().name(),
                transfer.getRecipientAddress(),
                transfer.getTxHash(),
                transfer.getUpdatedAt()
        );
    }

    private RemittanceOpsJobItemResponse toOpsJobItem(Job job) {
        return new RemittanceOpsJobItemResponse(
                job.getId(),
                job.getJobType().name(),
                job.getStatus().name(),
                job.getReferenceId(),
                job.getAttemptCount(),
                job.getLastError(),
                job.getUpdatedAt()
        );
    }
}
