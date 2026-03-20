package com.workproofpay.backend.admin.api;

import com.workproofpay.backend.admin.api.dto.response.AdminAdvanceRequestListResponse;
import com.workproofpay.backend.admin.service.AdminAdvanceRequestService;
import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/advance/requests")
@RequiredArgsConstructor
public class AdminAdvanceRequestController {

    private final AdminAdvanceRequestService adminAdvanceRequestService;

    @GetMapping
    public org.springframework.http.ResponseEntity<ApiResponse<AdminAdvanceRequestListResponse>> getRequests(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(adminAdvanceRequestService.getRequests(user.userId()));
    }

    @PostMapping("/{requestId}/approve")
    public org.springframework.http.ResponseEntity<ApiResponse<Void>> approve(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long requestId
    ) {
        adminAdvanceRequestService.approve(user.userId(), requestId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{requestId}/reject")
    public org.springframework.http.ResponseEntity<ApiResponse<Void>> reject(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long requestId
    ) {
        adminAdvanceRequestService.reject(user.userId(), requestId);
        return ApiResponse.success(null);
    }
}
