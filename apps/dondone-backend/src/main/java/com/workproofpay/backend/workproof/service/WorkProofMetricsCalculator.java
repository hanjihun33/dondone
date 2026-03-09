package com.workproofpay.backend.workproof.service;

import com.workproofpay.backend.workproof.api.dto.response.WorkProofResponse;
import com.workproofpay.backend.workproof.model.WorkProof;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WorkProofMetricsCalculator {

    private static final long STANDARD_WORKDAY_MINUTES = Duration.ofHours(8).toMinutes();
    private static final int MINUTES_PER_HOUR = 60;
    private static final LocalTime NIGHT_SHIFT_START = LocalTime.of(22, 0);
    private static final LocalTime NIGHT_SHIFT_END = LocalTime.of(6, 0);

    public WorkProofResponse toResponse(WorkProof workProof) {
        if (!workProof.isReflected()) {
            return WorkProofResponse.from(workProof, null, null, null);
        }

        long workedMinutes = workProof.workedMinutes();
        long overtimeMinutes = calculateOvertimeMinutes(List.of(workProof));
        long nightMinutes = calculateNightMinutes(workProof);
        return WorkProofResponse.from(workProof, workedMinutes, overtimeMinutes, nightMinutes);
    }

    public WorkProofMonthlyMetrics summarizeMonthlyMetrics(YearMonth targetMonth,
                                                           LocalDate effectiveAsOf,
                                                           List<WorkProof> filtered) {
        Map<Boolean, List<WorkProof>> workProofsByReflection = filtered.stream()
                .collect(Collectors.partitioningBy(WorkProof::isReflected));

        List<WorkProof> reflected = workProofsByReflection.getOrDefault(true, List.of());
        List<WorkProof> pending = workProofsByReflection.getOrDefault(false, List.of());

        return new WorkProofMonthlyMetrics(
                targetMonth.toString(),
                effectiveAsOf,
                countDistinctWorkDays(reflected),
                sumWorkedMinutes(reflected),
                calculateOvertimeMinutes(reflected),
                sumNightMinutes(reflected),
                countEditedRecords(filtered),
                reflected.size(),
                pending.size(),
                reflected.stream().map(WorkProof::getId).toList()
        );
    }

    public BigDecimal minutesToHours(long minutes) {
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), 1, RoundingMode.HALF_UP);
    }

    private int countDistinctWorkDays(List<WorkProof> workProofs) {
        return (int) workProofs.stream()
                .map(WorkProof::getWorkDate)
                .distinct()
                .count();
    }

    private long sumWorkedMinutes(List<WorkProof> workProofs) {
        return workProofs.stream()
                .mapToLong(WorkProof::workedMinutes)
                .sum();
    }

    private long sumNightMinutes(List<WorkProof> workProofs) {
        return workProofs.stream()
                .mapToLong(this::calculateNightMinutes)
                .sum();
    }

    private int countEditedRecords(List<WorkProof> workProofs) {
        return (int) workProofs.stream()
                .filter(WorkProof::isEdited)
                .count();
    }

    private long calculateOvertimeMinutes(List<WorkProof> reflected) {
        Map<LocalDate, Long> minutesByDay = reflected.stream()
                .collect(Collectors.groupingBy(WorkProof::getWorkDate, Collectors.summingLong(WorkProof::workedMinutes)));

        return minutesByDay.values().stream()
                .mapToLong(total -> Math.max(0L, total - STANDARD_WORKDAY_MINUTES))
                .sum();
    }

    private long calculateNightMinutes(WorkProof workProof) {
        if (!workProof.isReflected()) {
            return 0L;
        }

        LocalDateTime start = workProof.getClockInAt();
        LocalDateTime end = workProof.getClockOutAt();
        long nightMinutes = 0L;

        for (LocalDate date = start.toLocalDate().minusDays(1); !date.isAfter(end.toLocalDate()); date = date.plusDays(1)) {
            LocalDateTime nightStart = LocalDateTime.of(date, NIGHT_SHIFT_START);
            LocalDateTime nightEnd = LocalDateTime.of(date.plusDays(1), NIGHT_SHIFT_END);
            LocalDateTime overlapStart = start.isAfter(nightStart) ? start : nightStart;
            LocalDateTime overlapEnd = end.isBefore(nightEnd) ? end : nightEnd;
            if (overlapEnd.isAfter(overlapStart)) {
                nightMinutes += Duration.between(overlapStart, overlapEnd).toMinutes();
            }
        }

        return nightMinutes;
    }
}
