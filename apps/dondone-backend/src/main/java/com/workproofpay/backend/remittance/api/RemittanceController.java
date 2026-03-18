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
import com.workproofpay.backend.shared.security.AuthenticatedUser;
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
@RequiredArgsConstructor
public class RemittanceController {

    private final WalletService walletService;
    private final RecipientService recipientService;
    private final TransferService transferService;

    @PostMapping("/wallets/me")
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        WalletService.WalletCreateResult result = walletService.createWalletIfAbsent(user.userId());
        return result.created()
                ? ApiResponse.created(result.response())
                : ApiResponse.success(result.response());
    }

    @GetMapping("/wallets/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(walletService.getWallet(user.userId()));
    }

    @GetMapping("/wallets/me/balance")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> getWalletBalance(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(walletService.getWalletBalance(user.userId()));
    }

    @GetMapping("/recipients")
    public ResponseEntity<ApiResponse<RecipientListResponse>> getRecipients(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(recipientService.getRecipients(user.userId()));
    }

    @PostMapping("/recipients")
    public ResponseEntity<ApiResponse<RecipientItemResponse>> createRecipient(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpsertRecipientRequest request
    ) {
        return ApiResponse.created(recipientService.createRecipient(user.userId(), request));
    }

    @PutMapping("/recipients/{recipientId}")
    public ResponseEntity<ApiResponse<RecipientItemResponse>> updateRecipient(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String recipientId,
            @Valid @RequestBody UpsertRecipientRequest request
    ) {
        return ApiResponse.success(recipientService.updateRecipient(user.userId(), recipientId, request));
    }

    @PostMapping("/transfers/precheck")
    public ResponseEntity<ApiResponse<TransferPrecheckResponse>> precheck(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody TransferPrecheckRequest request
    ) {
        return ApiResponse.success(transferService.precheck(user.userId(), request));
    }

    @PostMapping("/transfers")
    public ResponseEntity<ApiResponse<CreateTransferResponse>> createTransfer(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateTransferRequest request
    ) {
        TransferCreateResult result = transferService.createTransfer(user.userId(), idempotencyKey, request);
        return result.replayed()
                ? ApiResponse.success(result.response())
                : ApiResponse.created(result.response());
    }

    @GetMapping("/transfers")
    public ResponseEntity<ApiResponse<TransferListResponse>> getTransfers(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @Positive(message = "limit must be greater than 0") Integer limit
    ) {
        return ApiResponse.success(transferService.getTransfers(user.userId(), limit));
    }

    @GetMapping("/transfers/{transferId}")
    public ResponseEntity<ApiResponse<TransferDetailResponse>> getTransfer(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String transferId
    ) {
        return ApiResponse.success(transferService.getTransfer(user.userId(), transferId));
    }
}
