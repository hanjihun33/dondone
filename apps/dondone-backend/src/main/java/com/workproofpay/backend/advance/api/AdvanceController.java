package com.workproofpay.backend.advance.api;

import com.workproofpay.backend.advance.api.dto.request.CreateAdvanceRequest;
import com.workproofpay.backend.advance.api.dto.response.AdvanceEligibilityResponse;
import com.workproofpay.backend.advance.api.dto.response.AdvanceRequestDetailResponse;
import com.workproofpay.backend.advance.api.dto.response.AdvanceRequestListResponse;
import com.workproofpay.backend.advance.api.dto.response.AdvanceRequestResponse;
import com.workproofpay.backend.advance.service.AdvanceCreateResult;
import com.workproofpay.backend.advance.service.AdvanceService;
import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/advance")
@RequiredArgsConstructor
public class AdvanceController {

    private final AdvanceService advanceService;

    @GetMapping("/eligibility")
    public ResponseEntity<ApiResponse<AdvanceEligibilityResponse>> getEligibility(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @Positive(message = "workplaceId must be greater than 0") Long workplaceId
    ) {
        return ApiResponse.success(advanceService.getEligibility(user.userId(), workplaceId));
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<AdvanceRequestResponse>> createRequest(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateAdvanceRequest request
    ) {
        AdvanceCreateResult result = advanceService.createRequest(user.userId(), idempotencyKey, request);
        return result.replayed()
                ? ApiResponse.success(result.response())
                : ApiResponse.created(result.response());
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<AdvanceRequestListResponse>> getRequests(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "month must follow YYYY-MM") String month
    ) {
        return ApiResponse.success(advanceService.getRequests(user.userId(), month));
    }

    @GetMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponse<AdvanceRequestDetailResponse>> getRequest(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive(message = "requestId must be greater than 0") Long requestId
    ) {
        return ApiResponse.success(advanceService.getRequest(user.userId(), requestId));
    }
}
