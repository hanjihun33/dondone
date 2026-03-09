package com.workproofpay.backend.demo.api;

import com.workproofpay.backend.demo.api.dto.response.DemoStateResponse;
import com.workproofpay.backend.demo.service.DemoStateService;
import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoStateService demoStateService;

    @GetMapping("/state")
    public ResponseEntity<ApiResponse<DemoStateResponse>> getState(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @RequestParam(required = false) @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "yearMonth must follow YYYY-MM") String yearMonth,
            @RequestParam long normalizedHourlyWage,
            @RequestParam(defaultValue = "25") @Min(value = 1, message = "paydayDay must be between 1 and 31") @Max(value = 31, message = "paydayDay must be between 1 and 31") int paydayDay
    ) {
        return ApiResponse.success(demoStateService.getState(user.userId(), asOf, yearMonth, normalizedHourlyWage, paydayDay));
    }
}
