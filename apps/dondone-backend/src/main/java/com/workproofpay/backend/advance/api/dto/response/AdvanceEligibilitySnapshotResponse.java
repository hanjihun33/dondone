package com.workproofpay.backend.advance.api.dto.response;

import java.math.BigDecimal;

public record AdvanceEligibilitySnapshotResponse(
        Long availableAmount,
        Long maxCap,
        BigDecimal policyRate,
        Integer reflectedWorkDays,
        Long reflectedWorkMinutes,
        Integer needsReviewRecordCount
) {
}
