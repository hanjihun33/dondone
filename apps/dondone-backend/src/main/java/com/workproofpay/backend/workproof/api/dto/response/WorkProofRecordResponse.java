package com.workproofpay.backend.workproof.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 근무 증거를 evidence-first 관점으로 펼쳐 보여주는 WorkProof lane 1 상세 응답이다.
 */
public record WorkProofRecordResponse(
        Long recordId,
        LocalDate workDate,
        WorkProofRecordStatus status,
        WorkplaceSnapshot workplace,
        CurrentContractResponse contract,
        EvidenceCaptureResponse checkIn,
        EvidenceCaptureResponse checkOut,
        Long workedMinutes,
        boolean modified,
        List<ModificationSummary> modifications,
        List<AttachmentSummary> attachments
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

    public record ModificationSummary(
            Long modificationId,
            String status,
            String reasonCode,
            String reasonMemo,
            LocalDateTime modifiedAt,
            int attachmentCount
    ) {
    }

    public record AttachmentSummary(
            String attachmentId,
            String fileName,
            String contentType,
            long size,
            LocalDateTime uploadedAt
    ) {
    }
}
