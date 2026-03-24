package com.workproofpay.backend.advance.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdvanceRequestDetailResponse(
        Long requestId,
        Long workplaceId,
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
        String payoutTxHash,
        LocalDate repaymentDueDate,
        AdvanceEligibilitySnapshotResponse eligibilitySnapshot,
        LocalDateTime createdAt
) {
}
