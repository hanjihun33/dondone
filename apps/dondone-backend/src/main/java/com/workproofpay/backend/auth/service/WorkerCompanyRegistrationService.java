package com.workproofpay.backend.auth.service;

import com.workproofpay.backend.auth.api.dto.request.RedeemWorkerRegistrationCodeRequest;
import com.workproofpay.backend.auth.api.dto.response.WorkerCompanyRegistrationResponse;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.model.UserRole;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.model.EmploymentMembership;
import com.workproofpay.backend.employer.model.EmploymentMembershipStatus;
import com.workproofpay.backend.employer.model.WorkerRegistrationCode;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.employer.repo.EmploymentMembershipRepository;
import com.workproofpay.backend.employer.repo.WorkerRegistrationCodeRepository;
import com.workproofpay.backend.employerauth.model.EmployerInvitationToken;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkerCompanyRegistrationService {

    private final UserRepository userRepository;
    private final WorkerRegistrationCodeRepository workerRegistrationCodeRepository;
    private final CompanyRepository companyRepository;
    private final WorkplaceRepository workplaceRepository;
    private final EmploymentMembershipRepository employmentMembershipRepository;

    @Transactional
    public WorkerCompanyRegistrationResponse redeem(Long userId, RedeemWorkerRegistrationCodeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != UserRole.USER) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        String normalizedCode = request.registrationCode().trim().toUpperCase();
        WorkerRegistrationCode registrationCode = workerRegistrationCodeRepository
                .findByCodeHash(EmployerInvitationToken.hash(normalizedCode))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_WORKER_REGISTRATION_CODE));

        if (!registrationCode.isUsable()) {
            throw new ApiException(ErrorCode.INVALID_WORKER_REGISTRATION_CODE);
        }

        Company company = companyRepository.findById(registrationCode.getCompanyId())
                .orElseThrow(() -> new ApiException(ErrorCode.COMPANY_NOT_FOUND));
        Workplace workplace = workplaceRepository.findById(registrationCode.getWorkplaceId())
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPLACE_NOT_FOUND));

        if (workplace.getCompanyId() == null || !workplace.getCompanyId().equals(company.getId())) {
            throw new ApiException(ErrorCode.INVALID_WORKER_REGISTRATION_CODE);
        }

        LocalDate today = LocalDate.now();
        List<EmploymentMembership> activeMemberships = employmentMembershipRepository.findActiveWorkerMembershipByScope(
                user.getId(),
                company.getId(),
                workplace.getId(),
                EmploymentMembershipStatus.ACTIVE,
                today
        );

        EmploymentMembership membership = activeMemberships.stream()
                .findFirst()
                .orElseGet(() -> employmentMembershipRepository.save(EmploymentMembership.create(
                        user.getId(),
                        company.getId(),
                        workplace.getId(),
                        today
                )));

        return new WorkerCompanyRegistrationResponse(
                company.getId(),
                company.getName(),
                company.getCompanyCode(),
                workplace.getId(),
                workplace.getName(),
                membership.getStatus().name(),
                membership.getEffectiveFrom()
        );
    }
}
