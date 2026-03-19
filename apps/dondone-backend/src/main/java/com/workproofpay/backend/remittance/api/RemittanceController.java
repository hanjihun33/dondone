package com.workproofpay.backend.remittance.api;

import com.workproofpay.backend.remittance.api.dto.request.CreateTransferRequest;
import com.workproofpay.backend.remittance.api.dto.request.TransferPrecheckRequest;
import com.workproofpay.backend.remittance.api.dto.request.UpsertRecipientRequest;
import com.workproofpay.backend.remittance.api.dto.response.CreateTransferResponse;
import com.workproofpay.backend.remittance.api.dto.response.RecipientItemResponse;
import com.workproofpay.backend.remittance.api.dto.response.RecipientListResponse;
import com.workproofpay.backend.remittance.api.dto.response.TransferDetailResponse;
import com.workproofpay.backend.remittance.api.dto.response.TransferListResponse;
import com.workproofpay.backend.remittance.api.dto.response.TransferPrecheckResponse;
import com.workproofpay.backend.remittance.api.dto.response.WalletBalanceResponse;
import com.workproofpay.backend.remittance.api.dto.response.WalletResponse;
import com.workproofpay.backend.remittance.service.RecipientService;
import com.workproofpay.backend.remittance.service.TransferCreateResult;
import com.workproofpay.backend.remittance.service.TransferService;
import com.workproofpay.backend.remittance.service.WalletService;
import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.config.OpenApiConfig;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/remittance")
@Tag(name = "Remittance", description = "서버 지갑 생성, 수신자 허용목록, precheck, 테스트넷 송금 조회/요청 API")
@RequiredArgsConstructor
public class RemittanceController {

    private final WalletService walletService;
    private final RecipientService recipientService;
    private final TransferService transferService;

    @PostMapping("/wallets/me")
    @Operation(
            summary = "내 송금 지갑 생성 또는 조회",
            description = """
                    인증된 사용자의 서버 관리형 remittance 지갑을 생성합니다.

                    동작 규칙:
                    - 첫 호출이면 지갑을 생성하고 초기 Sepolia ETH + dUSDC funding을 시도합니다.
                    - 이미 지갑이 있으면 기존 지갑 정보를 그대로 반환합니다.
                    - 신규 생성 시 `201`, 기존 지갑이면 `200`으로 응답합니다.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "기존 지갑을 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "새 지갑을 생성했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        WalletService.WalletCreateResult result = walletService.createWalletIfAbsent(user.userId());
        return result.created()
                ? ApiResponse.created(result.response())
                : ApiResponse.success(result.response());
    }

    @GetMapping("/wallets/me")
    @Operation(
            summary = "내 송금 지갑 조회",
            description = "인증된 사용자의 서버 관리형 remittance 지갑 상태를 반환합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지갑 정보를 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "생성된 지갑이 없습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(walletService.getWallet(user.userId()));
    }

    @GetMapping("/wallets/me/balance")
    @Operation(
            summary = "내 송금 지갑 잔액 조회",
            description = "사용자 remittance 지갑의 dUSDC 잔액과 Sepolia native balance를 조회합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "잔액을 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "생성된 지갑이 없습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> getWalletBalance(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(walletService.getWalletBalance(user.userId()));
    }

    @GetMapping("/recipients")
    @Operation(
            summary = "수신자 허용목록 조회",
            description = "사용자가 등록한 외부 지갑 수신자 목록을 반환합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수신자 목록을 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<RecipientListResponse>> getRecipients(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(recipientService.getRecipients(user.userId()));
    }

    @PostMapping("/recipients")
    @Operation(
            summary = "수신자 등록",
            description = """
                    송금 가능한 외부 지갑 주소를 허용목록에 등록합니다.

                    현재 범위:
                    - DonDone 회원 여부와 무관한 외부 EVM 주소를 등록할 수 있습니다.
                    - `allowed=true` 인 수신자에게만 송금할 수 있습니다.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "수신자를 등록했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 등록된 지갑 주소입니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<RecipientItemResponse>> createRecipient(
            @AuthenticationPrincipal AuthenticatedUser user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수신자 등록 요청 본문",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UpsertRecipientRequest.class),
                            examples = @ExampleObject(
                                    name = "가족 수신자",
                                    value = """
                                            {
                                              "alias": "엄마",
                                              "relation": "FAMILY",
                                              "walletAddress": "0x1111111111111111111111111111111111111111",
                                              "allowed": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpsertRecipientRequest request
    ) {
        return ApiResponse.created(recipientService.createRecipient(user.userId(), request));
    }

    @PutMapping("/recipients/{recipientId}")
    @Operation(
            summary = "수신자 수정",
            description = "기존 허용목록 수신자의 별칭, 관계, 주소, 허용 여부를 수정합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수신자를 수정했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "수신자를 찾을 수 없습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 등록된 지갑 주소입니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<RecipientItemResponse>> updateRecipient(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "수정할 수신자 식별자", example = "rec_01HXYZABCDEF1234567890")
            @PathVariable String recipientId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수신자 수정 요청 본문",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpsertRecipientRequest.class))
            )
            @Valid @RequestBody UpsertRecipientRequest request
    ) {
        return ApiResponse.success(recipientService.updateRecipient(user.userId(), recipientId, request));
    }

    @PostMapping("/transfers/precheck")
    @Operation(
            summary = "송금 사전 점검",
            description = """
                    실제 송금 생성 전에 정책과 잔액 조건을 점검합니다.

                    확인 항목:
                    - 수신자 허용목록 여부
                    - 최근 수정 수신자 추가 확인 필요 여부
                    - 고액 송금 추가 확인 필요 여부
                    - 현재 토큰 / gas 잔액
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사전 점검 결과를 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "수신자 또는 지갑을 찾을 수 없습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<TransferPrecheckResponse>> precheck(
            @AuthenticationPrincipal AuthenticatedUser user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "송금 사전 점검 요청 본문",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = TransferPrecheckRequest.class),
                            examples = @ExampleObject(
                                    name = "소액 송금 점검",
                                    value = """
                                            {
                                              "recipientId": "rec_01HXYZABCDEF1234567890",
                                              "amountAtomic": 50000000,
                                              "highAmountConfirmed": false,
                                              "recentRecipientConfirmed": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody TransferPrecheckRequest request
    ) {
        return ApiResponse.success(transferService.precheck(user.userId(), request));
    }

    @PostMapping("/transfers")
    @Operation(
            summary = "송금 요청 생성",
            description = """
                    비동기 remittance 송금 요청을 생성합니다.

                    주의:
                    - 이 API는 즉시 체인 확정이 아니라 송금 job 생성을 의미합니다.
                    - 중복 생성 방지를 위해 `Idempotency-Key` 헤더를 함께 보내는 것을 권장합니다.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "같은 idempotency key 재요청 결과를 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "새 송금 요청을 생성했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증 또는 정책 검사에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "수신자 또는 지갑을 찾을 수 없습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "동일 사용자에 진행 중인 송금이 있습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<CreateTransferResponse>> createTransfer(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(
                    in = ParameterIn.HEADER,
                    name = "Idempotency-Key",
                    description = "동일 송금 요청 중복 생성을 방지하는 헤더",
                    example = "remittance-e2e-001"
            )
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "송금 생성 요청 본문",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateTransferRequest.class),
                            examples = @ExampleObject(
                                    name = "소액 송금 생성",
                                    value = """
                                            {
                                              "recipientId": "rec_01HXYZABCDEF1234567890",
                                              "amountAtomic": 50000000,
                                              "highAmountConfirmed": false,
                                              "recentRecipientConfirmed": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreateTransferRequest request
    ) {
        TransferCreateResult result = transferService.createTransfer(user.userId(), idempotencyKey, request);
        return result.replayed()
                ? ApiResponse.success(result.response())
                : ApiResponse.created(result.response());
    }

    @GetMapping("/transfers")
    @Operation(
            summary = "송금 목록 조회",
            description = "인증된 사용자의 송금 요청 목록을 최신순으로 반환합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "송금 목록을 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<TransferListResponse>> getTransfers(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "반환할 최대 건수", example = "20")
            @RequestParam(required = false) @Positive(message = "limit must be greater than 0") Integer limit
    ) {
        return ApiResponse.success(transferService.getTransfers(user.userId(), limit));
    }

    @GetMapping("/transfers/{transferId}")
    @Operation(
            summary = "송금 상세 조회",
            description = "인증된 사용자의 특정 송금 요청 상세와 현재 상태를 반환합니다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "송금 상세를 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "송금 요청을 찾을 수 없습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<TransferDetailResponse>> getTransfer(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "조회할 송금 식별자", example = "trf_01HXYZABCDEF1234567890")
            @PathVariable String transferId
    ) {
        return ApiResponse.success(transferService.getTransfer(user.userId(), transferId));
    }
}
