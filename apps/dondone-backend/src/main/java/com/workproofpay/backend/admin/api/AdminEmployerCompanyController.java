package com.workproofpay.backend.admin.api;

import com.workproofpay.backend.admin.api.dto.request.AdminCreateEmployerCompanyRequest;
import com.workproofpay.backend.admin.api.dto.response.AdminEmployerCompaniesResponse;
import com.workproofpay.backend.admin.api.dto.response.AdminEmployerCompanyEmployersResponse;
import com.workproofpay.backend.admin.api.dto.response.AdminEmployerCompanyCreatedResponse;
import com.workproofpay.backend.admin.api.dto.response.AdminEmployerSignupCodeResponse;
import com.workproofpay.backend.admin.service.AdminEmployerCompanyService;
import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/admin/employers/companies")
@RequiredArgsConstructor
public class AdminEmployerCompanyController {

    private final AdminEmployerCompanyService adminEmployerCompanyService;

    @PostMapping
    public org.springframework.http.ResponseEntity<ApiResponse<AdminEmployerCompanyCreatedResponse>> createCompany(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AdminCreateEmployerCompanyRequest request
    ) {
        return ApiResponse.created(adminEmployerCompanyService.createCompany(user.userId(), request));
    }

    @GetMapping
    public org.springframework.http.ResponseEntity<ApiResponse<AdminEmployerCompaniesResponse>> getCompanies(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(adminEmployerCompanyService.getCompanies(user.userId()));
    }

    @GetMapping("/{companyId}/signup-code")
    public org.springframework.http.ResponseEntity<ApiResponse<AdminEmployerSignupCodeResponse>> getEmployerSignupCode(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long companyId
    ) {
        return ApiResponse.success(adminEmployerCompanyService.getEmployerSignupCode(user.userId(), companyId));
    }

    @GetMapping("/{companyId}/employers")
    public org.springframework.http.ResponseEntity<ApiResponse<AdminEmployerCompanyEmployersResponse>> getCompanyEmployers(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long companyId
    ) {
        return ApiResponse.success(adminEmployerCompanyService.getCompanyEmployers(user.userId(), companyId));
    }
}
