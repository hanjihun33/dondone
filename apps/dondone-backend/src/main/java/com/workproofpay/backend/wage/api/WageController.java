package com.workproofpay.backend.wage.api;

import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import com.workproofpay.backend.wage.api.dto.request.CreateWageDepositRequest;
import com.workproofpay.backend.wage.api.dto.response.WageDepositResponse;
import com.workproofpay.backend.wage.api.dto.response.WageSummaryResponse;
import com.workproofpay.backend.wage.service.WageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/wage")
@RequiredArgsConstructor
public class WageController {

    private final WageService wageService;

    @PostMapping("/deposits")
    public ResponseEntity<ApiResponse<WageDepositResponse>> createDeposit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateWageDepositRequest request
    ) {
        return ApiResponse.created(wageService.createDeposit(user.userId(), request));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<WageSummaryResponse>> getSummary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "yearMonth must follow YYYY-MM") String yearMonth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @RequestParam long normalizedHourlyWage,
            @RequestParam(defaultValue = "25") @Min(value = 1, message = "paydayDay must be between 1 and 31") @Max(value = 31, message = "paydayDay must be between 1 and 31") int paydayDay
    ) {
        return ApiResponse.success(wageService.getSummary(user.userId(), yearMonth, asOf, normalizedHourlyWage, paydayDay));
    }
}
