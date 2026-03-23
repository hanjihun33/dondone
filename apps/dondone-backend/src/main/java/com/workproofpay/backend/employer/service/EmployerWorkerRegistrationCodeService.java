package com.workproofpay.backend.employer.service;

import com.workproofpay.backend.employer.api.dto.response.EmployerWorkerRegistrationCodeResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerWorkerRegistrationCodesResponse;
import com.workproofpay.backend.employer.model.WorkerRegistrationCode;
import com.workproofpay.backend.employer.repo.WorkerRegistrationCodeRepository;
import com.workproofpay.backend.employerauth.model.EmployerInvitationToken;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmployerWorkerRegistrationCodeService {

    private final EmployerAccessScopeService employerAccessScopeService;
    private final WorkerRegistrationCodeRepository workerRegistrationCodeRepository;
    private final WorkerRegistrationCodeCryptoService workerRegistrationCodeCryptoService;

    @Transactional
    public EmployerWorkerRegistrationCodeResponse issue(Long employerAccountId) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(employerAccountId);
        revokeActiveCodes(scope, employerAccountId);

        String rawCode = generateWorkerRegistrationCode();
        WorkerRegistrationCode saved = workerRegistrationCodeRepository.save(WorkerRegistrationCode.create(
                rawCode,
                workerRegistrationCodeCryptoService.encrypt(rawCode),
                scope.companyId(),
                scope.defaultWorkplaceId(),
                employerAccountId
        ));
        return EmployerWorkerRegistrationCodeResponse.of(saved, rawCode);
    }

    @Transactional(readOnly = true)
    public EmployerWorkerRegistrationCodesResponse getCodes(Long employerAccountId) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(employerAccountId);
        List<EmployerWorkerRegistrationCodeResponse> responses = workerRegistrationCodeRepository
                .findByCompanyIdAndWorkplaceIdOrderByCreatedAtDescIdDesc(scope.companyId(), scope.defaultWorkplaceId())
                .stream()
                .map(code -> EmployerWorkerRegistrationCodeResponse.of(
                        code,
                        workerRegistrationCodeCryptoService.decrypt(code.getEncryptedCode())
                ))
                .toList();

        return new EmployerWorkerRegistrationCodesResponse(
                scope.companyId(),
                scope.companyName(),
                scope.defaultWorkplaceId(),
                scope.defaultWorkplaceName(),
                responses
        );
    }

    @Transactional
    public EmployerWorkerRegistrationCodeResponse revoke(Long employerAccountId, Long codeId) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(employerAccountId);
        WorkerRegistrationCode code = workerRegistrationCodeRepository
                .findByIdAndCompanyIdAndWorkplaceId(codeId, scope.companyId(), scope.defaultWorkplaceId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_WORKER_REGISTRATION_CODE));

        if (code.isUsable()) {
            code.revoke(LocalDateTime.now());
        }

        return EmployerWorkerRegistrationCodeResponse.of(
                code,
                workerRegistrationCodeCryptoService.decrypt(code.getEncryptedCode())
        );
    }

    private void revokeActiveCodes(EmployerAccessScope scope, Long employerAccountId) {
        LocalDateTime revokedAt = LocalDateTime.now();
        List<WorkerRegistrationCode> activeCodes = workerRegistrationCodeRepository
                .findByCompanyIdAndWorkplaceIdOrderByCreatedAtDescIdDesc(scope.companyId(), scope.defaultWorkplaceId())
                .stream()
                .filter(WorkerRegistrationCode::isUsable)
                .toList();
        for (WorkerRegistrationCode activeCode : activeCodes) {
            activeCode.revoke(revokedAt);
        }
    }

    private String generateWorkerRegistrationCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = buildReadableCode();
            if (workerRegistrationCodeRepository.findByCodeHash(EmployerInvitationToken.hash(candidate)).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to issue a unique worker registration code.");
    }

    private String buildReadableCode() {
        return "WORKER-" + randomSegment() + "-" + randomSegment();
    }

    private String randomSegment() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder builder = new StringBuilder(4);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int index = 0; index < 4; index++) {
            builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return builder.toString().toUpperCase(Locale.ROOT);
    }
}
