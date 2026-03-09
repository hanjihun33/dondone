package com.workproofpay.backend.workproof.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkProofResponse(
        Long id,
        LocalDate workDate,
        LocalDateTime clockInAt,
        LocalDateTime clockOutAt,
        LocalDateTime deviceClockInAt,
        LocalDateTime deviceClockOutAt,
        LocalDateTime serverClockInAt,
        LocalDateTime serverClockOutAt,
        Double clockInLatitude,
        Double clockInLongitude,
        Double clockOutLatitude,
        Double clockOutLongitude,
        String memo,
        String editReason,
        int attachmentCount,
        boolean edited,
        String financialStatus,
        Long workedMinutes,
        Long overtimeMinutes,
        Long nightMinutes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
