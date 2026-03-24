package com.workproofpay.backend.employer.api;

import com.workproofpay.backend.employer.api.dto.response.EmployerReviewRecordConfirmResponse;
import com.workproofpay.backend.employer.service.EmployerIssueCommandService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/employer/issues")
@Tag(name = "Employer Issue Command", description = "Employer issue queue command endpoints")
@RequiredArgsConstructor
public class EmployerIssueCommandController {

    private final EmployerIssueCommandService employerIssueCommandService;

    @PostMapping("/review-records/{workProofId}/confirm")
    @Operation(
            summary = "Confirm review-required work proof",
            description = "Marks a scoped review-required work proof as confirmed and removes it from the employer issue queue.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review-required work proof confirmed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Employer role required or out of scope", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review-required work proof not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Employer scope is not ready", content = @Content)
    })
    public ResponseEntity<ApiResponse<EmployerReviewRecordConfirmResponse>> confirmReviewRecord(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long workProofId
    ) {
        return ApiResponse.success(employerIssueCommandService.confirmReviewRecord(user.userId(), workProofId));
    }
}
