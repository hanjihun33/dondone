package com.workproofpay.backend.employer.service;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.employer.api.dto.request.EmployerAttendanceBoardQuery;
import com.workproofpay.backend.employer.api.dto.request.EmployerWorkersQuery;
import com.workproofpay.backend.employer.api.dto.response.EmployerAttendanceBoardDayResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerAttendanceBoardResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerAttendanceBoardRowResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerDashboardSummaryResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerWorkerAttendanceStatus;
import com.workproofpay.backend.employer.api.dto.response.EmployerWorkerDetailResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerWorkerLatestRecordResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerWorkerRecentDayResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerWorkerSummaryResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerWorkersResponse;
import com.workproofpay.backend.employer.model.EmploymentMembership;
import com.workproofpay.backend.employer.model.EmploymentMembershipStatus;
import com.workproofpay.backend.employer.repo.EmploymentMembershipRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordStatus;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofReflectionStatus;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class EmployerWorkerReadModelService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int BOARD_DAYS = 7;
    private static final int RECENT_DAY_COUNT = 7;

    private final EmployerAccessScopeService employerAccessScopeService;
    private final EmploymentMembershipRepository employmentMembershipRepository;
    private final UserRepository userRepository;
    private final WorkProofRepository workProofRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public EmployerWorkersResponse getWorkers(Long accountId, EmployerWorkersQuery query) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(accountId);
        LocalDate today = LocalDate.now(clock);
        List<ScopedWorkerSnapshot> scopedWorkers = loadTodaySnapshots(scope, today);
        List<ScopedWorkerSnapshot> filteredWorkers = applyWorkerFilters(scopedWorkers, query);
        Pagination pagination = paginate(filteredWorkers.size(), query.getPage(), query.getSize());

        List<EmployerWorkerSummaryResponse> rows = filteredWorkers.subList(pagination.fromIndex(), pagination.toIndex()).stream()
                .map(this::toWorkerSummaryResponse)
                .toList();

        return new EmployerWorkersResponse(
                rows,
                pagination.page(),
                pagination.size(),
                filteredWorkers.size(),
                pagination.totalPages()
        );
    }

    @Transactional(readOnly = true)
    public EmployerDashboardSummaryResponse getDashboardSummary(Long accountId) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(accountId);
        LocalDate today = LocalDate.now(clock);
        List<ScopedWorkerSnapshot> scopedWorkers = loadTodaySnapshots(scope, today);

        long workingCount = scopedWorkers.stream()
                .filter(snapshot -> snapshot.attendanceStatus() == EmployerWorkerAttendanceStatus.WORKING)
                .count();
        long completedCount = scopedWorkers.stream()
                .filter(snapshot -> snapshot.attendanceStatus() == EmployerWorkerAttendanceStatus.COMPLETED)
                .count();
        long needsReviewCount = scopedWorkers.stream()
                .filter(snapshot -> snapshot.attendanceStatus() == EmployerWorkerAttendanceStatus.NEEDS_REVIEW)
                .count();
        long noRecordCount = scopedWorkers.stream()
                .filter(snapshot -> snapshot.attendanceStatus() == EmployerWorkerAttendanceStatus.NO_RECORD)
                .count();

        return new EmployerDashboardSummaryResponse(
                scopedWorkers.size(),
                workingCount,
                completedCount,
                needsReviewCount,
                noRecordCount,
                today
        );
    }

    @Transactional(readOnly = true)
    public EmployerWorkerDetailResponse getWorkerDetail(Long accountId, Long workerId) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(accountId);
        LocalDate today = LocalDate.now(clock);
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        EmploymentMembership membership = getRequiredAccessibleMembership(scope, workerId, today);

        WorkProof latestWorkProof = workProofRepository.findFirstByUserIdAndWorkplaceIdOrderByWorkDateDescClockInAtDesc(
                workerId,
                scope.defaultWorkplaceId()
        ).orElse(null);
        DaySnapshot latestDaySnapshot = buildDaySnapshot(latestWorkProof, latestWorkProof == null ? null : latestWorkProof.getWorkDate());

        LocalDate recentStartDate = today.minusDays(RECENT_DAY_COUNT - 1L);
        Map<LocalDate, DaySnapshot> recentSnapshotsByDate = latestDaySnapshotsByDate(
                workProofRepository.findByUserIdAndWorkplaceIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(
                        workerId,
                        scope.defaultWorkplaceId(),
                        recentStartDate,
                        today
                )
        );

        List<EmployerWorkerRecentDayResponse> recentDays = new ArrayList<>(RECENT_DAY_COUNT);
        for (int index = 0; index < RECENT_DAY_COUNT; index++) {
            LocalDate date = recentStartDate.plusDays(index);
            DaySnapshot daySnapshot = recentSnapshotsByDate.getOrDefault(date, emptyDaySnapshot(date));
            recentDays.add(new EmployerWorkerRecentDayResponse(
                    daySnapshot.date(),
                    daySnapshot.recordStatus(),
                    daySnapshot.reflectionStatus(),
                    daySnapshot.attendanceStatus(),
                    daySnapshot.workedMinutes()
            ));
        }

        return new EmployerWorkerDetailResponse(
                worker.getId(),
                null,
                worker.getName(),
                null,
                null,
                worker.getEmail(),
                null,
                null,
                membership.getEffectiveFrom(),
                membership.getEffectiveTo(),
                latestDaySnapshot.recordStatus(),
                latestDaySnapshot.reflectionStatus(),
                latestDaySnapshot.attendanceStatus(),
                latestDaySnapshot.date(),
                toLatestRecordResponse(latestWorkProof, latestDaySnapshot),
                List.copyOf(recentDays)
        );
    }

    @Transactional(readOnly = true)
    public EmployerAttendanceBoardResponse getAttendanceBoard(Long accountId, EmployerAttendanceBoardQuery query) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(accountId);
        WeekRange weekRange = resolveWeekRange(query.getWeekStart());
        List<ScopedBoardRowSnapshot> rows = loadBoardSnapshots(scope, weekRange);
        List<ScopedBoardRowSnapshot> filteredRows = applyBoardFilters(rows, query);
        Pagination pagination = paginate(filteredRows.size(), query.getPage(), query.getSize());

        List<EmployerAttendanceBoardRowResponse> responseRows = filteredRows.subList(pagination.fromIndex(), pagination.toIndex()).stream()
                .map(this::toAttendanceBoardRowResponse)
                .toList();

        return new EmployerAttendanceBoardResponse(
                weekRange.startDate(),
                weekRange.endDate(),
                responseRows,
                pagination.page(),
                pagination.size(),
                filteredRows.size(),
                pagination.totalPages(),
                pagination.hasNext()
        );
    }

    private List<ScopedWorkerSnapshot> loadTodaySnapshots(EmployerAccessScope scope, LocalDate today) {
        Map<Long, EmploymentMembership> membershipByWorkerId = membershipMap(
                employmentMembershipRepository.findActiveByScope(
                        scope.companyId(),
                        scope.defaultWorkplaceId(),
                        EmploymentMembershipStatus.ACTIVE,
                        today
                )
        );
        if (membershipByWorkerId.isEmpty()) {
            return List.of();
        }

        List<Long> workerIds = new ArrayList<>(membershipByWorkerId.keySet());
        Map<Long, User> usersById = usersById(workerIds);
        Map<Long, WorkProof> latestWorkProofByWorkerId = latestWorkProofByWorkerId(
                workerIds,
                scope.defaultWorkplaceId(),
                today
        );

        return workerIds.stream()
                .map(workerId -> toScopedWorkerSnapshot(
                        usersById.get(workerId),
                        membershipByWorkerId.get(workerId),
                        latestWorkProofByWorkerId.get(workerId)
                ))
                .filter(Objects::nonNull)
                .sorted(workerSnapshotComparator())
                .toList();
    }

    private List<ScopedBoardRowSnapshot> loadBoardSnapshots(EmployerAccessScope scope, WeekRange weekRange) {
        Map<Long, EmploymentMembership> membershipByWorkerId = membershipMap(
                employmentMembershipRepository.findOverlappingByScope(
                        scope.companyId(),
                        scope.defaultWorkplaceId(),
                        EmploymentMembershipStatus.ACTIVE,
                        weekRange.startDate(),
                        weekRange.endDate()
                )
        );
        if (membershipByWorkerId.isEmpty()) {
            return List.of();
        }

        List<Long> workerIds = new ArrayList<>(membershipByWorkerId.keySet());
        Map<Long, User> usersById = usersById(workerIds);
        Map<Long, Map<LocalDate, DaySnapshot>> snapshotsByWorkerId = daySnapshotsByWorkerId(
                workerIds,
                scope.defaultWorkplaceId(),
                weekRange
        );

        return workerIds.stream()
                .map(workerId -> toScopedBoardRowSnapshot(
                        usersById.get(workerId),
                        membershipByWorkerId.get(workerId),
                        snapshotsByWorkerId.getOrDefault(workerId, Map.of()),
                        weekRange
                ))
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(ScopedBoardRowSnapshot::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ScopedBoardRowSnapshot::workerId))
                .toList();
    }

    private Map<Long, EmploymentMembership> membershipMap(List<EmploymentMembership> memberships) {
        Map<Long, EmploymentMembership> membershipByWorkerId = new LinkedHashMap<>();
        for (EmploymentMembership membership : memberships) {
            membershipByWorkerId.putIfAbsent(membership.getWorkerAccountId(), membership);
        }
        return membershipByWorkerId;
    }

    private Map<Long, User> usersById(List<Long> workerIds) {
        return userRepository.findAllById(workerIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<Long, WorkProof> latestWorkProofByWorkerId(List<Long> workerIds, Long workplaceId, LocalDate workDate) {
        if (workerIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, WorkProof> latestByWorkerId = new LinkedHashMap<>();
        for (WorkProof workProof : workProofRepository.findByUserIdInAndWorkplaceIdAndWorkDateOrderByUserIdAscCreatedAtDescIdDesc(
                workerIds,
                workplaceId,
                workDate
        )) {
            latestByWorkerId.putIfAbsent(workProof.getUser().getId(), workProof);
        }
        return latestByWorkerId;
    }

    private EmploymentMembership getRequiredAccessibleMembership(EmployerAccessScope scope, Long workerId, LocalDate targetDate) {
        return employmentMembershipRepository.findActiveWorkerMembershipByScope(
                        workerId,
                        scope.companyId(),
                        scope.defaultWorkplaceId(),
                        EmploymentMembershipStatus.ACTIVE,
                        targetDate
                ).stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));
    }

    private Map<Long, Map<LocalDate, DaySnapshot>> daySnapshotsByWorkerId(List<Long> workerIds,
                                                                          Long workplaceId,
                                                                          WeekRange weekRange) {
        if (workerIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Map<LocalDate, WorkProof>> latestByWorkerAndDate = new LinkedHashMap<>();
        for (WorkProof workProof : workProofRepository.findByUserIdInAndWorkplaceIdAndWorkDateBetweenOrderByUserIdAscWorkDateAscCreatedAtDescIdDesc(
                workerIds,
                workplaceId,
                weekRange.startDate(),
                weekRange.endDate()
        )) {
            latestByWorkerAndDate
                    .computeIfAbsent(workProof.getUser().getId(), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(workProof.getWorkDate(), workProof);
        }

        Map<Long, Map<LocalDate, DaySnapshot>> snapshotsByWorkerId = new LinkedHashMap<>();
        for (Long workerId : workerIds) {
            Map<LocalDate, WorkProof> proofsByDate = latestByWorkerAndDate.getOrDefault(workerId, Map.of());
            Map<LocalDate, DaySnapshot> days = new LinkedHashMap<>();
            for (LocalDate date : weekRange.dates()) {
                days.put(date, buildDaySnapshot(proofsByDate.get(date), date));
            }
            snapshotsByWorkerId.put(workerId, Map.copyOf(days));
        }
        return snapshotsByWorkerId;
    }

    private Map<LocalDate, DaySnapshot> latestDaySnapshotsByDate(List<WorkProof> workProofs) {
        Map<LocalDate, DaySnapshot> snapshotsByDate = new LinkedHashMap<>();
        for (WorkProof workProof : workProofs) {
            snapshotsByDate.putIfAbsent(workProof.getWorkDate(), buildDaySnapshot(workProof, workProof.getWorkDate()));
        }
        return snapshotsByDate;
    }

    private ScopedWorkerSnapshot toScopedWorkerSnapshot(User user,
                                                        EmploymentMembership membership,
                                                        WorkProof latestWorkProof) {
        if (user == null || membership == null) {
            return null;
        }

        DaySnapshot daySnapshot = buildDaySnapshot(latestWorkProof, latestWorkProof == null ? null : latestWorkProof.getWorkDate());
        return new ScopedWorkerSnapshot(
                user.getId(),
                user.getName(),
                user.getEmail(),
                daySnapshot.recordStatus(),
                daySnapshot.reflectionStatus(),
                daySnapshot.attendanceStatus(),
                daySnapshot.date()
        );
    }

    private ScopedBoardRowSnapshot toScopedBoardRowSnapshot(User user,
                                                            EmploymentMembership membership,
                                                            Map<LocalDate, DaySnapshot> snapshotsByDate,
                                                            WeekRange weekRange) {
        if (user == null || membership == null) {
            return null;
        }

        List<DaySnapshot> days = weekRange.dates().stream()
                .map(date -> snapshotsByDate.getOrDefault(date, emptyDaySnapshot(date)))
                .toList();

        return new ScopedBoardRowSnapshot(
                user.getId(),
                user.getName(),
                user.getEmail(),
                days
        );
    }

    private List<ScopedWorkerSnapshot> applyWorkerFilters(List<ScopedWorkerSnapshot> scopedWorkers, EmployerWorkersQuery query) {
        String normalizedQuery = normalizeQuery(query.getQuery());
        Set<EmployerWorkerAttendanceStatus> requestedStatuses = normalizedStatuses(query.getStatuses());

        return scopedWorkers.stream()
                .filter(snapshot -> matchesQuery(snapshot.name(), snapshot.email(), normalizedQuery))
                .filter(snapshot -> requestedStatuses.isEmpty() || requestedStatuses.contains(snapshot.attendanceStatus()))
                .toList();
    }

    private List<ScopedBoardRowSnapshot> applyBoardFilters(List<ScopedBoardRowSnapshot> rows, EmployerAttendanceBoardQuery query) {
        String normalizedQuery = normalizeQuery(query.getQuery());
        Set<EmployerWorkerAttendanceStatus> requestedStatuses = normalizedStatuses(query.getStatuses());

        return rows.stream()
                .filter(row -> matchesQuery(row.name(), row.email(), normalizedQuery))
                .filter(row -> requestedStatuses.isEmpty()
                        || row.days().stream().anyMatch(day -> requestedStatuses.contains(day.attendanceStatus())))
                .toList();
    }

    private boolean matchesQuery(String name, String email, String normalizedQuery) {
        if (normalizedQuery == null) {
            return true;
        }
        return name.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || email.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private EmployerWorkerSummaryResponse toWorkerSummaryResponse(ScopedWorkerSnapshot snapshot) {
        return new EmployerWorkerSummaryResponse(
                snapshot.workerId(),
                null,
                snapshot.name(),
                null,
                null,
                snapshot.email(),
                null,
                null,
                snapshot.recordStatus(),
                snapshot.reflectionStatus(),
                snapshot.attendanceStatus(),
                snapshot.latestWorkDate()
        );
    }

    private EmployerAttendanceBoardRowResponse toAttendanceBoardRowResponse(ScopedBoardRowSnapshot snapshot) {
        return new EmployerAttendanceBoardRowResponse(
                snapshot.workerId(),
                snapshot.name(),
                null,
                null,
                snapshot.days().stream()
                        .map(day -> new EmployerAttendanceBoardDayResponse(
                                day.date(),
                                day.recordStatus(),
                                day.reflectionStatus(),
                                day.attendanceStatus(),
                                day.workedMinutes()
                        ))
                        .toList()
        );
    }

    private EmployerWorkerLatestRecordResponse toLatestRecordResponse(WorkProof latestWorkProof, DaySnapshot latestDaySnapshot) {
        if (latestWorkProof == null) {
            return null;
        }

        return new EmployerWorkerLatestRecordResponse(
                latestWorkProof.getWorkDate(),
                latestWorkProof.getClockInAt(),
                latestWorkProof.getClockOutAt(),
                latestWorkProof.resolveRecognizedClockInAt(),
                latestWorkProof.resolveRecognizedClockOutAt(),
                latestDaySnapshot.recordStatus(),
                latestDaySnapshot.reflectionStatus(),
                latestDaySnapshot.attendanceStatus(),
                latestDaySnapshot.workedMinutes(),
                latestWorkProof.isNeedsReview(),
                latestWorkProof.isClockOutOutsideAllowedRadius(),
                latestWorkProof.isEdited(),
                latestWorkProof.resolveWorkplaceName(),
                latestWorkProof.resolveWorkplaceAddress(),
                latestWorkProof.resolveWorkplaceMapLabel(),
                latestWorkProof.getClockInLocationLabel(),
                latestWorkProof.getClockOutLocationLabel()
        );
    }

    private DaySnapshot buildDaySnapshot(WorkProof workProof, LocalDate date) {
        if (workProof == null) {
            return emptyDaySnapshot(date);
        }

        return new DaySnapshot(
                workProof.getWorkDate(),
                resolveRecordStatus(workProof),
                resolveReflectionStatus(workProof),
                resolveAttendanceStatus(workProof),
                workProof.resolveRecognizedClockOutAt() == null ? null : workProof.workedMinutes()
        );
    }

    private DaySnapshot emptyDaySnapshot(LocalDate date) {
        return new DaySnapshot(
                date,
                null,
                null,
                EmployerWorkerAttendanceStatus.NO_RECORD,
                null
        );
    }

    private EmployerWorkerAttendanceStatus resolveAttendanceStatus(WorkProof latestWorkProof) {
        if (latestWorkProof == null) {
            return EmployerWorkerAttendanceStatus.NO_RECORD;
        }
        if (latestWorkProof.isCheckedIn()) {
            return EmployerWorkerAttendanceStatus.WORKING;
        }
        if (latestWorkProof.isNeedsReview()) {
            return EmployerWorkerAttendanceStatus.NEEDS_REVIEW;
        }
        return EmployerWorkerAttendanceStatus.COMPLETED;
    }

    private WorkProofRecordStatus resolveRecordStatus(WorkProof latestWorkProof) {
        if (latestWorkProof == null) {
            return null;
        }
        return latestWorkProof.isCheckedIn()
                ? WorkProofRecordStatus.CHECKED_IN
                : WorkProofRecordStatus.CHECKED_OUT;
    }

    private WorkProofReflectionStatus resolveReflectionStatus(WorkProof latestWorkProof) {
        if (latestWorkProof == null) {
            return null;
        }
        if (latestWorkProof.isReflected()) {
            return WorkProofReflectionStatus.REFLECTED;
        }
        if (latestWorkProof.isNeedsReview()) {
            return WorkProofReflectionStatus.NEEDS_REVIEW;
        }
        return WorkProofReflectionStatus.PENDING;
    }

    private WeekRange resolveWeekRange(LocalDate requestedWeekStart) {
        LocalDate anchor = requestedWeekStart == null ? LocalDate.now(clock) : requestedWeekStart;
        LocalDate start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        return new WeekRange(start, start.plusDays(BOARD_DAYS - 1));
    }

    private Comparator<ScopedWorkerSnapshot> workerSnapshotComparator() {
        return Comparator
                .comparing(ScopedWorkerSnapshot::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ScopedWorkerSnapshot::workerId);
    }

    private Set<EmployerWorkerAttendanceStatus> normalizedStatuses(List<EmployerWorkerAttendanceStatus> statuses) {
        return Set.copyOf(statuses == null ? List.of() : statuses);
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private Pagination paginate(int totalElements, Integer requestedPage, Integer requestedSize) {
        int page = normalizePage(requestedPage);
        int size = normalizeSize(requestedSize);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min((page - 1) * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        boolean hasNext = totalPages > 0 && page < totalPages;
        return new Pagination(page, size, totalPages, fromIndex, toIndex, hasNext);
    }

    private int normalizePage(Integer page) {
        int resolved = page == null ? DEFAULT_PAGE : page;
        if (resolved < 1) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return resolved;
    }

    private int normalizeSize(Integer size) {
        int resolved = size == null ? DEFAULT_SIZE : size;
        if (resolved < 1 || resolved > MAX_SIZE) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return resolved;
    }

    private record ScopedWorkerSnapshot(
            Long workerId,
            String name,
            String email,
            WorkProofRecordStatus recordStatus,
            WorkProofReflectionStatus reflectionStatus,
            EmployerWorkerAttendanceStatus attendanceStatus,
            LocalDate latestWorkDate
    ) {
    }

    private record ScopedBoardRowSnapshot(
            Long workerId,
            String name,
            String email,
            List<DaySnapshot> days
    ) {
    }

    private record DaySnapshot(
            LocalDate date,
            WorkProofRecordStatus recordStatus,
            WorkProofReflectionStatus reflectionStatus,
            EmployerWorkerAttendanceStatus attendanceStatus,
            Long workedMinutes
    ) {
    }

    private record WeekRange(
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<LocalDate> dates() {
            List<LocalDate> dates = new ArrayList<>(BOARD_DAYS);
            for (int i = 0; i < BOARD_DAYS; i++) {
                dates.add(startDate.plusDays(i));
            }
            return List.copyOf(dates);
        }
    }

    private record Pagination(
            int page,
            int size,
            int totalPages,
            int fromIndex,
            int toIndex,
            boolean hasNext
    ) {
    }
}
