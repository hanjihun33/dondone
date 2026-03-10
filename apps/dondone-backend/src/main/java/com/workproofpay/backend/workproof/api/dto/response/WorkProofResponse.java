package com.workproofpay.backend.workproof.api.dto.response;

import com.workproofpay.backend.workproof.model.WorkProof;

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
    public static WorkProofResponse from(WorkProof workProof,
                                         Long workedMinutes,
                                         Long overtimeMinutes,
                                         Long nightMinutes) {
        boolean exposeCapturedEvidence = !workProof.isEdited();
        return new WorkProofResponse(
                workProof.getId(),
                workProof.getWorkDate(),
                workProof.getClockInAt(),
                workProof.getClockOutAt(),
                exposeCapturedEvidence ? workProof.getDeviceClockInAt() : null,
                exposeCapturedEvidence ? workProof.getDeviceClockOutAt() : null,
                exposeCapturedEvidence ? workProof.getServerClockInAt() : null,
                exposeCapturedEvidence ? workProof.getServerClockOutAt() : null,
                exposeCapturedEvidence ? workProof.getClockInLatitude() : null,
                exposeCapturedEvidence ? workProof.getClockInLongitude() : null,
                exposeCapturedEvidence ? workProof.getClockOutLatitude() : null,
                exposeCapturedEvidence ? workProof.getClockOutLongitude() : null,
                workProof.getMemo(),
                workProof.getEditReason(),
                workProof.getAttachmentCount(),
                workProof.isEdited(),
                workProof.getFinancialStatus().name(),
                workedMinutes,
                overtimeMinutes,
                nightMinutes,
                workProof.getCreatedAt(),
                workProof.getUpdatedAt()
        );
    }
}
