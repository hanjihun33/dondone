package com.workproofpay.backend.documents.api;

import com.workproofpay.backend.documents.api.dto.request.CreateWorkproofDocumentRequest;
import com.workproofpay.backend.documents.api.dto.response.DocumentGenerationAcceptedResponse;
import com.workproofpay.backend.documents.api.dto.request.WorkproofDocumentPreviewRequest;
import com.workproofpay.backend.documents.api.dto.response.WorkproofDocumentPreviewResponse;
import com.workproofpay.backend.documents.service.WorkproofDocumentService;
import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.config.OpenApiConfig;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/workproof/documents")
@Tag(name = "Workproof Documents", description = "기간 기반 근무 기록 문서 미리보기 및 생성 API")
@RequiredArgsConstructor
public class WorkproofDocumentsController {

    private final WorkproofDocumentService workproofDocumentService;

    @GetMapping("/preview")
    @Operation(
            summary = "근무 기록 문서 미리보기 조회",
            description = """
                    인증된 근로자가 선택한 기간의 출퇴근 기록 문서 요약을 미리 확인합니다.

                    현재 범위:
                    - 기간별 기록 수, 상태 건수, 수정 건수, 첨부 수, 총 근무시간만 반환합니다.
                    - 실제 PDF 생성 요청은 후속 create API에서 분리합니다.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문서 미리보기를 반환했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "소유한 근무지를 찾을 수 없습니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<WorkproofDocumentPreviewResponse>> preview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid
            @ParameterObject
            @ModelAttribute
            WorkproofDocumentPreviewRequest request
    ) {
        return ApiResponse.success(
                WorkproofDocumentPreviewResponse.from(
                        workproofDocumentService.preview(user.userId(), request.toQuery())
                )
        );
    }

    @PostMapping
    @Operation(
            summary = "근무 기록 문서 생성 요청 접수",
            description = """
                    인증된 근로자가 기간 기반 근무 기록 문서 생성을 요청합니다.

                    현재 범위:
                    - WORKPROOF_STATEMENT 문서 타입만 지원합니다.
                    - 요청이 저장되면 requestId와 download 경로를 함께 반환합니다.
                    - 실제 PDF 렌더링은 download 호출 시점에 수행합니다.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "문서 생성 요청이 접수되었습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증에 실패했습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "소유한 근무지를 찾을 수 없습니다.", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "같은 idempotency key의 문서 요청이 이미 존재합니다.", content = @Content)
    })
    public ResponseEntity<ApiResponse<DocumentGenerationAcceptedResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader("Idempotency-Key") @NotBlank(message = "Idempotency-Key header is required") String idempotencyKey,
            @Valid @RequestBody CreateWorkproofDocumentRequest request
    ) {
        var accepted = workproofDocumentService.create(user.userId(), request.toCommand(idempotencyKey));
        return ApiResponse.accepted(new DocumentGenerationAcceptedResponse(
                accepted.requestId(),
                accepted.documentType(),
                accepted.status(),
                accepted.pollUrl(),
                accepted.documentUrl()
        ));
    }
}
