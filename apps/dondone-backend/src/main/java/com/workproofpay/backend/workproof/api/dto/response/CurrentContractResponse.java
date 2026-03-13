package com.workproofpay.backend.workproof.api.dto.response;

import com.workproofpay.backend.workproof.model.WorkProofPayUnit;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CurrentContractResponse(
        Long contractId,
        Long workplaceId,
        WorkProofPayUnit payUnit,
        BigDecimal basePayAmount,
        Integer dailyWorkMinutes,
        Integer monthlyWorkMinutes,
        BigDecimal normalizedHourlyWage,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean isActive
) {
}
