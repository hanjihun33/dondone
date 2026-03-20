package com.workproofpay.backend.employerauth.service;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.model.UserRole;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.auth.support.EmailNormalizer;
import com.workproofpay.backend.employer.model.EmployerProfile;
import com.workproofpay.backend.employer.repo.EmployerProfileRepository;
import com.workproofpay.backend.employer.service.EmployerAccessScope;
import com.workproofpay.backend.employer.service.EmployerAccessScopeService;
import com.workproofpay.backend.employerauth.api.dto.request.EmployerInvitationAcceptRequest;
import com.workproofpay.backend.employerauth.api.dto.request.EmployerLoginRequest;
import com.workproofpay.backend.employerauth.api.dto.response.EmployerAuthResponse;
import com.workproofpay.backend.employerauth.model.EmployerInvitationToken;
import com.workproofpay.backend.employerauth.repo.EmployerInvitationTokenRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmployerAuthService {

    private final UserRepository userRepository;
    private final EmployerInvitationTokenRepository employerInvitationTokenRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmployerAccessScopeService employerAccessScopeService;

    @Transactional
    public EmployerAuthResponse acceptInvitation(EmployerInvitationAcceptRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());
        LocalDateTime now = LocalDateTime.now();

        EmployerInvitationToken invitation = employerInvitationTokenRepository.findByTokenHash(
                        EmployerInvitationToken.hash(request.token()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_EMPLOYER_INVITATION));

        if (!invitation.isUsableAt(now) || !invitation.matchesInviteeEmail(normalizedEmail)) {
            throw new ApiException(ErrorCode.INVALID_EMPLOYER_INVITATION);
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        employerAccessScopeService.assertCompanyWorkplaceBinding(
                invitation.getCompanyId(),
                invitation.getDefaultWorkplaceId()
        );

        User savedUser = userRepository.save(User.registerEmployer(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.displayName()
        ));

        employerProfileRepository.save(EmployerProfile.create(
                savedUser.getId(),
                invitation.getCompanyId(),
                invitation.getDefaultWorkplaceId(),
                request.displayName()
        ));

        invitation.markUsed(now);

        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(savedUser.getId());
        String accessToken = jwtTokenProvider.createAccessToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        return EmployerAuthResponse.of(accessToken, jwtTokenProvider.getAccessExpirationSeconds(), scope);
    }

    @Transactional(readOnly = true)
    public EmployerAuthResponse login(EmployerLoginRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getRole() != UserRole.EMPLOYER || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(user.getId());
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return EmployerAuthResponse.of(accessToken, jwtTokenProvider.getAccessExpirationSeconds(), scope);
    }
}
