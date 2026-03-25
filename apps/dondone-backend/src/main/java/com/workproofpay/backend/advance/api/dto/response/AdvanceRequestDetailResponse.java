package com.workproofpay.backend.advance.api.dto.response;

import com.workproofpay.backend.advance.model.AdvanceSettlementStatus;

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
        AdvanceSettlementStatus settlementStatus,
        LocalDate settlementDueDate,
        LocalDate repaymentDueDate,
        AdvanceEligibilitySnapshotResponse eligibilitySnapshot,
        LocalDateTime createdAt
) {
}
