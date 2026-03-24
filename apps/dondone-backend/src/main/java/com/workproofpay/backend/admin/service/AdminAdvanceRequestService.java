package com.workproofpay.backend.admin.service;

import com.workproofpay.backend.admin.api.dto.response.AdminAdvanceRequestItemResponse;
import com.workproofpay.backend.admin.api.dto.response.AdminAdvanceRequestListResponse;
import com.workproofpay.backend.advance.model.AdvancePayout;
import com.workproofpay.backend.advance.model.AdvanceRequest;
import com.workproofpay.backend.advance.repo.AdvancePayoutRepository;
import com.workproofpay.backend.advance.repo.AdvanceRequestRepository;
import com.workproofpay.backend.advance.service.AdvancePayoutService;
import com.workproofpay.backend.advance.service.AdvanceRequestViewStatusResolver;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.model.Workplace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAdvanceRequestService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final AdvancePayoutRepository advancePayoutRepository;
    private final AdvanceRequestRepository advanceRequestRepository;
    private final AdvancePayoutService advancePayoutService;
    private final AdvanceRequestViewStatusResolver advanceRequestViewStatusResolver;

    @Transactional(readOnly = true)
    public AdminAdvanceRequestListResponse getRequests(Long adminAccountId) {
        ensureAdminUserExists(adminAccountId);

        List<AdvanceRequest> requests = advanceRequestRepository.findAllByOrderByRequestedAtDescCreatedAtDesc();
        Map<Long, AdvancePayout> payoutsByRequestId = advancePayoutRepository.findByAdvanceRequestIdIn(
                        requests.stream().map(AdvanceRequest::getId).toList()
                ).stream()
                .collect(Collectors.toMap(AdvancePayout::getAdvanceRequestId, Function.identity()));
        Map<Long, Company> companiesById = companyRepository.findAllById(
                        requests.stream()
                                .map(AdvanceRequest::getWorkplace)
                                .map(Workplace::getCompanyId)
                                .filter(id -> id != null)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(Company::getId, Function.identity()));

        return new AdminAdvanceRequestListResponse(
                requests.stream()
                        .map(request -> toItemResponse(
                                request,
                                payoutsByRequestId.get(request.getId()),
                                companiesById.get(request.getWorkplace().getCompanyId())
                        ))
                        .toList()
        );
    }

    @Transactional
    public void approve(Long adminAccountId, Long requestId) {
        ensureAdminUserExists(adminAccountId);
        AdvanceRequest request = getRequest(requestId);
        ensureSubmitted(request);
        request.approve(adminAccountId);
        advancePayoutService.createRequestedPayout(request);
    }

    @Transactional
    public void reject(Long adminAccountId, Long requestId) {
        ensureAdminUserExists(adminAccountId);
        AdvanceRequest request = getRequest(requestId);
        ensureSubmitted(request);
        request.reject(adminAccountId);
    }

    private void ensureAdminUserExists(Long adminAccountId) {
        if (!userRepository.existsById(adminAccountId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private AdvanceRequest getRequest(Long requestId) {
        return advanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.ADVANCE_REQUEST_NOT_FOUND));
    }

    private void ensureSubmitted(AdvanceRequest request) {
        if (!request.isSubmitted()) {
            throw new ApiException(ErrorCode.ADVANCE_REQUEST_ALREADY_PROCESSED);
        }
    }

    private AdminAdvanceRequestItemResponse toItemResponse(AdvanceRequest request, AdvancePayout payout, Company company) {
        User worker = request.getUser();
        Workplace workplace = request.getWorkplace();
        AdvanceRequestViewStatusResolver.AdvanceRequestViewStatus viewStatus = advanceRequestViewStatusResolver.resolve(request, payout);
        return new AdminAdvanceRequestItemResponse(
                request.getId(),
                worker.getId(),
                worker.getName(),
                worker.getEmail(),
                company != null ? company.getName() : null,
                workplace.getName(),
                request.getAssetSymbol(),
                request.getAssetDecimals(),
                request.getExchangeRateSnapshot(),
                request.getRequestedAmountAtomic(),
                request.getRequestedDisplayKrwAmount(),
                request.getApprovedAmountAtomic(),
                request.getApprovedDisplayKrwAmount(),
                request.getFeeAmountAtomic(),
                request.getFeeDisplayKrwAmount(),
                viewStatus.status(),
                viewStatus.requestStatus(),
                viewStatus.payoutStatus(),
                request.getRepaymentDueDate(),
                request.getRequestedAt(),
                request.getSnapshotReflectedWorkDays(),
                request.getSnapshotReflectedWorkMinutes(),
                request.getSnapshotNeedsReviewRecordCount(),
                request.getReviewedAt()
        );
    }
}
