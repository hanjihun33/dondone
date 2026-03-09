package com.workproofpay.backend.wage.api.dto.response;

import com.workproofpay.backend.wage.model.WageDeposit;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WageDepositResponse(
        Long id,
        String yearMonth,
        LocalDate depositDate,
        Long actualDepositAmount,
        boolean deductionsKnown,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WageDepositResponse from(WageDeposit deposit) {
        return new WageDepositResponse(
                deposit.getId(),
                deposit.getYearMonth(),
                deposit.getDepositDate(),
                deposit.getActualDepositAmount(),
                deposit.isDeductionsKnown(),
                deposit.getNote(),
                deposit.getCreatedAt(),
                deposit.getUpdatedAt()
        );
    }
}
