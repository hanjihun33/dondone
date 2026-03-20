package com.workproofpay.backend.employer.service;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.employer.api.dto.response.EmployerProfileResponse;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployerProfileService {

    private final UserRepository userRepository;
    private final EmployerAccessScopeService employerAccessScopeService;

    @Transactional(readOnly = true)
    public EmployerProfileResponse getProfile(Long accountId) {
        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(accountId);
        return EmployerProfileResponse.from(user, scope);
    }
}
