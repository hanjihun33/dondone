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
        return new WorkProofResponse(
                workProof.getId(),
                workProof.getWorkDate(),
                workProof.getClockInAt(),
                workProof.getClockOutAt(),
                workProof.getDeviceClockInAt(),
                workProof.getDeviceClockOutAt(),
                workProof.getServerClockInAt(),
                workProof.getServerClockOutAt(),
                workProof.getClockInLatitude(),
                workProof.getClockInLongitude(),
                workProof.getClockOutLatitude(),
                workProof.getClockOutLongitude(),
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
