package com.workproofpay.backend.employer.api;

import com.workproofpay.backend.employer.api.dto.response.EmployerProfileResponse;
import com.workproofpay.backend.employer.service.EmployerProfileService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employer")
@Tag(name = "Employer Profile", description = "Employer profile bootstrap endpoints")
@RequiredArgsConstructor
public class EmployerProfileController {

    private final EmployerProfileService employerProfileService;

    @GetMapping("/profile")
    @Operation(
            summary = "Get employer profile",
            description = "Returns the authenticated employer profile and default scope.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Employer profile returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Employer profile inactive or wrong role", content = @Content)
    })
    public ResponseEntity<ApiResponse<EmployerProfileResponse>> getProfile(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(employerProfileService.getProfile(user.userId()));
    }
}
