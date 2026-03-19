package com.workproofpay.backend.employer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.correction.model.CorrectionDecisionAudit;
import com.workproofpay.backend.correction.model.CorrectionRequest;
import com.workproofpay.backend.correction.model.CorrectionRequestStatus;
import com.workproofpay.backend.correction.repo.CorrectionDecisionAuditRepository;
import com.workproofpay.backend.correction.repo.CorrectionRequestRepository;
import com.workproofpay.backend.employer.api.dto.request.EmployerApproveCorrectionRequest;
import com.workproofpay.backend.employer.api.dto.request.EmployerCorrectionRequestsQuery;
import com.workproofpay.backend.employer.api.dto.request.EmployerRejectCorrectionRequest;
import com.workproofpay.backend.employer.api.dto.response.EmployerCorrectionRequestDetailResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerCorrectionRequestSummaryResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerCorrectionRequestsResponse;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.api.dto.request.WorkProofAttachmentMetadataRequest;
import com.workproofpay.backend.workproof.api.dto.request.UpdateWorkProofRequest;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.WorkProofAuditLog;
import com.workproofpay.backend.workproof.repo.WorkProofAuditLogRepository;
import com.workproofpay.backend.workproof.service.WorkProofRequestValidator;
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
public class EmployerCorrectionRequestService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final EmployerAccessScopeService employerAccessScopeService;
    private final CorrectionRequestRepository correctionRequestRepository;
    private final CorrectionDecisionAuditRepository correctionDecisionAuditRepository;
    private final UserRepository userRepository;
    private final WorkProofRequestValidator workProofRequestValidator;
    private final WorkProofAuditLogRepository workProofAuditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public EmployerCorrectionRequestsResponse getCorrectionRequests(Long accountId, EmployerCorrectionRequestsQuery query) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(accountId);
        List<CorrectionRequest> scopedRequests = correctionRequestRepository
                .findByCompanyIdAndWorkplaceIdOrderByCreatedAtDescIdDesc(scope.companyId(), scope.defaultWorkplaceId());
        List<CorrectionRequest> filteredRequests = applyFilters(scopedRequests, query);
        Pagination pagination = paginate(filteredRequests.size(), query.page(), query.size());
        Map<Long, User> usersById = usersById(filteredRequests);

        List<EmployerCorrectionRequestSummaryResponse> rows = filteredRequests.subList(pagination.fromIndex(), pagination.toIndex()).stream()
                .map(request -> toSummaryResponse(request, usersById))
                .toList();

        return new EmployerCorrectionRequestsResponse(
                rows,
                pagination.page(),
                pagination.size(),
                filteredRequests.size(),
                pagination.totalPages()
        );
    }

    @Transactional(readOnly = true)
    public EmployerCorrectionRequestDetailResponse getCorrectionRequest(Long accountId, Long requestId) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(accountId);
        CorrectionRequest correctionRequest = getRequiredScopedRequest(scope, requestId);
        return toDetailResponse(correctionRequest, usersById(List.of(correctionRequest)));
    }

    @Transactional
    public EmployerCorrectionRequestDetailResponse approve(Long accountId, Long requestId, EmployerApproveCorrectionRequest request) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(accountId);
        CorrectionRequest correctionRequest = getRequiredScopedRequest(scope, requestId);
        ensurePending(correctionRequest);

        WorkProof workProof = correctionRequest.getWorkProof();
        UpdateWorkProofRequest updateRequest = new UpdateWorkProofRequest(
                correctionRequest.getRequestedClockInAt(),
                correctionRequest.getRequestedClockOutAt(),
                correctionRequest.getReason(),
                workProof.getMemo(),
                correctionRequest.getAttachmentCount(),
                null
        );
        workProofRequestValidator.validateForUpdate(workProof, updateRequest);

        LocalDateTime beforeClockInAt = workProof.getClockInAt();
        LocalDateTime beforeClockOutAt = workProof.getClockOutAt();
        String beforeEditReason = workProof.getEditReason();
        String beforeMemo = workProof.getMemo();
        int beforeAttachmentCount = workProof.getAttachmentCount();
        String beforeAttachmentMetadataJson = workProof.getAttachmentMetadataJson();

        workProof.updateTimes(
                correctionRequest.getRequestedClockInAt(),
                correctionRequest.getRequestedClockOutAt(),
                correctionRequest.getReason(),
                workProof.getMemo(),
                correctionRequest.getAttachmentCount(),
                correctionRequest.getAttachmentMetadataJson()
        );

        workProofAuditLogRepository.save(WorkProofAuditLog.record(
                workProof,
                accountId,
                beforeClockInAt,
                beforeClockOutAt,
                workProof.getClockInAt(),
                workProof.getClockOutAt(),
                beforeEditReason,
                workProof.getEditReason(),
                beforeMemo,
                workProof.getMemo(),
                beforeAttachmentCount,
                workProof.getAttachmentCount(),
                beforeAttachmentMetadataJson,
                workProof.getAttachmentMetadataJson()
        ));

        CorrectionRequestStatus beforeStatus = correctionRequest.getStatus();
        correctionRequest.approve(accountId, request.decisionMemo(), LocalDateTime.now());
        correctionDecisionAuditRepository.save(CorrectionDecisionAudit.record(
                correctionRequest,
                accountId,
                beforeStatus,
                correctionRequest.getStatus(),
                request.decisionMemo(),
                null
        ));

        return toDetailResponse(correctionRequest, usersById(List.of(correctionRequest)));
    }

    @Transactional
    public EmployerCorrectionRequestDetailResponse reject(Long accountId, Long requestId, EmployerRejectCorrectionRequest request) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(accountId);
        CorrectionRequest correctionRequest = getRequiredScopedRequest(scope, requestId);
        ensurePending(correctionRequest);

        CorrectionRequestStatus beforeStatus = correctionRequest.getStatus();
        correctionRequest.reject(accountId, request.decisionMemo(), request.rejectReasonCode(), LocalDateTime.now());
        correctionDecisionAuditRepository.save(CorrectionDecisionAudit.record(
                correctionRequest,
                accountId,
                beforeStatus,
                correctionRequest.getStatus(),
                request.decisionMemo(),
                request.rejectReasonCode()
        ));

        return toDetailResponse(correctionRequest, usersById(List.of(correctionRequest)));
    }

    private List<CorrectionRequest> applyFilters(List<CorrectionRequest> requests, EmployerCorrectionRequestsQuery query) {
        String normalizedQuery = normalizeQuery(query.query());
        Set<CorrectionRequestStatus> requestedStatuses = normalizedStatuses(query.statuses());

        return requests.stream()
                .filter(request -> requestedStatuses.isEmpty() || requestedStatuses.contains(request.getStatus()))
                .filter(request -> matchesQuery(request, normalizedQuery))
                .sorted(Comparator.comparing(CorrectionRequest::getCreatedAt).reversed().thenComparing(CorrectionRequest::getId).reversed())
                .toList();
    }

    private boolean matchesQuery(CorrectionRequest request, String normalizedQuery) {
        if (normalizedQuery == null) {
            return true;
        }
        User worker = request.getWorkProof().getUser();
        return worker.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || worker.getEmail().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || request.getReason().toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private CorrectionRequest getRequiredScopedRequest(EmployerAccessScope scope, Long requestId) {
        CorrectionRequest correctionRequest = correctionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORRECTION_REQUEST_NOT_FOUND));
        if (!correctionRequest.getCompanyId().equals(scope.companyId())
                || !correctionRequest.getWorkplaceId().equals(scope.defaultWorkplaceId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return correctionRequest;
    }

    private void ensurePending(CorrectionRequest correctionRequest) {
        if (!correctionRequest.isPending()) {
            throw new ApiException(ErrorCode.CORRECTION_REQUEST_ALREADY_PROCESSED);
        }
    }

    private EmployerCorrectionRequestSummaryResponse toSummaryResponse(CorrectionRequest correctionRequest, Map<Long, User> usersById) {
        User worker = usersById.get(correctionRequest.getWorkerAccountId());
        return new EmployerCorrectionRequestSummaryResponse(
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
                correctionRequest.getCreatedAt(),
                correctionRequest.getStatus()
        );
    }

    private EmployerCorrectionRequestDetailResponse toDetailResponse(CorrectionRequest correctionRequest, Map<Long, User> usersById) {
        User worker = usersById.get(correctionRequest.getWorkerAccountId());
        User decisionBy = correctionRequest.getDecisionByAccountId() == null ? null : usersById.get(correctionRequest.getDecisionByAccountId());
        return new EmployerCorrectionRequestDetailResponse(
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
                correctionRequest.getRequestMemo(),
                correctionRequest.getAttachmentCount(),
                resolveAttachments(correctionRequest),
                correctionRequest.getCreatedAt(),
                correctionRequest.getStatus(),
                correctionRequest.getDecisionByAccountId(),
                decisionBy == null ? null : decisionBy.getName(),
                correctionRequest.getDecisionAt(),
                correctionRequest.getDecisionMemo(),
                correctionRequest.getRejectReasonCode()
        );
    }

    private List<EmployerCorrectionRequestDetailResponse.AttachmentResponse> resolveAttachments(CorrectionRequest correctionRequest) {
        String attachmentMetadataJson = correctionRequest.getAttachmentMetadataJson();
        if (attachmentMetadataJson == null || attachmentMetadataJson.isBlank()) {
            return List.of();
        }
        try {
            List<WorkProofAttachmentMetadataRequest> attachments = objectMapper.readValue(
                    attachmentMetadataJson,
                    new TypeReference<>() {
                    }
            );
            return attachments.stream()
                    .map(attachment -> new EmployerCorrectionRequestDetailResponse.AttachmentResponse(
                            attachment.type() == null ? null : attachment.type().name(),
                            attachment.fileName(),
                            false
                    ))
                    .toList();
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private Map<Long, User> usersById(List<CorrectionRequest> requests) {
        List<Long> ids = requests.stream()
                .flatMap(request -> java.util.stream.Stream.of(request.getWorkerAccountId(), request.getDecisionByAccountId()))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        return userRepository.findAllById(ids).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, Function.identity()));
    }

    private Set<CorrectionRequestStatus> normalizedStatuses(List<CorrectionRequestStatus> statuses) {
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
