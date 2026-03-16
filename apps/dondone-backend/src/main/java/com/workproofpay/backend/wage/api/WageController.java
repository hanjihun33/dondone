package com.workproofpay.backend.wage.api;

import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.config.OpenApiConfig;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import com.workproofpay.backend.wage.api.dto.request.CreateWageDepositRequest;
import com.workproofpay.backend.wage.api.dto.response.WageDepositResponse;
import com.workproofpay.backend.wage.api.dto.response.WageEstimateResponse;
import com.workproofpay.backend.wage.api.dto.response.WageMonthlySummaryResponse;
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
import jakarta.validation.constraints.Size;
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

    @GetMapping("/monthly-summary")
    @Operation(
            summary = "Get wage monthly summary",
            description = """
                    Returns the WorkProof-based monthly wage summary for the authenticated worker.

                    Current scope:
                    - reads WorkProof lane 1 monthly summary as upstream input
                    - uses workplace-scoped contract and reflected records only
                    - keeps this response read-only until verification endpoints are added
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wage monthly summary returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Owned workplace or active contract not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<WageMonthlySummaryResponse>> getMonthlySummary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam
            @Size(min = 7, max = 7, message = "month must be exactly 7 characters")
            @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "month must follow YYYY-MM")
            String month,
            @RequestParam @Min(value = 1, message = "workplaceId must be greater than 0") long workplaceId
    ) {
        return ApiResponse.success(wageService.getMonthlySummary(user.userId(), month, workplaceId));
    }

    @GetMapping("/estimate")
    @Operation(
            summary = "Get reference-only wage estimate",
            description = """
                    Returns the reference-only wage estimate for the authenticated worker.

                    Current scope:
                    - uses WorkProof lane 1 monthly summary and active contract as inputs
                    - excludes payslip parsing and verification workflow
                    - keeps reference-only disclaimer in the response
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wage estimate returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Owned workplace or active contract not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<WageEstimateResponse>> getEstimate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam
            @Size(min = 7, max = 7, message = "month must be exactly 7 characters")
            @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "month must follow YYYY-MM")
            String month,
            @RequestParam @Min(value = 1, message = "workplaceId must be greater than 0") long workplaceId
    ) {
        return ApiResponse.success(wageService.getEstimate(user.userId(), month, workplaceId));
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
