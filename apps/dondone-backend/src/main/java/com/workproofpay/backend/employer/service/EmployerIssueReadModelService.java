package com.workproofpay.backend.employer.service;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.correction.model.CorrectionRequest;
import com.workproofpay.backend.correction.model.CorrectionRequestStatus;
import com.workproofpay.backend.correction.repo.CorrectionRequestRepository;
import com.workproofpay.backend.employer.api.dto.request.EmployerIssuesQuery;
import com.workproofpay.backend.employer.api.dto.response.EmployerIssueItemType;
import com.workproofpay.backend.employer.api.dto.response.EmployerIssuesResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerIssueStatus;
import com.workproofpay.backend.employer.api.dto.response.EmployerIssueSummaryResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerReviewRequiredRecordDetailResponse;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.WorkProofFinancialStatus;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordStatus;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofReflectionStatus;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class EmployerIssueReadModelService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String REVIEW_REASON_CODE_OUTSIDE_ALLOWED_RADIUS = "CLOCK_OUT_OUTSIDE_ALLOWED_RADIUS";
    private static final String REVIEW_REASON_CODE_NEEDS_REVIEW = "NEEDS_REVIEW";
    private static final String REVIEW_REASON_LABEL_OUTSIDE_ALLOWED_RADIUS = "Clock-out outside allowed radius";
    private static final String REVIEW_REASON_LABEL_NEEDS_REVIEW = "Needs review";

    private final EmployerAccessScopeService employerAccessScopeService;
    private final CorrectionRequestRepository correctionRequestRepository;
    private final WorkProofRepository workProofRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public EmployerIssuesResponse getIssues(Long accountId, EmployerIssuesQuery query) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(accountId);
        Set<EmployerIssueItemType> requestedTypes = normalizeItemTypes(query.itemTypes());
        Set<EmployerIssueStatus> requestedStatuses = normalizeStatuses(query.statuses());
        List<CorrectionRequest> correctionRequests = requestedTypes.contains(EmployerIssueItemType.CORRECTION_REQUEST)
                ? correctionRequestRepository.findByCompanyIdAndWorkplaceIdOrderByCreatedAtDescIdDesc(scope.companyId(), scope.defaultWorkplaceId()).stream()
                .filter(CorrectionRequest::isPending)
                .toList()
                : List.of();
        List<WorkProof> reviewRecords = requestedTypes.contains(EmployerIssueItemType.REVIEW_REQUIRED_RECORD)
                ? workProofRepository.findByWorkplaceIdAndFinancialStatusOrderByWorkDateDescClockOutAtDescIdDesc(
                        scope.defaultWorkplaceId(),
                        WorkProofFinancialStatus.NEEDS_REVIEW
                )
                : List.of();

        Map<Long, User> usersById = usersById(correctionRequests, reviewRecords);
        String normalizedQuery = normalizeQuery(query.query());
        List<EmployerIssueSummaryResponse> filteredIssues = java.util.stream.Stream.concat(
                        correctionRequests.stream().map(request -> toCorrectionIssue(request, usersById)),
                        reviewRecords.stream().map(record -> toReviewIssue(record, usersById))
                )
                .filter(issue -> requestedStatuses.isEmpty() || requestedStatuses.contains(issue.issueStatus()))
                .filter(issue -> matchesQuery(issue, normalizedQuery))
                .sorted(Comparator.comparing(EmployerIssueSummaryResponse::raisedAt).reversed()
                        .thenComparing(EmployerIssueSummaryResponse::workProofId, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(EmployerIssueSummaryResponse::requestId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        Pagination pagination = paginate(filteredIssues.size(), query.page(), query.size());
        return new EmployerIssuesResponse(
                filteredIssues.subList(pagination.fromIndex(), pagination.toIndex()),
                pagination.page(),
                pagination.size(),
                filteredIssues.size(),
                pagination.totalPages()
        );
    }

    @Transactional(readOnly = true)
    public EmployerReviewRequiredRecordDetailResponse getReviewRecord(Long accountId, Long workProofId) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(accountId);
        WorkProof workProof = workProofRepository.findByIdAndWorkplaceIdAndFinancialStatus(
                        workProofId,
                        scope.defaultWorkplaceId(),
                        WorkProofFinancialStatus.NEEDS_REVIEW
                )
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPROOF_NOT_FOUND));
        User worker = userRepository.findById(workProof.getUser().getId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return new EmployerReviewRequiredRecordDetailResponse(
                workProof.getId(),
                worker.getId(),
                worker.getName(),
                worker.getEmail(),
                workProof.getWorkDate(),
                workProof.getClockOutAt() == null ? WorkProofRecordStatus.CHECKED_IN : WorkProofRecordStatus.CHECKED_OUT,
                workProof.isNeedsReview() ? WorkProofReflectionStatus.NEEDS_REVIEW : WorkProofReflectionStatus.REFLECTED,
                resolveReviewReasonCode(workProof),
                resolveReviewReasonLabel(workProof),
                workProof.workedMinutes(),
                workProof.isClockOutOutsideAllowedRadius(),
                workProof.isEdited(),
                workProof.getEditReason(),
                workProof.getMemo(),
                workProof.getAttachmentCount(),
                new EmployerReviewRequiredRecordDetailResponse.WorkplaceSnapshot(
                        workProof.getWorkplace() == null ? null : workProof.getWorkplace().getId(),
                        workProof.resolveWorkplaceName(),
                        workProof.resolveWorkplaceAddress(),
                        workProof.resolveWorkplaceMapLabel(),
                        workProof.resolveWorkplaceLatitude(),
                        workProof.resolveWorkplaceLongitude()
                ),
                new EmployerReviewRequiredRecordDetailResponse.EvidenceCaptureResponse(
                        workProof.getDeviceClockInAt(),
                        workProof.getServerClockInAt(),
                        workProof.getClockInLatitude(),
                        workProof.getClockInLongitude(),
                        workProof.getClockInLocationLabel()
                ),
                workProof.getClockOutAt() == null ? null : new EmployerReviewRequiredRecordDetailResponse.EvidenceCaptureResponse(
                        workProof.getDeviceClockOutAt(),
                        workProof.getServerClockOutAt(),
                        workProof.getClockOutLatitude(),
                        workProof.getClockOutLongitude(),
                        workProof.getClockOutLocationLabel()
                )
        );
    }

    private EmployerIssueSummaryResponse toCorrectionIssue(CorrectionRequest correctionRequest, Map<Long, User> usersById) {
        User worker = usersById.get(correctionRequest.getWorkerAccountId());
        return new EmployerIssueSummaryResponse(
                EmployerIssueItemType.CORRECTION_REQUEST,
                EmployerIssueStatus.PENDING,
                correctionRequest.getId(),
                correctionRequest.getWorkProof().getId(),
                correctionRequest.getWorkerAccountId(),
                worker == null ? null : worker.getName(),
                worker == null ? null : worker.getEmail(),
                null,
                correctionRequest.getWorkDate(),
                correctionRequest.getOriginalClockInAt(),
                correctionRequest.getOriginalClockOutAt(),
                correctionRequest.getRequestedClockInAt(),
                correctionRequest.getRequestedClockOutAt(),
                correctionRequest.getReason(),
                null,
                correctionRequest.getCreatedAt()
        );
    }

    private EmployerIssueSummaryResponse toReviewIssue(WorkProof workProof, Map<Long, User> usersById) {
        User worker = usersById.get(workProof.getUser().getId());
        return new EmployerIssueSummaryResponse(
                EmployerIssueItemType.REVIEW_REQUIRED_RECORD,
                EmployerIssueStatus.NEEDS_REVIEW,
                null,
                workProof.getId(),
                workProof.getUser().getId(),
                worker == null ? null : worker.getName(),
                worker == null ? null : worker.getEmail(),
                null,
                workProof.getWorkDate(),
                workProof.getClockInAt(),
                workProof.getClockOutAt(),
                null,
                null,
                resolveReviewReasonLabel(workProof),
                resolveReviewReasonCode(workProof),
                workProof.getServerClockOutAt() == null ? workProof.getCreatedAt() : workProof.getServerClockOutAt()
        );
    }

    private String resolveReviewReasonCode(WorkProof workProof) {
        if (workProof.isClockOutOutsideAllowedRadius()) {
            return REVIEW_REASON_CODE_OUTSIDE_ALLOWED_RADIUS;
        }
        return REVIEW_REASON_CODE_NEEDS_REVIEW;
    }

    private String resolveReviewReasonLabel(WorkProof workProof) {
        if (workProof.isClockOutOutsideAllowedRadius()) {
            return REVIEW_REASON_LABEL_OUTSIDE_ALLOWED_RADIUS;
        }
        return REVIEW_REASON_LABEL_NEEDS_REVIEW;
    }

    private boolean matchesQuery(EmployerIssueSummaryResponse issue, String normalizedQuery) {
        if (normalizedQuery == null) {
            return true;
        }
        return contains(issue.workerName(), normalizedQuery)
                || contains(issue.workerEmail(), normalizedQuery)
                || contains(issue.reason(), normalizedQuery);
    }

    private boolean contains(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private Map<Long, User> usersById(List<CorrectionRequest> correctionRequests, List<WorkProof> reviewRecords) {
        List<Long> ids = java.util.stream.Stream.concat(
                        correctionRequests.stream().map(CorrectionRequest::getWorkerAccountId),
                        reviewRecords.stream().map(record -> record.getUser().getId())
                )
                .distinct()
                .toList();
        return userRepository.findAllById(ids).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, Function.identity()));
    }

    private Set<EmployerIssueItemType> normalizeItemTypes(List<EmployerIssueItemType> itemTypes) {
        if (itemTypes == null || itemTypes.isEmpty()) {
            return Set.of(EmployerIssueItemType.CORRECTION_REQUEST, EmployerIssueItemType.REVIEW_REQUIRED_RECORD);
        }
        return Set.copyOf(itemTypes);
    }

    private Set<EmployerIssueStatus> normalizeStatuses(List<EmployerIssueStatus> statuses) {
        return Set.copyOf(statuses == null ? List.of() : statuses);
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private Pagination paginate(int totalElements, Integer requestedPage, Integer requestedSize) {
        int page = normalizePage(requestedPage);
        int size = normalizeSize(requestedSize);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min((page - 1) * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        return new Pagination(page, size, totalPages, fromIndex, toIndex);
    }

    private int normalizePage(Integer page) {
        int resolved = page == null ? DEFAULT_PAGE : page;
        if (resolved < 1) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return resolved;
    }

    private int normalizeSize(Integer size) {
        int resolved = size == null ? DEFAULT_SIZE : size;
        if (resolved < 1 || resolved > MAX_SIZE) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return resolved;
    }

    private record Pagination(
            int page,
            int size,
            int totalPages,
            int fromIndex,
            int toIndex
    ) {
    }
}
