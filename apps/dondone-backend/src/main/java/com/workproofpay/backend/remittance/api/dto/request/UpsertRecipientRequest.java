package com.workproofpay.backend.remittance.api.dto.request;

import com.workproofpay.backend.remittance.model.RecipientRelation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertRecipientRequest(
        @Schema(description = "수신자 별칭", example = "엄마")
        @NotBlank(message = "alias is required")
        String alias,
        @Schema(description = "수신자 관계", example = "FAMILY")
        @NotNull(message = "relation is required")
        RecipientRelation relation,
        @Schema(description = "외부 EVM 지갑 주소. targetUserId가 없을 때 사용", example = "0x1111111111111111111111111111111111111111")
        String walletAddress,
        @Schema(description = "전화번호 검색 결과로 선택한 DonDone 사용자 식별자", example = "12")
        Long targetUserId,
        @Schema(description = "허용목록 활성 여부", example = "true")
        boolean allowed
) {
}
