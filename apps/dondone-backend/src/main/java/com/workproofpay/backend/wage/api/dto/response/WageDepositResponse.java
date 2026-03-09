package com.workproofpay.backend.wage.api.dto.response;

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
}
