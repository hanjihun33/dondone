package com.workproofpay.backend.workproof.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWorkplaceRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be 100 characters or less")
        String name,

        @NotBlank(message = "address is required")
        @Size(max = 255, message = "address must be 255 characters or less")
        String address,

        @Size(max = 100, message = "mapLabel must be 100 characters or less")
        String mapLabel,

        @NotNull(message = "latitude is required")
        @DecimalMin(value = "-90.0", message = "latitude must be -90 or greater")
        @DecimalMax(value = "90.0", message = "latitude must be 90 or less")
        Double latitude,

        @NotNull(message = "longitude is required")
        @DecimalMin(value = "-180.0", message = "longitude must be -180 or greater")
        @DecimalMax(value = "180.0", message = "longitude must be 180 or less")
        Double longitude
) {
}
