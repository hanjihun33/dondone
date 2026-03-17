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

/**
 * WorkProof lane 1의 근무지, 활성 계약, 출퇴근 기록 흐름을 하나의 use-case 단위로 묶는다.
 * Wage/Advance가 재사용할 upstream 입력을 먼저 안정화하는 역할을 맡는다.
 */
@Service
@RequiredArgsConstructor
public class WorkProofLane1Service {

    private static final int DEFAULT_DAILY_WORK_MINUTES = 480;
    private static final int DEFAULT_ALLOWED_RADIUS_METERS = 1_000;
    // PRD에서 월 기준 시간이 고정되기 전까지는 작은 기본값으로 유지한다.
    private static final int DEFAULT_MONTHLY_WORK_MINUTES = 12_540;
    private static final double EARTH_RADIUS_METERS = 6_371_000d;
    private static final String TEMPORARY_WORKPLACE_NAME = "SSAFY (임시)";
    private static final String TEMPORARY_WORKPLACE_ADDRESS = "광주광역시 광산구 하남산단 6번로 107";
    private static final String TEMPORARY_WORKPLACE_MAP_LABEL = "광주 SSAFY";
    private static final double TEMPORARY_WORKPLACE_LATITUDE = 35.2031092d;
    private static final double TEMPORARY_WORKPLACE_LONGITUDE = 126.8083831d;

    private final UserRepository userRepository;
    private final WorkplaceRepository workplaceRepository;
    private final WorkContractRepository workContractRepository;
    private final WorkProofRepository workProofRepository;
    private final WorkProofLane1DraftValidator draftValidator;
    private final WorkProofMetricsCalculator workProofMetricsCalculator;

    /**
     * 사용자가 이후 출퇴근/월간 집계를 매달 수 있는 근무지 기준축을 생성한다.
     */
    @Transactional
    public WorkplaceResponse createWorkplace(Long userId, CreateWorkplaceRequest request) {
        Workplace saved = workplaceRepository.save(Workplace.create(
                findUser(userId),
                request.name(),
                request.address(),
                request.mapLabel(),
                request.latitude(),
                request.longitude(),
                DEFAULT_ALLOWED_RADIUS_METERS
        ));
        return toWorkplaceResponse(saved, false);
    }

    /**
     * 근무지 목록과 각 근무지의 활성 계약 존재 여부를 함께 돌려준다.
     */
    @Transactional
    public WorkplaceListResponse getWorkplaces(Long userId) {
        List<WorkplaceResponse> workplaces = getOrCreateSelectableWorkplaces(userId).stream()
                .map(workplace -> toWorkplaceResponse(
                        workplace,
                        workContractRepository.existsByWorkplaceIdAndWorkplaceUserIdAndEffectiveToIsNull(workplace.getId(), userId)
                ))
                .toList();
        return new WorkplaceListResponse(workplaces);
    }

    /**
     * lane 1에서는 근무지마다 활성 계약을 하나만 유지하도록 막고 계약 기준 급여 단위를 고정한다.
     */
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

    /**
     * 출퇴근 기록 생성 전에 현재 유효한 계약을 읽어갈 수 있게 활성 계약을 조회한다.
     */
    @Transactional(readOnly = true)
    public CurrentContractResponse getCurrentContract(Long userId, Long workplaceId) {
        getOwnedWorkplace(userId, workplaceId);
        return toCurrentContractResponse(getActiveContract(userId, workplaceId, ErrorCode.ACTIVE_CONTRACT_NOT_FOUND));
    }

    /**
     * 활성 계약이 있는 근무지에서만 출근을 열고, 동시에 하나의 열린 기록만 존재하게 강제한다.
     */
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

        validateWithinAllowedRadius(workplace, request.latitude(), request.longitude());

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

    /**
     * 현재 열려 있는 출근 기록 하나를 닫아 check-in/check-out 한 쌍을 완성한다.
     */
    @Transactional
    public WorkProofRecordResponse checkOut(Long userId, CheckOutWorkProofRequest request) {
        WorkProof active = workProofRepository.findFirstByUserIdAndClockOutAtIsNullOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACTIVE_WORKPROOF_NOT_FOUND));

        draftValidator.validateCheckOutSequence(active.getDeviceClockInAt(), request);
        if (active.getWorkplace() != null) {
            validateWithinAllowedRadius(active.getWorkplace(), request.latitude(), request.longitude());
        }
        active.completeCheckOut(
                request.deviceAt(),
                LocalDateTime.now(),
                request.latitude(),
                request.longitude(),
                request.locationLabel()
        );
        return toRecordResponse(active);
    }

    /**
     * 월/근무지 기준 목록 조회는 이후 Wage 월간 계산이 재사용할 기록 집합을 그대로 보여주는 용도다.
     */
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

    /**
     * 상세 조회는 workplace/contract snapshot과 evidence capture를 같이 내려 후속 증빙 흐름의 기준으로 쓴다.
     */
    @Transactional(readOnly = true)
    public WorkProofRecordResponse getRecord(Long userId, Long recordId) {
        WorkProof workProof = workProofRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPROOF_NOT_FOUND));
        return toRecordResponse(workProof);
    }

    /**
     * lane 1 월간 요약은 reflected 기준 집계를 먼저 고정해 Wage가 읽을 최소 월간 입력을 만든다.
     */
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

        // v1 요약은 reflected record만 급여 계산 입력으로 간주하고, recorded/reflected 차이는 integrity에 남긴다.
        List<WorkProof> reflected = records.stream().filter(WorkProof::isReflected).toList();
        List<WorkProof> needsReview = records.stream().filter(record -> !record.isReflected() || record.isEdited()).toList();
        int recordedWorkDays = (int) records.stream().map(WorkProof::getWorkDate).distinct().count();
        int reflectedWorkDays = (int) reflected.stream().map(WorkProof::getWorkDate).distinct().count();
        long totalWorkMinutes = reflected.stream().mapToLong(WorkProof::workedMinutes).sum();
        long overtimeMinutes = calculateOvertimeMinutes(reflected);
        long nightMinutes = calculateNightMinutes(reflected);
        int modifiedRecordCount = (int) records.stream().filter(WorkProof::isEdited).count();
        long pendingMinutes = needsReview.stream()
                .filter(record -> !record.isReflected())
                .mapToLong(WorkProof::workedMinutes)
                .sum();
        List<String> riskFlags = buildRiskFlags(records);

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
                        needsReview.size(),
                        0
                ),
                new WorkProofMonthlySummaryContractResponse.IntegritySummary(
                        recordedWorkDays,
                        reflectedWorkDays,
                        totalWorkMinutes,
                        pendingMinutes,
                        riskFlags
                ),
                new WorkProofMonthlySummaryContractResponse.FinanceReadinessSummary(
                        reflectedWorkDays,
                        reflectedWorkDays
                )
        );
    }

    // 연장근무는 일 단위 총 근무시간에서 lane 1 기본 소정근로시간(480분)을 초과한 분만 합산한다.
    private long calculateOvertimeMinutes(List<WorkProof> reflected) {
        return reflected.stream()
                .collect(java.util.stream.Collectors.groupingBy(WorkProof::getWorkDate, java.util.stream.Collectors.summingLong(WorkProof::workedMinutes)))
                .values()
                .stream()
                .mapToLong(total -> Math.max(0L, total - DEFAULT_DAILY_WORK_MINUTES))
                .sum();
    }

    // 야간근무 분은 기존 metrics calculator 결과를 재사용해 월간 응답 shape로만 모아준다.
    private long calculateNightMinutes(List<WorkProof> reflected) {
        return reflected.stream().mapToLong(record -> {
            Long nightMinutes = workProofMetricsCalculator.toResponse(record).nightMinutes();
            return nightMinutes == null ? 0L : nightMinutes;
        }).sum();
    }

    private List<String> buildRiskFlags(List<WorkProof> records) {
        java.util.ArrayList<String> flags = new java.util.ArrayList<>();
        if (records.stream().anyMatch(WorkProof::isEdited)) {
            flags.add("MODIFIED_RECORD_PRESENT");
        }
        if (records.stream().anyMatch(record -> !record.isReflected())) {
            flags.add("PENDING_WORKPROOF_PRESENT");
        }
        return List.copyOf(flags);
    }

    // 고용주 등록 기능이 들어오기 전까지는 lane 1 출퇴근 흐름이 끊기지 않도록 임시 근무지를 보장한다.
    private List<Workplace> getOrCreateSelectableWorkplaces(Long userId) {
        List<Workplace> workplaces = workplaceRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (!workplaces.isEmpty()) {
            return workplaces;
        }

        Workplace temporary = workplaceRepository.save(Workplace.create(
                findUser(userId),
                TEMPORARY_WORKPLACE_NAME,
                TEMPORARY_WORKPLACE_ADDRESS,
                TEMPORARY_WORKPLACE_MAP_LABEL,
                TEMPORARY_WORKPLACE_LATITUDE,
                TEMPORARY_WORKPLACE_LONGITUDE,
                DEFAULT_ALLOWED_RADIUS_METERS
        ));
        return List.of(temporary);
    }

    private Workplace getOwnedWorkplace(Long userId, Long workplaceId) {
        return workplaceRepository.findByIdAndUserId(workplaceId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPLACE_NOT_FOUND));
    }

    // P0에서는 GPS를 백그라운드 추적이 아니라 출퇴근 허용 범위를 막는 1차 차단 규칙으로 쓴다.
    private void validateWithinAllowedRadius(Workplace workplace, Double latitude, Double longitude) {
        double distanceMeters = calculateDistanceMeters(
                workplace.getLatitude(),
                workplace.getLongitude(),
                latitude,
                longitude
        );
        if (distanceMeters > workplace.resolveAllowedRadiusMeters(DEFAULT_ALLOWED_RADIUS_METERS)) {
            throw new ApiException(ErrorCode.WORKPLACE_RADIUS_EXCEEDED);
        }
    }

    private double calculateDistanceMeters(double startLatitude,
                                           double startLongitude,
                                           double endLatitude,
                                           double endLongitude) {
        double latitudeDelta = Math.toRadians(endLatitude - startLatitude);
        double longitudeDelta = Math.toRadians(endLongitude - startLongitude);
        double startLatitudeRadians = Math.toRadians(startLatitude);
        double endLatitudeRadians = Math.toRadians(endLatitude);

        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(startLatitudeRadians) * Math.cos(endLatitudeRadians)
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double angularDistance = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return EARTH_RADIUS_METERS * angularDistance;
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

    // DAILY/MONTHLY 계약도 Wage에서 같은 기준으로 비교할 수 있게 시간당 단가로 정규화한다.
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
                workplace.resolveAllowedRadiusMeters(DEFAULT_ALLOWED_RADIUS_METERS),
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
