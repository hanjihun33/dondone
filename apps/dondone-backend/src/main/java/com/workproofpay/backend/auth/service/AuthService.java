package com.workproofpay.backend.auth.service;

import com.workproofpay.backend.auth.api.dto.request.LoginRequest;
import com.workproofpay.backend.auth.api.dto.request.SignupRequest;
import com.workproofpay.backend.auth.api.dto.request.UpdateCompanyCodeRequest;
import com.workproofpay.backend.auth.api.dto.request.UpdateProfileRequest;
import com.workproofpay.backend.auth.api.dto.response.LoginResponse;
import com.workproofpay.backend.auth.api.dto.response.MeResponse;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.auth.support.EmailNormalizer;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.shared.util.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public MeResponse signup(SignupRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        String normalizedPhoneNumber = PhoneNumberUtils.normalizeOrThrow(request.phoneNumber());
        ensurePhoneNumberAvailable(normalizedPhoneNumber, null);

        User user = User.register(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                normalizedPhoneNumber,
                null
        );
        User saved = userRepository.save(user);

        return MeResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return LoginResponse.of(accessToken, jwtTokenProvider.getAccessExpirationSeconds(), user);
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        return MeResponse.from(user);
    }

    @Transactional
    public MeResponse updateMe(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        String normalizedPhoneNumber = PhoneNumberUtils.normalizeOrThrow(request.phoneNumber());
        ensurePhoneNumberAvailable(normalizedPhoneNumber, userId);
        user.updateProfile(request.name().trim(), normalizedPhoneNumber);
        return MeResponse.from(user);
    }

    @Transactional
    public MeResponse updateCompanyCode(Long userId, UpdateCompanyCodeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        user.updateCompanyCode(request.companyCode());
        return MeResponse.from(user);
    }

    private void ensurePhoneNumberAvailable(String phoneNumber, Long userId) {
        boolean exists = userId == null
                ? userRepository.existsByPhoneNumber(phoneNumber)
                : userRepository.existsByPhoneNumberAndIdNot(phoneNumber, userId);
        if (exists) {
            throw new ApiException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
        }
    }
}
