package com.workproofpay.backend.employerauth.api;

import com.workproofpay.backend.employerauth.api.dto.request.EmployerInvitationAcceptRequest;
import com.workproofpay.backend.employerauth.api.dto.request.EmployerLoginRequest;
import com.workproofpay.backend.employerauth.api.dto.response.EmployerAuthResponse;
import com.workproofpay.backend.employerauth.service.EmployerAuthService;
import com.workproofpay.backend.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employer-auth")
@Tag(name = "Employer Auth", description = "Employer invitation acceptance and login endpoints")
@RequiredArgsConstructor
public class EmployerAuthController {

    private final EmployerAuthService employerAuthService;

    @PostMapping("/invitations/accept")
    @Operation(
            summary = "Accept employer invitation",
            description = "Creates a new employer account from an invitation token.",
            security = {}
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Employer invitation accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invitation token invalid", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already exists", content = @Content)
    })
    public ResponseEntity<ApiResponse<EmployerAuthResponse>> acceptInvitation(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Employer invitation acceptance payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = EmployerInvitationAcceptRequest.class))
            )
            @Valid @RequestBody EmployerInvitationAcceptRequest request) {
        return ApiResponse.created(employerAuthService.acceptInvitation(request));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in employer",
            description = "Returns an employer JWT access token after scope validation.",
            security = {}
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Employer login succeeded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Employer profile inactive", content = @Content)
    })
    public ResponseEntity<ApiResponse<EmployerAuthResponse>> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Employer login payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = EmployerLoginRequest.class))
            )
            @Valid @RequestBody EmployerLoginRequest request) {
        return ApiResponse.success(employerAuthService.login(request));
    }
}
