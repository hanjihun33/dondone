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
import com.workproofpay.backend.shared.config.OpenApiConfig;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Remittance Admin", description = "ADMIN 전용 remittance 운영 조회 및 복구 API")
@RequiredArgsConstructor
public class RemittanceAdminController {

    private final RemittanceOpsService remittanceOpsService;

    @GetMapping("/summary")
    @Operation(
            summary = "운영 요약 조회",
            description = "송금 상태, wallet funding 상태, job 상태 집계와 최근 실패 원인을 반환합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "운영 요약을 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<RemittanceOpsSummaryResponse>> getSummary(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        requireAdmin(user);
        return ApiResponse.success(remittanceOpsService.getSummary());
    }

    @GetMapping("/transfers")
    @Operation(
            summary = "운영 송금 목록 조회",
            description = "관리자 기준으로 송금 상태 필터, stuckOnly 조건을 적용해 운영 조회용 목록을 반환합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "운영 송금 목록을 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<RemittanceOpsTransferListResponse>> getTransfers(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "조회할 송금 상태 목록", example = "FAILED")
            @RequestParam(required = false) List<TransferStatus> statuses,
            @Parameter(description = "정체 상태의 송금만 조회할지 여부", example = "true")
            @RequestParam(defaultValue = "false") boolean stuckOnly,
            @Parameter(description = "반환할 최대 건수", example = "20")
            @RequestParam(required = false) @Positive(message = "limit must be greater than 0") Integer limit
    ) {
        requireAdmin(user);
        return ApiResponse.success(remittanceOpsService.getTransfers(statuses, stuckOnly, limit));
    }

    @GetMapping("/jobs")
    @Operation(
            summary = "운영 job 목록 조회",
            description = "remittance 비동기 job 상태를 관리자 관점에서 조회합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "운영 job 목록을 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<RemittanceOpsJobListResponse>> getJobs(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "조회할 job 상태 목록", example = "FAILED")
            @RequestParam(required = false) List<JobStatus> statuses,
            @Parameter(description = "반환할 최대 건수", example = "20")
            @RequestParam(required = false) @Positive(message = "limit must be greater than 0") Integer limit
    ) {
        requireAdmin(user);
        return ApiResponse.success(remittanceOpsService.getJobs(statuses, limit));
    }

    @PostMapping("/transfers/{transferId}/retry")
    @Operation(
            summary = "송금 재처리 요청",
            description = """
                    실패했거나 timeout 난 송금, 또는 broadcast 이후 receipt 추적이 필요한 송금에 대해 운영 재처리를 요청합니다.

                    현재 규칙:
                    - `CONFIRMED` 송금은 재처리할 수 없습니다.
                    - `BROADCASTED` 상태는 submit 재생성이 아니라 receipt poll 재큐잉으로 처리합니다.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "재처리 요청을 접수했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "송금 요청을 찾을 수 없습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "현재 상태에서는 재처리할 수 없습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<RemittanceAdminActionResponse>> retryTransfer(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "재처리할 송금 식별자", example = "trf_01HXYZABCDEF1234567890")
            @PathVariable String transferId
    ) {
        requireAdmin(user);
        return ApiResponse.accepted(remittanceOpsService.retryTransfer(transferId));
    }

    @PostMapping("/wallets/{userId}/retry-funding")
    @Operation(
            summary = "지갑 funding 재시도",
            description = "wallet funding 실패 또는 stale pending 상태 사용자의 초기 funding을 다시 시도합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "wallet funding 재시도 결과를 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한이 필요합니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "지갑 또는 사용자를 찾을 수 없습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "현재 상태에서는 funding 재시도를 할 수 없습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<WalletResponse>> retryWalletFunding(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "funding 재시도할 사용자 ID", example = "1")
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
