package com.workproofpay.backend.workproof.api;

import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.security.AuthenticatedUser;
import com.workproofpay.backend.workproof.api.dto.request.CheckInWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CheckOutWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkProofCorrectionRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateContractRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkplaceRequest;
import com.workproofpay.backend.workproof.api.dto.request.UpdateWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.response.CurrentContractResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofCorrectionRequestResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryContractResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordListResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkplaceListResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkplaceResponse;
import com.workproofpay.backend.workproof.service.WorkProofCorrectionRequestService;
import com.workproofpay.backend.workproof.service.WorkProofLane1Service;
import com.workproofpay.backend.workproof.service.WorkProofMonthlyMetrics;
import com.workproofpay.backend.workproof.service.WorkProofService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    private final WorkProofCorrectionRequestService workProofCorrectionRequestService;
    private final WorkProofLane1Service workProofLane1Service;

    @PostMapping("/workplaces")
    public ResponseEntity<ApiResponse<WorkplaceResponse>> createWorkplace(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateWorkplaceRequest request
    ) {
        return ApiResponse.created(workProofLane1Service.createWorkplace(user.userId(), request));
    }

    @GetMapping("/workplaces")
    public ResponseEntity<ApiResponse<WorkplaceListResponse>> getWorkplaces(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(workProofLane1Service.getWorkplaces(user.userId()));
    }

    @PostMapping("/contracts")
    public ResponseEntity<ApiResponse<CurrentContractResponse>> createContract(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateContractRequest request
    ) {
        return ApiResponse.created(workProofLane1Service.createContract(user.userId(), request));
    }

    @GetMapping("/contracts/current")
    public ResponseEntity<ApiResponse<CurrentContractResponse>> getCurrentContract(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @Positive(message = "workplaceId must be greater than 0") Long workplaceId
    ) {
        return ApiResponse.success(workProofLane1Service.getCurrentContract(user.userId(), workplaceId));
    }

    @PostMapping("/records/check-in")
    public ResponseEntity<ApiResponse<WorkProofRecordResponse>> checkIn(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CheckInWorkProofRequest request
    ) {
        return ApiResponse.created(workProofLane1Service.checkIn(user.userId(), request));
    }

    @PostMapping("/records/check-out")
    public ResponseEntity<ApiResponse<WorkProofRecordResponse>> checkOut(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CheckOutWorkProofRequest request
    ) {
        return ApiResponse.success(workProofLane1Service.checkOut(user.userId(), request));
    }

    @GetMapping("/records")
    public ResponseEntity<ApiResponse<WorkProofRecordListResponse>> getRecords(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam
            @Size(min = 7, max = 7, message = "month must be exactly 7 characters")
            @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "month must follow YYYY-MM")
            String month,
            @RequestParam @Positive(message = "workplaceId must be greater than 0") Long workplaceId
    ) {
        return ApiResponse.success(workProofLane1Service.getRecords(user.userId(), month, workplaceId));
    }

    @GetMapping("/records/{recordId}")
    public ResponseEntity<ApiResponse<WorkProofRecordResponse>> getRecord(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long recordId
    ) {
        return ApiResponse.success(workProofLane1Service.getRecord(user.userId(), recordId));
    }

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
            @RequestParam(required = false)
            @Size(min = 7, max = 7, message = "yearMonth must be exactly 7 characters")
            @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "yearMonth must follow YYYY-MM")
            String yearMonth,
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
    @Deprecated
    @Operation(
            summary = "Update work proof directly (legacy)",
            description = "Legacy direct edit endpoint. New clients should submit correction requests instead of calling this endpoint directly.",
            deprecated = true
    )
    public ResponseEntity<ApiResponse<WorkProofResponse>> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long workProofId,
            @Valid @RequestBody UpdateWorkProofRequest request
    ) {
        return ApiResponse.success(workProofService.update(user.userId(), workProofId, request));
    }

    @PostMapping("/{workProofId}/correction-requests")
    public ResponseEntity<ApiResponse<WorkProofCorrectionRequestResponse>> createCorrectionRequest(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long workProofId,
            @Valid @RequestBody CreateWorkProofCorrectionRequest request
    ) {
        return ApiResponse.created(workProofCorrectionRequestService.create(user.userId(), workProofId, request));
    }

    @GetMapping(value = "/monthly-summary", params = "yearMonth")
    public ResponseEntity<ApiResponse<WorkProofMonthlySummaryResponse>> getMonthlySummary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam
            @Size(min = 7, max = 7, message = "yearMonth must be exactly 7 characters")
            @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "yearMonth must follow YYYY-MM")
            String yearMonth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf
    ) {
        WorkProofMonthlyMetrics metrics = workProofService.getMonthlyMetrics(user.userId(), yearMonth, asOf);
        return ApiResponse.success(WorkProofMonthlySummaryResponse.from(metrics));
    }

    @GetMapping(value = "/monthly-summary", params = {"month", "workplaceId"})
    public ResponseEntity<ApiResponse<WorkProofMonthlySummaryContractResponse>> getLane1MonthlySummary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam
            @Size(min = 7, max = 7, message = "month must be exactly 7 characters")
            @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "month must follow YYYY-MM")
            String month,
            @RequestParam @Positive(message = "workplaceId must be greater than 0") Long workplaceId
    ) {
        return ApiResponse.success(workProofLane1Service.getMonthlySummary(user.userId(), month, workplaceId));
    }
}
