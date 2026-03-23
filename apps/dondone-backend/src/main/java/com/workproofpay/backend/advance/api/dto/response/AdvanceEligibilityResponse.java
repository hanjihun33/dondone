package com.workproofpay.backend.advance.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdvanceEligibilityResponse(
        Long workplaceId,
        Long availableAmount,
        Long maxCap,
        BigDecimal policyRate,
        String repaymentTier,
        Integer reflectedWorkDays,
        Long reflectedWorkMinutes,
        Long verifiedMinutes,
        Long pendingMinutes,
        Integer needsReviewRecordCount,
        List<String> blockReasonCodes,
        List<String> noticeReasonCodes,
        Long nextTierRemainingMinutes,
        Long estimatedFee,
        LocalDate estimatedRepaymentDate,
        String disclaimer
) {
}
