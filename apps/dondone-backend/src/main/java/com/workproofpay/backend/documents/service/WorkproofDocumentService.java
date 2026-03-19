package com.workproofpay.backend.documents.service;

import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkproofDocumentService {

    public static final String WORKPROOF_STATEMENT_DOCUMENT_TYPE = "WORKPROOF_STATEMENT";

    private final WorkplaceRepository workplaceRepository;
    private final WorkProofRepository workProofRepository;

    @Transactional(readOnly = true)
    public WorkproofDocumentPreviewResult preview(Long userId, WorkproofDocumentPreviewQuery query) {
        if (query.startDate() == null || query.endDate() == null || query.startDate().isAfter(query.endDate())) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String workplaceName = workplaceRepository.findByIdAndUserId(query.workplaceId(), userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPLACE_NOT_FOUND))
                .getName();

        List<WorkProof> records = workProofRepository.findByUserIdAndWorkplaceIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(
                userId,
                query.workplaceId(),
                query.startDate(),
                query.endDate()
        );

        int reflectedCount = (int) records.stream()
                .filter(WorkProof::isReflected)
                .count();
        int needsReviewCount = (int) records.stream()
                .filter(WorkProof::isNeedsReview)
                .count();
        int editedCount = (int) records.stream()
                .filter(WorkProof::isEdited)
                .count();
        int attachmentCount = records.stream()
                .mapToInt(WorkProof::getAttachmentCount)
                .sum();
        long totalWorkedMinutes = records.stream()
                .mapToLong(WorkProof::workedMinutes)
                .sum();

        return new WorkproofDocumentPreviewResult(
                WORKPROOF_STATEMENT_DOCUMENT_TYPE,
                query.workplaceId(),
                workplaceName,
                query.startDate(),
                query.endDate(),
                records.size(),
                reflectedCount,
                needsReviewCount,
                editedCount,
                attachmentCount,
                totalWorkedMinutes,
                formatWorkedHours(totalWorkedMinutes)
        );
    }

    private String formatWorkedHours(long totalWorkedMinutes) {
        long hours = totalWorkedMinutes / 60;
        long minutes = totalWorkedMinutes % 60;
        return "%d시간 %02d분".formatted(hours, minutes);
    }
}
