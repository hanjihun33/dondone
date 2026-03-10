package com.workproofpay.backend.workproof.service;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofResponse;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkProofService {

    private final WorkProofRepository workProofRepository;
    private final UserRepository userRepository;
    private final WorkProofRequestValidator workProofRequestValidator;
    private final WorkProofMetricsCalculator workProofMetricsCalculator;

    @Transactional
    public WorkProofResponse create(Long userId, CreateWorkProofRequest request) {
        workProofRequestValidator.validateForCreate(request);
        WorkProof workProof = WorkProof.record(
                findUser(userId),
                request.workDate(),
                request.clockInAt(),
                request.clockOutAt(),
                request.deviceClockInAt(),
                request.deviceClockOutAt(),
                LocalDateTime.now(),
                request.clockInLatitude(),
                request.clockInLongitude(),
                request.clockOutLatitude(),
                request.clockOutLongitude(),
                request.memo(),
                request.editReason(),
                request.attachmentCount()
        );

        return toResponse(workProofRepository.save(workProof));
    }

    @Transactional(readOnly = true)
    public List<WorkProofResponse> getWorkProofs(Long userId, String yearMonth, LocalDate asOf) {
        return loadVisibleWorkProofs(userId, yearMonth, asOf).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkProofResponse getWorkProof(Long userId, Long workProofId) {
        WorkProof workProof = workProofRepository.findByIdAndUserId(workProofId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPROOF_NOT_FOUND));
        return toResponse(workProof);
    }

    @Transactional(readOnly = true)
    public WorkProofMonthlyMetrics getMonthlyMetrics(Long userId, String yearMonth, LocalDate asOf) {
        YearMonth targetMonth = parseYearMonth(yearMonth);
        LocalDate effectiveAsOf = resolveAsOf(targetMonth, asOf);
        if (effectiveAsOf.isBefore(targetMonth.atDay(1))) {
            return WorkProofMonthlyMetrics.empty(targetMonth.toString(), effectiveAsOf);
        }

        return workProofMetricsCalculator.summarizeMonthlyMetrics(
                targetMonth,
                effectiveAsOf,
                loadVisibleWorkProofs(userId, targetMonth, effectiveAsOf)
        );
    }

    public YearMonth parseYearMonth(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INVALID_YEAR_MONTH);
        }
    }

    public WorkProofResponse toResponse(WorkProof workProof) {
        return workProofMetricsCalculator.toResponse(workProof);
    }

    private List<WorkProof> loadForMonth(Long userId, String yearMonth) {
        if (!StringUtils.hasText(yearMonth)) {
            return workProofRepository.findByUserIdOrderByWorkDateDescClockInAtDesc(userId);
        }
        YearMonth targetMonth = parseYearMonth(yearMonth);
        return workProofRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(
                userId,
                targetMonth.atDay(1),
                targetMonth.atEndOfMonth()
        );
    }

    private List<WorkProof> loadVisibleWorkProofs(Long userId, String yearMonth, LocalDate asOf) {
        return loadForMonth(userId, yearMonth).stream()
                .filter(workProof -> isVisibleAsOf(workProof, asOf))
                .toList();
    }

    private List<WorkProof> loadVisibleWorkProofs(Long userId, YearMonth targetMonth, LocalDate asOf) {
        return workProofRepository
                .findByUserIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(userId, targetMonth.atDay(1), targetMonth.atEndOfMonth())
                .stream()
                .filter(workProof -> isVisibleAsOf(workProof, asOf))
                .toList();
    }

    private boolean isVisibleAsOf(WorkProof workProof, LocalDate asOf) {
        return asOf == null || !workProof.getWorkDate().isAfter(asOf);
    }

    private LocalDate resolveAsOf(YearMonth targetMonth, LocalDate asOf) {
        return asOf == null ? targetMonth.atEndOfMonth() : asOf;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    public BigDecimal minutesToHours(long minutes) {
        return workProofMetricsCalculator.minutesToHours(minutes);
    }
}
