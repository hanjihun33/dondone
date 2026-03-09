package com.workproofpay.backend.demo.service;

import com.workproofpay.backend.demo.api.dto.response.DemoStateResponse;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.wage.api.dto.response.WageSummaryResponse;
import com.workproofpay.backend.wage.service.WageService;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofResponse;
import com.workproofpay.backend.workproof.service.WorkProofMonthlyMetrics;
import com.workproofpay.backend.workproof.service.WorkProofService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DemoStateService {

    private final WorkProofService workProofService;
    private final WageService wageService;

    @Transactional(readOnly = true)
    public DemoStateResponse getState(Long userId,
                                      LocalDate asOf,
                                      String yearMonth,
                                      long normalizedHourlyWage,
                                      int paydayDay) {
        if (asOf == null) {
            throw new ApiException(ErrorCode.AS_OF_REQUIRED);
        }

        String effectiveYearMonth = yearMonth;
        if (effectiveYearMonth == null || effectiveYearMonth.isBlank()) {
            effectiveYearMonth = YearMonth.from(asOf).toString();
        }

        WorkProofMonthlyMetrics metrics = workProofService.getMonthlyMetrics(userId, effectiveYearMonth, asOf);
        List<WorkProofResponse> workProofs = workProofService.getWorkProofs(userId, effectiveYearMonth, asOf);
        WageSummaryResponse wageSummary = wageService.getSummary(userId, effectiveYearMonth, asOf, normalizedHourlyWage, paydayDay);
        WorkProofMonthlySummaryResponse workProofSummary = new WorkProofMonthlySummaryResponse(
                metrics.yearMonth(),
                metrics.asOf(),
                metrics.totalWorkDays(),
                metrics.totalWorkedMinutes(),
                metrics.totalOvertimeMinutes(),
                metrics.totalNightMinutes(),
                metrics.editedRecordCount(),
                metrics.reflectedRecordCount(),
                metrics.pendingRecordCount(),
                metrics.reflectedWorkProofIds()
        );

        return new DemoStateResponse(asOf, effectiveYearMonth, workProofSummary, workProofs, wageSummary);
    }
}
