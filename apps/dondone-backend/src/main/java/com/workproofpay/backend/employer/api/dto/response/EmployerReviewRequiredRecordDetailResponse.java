package com.workproofpay.backend.employer.api.dto.response;

import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordStatus;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofReflectionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployerReviewRequiredRecordDetailResponse(
        Long workProofId,
        Long workerId,
        String workerName,
        String workerEmail,
        LocalDate workDate,
        WorkProofRecordStatus recordStatus,
        WorkProofReflectionStatus reflectionStatus,
        String reviewReasonCode,
        String reviewReason,
        LocalDateTime recognizedClockInAt,
        LocalDateTime recognizedClockOutAt,
        long workedMinutes,
        boolean clockOutOutsideAllowedRadius,
        boolean edited,
        String editReason,
        String memo,
        int attachmentCount,
        WorkplaceSnapshot workplace,
        EvidenceCaptureResponse checkIn,
        EvidenceCaptureResponse checkOut
) {
    public record WorkplaceSnapshot(
            Long workplaceId,
            String name,
            String address,
            String mapLabel,
            Double latitude,
            Double longitude
    ) {
    }

    public record EvidenceCaptureResponse(
            LocalDateTime deviceAt,
            LocalDateTime serverAt,
            Double latitude,
            Double longitude,
            String locationLabel
    ) {
    }
}
