package com.workproofpay.backend.workproof.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record WorkProofRecordListItemResponse(
        Long recordId,
        LocalDate workDate,
        WorkProofRecordStatus status,
        LocalDateTime checkInDeviceAt,
        LocalDateTime checkOutDeviceAt,
        Long workedMinutes,
        boolean modified,
        WorkProofReflectionStatus reflectionStatus,
        List<String> riskFlags
) {
}
