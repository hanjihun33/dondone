package com.workproofpay.backend.admin.service;

import com.workproofpay.backend.admin.api.dto.request.AdminCreateEmployerCompanyRequest;
import com.workproofpay.backend.admin.api.dto.response.AdminEmployerCompaniesResponse;
import com.workproofpay.backend.admin.api.dto.response.AdminEmployerCompanyEmployerSummaryResponse;
import com.workproofpay.backend.admin.api.dto.response.AdminEmployerCompanyEmployersResponse;
import com.workproofpay.backend.admin.api.dto.response.AdminEmployerCompanyCreatedResponse;
import com.workproofpay.backend.admin.api.dto.response.AdminEmployerCompanySummaryResponse;
import com.workproofpay.backend.admin.api.dto.response.AdminEmployerSignupCodeResponse;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.model.EmployerProfile;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.employer.repo.EmployerProfileRepository;
import com.workproofpay.backend.employerauth.model.EmployerInvitationToken;
import com.workproofpay.backend.employerauth.model.EmployerSignupCode;
import com.workproofpay.backend.employerauth.repo.EmployerSignupCodeRepository;
import com.workproofpay.backend.employerauth.service.EmployerSignupCodeCryptoService;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminEmployerCompanyService {

    private static final String DEFAULT_WORKPLACE_NAME_SUFFIX = " 기본 사업장";
    private static final String PLACEHOLDER_ADDRESS = "설정 필요";
    private static final double PLACEHOLDER_LATITUDE = 37.5665;
    private static final double PLACEHOLDER_LONGITUDE = 126.9780;
    private static final int PLACEHOLDER_ALLOWED_RADIUS_METERS = 300;
    private static final char[] SIGNUP_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int SIGNUP_CODE_SEGMENT_LENGTH = 4;
    private static final int SIGNUP_CODE_SEGMENT_COUNT = 3;
    private static final SecureRandom SIGNUP_CODE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final WorkplaceRepository workplaceRepository;
    private final EmployerSignupCodeRepository employerSignupCodeRepository;
    private final EmployerSignupCodeCryptoService employerSignupCodeCryptoService;

    @Transactional
    public AdminEmployerCompanyCreatedResponse createCompany(Long adminAccountId,
                                                             AdminCreateEmployerCompanyRequest request) {
        User adminUser = userRepository.findById(adminAccountId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        String normalizedCompanyCode = normalizeCompanyCode(request.companyCode());
        if (companyRepository.existsByCompanyCodeIgnoreCase(normalizedCompanyCode)) {
            throw new ApiException(ErrorCode.COMPANY_CODE_ALREADY_EXISTS);
        }

        Company company = companyRepository.save(Company.create(
                request.companyName().trim(),
                normalizedCompanyCode
        ));
        Workplace workplace = workplaceRepository.save(Workplace.create(
                adminUser,
                company.getId(),
                buildDefaultWorkplaceName(company.getName()),
                PLACEHOLDER_ADDRESS,
                null,
                PLACEHOLDER_LATITUDE,
                PLACEHOLDER_LONGITUDE,
                PLACEHOLDER_ALLOWED_RADIUS_METERS
        ));

        company.bindDefaultWorkplace(workplace.getId());
        company = companyRepository.save(company);

        String employerSignupCodeValue = generateEmployerSignupCode();
        EmployerSignupCode employerSignupCode = employerSignupCodeRepository.save(EmployerSignupCode.create(
                employerSignupCodeValue,
                employerSignupCodeCryptoService.encrypt(employerSignupCodeValue),
                company.getId(),
                workplace.getId(),
                adminAccountId
        ));

        return new AdminEmployerCompanyCreatedResponse(
                company.getId(),
                company.getName(),
                company.getCompanyCode(),
                workplace.getId(),
                workplace.getName(),
                workplace.getAddress(),
                workplace.resolveDetailAddress(),
                workplace.getLatitude(),
                workplace.getLongitude(),
                workplace.getAllowedRadiusMeters(),
                isWorkplaceSettingsConfigured(workplace),
                false,
                0,
                null,
                employerSignupCodeValue,
                employerSignupCode.getCreatedAt(),
                company.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public AdminEmployerCompaniesResponse getCompanies(Long adminAccountId) {
        if (!userRepository.existsById(adminAccountId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        List<Company> companies = companyRepository.findAll(
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        Map<Long, List<EmployerProfile>> employerProfilesByCompanyId = loadEmployerProfilesByCompanyId(companies);
        List<AdminEmployerCompanySummaryResponse> summaries = new ArrayList<>();

        for (Company company : companies) {
            if (company.getDefaultWorkplaceId() == null) {
                continue;
            }

            Optional<Workplace> workplaceOptional = workplaceRepository.findById(company.getDefaultWorkplaceId());
            if (workplaceOptional.isEmpty()) {
                continue;
            }

            Workplace workplace = workplaceOptional.get();
            List<EmployerProfile> employerProfiles = employerProfilesByCompanyId.getOrDefault(company.getId(), List.of());
            Optional<LocalDateTime> latestEmployerJoinedAt = employerProfiles.stream()
                    .map(EmployerProfile::getCreatedAt)
                    .max(LocalDateTime::compareTo);
            Optional<EmployerSignupCode> latestActiveCode = employerSignupCodeRepository
                    .findByCompanyIdAndDefaultWorkplaceId(company.getId(), workplace.getId())
                    .stream()
                    .filter(EmployerSignupCode::isUsable)
                    .max(Comparator.comparing(EmployerSignupCode::getCreatedAt));

            summaries.add(AdminEmployerCompanySummaryResponse.of(
                    company,
                    workplace,
                    isWorkplaceSettingsConfigured(workplace),
                    !employerProfiles.isEmpty(),
                    employerProfiles.size(),
                    latestEmployerJoinedAt.orElse(null),
                    latestActiveCode.isPresent(),
                    latestActiveCode.map(EmployerSignupCode::getCreatedAt).orElse(null)
            ));
        }

        return new AdminEmployerCompaniesResponse(summaries);
    }

    @Transactional(readOnly = true)
    public AdminEmployerSignupCodeResponse getEmployerSignupCode(Long adminAccountId, Long companyId) {
        if (!userRepository.existsById(adminAccountId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMPANY_NOT_FOUND));
        Long defaultWorkplaceId = company.getDefaultWorkplaceId();
        if (defaultWorkplaceId == null) {
            throw new ApiException(ErrorCode.EMPLOYER_SCOPE_NOT_READY);
        }

        Workplace workplace = workplaceRepository.findById(defaultWorkplaceId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPLACE_NOT_FOUND));

        EmployerSignupCode latestActiveCode = employerSignupCodeRepository
                .findByCompanyIdAndDefaultWorkplaceId(company.getId(), defaultWorkplaceId)
                .stream()
                .filter(EmployerSignupCode::isUsable)
                .max(Comparator.comparing(EmployerSignupCode::getCreatedAt))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_EMPLOYER_SIGNUP_CODE));

        if (latestActiveCode.getEncryptedCode() == null || latestActiveCode.getEncryptedCode().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_EMPLOYER_SIGNUP_CODE);
        }

        return new AdminEmployerSignupCodeResponse(
                company.getId(),
                company.getName(),
                workplace.getId(),
                workplace.getName(),
                employerSignupCodeCryptoService.decrypt(latestActiveCode.getEncryptedCode()),
                latestActiveCode.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public AdminEmployerCompanyEmployersResponse getCompanyEmployers(Long adminAccountId, Long companyId) {
        if (!userRepository.existsById(adminAccountId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMPANY_NOT_FOUND));

        List<EmployerProfile> employerProfiles = employerProfileRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
        if (employerProfiles.isEmpty()) {
            return new AdminEmployerCompanyEmployersResponse(company.getId(), company.getName(), List.of());
        }

        Map<Long, User> usersById = userRepository.findAllById(employerProfiles.stream()
                        .map(EmployerProfile::getAccountId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Workplace> workplacesById = workplaceRepository.findAllById(employerProfiles.stream()
                        .map(EmployerProfile::getDefaultWorkplaceId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(Workplace::getId, Function.identity()));

        List<AdminEmployerCompanyEmployerSummaryResponse> employers = employerProfiles.stream()
                .map(profile -> toEmployerSummary(profile, usersById.get(profile.getAccountId()), workplacesById.get(profile.getDefaultWorkplaceId())))
                .toList();

        return new AdminEmployerCompanyEmployersResponse(company.getId(), company.getName(), employers);
    }

    private String normalizeCompanyCode(String companyCode) {
        return companyCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildDefaultWorkplaceName(String companyName) {
        return companyName + DEFAULT_WORKPLACE_NAME_SUFFIX;
    }

    private AdminEmployerCompanyEmployerSummaryResponse toEmployerSummary(EmployerProfile profile,
                                                                         User user,
                                                                         Workplace workplace) {
        return new AdminEmployerCompanyEmployerSummaryResponse(
                profile.getId(),
                profile.getAccountId(),
                profile.getDisplayName(),
                user != null ? user.getEmail() : null,
                profile.getStatus().name(),
                profile.getDefaultWorkplaceId(),
                workplace != null ? workplace.getName() : null,
                workplace != null && isWorkplaceSettingsConfigured(workplace),
                profile.getCreatedAt()
        );
    }

    private Map<Long, List<EmployerProfile>> loadEmployerProfilesByCompanyId(List<Company> companies) {
        List<Long> companyIds = companies.stream()
                .map(Company::getId)
                .toList();
        Map<Long, List<EmployerProfile>> grouped = new HashMap<>();
        if (companyIds.isEmpty()) {
            return grouped;
        }

        for (EmployerProfile employerProfile : employerProfileRepository.findByCompanyIdIn(companyIds)) {
            grouped.computeIfAbsent(employerProfile.getCompanyId(), ignored -> new ArrayList<>())
                    .add(employerProfile);
        }
        return grouped;
    }

    private boolean isWorkplaceSettingsConfigured(Workplace workplace) {
        return !PLACEHOLDER_ADDRESS.equals(workplace.getAddress());
    }

    private String generateEmployerSignupCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = buildReadableSignupCode();
            if (employerSignupCodeRepository.findByCodeHash(EmployerInvitationToken.hash(candidate)).isEmpty()) {
                return candidate;
            }
        }

        throw new IllegalStateException("Failed to issue a unique employer signup code.");
    }

    private String buildReadableSignupCode() {
        StringBuilder builder = new StringBuilder("EMP");
        for (int segment = 0; segment < SIGNUP_CODE_SEGMENT_COUNT; segment++) {
            builder.append('-');
            for (int index = 0; index < SIGNUP_CODE_SEGMENT_LENGTH; index++) {
                builder.append(SIGNUP_CODE_ALPHABET[SIGNUP_CODE_RANDOM.nextInt(SIGNUP_CODE_ALPHABET.length)]);
            }
        }
        return builder.toString();
    }
}
