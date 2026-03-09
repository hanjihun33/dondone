package com.workproofpay.backend.auth.api;

import com.workproofpay.backend.auth.api.dto.request.LoginRequest;
import com.workproofpay.backend.auth.api.dto.request.SignupRequest;
import com.workproofpay.backend.auth.api.dto.response.LoginResponse;
import com.workproofpay.backend.auth.api.dto.response.MeResponse;
import com.workproofpay.backend.auth.service.AuthService;
import com.workproofpay.backend.shared.config.OpenApiConfig;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "JWT authentication and current user endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @Operation(
            summary = "Sign up",
            description = "Creates a new user account. This endpoint does not require authentication.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Signup succeeded"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content)
    })
    public ResponseEntity<com.workproofpay.backend.shared.api.ApiResponse<MeResponse>> signup(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Signup payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SignupRequest.class))
            )
            @Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(201).body(com.workproofpay.backend.shared.api.ApiResponse.success(authService.signup(request)));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in",
            description = """
                    Returns a JWT access token.

                    Seed account:
                    - email: `test@gmail.com`
                    - password: `qweqwe123`
                    """,
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login succeeded"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    public ResponseEntity<com.workproofpay.backend.shared.api.ApiResponse<LoginResponse>> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Login payload",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    name = "Seed account",
                                    value = """
                                            {
                                              "email": "test@gmail.com",
                                              "password": "qweqwe123"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(com.workproofpay.backend.shared.api.ApiResponse.success(authService.login(request)));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current user",
            description = "Returns the authenticated user profile from the JWT access token.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    })
    public ResponseEntity<com.workproofpay.backend.shared.api.ApiResponse<MeResponse>> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(com.workproofpay.backend.shared.api.ApiResponse.success(authService.getMe(user.userId())));
    }
}
