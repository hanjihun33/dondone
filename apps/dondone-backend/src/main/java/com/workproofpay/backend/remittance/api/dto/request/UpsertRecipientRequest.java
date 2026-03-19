package com.workproofpay.backend.remittance.api.dto.request;

import com.workproofpay.backend.remittance.model.RecipientRelation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpsertRecipientRequest(
        @Schema(description = "수신자 별칭", example = "엄마")
        @NotBlank(message = "alias is required")
        String alias,
        @Schema(description = "수신자 관계", example = "FAMILY")
        @NotNull(message = "relation is required")
        RecipientRelation relation,
        @Schema(description = "외부 EVM 지갑 주소", example = "0x1111111111111111111111111111111111111111")
        @NotBlank(message = "walletAddress is required")
        @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "walletAddress must be a valid EVM address")
        String walletAddress,
        @Schema(description = "허용목록 활성 여부", example = "true")
        boolean allowed
) {
}
