package com.workproofpay.backend.employer.api;

import com.workproofpay.backend.employer.api.dto.response.EmployerWorkerRegistrationCodeResponse;
import com.workproofpay.backend.employer.api.dto.response.EmployerWorkerRegistrationCodesResponse;
import com.workproofpay.backend.employer.service.EmployerWorkerRegistrationCodeService;
import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.config.OpenApiConfig;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employer/worker-registration-codes")
@Tag(name = "Employer Worker Registration Codes", description = "Employer-issued worker registration code endpoints")
@RequiredArgsConstructor
public class EmployerWorkerRegistrationCodeController {

    private final EmployerWorkerRegistrationCodeService employerWorkerRegistrationCodeService;

    @PostMapping
    @Operation(
            summary = "Issue worker registration code",
            description = "Issues a worker registration code for the employer scoped default workplace.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Code issued"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Employer access required", content = @Content)
    })
    public ResponseEntity<ApiResponse<EmployerWorkerRegistrationCodeResponse>> issue(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.created(employerWorkerRegistrationCodeService.issue(user.userId()));
    }

    @GetMapping
    @Operation(
            summary = "List worker registration codes",
            description = "Returns worker registration codes issued for the employer scoped default workplace.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    public ResponseEntity<ApiResponse<EmployerWorkerRegistrationCodesResponse>> getCodes(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(employerWorkerRegistrationCodeService.getCodes(user.userId()));
    }

    @PostMapping("/{codeId}/revoke")
    @Operation(
            summary = "Revoke worker registration code",
            description = "Revokes the selected worker registration code within the employer scoped default workplace.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    public ResponseEntity<ApiResponse<EmployerWorkerRegistrationCodeResponse>> revoke(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long codeId
    ) {
        return ApiResponse.success(employerWorkerRegistrationCodeService.revoke(user.userId(), codeId));
    }
}
