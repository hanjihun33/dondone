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
        LocalDateTime recognizedClockInAt,
        LocalDateTime recognizedClockOutAt,
        Long workedMinutes,
        boolean modified,
        WorkProofReflectionStatus reflectionStatus,
        String decisionMemo,
        List<String> riskFlags
) {
}
