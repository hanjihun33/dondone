package com.workproofpay.backend.workproof.api.dto.response;

import java.time.LocalDateTime;

public record WorkplaceResponse(
        Long workplaceId,
        String name,
        String address,
        String mapLabel,
        Double latitude,
        Double longitude,
        Integer allowedRadiusMeters,
        Boolean hasActiveContract,
        LocalDateTime createdAt
) {
}
