package com.workproofpay.backend.remittance.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RecipientSearchRequest(
        @Schema(description = "휴대폰 번호", example = "010-1234-5678")
        @NotBlank(message = "phoneNumber is required")
        @Pattern(regexp = "^[0-9-]{10,13}$", message = "phoneNumber format is invalid")
        String phoneNumber
) {
}
