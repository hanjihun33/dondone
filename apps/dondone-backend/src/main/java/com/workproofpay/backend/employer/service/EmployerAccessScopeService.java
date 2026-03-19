package com.workproofpay.backend.employer.service;

import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.model.EmployerProfile;
import com.workproofpay.backend.employer.model.EmployerProfileStatus;
import com.workproofpay.backend.employer.model.EmploymentMembership;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.employer.repo.EmployerProfileRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EmployerAccessScopeService {

    private final EmployerProfileRepository employerProfileRepository;
    private final CompanyRepository companyRepository;
    private final WorkplaceRepository workplaceRepository;

    @Transactional(readOnly = true)
    public EmployerAccessScope getRequiredScope(Long accountId) {
        EmployerProfile profile = employerProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ApiException(ErrorCode.EMPLOYER_PROFILE_NOT_FOUND));

        if (profile.getStatus() != EmployerProfileStatus.ACTIVE) {
            throw new ApiException(ErrorCode.EMPLOYER_PROFILE_INACTIVE);
        }

        Company company = companyRepository.findById(profile.getCompanyId())
                .orElseThrow(() -> new ApiException(ErrorCode.COMPANY_NOT_FOUND));

        Workplace workplace = getRequiredWorkplaceBoundToCompany(company.getId(), profile.getDefaultWorkplaceId());

        return EmployerAccessScope.from(profile, company, workplace);
    }

    @Transactional(readOnly = true)
    public void assertCompanyWorkplaceBinding(Long companyId, Long workplaceId) {
        companyRepository.findById(companyId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMPANY_NOT_FOUND));
        getRequiredWorkplaceBoundToCompany(companyId, workplaceId);
    }

    public void assertMembershipAccessible(EmployerAccessScope scope, EmploymentMembership membership) {
        if (!membership.matchesScope(scope.companyId(), scope.defaultWorkplaceId())
                || !membership.isActiveOn(LocalDate.now())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    private Workplace getRequiredWorkplaceBoundToCompany(Long companyId, Long workplaceId) {
        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPLACE_NOT_FOUND));

        if (workplace.getCompanyId() == null || !workplace.getCompanyId().equals(companyId)) {
            throw new ApiException(ErrorCode.EMPLOYER_SCOPE_NOT_READY);
        }

        return workplace;
    }
}
