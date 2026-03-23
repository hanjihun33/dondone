package com.workproofpay.backend.employer.service;

import com.workproofpay.backend.employer.api.dto.request.UpdateEmployerWorkplaceSettingsRequest;
import com.workproofpay.backend.employer.api.dto.response.EmployerWorkplaceSettingsResponse;
import com.workproofpay.backend.employer.model.AttendanceOvertimeRoundingUnit;
import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.model.EmploymentMembership;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.employer.repo.EmploymentMembershipRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class EmployerWorkplaceSettingsService {

    private final EmployerAccessScopeService employerAccessScopeService;
    private final CompanyRepository companyRepository;
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
        Company company = companyRepository.findById(scope.companyId())
                .orElseThrow(() -> new ApiException(ErrorCode.COMPANY_NOT_FOUND));
        Workplace workplace = employerAccessScopeService.getRequiredScopedWorkplace(scope);

        workplace.updateEmployerSettings(
                request.address(),
                request.detailAddress(),
                request.latitude(),
                request.longitude(),
                request.allowedRadiusMeters(),
                LocalDateTime.now(),
                accountId
        );
        company.updateAttendancePolicy(
                resolveScheduledClockInTime(request, company),
                resolveScheduledClockOutTime(request, company),
                resolveOvertimeRoundingUnit(request, company)
        );
        validateAttendancePolicy(company.getScheduledClockInTime(), company.getScheduledClockOutTime());

        Workplace saved = workplaceRepository.saveAndFlush(workplace);
        companyRepository.saveAndFlush(company);
        EmployerAccessScope refreshedScope = employerAccessScopeService.getRequiredScope(accountId);
        return toResponse(refreshedScope, saved);
    }

    private LocalTime resolveScheduledClockInTime(UpdateEmployerWorkplaceSettingsRequest request, Company company) {
        return request.scheduledClockInTime() != null
                ? request.scheduledClockInTime()
                : company.getScheduledClockInTime();
    }

    private LocalTime resolveScheduledClockOutTime(UpdateEmployerWorkplaceSettingsRequest request, Company company) {
        return request.scheduledClockOutTime() != null
                ? request.scheduledClockOutTime()
                : company.getScheduledClockOutTime();
    }

    private AttendanceOvertimeRoundingUnit resolveOvertimeRoundingUnit(UpdateEmployerWorkplaceSettingsRequest request,
                                                                       Company company) {
        return request.overtimeRoundingUnit() != null
                ? request.overtimeRoundingUnit()
                : company.getOvertimeRoundingUnit();
    }

    private void validateAttendancePolicy(LocalTime scheduledClockInTime, LocalTime scheduledClockOutTime) {
        if (!scheduledClockOutTime.isAfter(scheduledClockInTime)) {
            throw new ApiException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "scheduledClockOutTime must be after scheduledClockInTime"
            );
        }
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
