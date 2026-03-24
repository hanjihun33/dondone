package com.workproofpay.backend.admin.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminAdvanceRequestItemResponse(
        Long requestId,
        Long workerId,
        String workerName,
        String workerEmail,
        String companyName,
        String workplaceName,
        String assetSymbol,
        Integer assetDecimals,
        java.math.BigDecimal referenceExchangeRate,
        Long requestedAmountAtomic,
        Long requestedReferenceKrw,
        Long approvedAmountAtomic,
        Long approvedReferenceKrw,
        Long feeAmountAtomic,
        Long feeReferenceKrw,
        String status,
        LocalDate repaymentDueDate,
        LocalDateTime requestedAt,
        Integer reflectedWorkDays,
        Long reflectedWorkMinutes,
        Integer needsReviewRecordCount,
        LocalDateTime reviewedAt
) {
}
