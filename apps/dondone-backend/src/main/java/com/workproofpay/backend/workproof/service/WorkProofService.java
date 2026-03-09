package com.workproofpay.backend.workproof.service;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofResponse;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.WorkProofFinancialStatus;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkProofService {

    private final WorkProofRepository workProofRepository;
    private final UserRepository userRepository;

    @Transactional
    public WorkProofResponse create(Long userId, CreateWorkProofRequest request) {
        validateRequest(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        WorkProof workProof = new WorkProof(
                user,
                request.workDate(),
                request.clockInAt(),
                request.clockOutAt(),
                request.deviceClockInAt(),
                request.deviceClockOutAt(),
                now,
                request.clockOutAt() == null ? null : now,
                request.clockInLatitude(),
                request.clockInLongitude(),
                request.clockOutLatitude(),
                request.clockOutLongitude(),
                request.memo(),
                request.editReason(),
                request.attachmentCount() == null ? 0 : request.attachmentCount(),
                deriveStatus(request.clockOutAt())
        );

        return toResponse(workProofRepository.save(workProof));
    }

    @Transactional(readOnly = true)
    public List<WorkProofResponse> getWorkProofs(Long userId, String yearMonth, LocalDate asOf) {
        List<WorkProof> workProofs = loadForMonth(userId, yearMonth);
        return workProofs.stream()
                .filter(workProof -> asOf == null || !workProof.getWorkDate().isAfter(asOf))
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
        LocalDate effectiveAsOf = asOf == null ? targetMonth.atEndOfMonth() : asOf;
        if (effectiveAsOf.isBefore(targetMonth.atDay(1))) {
            return new WorkProofMonthlyMetrics(
                    targetMonth.toString(),
                    effectiveAsOf,
                    0,
                    0L,
                    0L,
                    0L,
                    0,
                    0,
                    0,
                    List.of()
            );
        }

        List<WorkProof> filtered = workProofRepository
                .findByUserIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(userId, targetMonth.atDay(1), targetMonth.atEndOfMonth())
                .stream()
                .filter(workProof -> !workProof.getWorkDate().isAfter(effectiveAsOf))
                .toList();

        List<WorkProof> reflected = filtered.stream()
                .filter(this::isReflected)
                .toList();
        List<WorkProof> pending = filtered.stream()
                .filter(workProof -> !isReflected(workProof))
                .toList();

        long totalWorkedMinutes = reflected.stream()
                .mapToLong(this::calculateWorkedMinutes)
                .sum();
        long totalNightMinutes = reflected.stream()
                .mapToLong(this::calculateNightMinutes)
                .sum();
        long totalOvertimeMinutes = calculateOvertimeMinutes(reflected);
        int editedRecordCount = (int) filtered.stream()
                .filter(this::isEdited)
                .count();
        int totalWorkDays = (int) reflected.stream()
                .map(WorkProof::getWorkDate)
                .distinct()
                .count();

        return new WorkProofMonthlyMetrics(
                targetMonth.toString(),
                effectiveAsOf,
                totalWorkDays,
                totalWorkedMinutes,
                totalOvertimeMinutes,
                totalNightMinutes,
                editedRecordCount,
                reflected.size(),
                pending.size(),
                reflected.stream().map(WorkProof::getId).toList()
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
        Long workedMinutes = isReflected(workProof) ? calculateWorkedMinutes(workProof) : null;
        Long overtimeMinutes = isReflected(workProof) ? calculateOvertimeMinutes(List.of(workProof)) : null;
        Long nightMinutes = isReflected(workProof) ? calculateNightMinutes(workProof) : null;

        return new WorkProofResponse(
                workProof.getId(),
                workProof.getWorkDate(),
                workProof.getClockInAt(),
                workProof.getClockOutAt(),
                workProof.getDeviceClockInAt(),
                workProof.getDeviceClockOutAt(),
                workProof.getServerClockInAt(),
                workProof.getServerClockOutAt(),
                workProof.getClockInLatitude(),
                workProof.getClockInLongitude(),
                workProof.getClockOutLatitude(),
                workProof.getClockOutLongitude(),
                workProof.getMemo(),
                workProof.getEditReason(),
                workProof.getAttachmentCount(),
                isEdited(workProof),
                workProof.getFinancialStatus().name(),
                workedMinutes,
                overtimeMinutes,
                nightMinutes,
                workProof.getCreatedAt(),
                workProof.getUpdatedAt()
        );
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

    private void validateRequest(CreateWorkProofRequest request) {
        if (!request.workDate().equals(request.clockInAt().toLocalDate())) {
            throw new ApiException(ErrorCode.INVALID_WORK_DATE);
        }
        if (request.clockOutAt() != null && !request.clockOutAt().isAfter(request.clockInAt())) {
            throw new ApiException(ErrorCode.INVALID_WORKPROOF_TIME);
        }
        boolean hasAnyClockOutField = request.clockOutAt() != null
                || request.deviceClockOutAt() != null
                || request.clockOutLatitude() != null
                || request.clockOutLongitude() != null;
        boolean hasAllClockOutFields = request.clockOutAt() != null
                && request.deviceClockOutAt() != null
                && request.clockOutLatitude() != null
                && request.clockOutLongitude() != null;
        if (hasAnyClockOutField && !hasAllClockOutFields) {
            throw new ApiException(ErrorCode.INCOMPLETE_CLOCK_OUT);
        }
        if (request.deviceClockOutAt() != null && !request.deviceClockOutAt().isAfter(request.deviceClockInAt())) {
            throw new ApiException(ErrorCode.INVALID_DEVICE_TIME);
        }
    }

    private WorkProofFinancialStatus deriveStatus(LocalDateTime clockOutAt) {
        return clockOutAt == null ? WorkProofFinancialStatus.PENDING : WorkProofFinancialStatus.REFLECTED;
    }

    private boolean isReflected(WorkProof workProof) {
        return workProof.getFinancialStatus() == WorkProofFinancialStatus.REFLECTED && workProof.getClockOutAt() != null;
    }

    private boolean isEdited(WorkProof workProof) {
        return StringUtils.hasText(workProof.getEditReason()) || workProof.getAttachmentCount() > 0;
    }

    private long calculateWorkedMinutes(WorkProof workProof) {
        if (workProof.getClockOutAt() == null) {
            return 0L;
        }
        return Duration.between(workProof.getClockInAt(), workProof.getClockOutAt()).toMinutes();
    }

    private long calculateOvertimeMinutes(List<WorkProof> reflected) {
        Map<LocalDate, Long> minutesByDay = reflected.stream()
                .collect(Collectors.groupingBy(WorkProof::getWorkDate, Collectors.summingLong(this::calculateWorkedMinutes)));

        return minutesByDay.values().stream()
                .mapToLong(total -> Math.max(0L, total - Duration.ofHours(8).toMinutes()))
                .sum();
    }

    private long calculateNightMinutes(WorkProof workProof) {
        if (workProof.getClockOutAt() == null) {
            return 0L;
        }
        LocalDateTime start = workProof.getClockInAt();
        LocalDateTime end = workProof.getClockOutAt();
        long nightMinutes = 0L;

        for (LocalDate date = start.toLocalDate().minusDays(1); !date.isAfter(end.toLocalDate()); date = date.plusDays(1)) {
            LocalDateTime nightStart = LocalDateTime.of(date, LocalTime.of(22, 0));
            LocalDateTime nightEnd = LocalDateTime.of(date.plusDays(1), LocalTime.of(6, 0));
            LocalDateTime overlapStart = start.isAfter(nightStart) ? start : nightStart;
            LocalDateTime overlapEnd = end.isBefore(nightEnd) ? end : nightEnd;
            if (overlapEnd.isAfter(overlapStart)) {
                nightMinutes += Duration.between(overlapStart, overlapEnd).toMinutes();
            }
        }

        return nightMinutes;
    }

    public BigDecimal minutesToHours(long minutes) {
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
    }
}
