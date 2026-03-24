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
        java.math.BigDecimal exchangeRateSnapshot,
        Long requestedAmountAtomic,
        Long requestedDisplayKrwAmount,
        Long approvedAmountAtomic,
        Long approvedDisplayKrwAmount,
        Long feeAmountAtomic,
        Long feeDisplayKrwAmount,
        String status,
        String requestStatus,
        String payoutStatus,
        LocalDate repaymentDueDate,
        LocalDateTime requestedAt,
        Integer reflectedWorkDays,
        Long reflectedWorkMinutes,
        Integer needsReviewRecordCount,
        LocalDateTime reviewedAt
) {
}
