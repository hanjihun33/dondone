package com.workproofpay.backend.wage.service;

import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.wage.model.WageVerification;
import com.workproofpay.backend.wage.repo.WageVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Documents / Claim가 같은 verification ownership 규칙을 재사용하게 하는 read service다.
 */
@Service
@RequiredArgsConstructor
public class WageVerificationQueryService {

    private final WageVerificationRepository wageVerificationRepository;

    /**
     * Documents / Claim에서 verification 소유권 검증을 중복 구현하지 않도록 공통 조회로 묶는다.
     */
    @Transactional(readOnly = true)
    public WageVerification getOwnedVerification(Long userId, Long verificationId) {
        return wageVerificationRepository.findByIdAndUserId(verificationId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WAGE_VERIFICATION_NOT_FOUND));
    }
}
