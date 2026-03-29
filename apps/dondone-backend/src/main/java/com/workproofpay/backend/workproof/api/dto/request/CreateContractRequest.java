package com.workproofpay.backend.workproof.api.dto.request;

import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateContractRequest(
        @NotNull(message = "workplaceId is required")
        @Positive(message = "workplaceId must be greater than 0")
        Long workplaceId,

        @NotNull(message = "payUnit is required")
        WorkProofPayUnit payUnit,

        @NotNull(message = "basePayAmount is required")
        @Positive(message = "basePayAmount must be greater than 0")
        BigDecimal basePayAmount,

        @Positive(message = "dailyWorkMinutes must be greater than 0")
        Integer dailyWorkMinutes,

        @Positive(message = "monthlyWorkMinutes must be greater than 0")
        Integer monthlyWorkMinutes,

        @Min(value = 1, message = "paydayDay must be between 1 and 31")
        @Max(value = 31, message = "paydayDay must be between 1 and 31")
        Integer paydayDay,

        LocalDate effectiveFrom
) {
    public CreateContractRequest(Long workplaceId,
                                 WorkProofPayUnit payUnit,
                                 BigDecimal basePayAmount,
                                 Integer dailyWorkMinutes,
                                 Integer monthlyWorkMinutes,
                                 LocalDate effectiveFrom) {
        this(workplaceId, payUnit, basePayAmount, dailyWorkMinutes, monthlyWorkMinutes, null, effectiveFrom);
    }
}
