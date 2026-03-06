package com.workproofpay.backend.auth.api;

import com.workproofpay.backend.auth.api.dto.request.LoginRequest;
import com.workproofpay.backend.auth.api.dto.request.SignupRequest;
import com.workproofpay.backend.auth.api.dto.response.LoginResponse;
import com.workproofpay.backend.auth.api.dto.response.MeResponse;
import com.workproofpay.backend.auth.service.AuthService;
import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<MeResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(authService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.success(authService.getMe(user.userId())));
    }
}
