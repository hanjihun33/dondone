package com.workproofpay.backend.workproof.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CheckOutWorkProofRequest(
        @NotNull(message = "deviceAt is required")
        LocalDateTime deviceAt,

        @NotNull(message = "latitude is required")
        @DecimalMin(value = "-90.0", message = "latitude must be -90 or greater")
        @DecimalMax(value = "90.0", message = "latitude must be 90 or less")
        Double latitude,

        @NotNull(message = "longitude is required")
        @DecimalMin(value = "-180.0", message = "longitude must be -180 or greater")
        @DecimalMax(value = "180.0", message = "longitude must be 180 or less")
        Double longitude,

        @Size(max = 100, message = "locationLabel must be 100 characters or less")
        String locationLabel
) {
}
