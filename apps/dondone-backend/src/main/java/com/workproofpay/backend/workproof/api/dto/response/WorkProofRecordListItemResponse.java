package com.workproofpay.backend.workproof.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkProofRecordListItemResponse(
        Long recordId,
        LocalDate workDate,
        WorkProofRecordStatus status,
        LocalDateTime checkInDeviceAt,
        LocalDateTime checkOutDeviceAt,
        Long workedMinutes,
        boolean modified,
        WorkProofReflectionStatus reflectionStatus
) {
}
