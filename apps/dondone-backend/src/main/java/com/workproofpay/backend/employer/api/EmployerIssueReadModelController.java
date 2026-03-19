package com.workproofpay.backend.employer.api;

import com.workproofpay.backend.employer.api.dto.request.EmployerIssuesQuery;
import com.workproofpay.backend.employer.api.dto.response.EmployerIssuesResponse;
import com.workproofpay.backend.employer.service.EmployerIssueReadModelService;
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
@RequestMapping("/api/employer/issues")
@Tag(name = "Employer Issue Read Model", description = "Employer issue queue read-model endpoints")
@RequiredArgsConstructor
public class EmployerIssueReadModelController {

    private final EmployerIssueReadModelService employerIssueReadModelService;

    @GetMapping
    @Operation(
            summary = "Get employer issue queue",
            description = "Returns the scoped actionable employer issue queue for the authenticated employer default workplace.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Issue queue returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Employer role required", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Employer scope is not ready", content = @Content)
    })
    public ResponseEntity<ApiResponse<EmployerIssuesResponse>> getIssues(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @ModelAttribute EmployerIssuesQuery query
    ) {
        return ApiResponse.success(employerIssueReadModelService.getIssues(user.userId(), query));
    }
}
