package com.workproofpay.backend.auth.api;

import com.workproofpay.backend.auth.api.dto.request.RedeemWorkerRegistrationCodeRequest;
import com.workproofpay.backend.auth.api.dto.response.WorkerCompanyRegistrationResponse;
import com.workproofpay.backend.auth.service.WorkerCompanyRegistrationService;
import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.config.OpenApiConfig;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/me/worker-registration-code")
@Tag(name = "Worker Company Registration", description = "Worker company registration endpoints")
@RequiredArgsConstructor
public class WorkerCompanyRegistrationController {

    private final WorkerCompanyRegistrationService workerCompanyRegistrationService;

    @PostMapping
    @Operation(
            summary = "Redeem worker registration code",
            description = "Creates or confirms a worker membership for the company/workplace bound to the employer-issued registration code.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registration succeeded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Worker access required", content = @Content)
    })
    public ResponseEntity<ApiResponse<WorkerCompanyRegistrationResponse>> redeem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Worker registration code redeem payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RedeemWorkerRegistrationCodeRequest.class))
            )
            @Valid @RequestBody RedeemWorkerRegistrationCodeRequest request
    ) {
        return ApiResponse.success(workerCompanyRegistrationService.redeem(user.userId(), request));
    }
}
