package com.workproofpay.backend.employer.api;

import com.workproofpay.backend.employer.api.dto.request.EmployerAttendanceBoardQuery;
import com.workproofpay.backend.employer.api.dto.response.EmployerAttendanceBoardResponse;
import com.workproofpay.backend.employer.api.dto.request.EmployerWorkersQuery;
import com.workproofpay.backend.employer.api.dto.response.EmployerDashboardSummaryResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerWorkersResponse;
import com.workproofpay.backend.employer.service.EmployerWorkerReadModelService;
import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.config.OpenApiConfig;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/employer")
@Tag(name = "Employer Worker Read Model", description = "Employer worker directory and dashboard read-model endpoints")
@RequiredArgsConstructor
public class EmployerWorkerReadModelController {

    private final EmployerWorkerReadModelService employerWorkerReadModelService;

    @GetMapping("/workers")
    @Operation(
            summary = "Get employer worker directory",
            description = "Returns the scoped worker directory for the authenticated employer default workplace.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Worker directory returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Employer role required", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Employer scope is not ready", content = @Content)
    })
    public ResponseEntity<ApiResponse<EmployerWorkersResponse>> getWorkers(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @ModelAttribute EmployerWorkersQuery query
    ) {
        return ApiResponse.success(employerWorkerReadModelService.getWorkers(user.userId(), query));
    }

    @GetMapping("/dashboard/summary")
    @Operation(
            summary = "Get employer dashboard summary",
            description = "Returns the current scoped dashboard summary for the authenticated employer default workplace.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard summary returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Employer role required", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Employer scope is not ready", content = @Content)
    })
    public ResponseEntity<ApiResponse<EmployerDashboardSummaryResponse>> getDashboardSummary(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(employerWorkerReadModelService.getDashboardSummary(user.userId()));
    }

    @GetMapping("/dashboard/attendance-board")
    @Operation(
            summary = "Get employer dashboard attendance board",
            description = "Returns the scoped weekly attendance board for the authenticated employer default workplace.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attendance board returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Employer role required", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Employer scope is not ready", content = @Content)
    })
    public ResponseEntity<ApiResponse<EmployerAttendanceBoardResponse>> getAttendanceBoard(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @ModelAttribute EmployerAttendanceBoardQuery query
    ) {
        return ApiResponse.success(employerWorkerReadModelService.getAttendanceBoard(user.userId(), query));
    }
}
