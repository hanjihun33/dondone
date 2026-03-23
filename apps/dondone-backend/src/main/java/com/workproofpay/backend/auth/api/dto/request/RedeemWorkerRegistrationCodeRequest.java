package com.workproofpay.backend.auth.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RedeemWorkerRegistrationCodeRequest(
        @Schema(description = "Employer-issued worker registration code", example = "WORKER-AB12-CD34")
        @NotBlank(message = "Registration code is required")
        @Pattern(regexp = "^[A-Za-z0-9-]{8,32}$", message = "Registration code format is invalid")
        String registrationCode
) {
}
