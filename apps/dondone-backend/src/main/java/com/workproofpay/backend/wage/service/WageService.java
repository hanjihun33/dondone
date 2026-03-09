package com.workproofpay.backend.wage.service;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.wage.api.dto.request.CreateWageDepositRequest;
import com.workproofpay.backend.wage.api.dto.response.WageDepositResponse;
import com.workproofpay.backend.wage.api.dto.response.WageSummaryResponse;
import com.workproofpay.backend.wage.model.WageDeposit;
import com.workproofpay.backend.wage.repo.WageDepositRepository;
import com.workproofpay.backend.workproof.service.WorkProofMonthlyMetrics;
import com.workproofpay.backend.workproof.service.WorkProofService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class WageService {

    private static final String REFERENCE_ONLY_DISCLAIMER =
            "Reference-only estimate. Actual payment and deductions can differ by contract, payroll rules, and payslip details.";

    private final WageDepositRepository wageDepositRepository;
    private final UserRepository userRepository;
    private final WorkProofService workProofService;
    private final WageSummaryCalculator wageSummaryCalculator;

    @Transactional
    public WageDepositResponse createDeposit(Long userId, CreateWageDepositRequest request) {
        YearMonth yearMonth = parseYearMonth(request.yearMonth());
        if (!YearMonth.from(request.depositDate()).equals(yearMonth)) {
            throw new ApiException(ErrorCode.INVALID_DEPOSIT_DATE);
        }

        WageDeposit saved = wageDepositRepository.save(WageDeposit.record(
                findUser(userId),
                yearMonth.toString(),
                request.depositDate(),
                request.actualDepositAmount(),
                request.deductionsKnown(),
                request.note()
        ));

        return WageDepositResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public WageSummaryResponse getSummary(Long userId,
                                          String yearMonth,
                                          LocalDate asOf,
                                          long normalizedHourlyWage,
                                          int paydayDay) {
        if (normalizedHourlyWage <= 0) {
            throw new ApiException(ErrorCode.INVALID_HOURLY_WAGE);
        }
        if (paydayDay < 1 || paydayDay > 31) {
            throw new ApiException(ErrorCode.INVALID_PAYDAY);
        }

        WorkProofMonthlyMetrics metrics = workProofService.getMonthlyMetrics(userId, yearMonth, asOf);
        WageDeposit latestDeposit = findLatestDeposit(userId, metrics.yearMonth(), metrics.asOf());
        WageSummaryCalculator.WageSummarySnapshot snapshot =
                wageSummaryCalculator.summarize(metrics, normalizedHourlyWage, latestDeposit);
        return WageSummaryResponse.from(metrics, snapshot, paydayDay, REFERENCE_ONLY_DISCLAIMER);
    }

    private WageDeposit findLatestDeposit(Long userId, String yearMonth, LocalDate asOf) {
        if (asOf == null) {
            return wageDepositRepository.findFirstByUserIdAndYearMonthOrderByDepositDateDescCreatedAtDesc(userId, yearMonth)
                    .orElse(null);
        }
        return wageDepositRepository.findFirstByUserIdAndYearMonthAndDepositDateLessThanEqualOrderByDepositDateDescCreatedAtDesc(
                        userId,
                        yearMonth,
                        asOf
                )
                .orElse(null);
    }

    private YearMonth parseYearMonth(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INVALID_YEAR_MONTH);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
