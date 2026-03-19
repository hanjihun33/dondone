package com.workproofpay.backend.remittance.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTransferRequest(
        @Schema(description = "송금할 수신자 식별자", example = "rec_01HXYZABCDEF1234567890")
        @NotBlank(message = "recipientId is required")
        String recipientId,
        @Schema(description = "송금 금액 atomic 단위", example = "50000000")
        @NotNull(message = "amountAtomic is required")
        @Positive(message = "amountAtomic must be greater than 0")
        Long amountAtomic,
        @Schema(description = "고액 송금 확인 여부", example = "false")
        boolean highAmountConfirmed,
        @Schema(description = "최근 수정 수신자 추가 확인 여부", example = "true")
        boolean recentRecipientConfirmed
) {
}
