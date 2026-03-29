package com.workproofpay.backend.wage.api;

import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.config.OpenApiConfig;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import com.workproofpay.backend.wage.api.dto.request.CreateWageDepositRequest;
import com.workproofpay.backend.wage.api.dto.request.CreateWageVerificationRequest;
import com.workproofpay.backend.wage.api.dto.response.WageDepositResponse;
import com.workproofpay.backend.wage.api.dto.response.WageEstimateResponse;
import com.workproofpay.backend.wage.api.dto.response.WageMonthlySummaryResponse;
import com.workproofpay.backend.wage.api.dto.response.WageSummaryResponse;
import com.workproofpay.backend.wage.api.dto.response.WageVerificationCreatedResponse;
import com.workproofpay.backend.wage.api.dto.response.WageVerificationDetailResponse;
import com.workproofpay.backend.wage.service.WageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/wage")
@Tag(name = "Wage", description = "참고용 예상 급여와 근로자 실수령 입력을 기반으로 급여를 확인하는 API")
@RequiredArgsConstructor
/**
 * Wage lane 1에서는 WorkProof monthly summary를 읽는 조회 endpoint를 먼저 열고,
 * 기존 입금 기록/차액 요약 흐름은 호환성을 위해 유지한다.
 */
public class WageController {

    private final WageService wageService;

    @PostMapping("/deposits")
    @Operation(
            summary = "실수령 급여 등록",
            description = """
                    근로자가 특정 월의 실제 입금 금액을 직접 기록합니다.

                    현재 범위:
                    - 근로자 본인 입력만 지원합니다.
                    - 사업주 확인이나 분쟁 처리 흐름은 아직 이 API 계약에 포함되지 않습니다.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "실수령 급여가 등록되었습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<WageDepositResponse>> createDeposit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "근로자가 직접 입력하는 실수령 급여 요청 본문",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateWageDepositRequest.class))
            )
            @Valid @RequestBody CreateWageDepositRequest request
    ) {
        return ApiResponse.created(wageService.createDeposit(user.userId(), request));
    }

    @PostMapping("/verifications")
    @Operation(
            summary = "실제 지급 결과 확인과 verification 생성",
            description = """
                    인증된 근로자가 실제 받은 돈을 확인해 Wage verification snapshot을 생성합니다.

                    현재 범위:
                    - worker self-check만 지원합니다.
                    - employer confirmation과 documents/claim 연결은 후속 단계에서 확장합니다.
                    - 응답은 최종 급여 확정이 아니라 확인 필요 상태와 근거 요약입니다.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "verification이 생성되었습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "소유한 근무지 또는 활성 계약을 찾을 수 없습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<WageVerificationCreatedResponse>> createVerification(
            @AuthenticationPrincipal AuthenticatedUser user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "worker self-check verification request body",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateWageVerificationRequest.class))
            )
            @Valid @RequestBody CreateWageVerificationRequest request
    ) {
        return ApiResponse.created(wageService.createVerification(user.userId(), request));
    }

    @GetMapping("/verifications/{verificationId}")
    @Operation(
            summary = "verification 상세 조회",
            description = """
                    인증된 근로자의 Wage verification snapshot 상세를 반환합니다.

                    현재 범위:
                    - Documents/Claim downstream이 재사용할 수 있는 상세 snapshot을 반환합니다.
                    - employer support는 readiness 수준의 상태만 포함합니다.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "verification 상세를 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "verification을 찾을 수 없습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<WageVerificationDetailResponse>> getVerification(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Min(value = 1, message = "verificationId must be greater than 0") long verificationId
    ) {
        return ApiResponse.success(wageService.getVerification(user.userId(), verificationId));
    }

    @GetMapping("/monthly-summary")
    @Operation(
            summary = "월간 급여 요약 조회",
            description = """
                    인증된 근로자의 WorkProof 기반 월간 급여 요약을 반환합니다.

                    현재 범위:
                    - WorkProof lane 1 월간 집계를 상위 입력으로 사용합니다.
                    - 근무지 기준 활성 계약과 reflected 기록만 반영합니다.
                    - verification API가 추가되기 전까지는 읽기 전용 응답으로 유지합니다.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "월간 급여 요약을 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "소유한 근무지 또는 활성 계약을 찾을 수 없습니다.", content = @Content)
    })
    /**
     * Wage 화면이 먼저 보여줄 월간 근무/계약 요약 read-model을 반환한다.
     */
    public ResponseEntity<ApiResponse<WageMonthlySummaryResponse>> getMonthlySummary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam
            @Size(min = 7, max = 7, message = "month must be exactly 7 characters")
            @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "month must follow YYYY-MM")
            String month,
            @RequestParam @Min(value = 1, message = "workplaceId must be greater than 0") long workplaceId
    ) {
        return ApiResponse.success(wageService.getMonthlySummary(user.userId(), month, workplaceId));
    }

    @GetMapping("/estimate")
    @Operation(
            summary = "참고용 예상 급여 조회",
            description = """
                    인증된 근로자의 참고용 예상 급여를 반환합니다.

                    현재 범위:
                    - WorkProof lane 1 월간 집계와 활성 계약을 입력으로 사용합니다.
                    - 급여명세서 파싱과 verification 흐름은 포함하지 않습니다.
                    - 응답에 참고용 안내 문구를 유지합니다.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "예상 급여를 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "소유한 근무지 또는 활성 계약을 찾을 수 없습니다.", content = @Content)
    })
    /**
     * verification 전 단계의 참고용 예상 급여를 같은 month/workplace 축으로 계산해 반환한다.
     */
    public ResponseEntity<ApiResponse<WageEstimateResponse>> getEstimate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam
            @Size(min = 7, max = 7, message = "month must be exactly 7 characters")
            @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "month must follow YYYY-MM")
            String month,
            @RequestParam @Min(value = 1, message = "workplaceId must be greater than 0") long workplaceId
    ) {
        return ApiResponse.success(wageService.getEstimate(user.userId(), month, workplaceId));
    }

    @GetMapping("/summary")
    @Operation(
            summary = "월간 급여 확인 요약 조회",
            description = """
                    인증된 근로자의 월간 급여 확인 요약을 반환합니다.

                    응답에는 아래 정보가 함께 포함됩니다.
                    - WorkProof 기반 참고용 예상 급여
                    - 가장 최근의 실수령 입력값
                    - 후속 확인을 위한 차이와 이상 징후 미리보기
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "월간 급여 확인 요약을 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<WageSummaryResponse>> getSummary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "yearMonth must follow YYYY-MM") String yearMonth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf
    ) {
        return ApiResponse.success(wageService.getSummary(user.userId(), yearMonth, asOf));
    }
}
