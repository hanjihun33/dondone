package com.workproofpay.backend.workproof.api;

import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.UpdateWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofResponse;
import com.workproofpay.backend.workproof.service.WorkProofMonthlyMetrics;
import com.workproofpay.backend.workproof.service.WorkProofService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/workproof")
@RequiredArgsConstructor
public class WorkProofController {

    private final WorkProofService workProofService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkProofResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateWorkProofRequest request
    ) {
        return ApiResponse.created(workProofService.create(user.userId(), request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkProofResponse>>> getWorkProofs(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "yearMonth must follow YYYY-MM") String yearMonth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf
    ) {
        return ApiResponse.success(workProofService.getWorkProofs(user.userId(), yearMonth, asOf));
    }

    @GetMapping("/{workProofId}")
    public ResponseEntity<ApiResponse<WorkProofResponse>> getWorkProof(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long workProofId
    ) {
        return ApiResponse.success(workProofService.getWorkProof(user.userId(), workProofId));
    }

    @PatchMapping("/{workProofId}")
    public ResponseEntity<ApiResponse<WorkProofResponse>> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long workProofId,
            @Valid @RequestBody UpdateWorkProofRequest request
    ) {
        return ApiResponse.success(workProofService.update(user.userId(), workProofId, request));
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<ApiResponse<WorkProofMonthlySummaryResponse>> getMonthlySummary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "yearMonth must follow YYYY-MM") String yearMonth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf
    ) {
        WorkProofMonthlyMetrics metrics = workProofService.getMonthlyMetrics(user.userId(), yearMonth, asOf);
        return ApiResponse.success(WorkProofMonthlySummaryResponse.from(metrics));
    }
}
