package com.workproofpay.backend.workproof.service;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.api.dto.request.CheckInWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CheckOutWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateContractRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkplaceRequest;
import com.workproofpay.backend.workproof.api.dto.response.CurrentContractResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryContractResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordListItemResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordListResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordStatus;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofReflectionStatus;
import com.workproofpay.backend.workproof.api.dto.response.WorkplaceListResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkplaceResponse;
import com.workproofpay.backend.workproof.model.WorkContract;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * WorkProof lane 1의 근무지, 활성 계약, 출퇴근 기록 흐름을 묶는 애플리케이션 서비스다.
 */
public class WorkProofLane1Service {

    private static final int DEFAULT_DAILY_WORK_MINUTES = 480;
    // Assumption: keep a small configurable default until PRD fixes the concrete monthly baseline.
    private static final int DEFAULT_MONTHLY_WORK_MINUTES = 12_540;

    private final UserRepository userRepository;
    private final WorkplaceRepository workplaceRepository;
    private final WorkContractRepository workContractRepository;
    private final WorkProofRepository workProofRepository;
    private final WorkProofLane1DraftValidator draftValidator;
    private final WorkProofMetricsCalculator workProofMetricsCalculator;

    @Transactional
    public WorkplaceResponse createWorkplace(Long userId, CreateWorkplaceRequest request) {
        Workplace saved = workplaceRepository.save(Workplace.create(
                findUser(userId),
                request.name(),
                request.address(),
                request.mapLabel(),
                request.latitude(),
                request.longitude()
        ));
        return toWorkplaceResponse(saved, false);
    }

    @Transactional(readOnly = true)
    public WorkplaceListResponse getWorkplaces(Long userId) {
        List<WorkplaceResponse> workplaces = workplaceRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(workplace -> toWorkplaceResponse(
                        workplace,
                        workContractRepository.existsByWorkplaceIdAndWorkplaceUserIdAndEffectiveToIsNull(workplace.getId(), userId)
                ))
                .toList();
        return new WorkplaceListResponse(workplaces);
    }

    @Transactional
    public CurrentContractResponse createContract(Long userId, CreateContractRequest request) {
        draftValidator.validateCreateContract(request);

        Workplace workplace = getOwnedWorkplace(userId, request.workplaceId());
        if (workContractRepository.existsByWorkplaceIdAndWorkplaceUserIdAndEffectiveToIsNull(workplace.getId(), userId)) {
            throw new ApiException(ErrorCode.ACTIVE_CONTRACT_EXISTS);
        }

        WorkContract saved = workContractRepository.save(WorkContract.activate(
                workplace,
                request.payUnit(),
                request.basePayAmount(),
                resolveDailyWorkMinutes(request),
                resolveMonthlyWorkMinutes(request),
                calculateNormalizedHourlyWage(request),
                request.effectiveFrom() != null ? request.effectiveFrom() : LocalDate.now()
        ));
        return toCurrentContractResponse(saved);
    }

    @Transactional(readOnly = true)
    public CurrentContractResponse getCurrentContract(Long userId, Long workplaceId) {
        getOwnedWorkplace(userId, workplaceId);
        return toCurrentContractResponse(getActiveContract(userId, workplaceId, ErrorCode.ACTIVE_CONTRACT_NOT_FOUND));
    }

    @Transactional
    public WorkProofRecordResponse checkIn(Long userId, CheckInWorkProofRequest request) {
        Workplace workplace = getOwnedWorkplace(userId, request.workplaceId());
        WorkContract contract = getActiveContract(userId, workplace.getId(), ErrorCode.ACTIVE_CONTRACT_REQUIRED);

        if (workProofRepository.findFirstByUserIdAndClockOutAtIsNullOrderByCreatedAtDesc(userId).isPresent()) {
            throw new ApiException(ErrorCode.ACTIVE_WORKPROOF_EXISTS);
        }

        LocalDate workDate = request.deviceAt().toLocalDate();
        if (workProofRepository.existsByUserIdAndWorkDate(userId, workDate)) {
            throw new ApiException(ErrorCode.WORK_DATE_ALREADY_EXISTS);
        }

        WorkProof saved = workProofRepository.save(WorkProof.checkIn(
                findUser(userId),
                workplace,
                contract,
                request.deviceAt(),
                LocalDateTime.now(),
                request.latitude(),
                request.longitude(),
                request.locationLabel()
        ));
        return toRecordResponse(saved);
    }

    @Transactional
    public WorkProofRecordResponse checkOut(Long userId, CheckOutWorkProofRequest request) {
        WorkProof active = workProofRepository.findFirstByUserIdAndClockOutAtIsNullOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACTIVE_WORKPROOF_NOT_FOUND));

        draftValidator.validateCheckOutSequence(active.getDeviceClockInAt(), request);
        active.completeCheckOut(
                request.deviceAt(),
                LocalDateTime.now(),
                request.latitude(),
                request.longitude(),
                request.locationLabel()
        );
        return toRecordResponse(active);
    }

    @Transactional(readOnly = true)
    public WorkProofRecordListResponse getRecords(Long userId, String month, Long workplaceId) {
        YearMonth targetMonth = parseYearMonth(month);
        getOwnedWorkplace(userId, workplaceId);

        List<WorkProofRecordListItemResponse> records = workProofRepository
                .findByUserIdAndWorkplaceIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(
                        userId,
                        workplaceId,
                        targetMonth.atDay(1),
                        targetMonth.atEndOfMonth()
                )
                .stream()
                .map(this::toRecordListItemResponse)
                .toList();

        return new WorkProofRecordListResponse(targetMonth.toString(), workplaceId, records);
    }

    @Transactional(readOnly = true)
    public WorkProofRecordResponse getRecord(Long userId, Long recordId) {
        WorkProof workProof = workProofRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPROOF_NOT_FOUND));
        return toRecordResponse(workProof);
    }

    @Transactional(readOnly = true)
    public WorkProofMonthlySummaryContractResponse getMonthlySummary(Long userId, String month, Long workplaceId) {
        YearMonth targetMonth = parseYearMonth(month);
        getOwnedWorkplace(userId, workplaceId);

        List<WorkProof> records = workProofRepository.findByUserIdAndWorkplaceIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(
                userId,
                workplaceId,
                targetMonth.atDay(1),
                targetMonth.atEndOfMonth()
        );

        List<WorkProof> reflected = records.stream().filter(WorkProof::isReflected).toList();
        int recordedWorkDays = (int) records.stream().map(WorkProof::getWorkDate).distinct().count();
        int reflectedWorkDays = (int) reflected.stream().map(WorkProof::getWorkDate).distinct().count();
        long totalWorkMinutes = reflected.stream().mapToLong(WorkProof::workedMinutes).sum();
        long overtimeMinutes = calculateOvertimeMinutes(reflected);
        long nightMinutes = calculateNightMinutes(reflected);
        int modifiedRecordCount = (int) records.stream().filter(WorkProof::isEdited).count();

        return new WorkProofMonthlySummaryContractResponse(
                targetMonth.toString(),
                workplaceId,
                reflectedWorkDays,
                totalWorkMinutes,
                overtimeMinutes,
                nightMinutes,
                modifiedRecordCount,
                new WorkProofMonthlySummaryContractResponse.ReflectionSummary(
                        reflected.size(),
                        0,
                        0
                ),
                new WorkProofMonthlySummaryContractResponse.IntegritySummary(
                        recordedWorkDays,
                        reflectedWorkDays,
                        totalWorkMinutes,
                        0,
                        List.of()
                ),
                new WorkProofMonthlySummaryContractResponse.FinanceReadinessSummary(
                        reflectedWorkDays,
                        reflectedWorkDays
                )
        );
    }

    private long calculateOvertimeMinutes(List<WorkProof> reflected) {
        return reflected.stream()
                .collect(java.util.stream.Collectors.groupingBy(WorkProof::getWorkDate, java.util.stream.Collectors.summingLong(WorkProof::workedMinutes)))
                .values()
                .stream()
                .mapToLong(total -> Math.max(0L, total - DEFAULT_DAILY_WORK_MINUTES))
                .sum();
    }

    private long calculateNightMinutes(List<WorkProof> reflected) {
        return reflected.stream().mapToLong(record -> {
            Long nightMinutes = workProofMetricsCalculator.toResponse(record).nightMinutes();
            return nightMinutes == null ? 0L : nightMinutes;
        }).sum();
    }

    private Workplace getOwnedWorkplace(Long userId, Long workplaceId) {
        return workplaceRepository.findByIdAndUserId(workplaceId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPLACE_NOT_FOUND));
    }

    private WorkContract getActiveContract(Long userId, Long workplaceId, ErrorCode errorCode) {
        return workContractRepository.findFirstByWorkplaceIdAndWorkplaceUserIdAndEffectiveToIsNullOrderByEffectiveFromDesc(workplaceId, userId)
                .orElseThrow(() -> new ApiException(errorCode));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    private YearMonth parseYearMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INVALID_YEAR_MONTH);
        }
    }

    private Integer resolveDailyWorkMinutes(CreateContractRequest request) {
        if (request.payUnit() != WorkProofPayUnit.DAILY) {
            return null;
        }
        return request.dailyWorkMinutes() != null ? request.dailyWorkMinutes() : DEFAULT_DAILY_WORK_MINUTES;
    }

    private Integer resolveMonthlyWorkMinutes(CreateContractRequest request) {
        if (request.payUnit() != WorkProofPayUnit.MONTHLY) {
            return null;
        }
        return request.monthlyWorkMinutes() != null ? request.monthlyWorkMinutes() : DEFAULT_MONTHLY_WORK_MINUTES;
    }

    private BigDecimal calculateNormalizedHourlyWage(CreateContractRequest request) {
        return switch (request.payUnit()) {
            case HOURLY -> request.basePayAmount();
            case DAILY -> request.basePayAmount()
                    .multiply(BigDecimal.valueOf(60))
                    .divide(BigDecimal.valueOf(resolveDailyWorkMinutes(request)), 2, RoundingMode.HALF_UP);
            case MONTHLY -> request.basePayAmount()
                    .multiply(BigDecimal.valueOf(60))
                    .divide(BigDecimal.valueOf(resolveMonthlyWorkMinutes(request)), 2, RoundingMode.HALF_UP);
        };
    }

    private WorkplaceResponse toWorkplaceResponse(Workplace workplace, boolean hasActiveContract) {
        return new WorkplaceResponse(
                workplace.getId(),
                workplace.getName(),
                workplace.getAddress(),
                workplace.getMapLabel(),
                workplace.getLatitude(),
                workplace.getLongitude(),
                hasActiveContract,
                workplace.getCreatedAt()
        );
    }

    private CurrentContractResponse toCurrentContractResponse(WorkContract contract) {
        return new CurrentContractResponse(
                contract.getId(),
                contract.getWorkplace().getId(),
                contract.getPayUnit(),
                contract.getBasePayAmount(),
                contract.getDailyWorkMinutes(),
                contract.getMonthlyWorkMinutes(),
                contract.getNormalizedHourlyWage(),
                contract.getEffectiveFrom(),
                contract.getEffectiveTo(),
                contract.isActive()
        );
    }

    private WorkProofRecordListItemResponse toRecordListItemResponse(WorkProof workProof) {
        return new WorkProofRecordListItemResponse(
                workProof.getId(),
                workProof.getWorkDate(),
                workProof.isCheckedIn() ? WorkProofRecordStatus.CHECKED_IN : WorkProofRecordStatus.CHECKED_OUT,
                workProof.getDeviceClockInAt(),
                workProof.getDeviceClockOutAt(),
                workProof.isReflected() ? workProof.workedMinutes() : null,
                workProof.isEdited(),
                workProof.isReflected() ? WorkProofReflectionStatus.REFLECTED : WorkProofReflectionStatus.PENDING
        );
    }

    private WorkProofRecordResponse toRecordResponse(WorkProof workProof) {
        Workplace workplace = workProof.getWorkplace();
        WorkContract contract = workProof.getContract();
        return new WorkProofRecordResponse(
                workProof.getId(),
                workProof.getWorkDate(),
                workProof.isCheckedIn() ? WorkProofRecordStatus.CHECKED_IN : WorkProofRecordStatus.CHECKED_OUT,
                workplace == null ? null : new WorkProofRecordResponse.WorkplaceSnapshot(
                        workplace.getId(),
                        workplace.getName(),
                        workplace.getAddress(),
                        workplace.getMapLabel(),
                        workplace.getLatitude(),
                        workplace.getLongitude()
                ),
                contract == null ? null : toCurrentContractResponse(contract),
                new WorkProofRecordResponse.EvidenceCaptureResponse(
                        workProof.getDeviceClockInAt(),
                        workProof.getServerClockInAt(),
                        workProof.getClockInLatitude(),
                        workProof.getClockInLongitude(),
                        workProof.getClockInLocationLabel()
                ),
                workProof.getDeviceClockOutAt() == null ? null : new WorkProofRecordResponse.EvidenceCaptureResponse(
                        workProof.getDeviceClockOutAt(),
                        workProof.getServerClockOutAt(),
                        workProof.getClockOutLatitude(),
                        workProof.getClockOutLongitude(),
                        workProof.getClockOutLocationLabel()
                ),
                workProof.isReflected() ? workProof.workedMinutes() : null,
                workProof.isEdited(),
                List.of(),
                List.of()
        );
    }
}
