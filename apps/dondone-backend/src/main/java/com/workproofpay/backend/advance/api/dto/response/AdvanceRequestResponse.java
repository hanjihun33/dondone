package com.workproofpay.backend.advance.api.dto.response;

import com.workproofpay.backend.advance.model.AdvanceSettlementStatus;

import java.time.LocalDate;

public record AdvanceRequestResponse(
        Long requestId,
        String assetSymbol,
        Integer assetDecimals,
        java.math.BigDecimal exchangeRateSnapshot,
        String status,
        String requestStatus,
        String payoutStatus,
        Long approvedAmountAtomic,
        Long approvedDisplayKrwAmount,
        Long feeAmountAtomic,
        Long feeDisplayKrwAmount,
        AdvanceSettlementStatus settlementStatus,
        LocalDate settlementDueDate,
        LocalDate repaymentDueDate,
        AdvanceEligibilitySnapshotResponse eligibilitySnapshot
) {
}
