package com.workproofpay.backend.employer.api;

import com.workproofpay.backend.employer.api.dto.request.EmployerApproveCorrectionRequest;
import com.workproofpay.backend.employer.api.dto.request.EmployerCorrectionRequestsQuery;
import com.workproofpay.backend.employer.api.dto.request.EmployerRejectCorrectionRequest;
import com.workproofpay.backend.employer.api.dto.response.EmployerCorrectionRequestDetailResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerCorrectionRequestsResponse;
import com.workproofpay.backend.employer.service.EmployerCorrectionRequestService;
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
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/employer/correction-requests")
@Tag(name = "Employer Correction Request", description = "Employer correction request queue endpoints")
@RequiredArgsConstructor
public class EmployerCorrectionRequestController {

    private final EmployerCorrectionRequestService employerCorrectionRequestService;

    @GetMapping
    @Operation(
            summary = "Get employer correction request queue",
            description = "Returns the scoped correction request queue for the authenticated employer default workplace.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Correction request queue returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Employer role required or out of scope", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Employer scope is not ready", content = @Content)
    })
    public ResponseEntity<ApiResponse<EmployerCorrectionRequestsResponse>> getCorrectionRequests(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @ModelAttribute EmployerCorrectionRequestsQuery query
    ) {
        return ApiResponse.success(employerCorrectionRequestService.getCorrectionRequests(user.userId(), query));
    }

    @GetMapping("/{requestId}")
    @Operation(
            summary = "Get employer correction request detail",
            description = "Returns a scoped correction request detail for the authenticated employer default workplace.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    public ResponseEntity<ApiResponse<EmployerCorrectionRequestDetailResponse>> getCorrectionRequest(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long requestId
    ) {
        return ApiResponse.success(employerCorrectionRequestService.getCorrectionRequest(user.userId(), requestId));
    }

    @PostMapping("/{requestId}/approve")
    @Operation(
            summary = "Approve employer correction request",
            description = "Approves a scoped correction request and reflects the requested time update to WorkProof.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    public ResponseEntity<ApiResponse<EmployerCorrectionRequestDetailResponse>> approveCorrectionRequest(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long requestId,
            @Valid @RequestBody EmployerApproveCorrectionRequest request
    ) {
        return ApiResponse.success(employerCorrectionRequestService.approve(user.userId(), requestId, request));
    }

    @PostMapping("/{requestId}/reject")
    @Operation(
            summary = "Reject employer correction request",
            description = "Rejects a scoped correction request without modifying the underlying WorkProof.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    public ResponseEntity<ApiResponse<EmployerCorrectionRequestDetailResponse>> rejectCorrectionRequest(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long requestId,
            @Valid @RequestBody EmployerRejectCorrectionRequest request
    ) {
        return ApiResponse.success(employerCorrectionRequestService.reject(user.userId(), requestId, request));
    }
}
