package com.workproofpay.backend.employer.api;

import com.workproofpay.backend.employer.api.dto.request.UpdateEmployerWorkplaceSettingsRequest;
import com.workproofpay.backend.employer.api.dto.response.EmployerWorkplaceSettingsResponse;
import com.workproofpay.backend.employer.service.EmployerWorkplaceSettingsService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/employer")
@Tag(name = "Employer Workplace Settings", description = "Employer workplace settings endpoints")
@RequiredArgsConstructor
public class EmployerWorkplaceSettingsController {

    private final EmployerWorkplaceSettingsService employerWorkplaceSettingsService;

    @GetMapping("/workplace-settings")
    @Operation(
            summary = "Get employer workplace settings",
            description = "Returns the authenticated employer default workplace settings.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Workplace settings returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Employer role required", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Employer scope is not ready", content = @Content)
    })
    public ResponseEntity<ApiResponse<EmployerWorkplaceSettingsResponse>> getSettings(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(employerWorkplaceSettingsService.getSettings(user.userId()));
    }

    @PutMapping("/workplace-settings")
    @Operation(
            summary = "Update employer workplace settings",
            description = "Updates the authenticated employer default workplace settings. Existing WorkProof records are not retroactively recalculated.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Workplace settings updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Employer role required", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Employer scope is not ready", content = @Content)
    })
    public ResponseEntity<ApiResponse<EmployerWorkplaceSettingsResponse>> updateSettings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateEmployerWorkplaceSettingsRequest request
    ) {
        return ApiResponse.success(employerWorkplaceSettingsService.updateSettings(user.userId(), request));
    }
}
