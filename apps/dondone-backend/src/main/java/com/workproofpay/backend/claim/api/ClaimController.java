package com.workproofpay.backend.claim.api;

import com.workproofpay.backend.claim.api.dto.response.ClaimPreparationDetailResponse;
import com.workproofpay.backend.claim.api.dto.request.CreateClaimPreparationRequest;
import com.workproofpay.backend.claim.api.dto.response.ClaimPreparationResponse;
import com.workproofpay.backend.claim.service.ClaimService;
import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/claim")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    /**
     * Instant Claim v0 준비 데이터는 verification snapshot을 바탕으로 동기 생성한다.
     */
    @PostMapping("/preparations")
    public ResponseEntity<ApiResponse<ClaimPreparationResponse>> createPreparation(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateClaimPreparationRequest request
    ) {
        return ApiResponse.created(claimService.createPreparation(user.userId(), request));
    }

    @GetMapping("/preparations/{preparationId}")
    public ResponseEntity<ApiResponse<ClaimPreparationDetailResponse>> getPreparation(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive(message = "preparationId must be greater than 0") Long preparationId
    ) {
        return ApiResponse.success(claimService.getPreparation(user.userId(), preparationId));
    }
}
