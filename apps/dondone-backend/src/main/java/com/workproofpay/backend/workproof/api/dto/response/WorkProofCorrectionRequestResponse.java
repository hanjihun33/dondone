package com.workproofpay.backend.workproof.api.dto.response;

import com.workproofpay.backend.correction.model.CorrectionRequest;
import com.workproofpay.backend.correction.model.CorrectionRequestReasonCode;
import com.workproofpay.backend.correction.model.CorrectionReviewReasonCode;
import com.workproofpay.backend.correction.model.CorrectionRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkProofCorrectionRequestResponse(
        Long requestId,
        Long workProofId,
        LocalDate workDate,
        LocalDateTime originalClockInAt,
        LocalDateTime originalClockOutAt,
        LocalDateTime requestedClockInAt,
        LocalDateTime requestedClockOutAt,
        CorrectionRequestReasonCode reasonCode,
        CorrectionReviewReasonCode reviewReasonCode,
        String reason,
        String memo,
        int attachmentCount,
        CorrectionRequestStatus status,
        LocalDateTime requestedAt
) {
    public static WorkProofCorrectionRequestResponse from(CorrectionRequest correctionRequest) {
        return new WorkProofCorrectionRequestResponse(
                correctionRequest.getId(),
                correctionRequest.getWorkProof().getId(),
                correctionRequest.getWorkDate(),
                correctionRequest.getOriginalClockInAt(),
                correctionRequest.getOriginalClockOutAt(),
                correctionRequest.getRequestedClockInAt(),
                correctionRequest.getRequestedClockOutAt(),
                correctionRequest.getReasonCode(),
                correctionRequest.getReviewReasonCode(),
                correctionRequest.getReason(),
                correctionRequest.getRequestMemo(),
                correctionRequest.getAttachmentCount(),
                correctionRequest.getStatus(),
                correctionRequest.getCreatedAt()
        );
    }
}
