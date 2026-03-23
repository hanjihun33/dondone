package com.workproofpay.backend.admin.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCreateEmployerCompanyRequest(
        @NotBlank(message = "companyName is required")
        @Size(max = 120, message = "companyName must be 120 characters or fewer")
        String companyName,

        @NotBlank(message = "companyCode is required")
        @Pattern(regexp = "^[A-Za-z0-9-]{6,50}$", message = "companyCode format is invalid")
        @Size(max = 50, message = "companyCode must be 50 characters or fewer")
        String companyCode
) {
}
