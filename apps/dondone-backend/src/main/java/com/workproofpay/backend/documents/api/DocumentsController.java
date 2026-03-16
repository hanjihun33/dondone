package com.workproofpay.backend.documents.api;

import com.workproofpay.backend.documents.api.dto.request.CreateProofPackRequest;
import com.workproofpay.backend.documents.api.dto.response.DocumentGenerationAcceptedResponse;
import com.workproofpay.backend.documents.service.DocumentsService;
import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
}
