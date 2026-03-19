package com.workproofpay.backend.employer.service;

import com.workproofpay.backend.employer.api.dto.request.UpdateEmployerWorkplaceSettingsRequest;
import com.workproofpay.backend.employer.api.dto.response.EmployerWorkplaceSettingsResponse;
import com.workproofpay.backend.employer.model.EmploymentMembership;
import com.workproofpay.backend.employer.repo.EmploymentMembershipRepository;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmployerWorkplaceSettingsService {

    private final EmployerAccessScopeService employerAccessScopeService;
    private final EmploymentMembershipRepository employmentMembershipRepository;
    private final WorkplaceRepository workplaceRepository;

    @Transactional(readOnly = true)
    public EmployerWorkplaceSettingsResponse getSettings(Long accountId) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(accountId);
        Workplace workplace = employerAccessScopeService.getRequiredScopedWorkplace(scope);
        return toResponse(scope, workplace);
    }

    @Transactional
    public EmployerWorkplaceSettingsResponse updateSettings(Long accountId,
                                                            UpdateEmployerWorkplaceSettingsRequest request) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(accountId);
        Workplace workplace = employerAccessScopeService.getRequiredScopedWorkplace(scope);

        // MVP 정책: 변경 효력은 저장 완료 시점부터 미래 check-in/check-out에만 적용한다.
        workplace.updateEmployerSettings(
                request.address(),
                request.detailAddress(),
                request.latitude(),
                request.longitude(),
                request.allowedRadiusMeters(),
                LocalDateTime.now(),
                accountId
        );

        Workplace saved = workplaceRepository.saveAndFlush(workplace);
        return toResponse(scope, saved);
    }

    private EmployerWorkplaceSettingsResponse toResponse(EmployerAccessScope scope, Workplace workplace) {
        return EmployerWorkplaceSettingsResponse.from(
                scope,
                workplace,
                countActiveMemberships(scope)
        );
    }

    private long countActiveMemberships(EmployerAccessScope scope) {
        LocalDate today = LocalDate.now();
        return employmentMembershipRepository.findByCompanyIdAndWorkplaceId(scope.companyId(), scope.defaultWorkplaceId())
                .stream()
                .filter(membership -> membership.isActiveOn(today))
                .map(EmploymentMembership::getId)
                .count();
    }
}
