package com.workproofpay.backend.remittance.api;

import com.workproofpay.backend.auth.model.UserRole;
import com.workproofpay.backend.jobs.model.JobStatus;
import com.workproofpay.backend.remittance.api.dto.response.RemittanceAdminActionResponse;
import com.workproofpay.backend.remittance.api.dto.response.RemittanceOpsJobListResponse;
import com.workproofpay.backend.remittance.api.dto.response.RemittanceOpsSummaryResponse;
import com.workproofpay.backend.remittance.api.dto.response.RemittanceOpsTransferListResponse;
import com.workproofpay.backend.remittance.api.dto.response.WalletResponse;
import com.workproofpay.backend.remittance.model.TransferStatus;
import com.workproofpay.backend.remittance.service.RemittanceOpsService;
import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin/remittance")
@RequiredArgsConstructor
public class RemittanceAdminController {

    private final RemittanceOpsService remittanceOpsService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<RemittanceOpsSummaryResponse>> getSummary(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        requireAdmin(user);
        return ApiResponse.success(remittanceOpsService.getSummary());
    }

    @GetMapping("/transfers")
    public ResponseEntity<ApiResponse<RemittanceOpsTransferListResponse>> getTransfers(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) List<TransferStatus> statuses,
            @RequestParam(defaultValue = "false") boolean stuckOnly,
            @RequestParam(required = false) @Positive(message = "limit must be greater than 0") Integer limit
    ) {
        requireAdmin(user);
        return ApiResponse.success(remittanceOpsService.getTransfers(statuses, stuckOnly, limit));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<RemittanceOpsJobListResponse>> getJobs(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) List<JobStatus> statuses,
            @RequestParam(required = false) @Positive(message = "limit must be greater than 0") Integer limit
    ) {
        requireAdmin(user);
        return ApiResponse.success(remittanceOpsService.getJobs(statuses, limit));
    }

    @PostMapping("/transfers/{transferId}/retry")
    public ResponseEntity<ApiResponse<RemittanceAdminActionResponse>> retryTransfer(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String transferId
    ) {
        requireAdmin(user);
        return ApiResponse.accepted(remittanceOpsService.retryTransfer(transferId));
    }

    @PostMapping("/wallets/{userId}/retry-funding")
    public ResponseEntity<ApiResponse<WalletResponse>> retryWalletFunding(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long userId
    ) {
        requireAdmin(user);
        return ApiResponse.success(remittanceOpsService.retryWalletFunding(userId));
    }

    private void requireAdmin(AuthenticatedUser user) {
        if (user == null || !UserRole.ADMIN.name().equals(user.role())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }
}
