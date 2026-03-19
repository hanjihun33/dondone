package com.workproofpay.backend.employer.service;

import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.model.EmployerProfile;
import com.workproofpay.backend.employer.model.EmployerProfileStatus;
import com.workproofpay.backend.workproof.model.Workplace;

public record EmployerAccessScope(
        Long employerId,
        Long accountId,
        String displayName,
        Long companyId,
        String companyName,
        String companyCode,
        Long defaultWorkplaceId,
        String defaultWorkplaceName,
        EmployerProfileStatus status
) {
    public static EmployerAccessScope from(EmployerProfile profile, Company company, Workplace workplace) {
        return new EmployerAccessScope(
                profile.getId(),
                profile.getAccountId(),
                profile.getDisplayName(),
                company.getId(),
                company.getName(),
                company.getCompanyCode(),
                workplace.getId(),
                workplace.getName(),
                profile.getStatus()
        );
    }
}
