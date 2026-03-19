package com.workproofpay.backend.documents.api;

import com.workproofpay.backend.documents.api.dto.request.CreateClaimKitRequest;
import com.workproofpay.backend.documents.api.dto.response.DocumentDetailResponse;
import com.workproofpay.backend.documents.api.dto.request.CreateProofPackRequest;
import com.workproofpay.backend.documents.api.dto.response.DocumentGenerationAcceptedResponse;
import com.workproofpay.backend.documents.api.dto.response.DocumentGenerationRequestStatusResponse;
import com.workproofpay.backend.documents.pdf.RenderedPdf;
import com.workproofpay.backend.documents.service.DocumentsService;
import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentsController {

    private final DocumentsService documentsService;

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, String>>> ping() {
        return ApiResponse.success(Map.of("module", "documents", "status", "skeleton-ready"));
    }

    /**
     * Proof Pack 생성은 verification anchor 하나만 받아 downstream 문맥을 고정한다.
     */
    @PostMapping("/proof-packs")
    public ResponseEntity<ApiResponse<DocumentGenerationAcceptedResponse>> createProofPack(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader("Idempotency-Key") @NotBlank(message = "Idempotency-Key header is required") String idempotencyKey,
            @Valid @RequestBody CreateProofPackRequest request
    ) {
        return ApiResponse.accepted(documentsService.createProofPack(user.userId(), idempotencyKey, request));
    }

    @PostMapping("/claim-kits")
    public ResponseEntity<ApiResponse<DocumentGenerationAcceptedResponse>> createClaimKit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader("Idempotency-Key") @NotBlank(message = "Idempotency-Key header is required") String idempotencyKey,
            @Valid @RequestBody CreateClaimKitRequest request
    ) {
        return ApiResponse.accepted(documentsService.createClaimKit(user.userId(), idempotencyKey, request));
    }

    @GetMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponse<DocumentGenerationRequestStatusResponse>> getRequestStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String requestId
    ) {
        return ApiResponse.success(documentsService.getRequestStatus(user.userId(), requestId));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<DocumentDetailResponse>> getDocumentDetail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive(message = "documentId must be greater than 0") Long documentId
    ) {
        return ApiResponse.success(documentsService.getDocumentDetail(user.userId(), documentId));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive(message = "documentId must be greater than 0") Long documentId
    ) {
        RenderedPdf renderedPdf = documentsService.generateProofPackPdf(user.userId(), documentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + renderedPdf.fileName() + "\"")
                .header("X-Document-Sha256", renderedPdf.sha256())
                .contentType(MediaType.parseMediaType(renderedPdf.contentType()))
                .body(renderedPdf.bytes());
    }
}
