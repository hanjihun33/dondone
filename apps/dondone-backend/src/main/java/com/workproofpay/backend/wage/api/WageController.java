package com.workproofpay.backend.wage.api;

import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.config.OpenApiConfig;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import com.workproofpay.backend.wage.api.dto.request.CreateWageDepositRequest;
import com.workproofpay.backend.wage.api.dto.response.WageDepositResponse;
import com.workproofpay.backend.wage.api.dto.response.WageSummaryResponse;
import com.workproofpay.backend.wage.service.WageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Wage", description = "Worker wage confirmation endpoints based on reference-only estimate and self-reported actual deposit")
@RequiredArgsConstructor
public class WageController {

    private final WageService wageService;

    @PostMapping("/deposits")
    @Operation(
            summary = "Record actual deposit",
            description = """
                    Records the worker's self-reported actual received deposit for a month.

                    Current scope:
                    - worker self-report only
                    - employer confirmation or dispute workflow is not part of this API contract yet
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Actual deposit recorded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    })
    public ResponseEntity<ApiResponse<WageDepositResponse>> createDeposit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Worker self-reported actual deposit payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateWageDepositRequest.class))
            )
            @Valid @RequestBody CreateWageDepositRequest request
    ) {
        return ApiResponse.created(wageService.createDeposit(user.userId(), request));
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Get monthly wage confirmation summary",
            description = """
                    Returns a monthly wage summary for the authenticated worker.

                    The response combines:
                    - WorkProof-based reference-only estimate
                    - latest self-reported actual deposit
                    - difference and anomaly preview for follow-up confirmation
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly wage summary returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    })
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
