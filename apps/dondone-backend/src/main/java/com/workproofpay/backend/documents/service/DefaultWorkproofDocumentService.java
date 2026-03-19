package com.workproofpay.backend.documents.service;

import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultWorkproofDocumentService implements WorkproofDocumentService {

    static final String WORKPROOF_STATEMENT_DOCUMENT_TYPE = "WORKPROOF_STATEMENT";
    private static final long MAX_PREVIEW_RANGE_DAYS = 366L;

    private final WorkplaceRepository workplaceRepository;
    private final WorkProofRepository workProofRepository;

    @Override
    @Transactional(readOnly = true)
    public WorkproofDocumentPreviewResult preview(Long userId, WorkproofDocumentPreviewQuery query) {
        validateQuery(query);

        String workplaceName = workplaceRepository.findByIdAndUserId(query.workplaceId(), userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPLACE_NOT_FOUND))
                .getName();

        List<WorkProof> records = workProofRepository.findByUserIdAndWorkplaceIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(
                userId,
                query.workplaceId(),
                query.startDate(),
                query.endDate()
        );

        PreviewMetrics metrics = summarize(records);
        return new WorkproofDocumentPreviewResult(
                WORKPROOF_STATEMENT_DOCUMENT_TYPE,
                query.workplaceId(),
                workplaceName,
                query.startDate(),
                query.endDate(),
                records.size(),
                metrics.reflectedCount(),
                metrics.needsReviewCount(),
                metrics.editedCount(),
                metrics.attachmentCount(),
                metrics.totalWorkedMinutes(),
                formatWorkedHours(metrics.totalWorkedMinutes())
        );
    }

    private void validateQuery(WorkproofDocumentPreviewQuery query) {
        if (query == null || query.workplaceId() == null || query.startDate() == null || query.endDate() == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (query.startDate().isAfter(query.endDate())) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        long inclusiveDays = ChronoUnit.DAYS.between(query.startDate(), query.endDate()) + 1;
        if (inclusiveDays > MAX_PREVIEW_RANGE_DAYS) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private PreviewMetrics summarize(List<WorkProof> records) {
        int reflectedCount = 0;
        int needsReviewCount = 0;
        int editedCount = 0;
        int attachmentCount = 0;
        long totalWorkedMinutes = 0L;

        for (WorkProof record : records) {
            if (record.isReflected()) {
                reflectedCount++;
            }
            if (record.isNeedsReview()) {
                needsReviewCount++;
            }
            if (record.isEdited()) {
                editedCount++;
            }
            attachmentCount += record.getAttachmentCount();
            totalWorkedMinutes += record.workedMinutes();
        }

        return new PreviewMetrics(
                reflectedCount,
                needsReviewCount,
                editedCount,
                attachmentCount,
                totalWorkedMinutes
        );
    }

    private String formatWorkedHours(long totalWorkedMinutes) {
        long hours = totalWorkedMinutes / 60;
        long minutes = totalWorkedMinutes % 60;
        return "%d시간 %02d분".formatted(hours, minutes);
    }

    private record PreviewMetrics(
            int reflectedCount,
            int needsReviewCount,
            int editedCount,
            int attachmentCount,
            long totalWorkedMinutes
    ) {
    }
}
