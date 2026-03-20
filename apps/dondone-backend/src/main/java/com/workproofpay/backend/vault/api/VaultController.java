package com.workproofpay.backend.vault.api;

import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.config.OpenApiConfig;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import com.workproofpay.backend.vault.api.dto.request.CreateVaultTransactionRequest;
import com.workproofpay.backend.vault.api.dto.response.CreateVaultTransactionResponse;
import com.workproofpay.backend.vault.api.dto.response.VaultSummaryResponse;
import com.workproofpay.backend.vault.api.dto.response.VaultTransactionDetailResponse;
import com.workproofpay.backend.vault.api.dto.response.VaultTransactionListResponse;
import com.workproofpay.backend.vault.service.VaultCreateResult;
import com.workproofpay.backend.vault.service.VaultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/vault")
@Tag(name = "Vault", description = "StableVault testnet 예치/출금 및 요약 API")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    @GetMapping("/summary")
    @Operation(
            summary = "Vault 요약 조회",
            description = "내 remittance 지갑과 Vault 포지션을 기준으로 보관 금액, 사용 가능 금액, 예상 이자 미리보기를 반환합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    public ResponseEntity<ApiResponse<VaultSummaryResponse>> getSummary(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(vaultService.getSummary(user.userId()));
    }

    @PostMapping("/deposits")
    @Operation(
            summary = "Vault 예치 요청 생성",
            description = "사용자 서버 지갑에서 Vault로 testnet deposit 트랜잭션을 비동기 생성합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "같은 idempotency key 재요청 결과를 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "새 Vault 예치 요청을 접수했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "지갑이 없습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "잔액 또는 상태가 예치 조건을 만족하지 않습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<CreateVaultTransactionResponse>> createDeposit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateVaultTransactionRequest request
    ) {
        VaultCreateResult result = vaultService.createDeposit(user.userId(), idempotencyKey, request);
        return result.replayed() ? ApiResponse.success(result.response()) : ApiResponse.accepted(result.response());
    }

    @PostMapping("/withdrawals")
    @Operation(
            summary = "Vault 출금 요청 생성",
            description = "Vault에서 사용자 서버 지갑으로 testnet withdraw 트랜잭션을 비동기 생성합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    public ResponseEntity<ApiResponse<CreateVaultTransactionResponse>> createWithdrawal(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateVaultTransactionRequest request
    ) {
        VaultCreateResult result = vaultService.createWithdrawal(user.userId(), idempotencyKey, request);
        return result.replayed() ? ApiResponse.success(result.response()) : ApiResponse.accepted(result.response());
    }

    @GetMapping("/transactions")
    @Operation(
            summary = "Vault 거래 목록 조회",
            description = "내 Vault deposit/withdraw 비동기 처리 결과 목록을 최신순으로 조회합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    public ResponseEntity<ApiResponse<VaultTransactionListResponse>> getTransactions(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "반환할 최대 건수", example = "20")
            @RequestParam(required = false) @Positive(message = "limit must be greater than 0") Integer limit
    ) {
        return ApiResponse.success(vaultService.getTransactions(user.userId(), limit));
    }

    @GetMapping("/transactions/{vaultTransactionId}")
    @Operation(
            summary = "Vault 거래 상세 조회",
            description = "요청 생성 후 txHash, 상태, 확정 시각을 확인합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    public ResponseEntity<ApiResponse<VaultTransactionDetailResponse>> getTransaction(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String vaultTransactionId
    ) {
        return ApiResponse.success(vaultService.getTransaction(user.userId(), vaultTransactionId));
    }
}
