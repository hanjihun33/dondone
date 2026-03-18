package com.workproofpay.remittance.controller;

import com.workproofpay.remittance.dto.*;
import com.workproofpay.remittance.service.DemoSeedService;
import com.workproofpay.remittance.service.RecipientService;
import com.workproofpay.remittance.service.TransferService;
import com.workproofpay.remittance.service.TransferIntegrityService;
import com.workproofpay.remittance.service.UserWalletService;
import com.workproofpay.remittance.util.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/remittance")
public class RemittanceController {
    private final RecipientService recipientService;
    private final TransferService transferService;
    private final TransferIntegrityService transferIntegrityService;
    private final DemoSeedService demoSeedService;
    private final UserWalletService userWalletService;
    private final UserContext userContext;

    public RemittanceController(
            RecipientService recipientService,
            TransferService transferService,
            TransferIntegrityService transferIntegrityService,
            DemoSeedService demoSeedService,
            UserWalletService userWalletService,
            UserContext userContext
    ) {
        this.recipientService = recipientService;
        this.transferService = transferService;
        this.transferIntegrityService = transferIntegrityService;
        this.demoSeedService = demoSeedService;
        this.userWalletService = userWalletService;
        this.userContext = userContext;
    }

    @PostMapping("/demo/seed")
    public DemoSeedResponse seedDemoData() {
        return demoSeedService.seedDefault();
    }

    @PostMapping("/wallets/me")
    public UserWalletResponse createMyWallet(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long userId = userContext.resolveUserId(userIdHeader);
        return userWalletService.createWalletIfAbsent(userId);
    }

    @GetMapping("/wallets/me")
    public UserWalletResponse getMyWallet(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long userId = userContext.resolveUserId(userIdHeader);
        return userWalletService.getWallet(userId);
    }

    @GetMapping("/recipients")
    public RecipientListResponse getRecipients(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        Long userId = userContext.resolveUserId(userIdHeader);
        return new RecipientListResponse(recipientService.getRecipients(userId));
    }

    @PutMapping("/recipients/{recipientId}")
    public RecipientItemResponse upsertRecipient(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @PathVariable String recipientId,
            @Valid @RequestBody RecipientUpsertRequest request
    ) {
        Long userId = userContext.resolveUserId(userIdHeader);
        return recipientService.upsertRecipient(userId, recipientId, request);
    }

    @PostMapping("/transfers/precheck")
    public TransferPrecheckResponse precheck(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody TransferPrecheckRequest request
    ) {
        Long userId = userContext.resolveUserId(userIdHeader);
        return transferService.precheck(userId, request);
    }

    @PostMapping("/transfers")
    public CreateTransferResponse createTransfer(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Sender-Address", required = false) String senderAddressHeader,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateTransferRequest request
    ) {
        Long userId = userContext.resolveUserId(userIdHeader);
        String senderAddress = userContext.resolveSenderAddress(senderAddressHeader);
        return transferService.createTransfer(userId, senderAddress, idempotencyKey, request);
    }

    @GetMapping("/transfers/{transferId}")
    public TransferDetailResponse getTransfer(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @PathVariable String transferId
    ) {
        Long userId = userContext.resolveUserId(userIdHeader);
        return transferService.getTransfer(userId, transferId);
    }

    @GetMapping("/transfers/{transferId}/integrity-hash")
    public IntegrityHashResponse getTransferIntegrityHash(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @PathVariable String transferId
    ) {
        Long userId = userContext.resolveUserId(userIdHeader);
        return transferIntegrityService.generateForTransfer(userId, transferId);
    }

    @PostMapping("/transfers/{transferId}/receipt-link")
    public ReceiptLinkResponse issueReceiptLink(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @PathVariable String transferId
    ) {
        Long userId = userContext.resolveUserId(userIdHeader);
        return transferService.issueReceiptLink(userId, transferId);
    }

    @GetMapping(value = "/transfers/{transferId}/receipt.pdf", produces = MediaType.TEXT_PLAIN_VALUE)
    public String downloadReceipt(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @PathVariable String transferId,
            @RequestParam("token") String token
    ) {
        Long userId = userContext.resolveUserId(userIdHeader);
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token is required");
        }
        return transferService.renderReceiptText(userId, transferId);
    }
}
