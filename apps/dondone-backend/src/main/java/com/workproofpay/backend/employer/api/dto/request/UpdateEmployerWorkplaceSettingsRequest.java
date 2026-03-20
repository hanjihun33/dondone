package com.workproofpay.backend.employer.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateEmployerWorkplaceSettingsRequest(
        @NotBlank(message = "address is required")
        @Size(max = 255, message = "address must be 255 characters or less")
        String address,

        @Size(max = 100, message = "detailAddress must be 100 characters or less")
        String detailAddress,

        @NotNull(message = "latitude is required")
        @DecimalMin(value = "-90.0", message = "latitude must be -90 or greater")
        @DecimalMax(value = "90.0", message = "latitude must be 90 or less")
        Double latitude,

        @NotNull(message = "longitude is required")
        @DecimalMin(value = "-180.0", message = "longitude must be -180 or greater")
        @DecimalMax(value = "180.0", message = "longitude must be 180 or less")
        Double longitude,

        @NotNull(message = "allowedRadiusMeters is required")
        @Min(value = 50, message = "allowedRadiusMeters must be 50 or greater")
        @Max(value = 5000, message = "allowedRadiusMeters must be 5000 or less")
        Integer allowedRadiusMeters
) {
}
