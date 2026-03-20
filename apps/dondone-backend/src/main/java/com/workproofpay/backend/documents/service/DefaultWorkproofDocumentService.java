package com.workproofpay.backend.documents.service;

import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentType;
import com.workproofpay.backend.documents.repo.DocumentGenerationRequestRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultWorkproofDocumentService implements WorkproofDocumentService {

    static final String WORKPROOF_STATEMENT_DOCUMENT_TYPE = "WORKPROOF_STATEMENT";
    private static final long MAX_PREVIEW_RANGE_DAYS = 366L;
    private static final String DUPLICATE_REQUEST_CONSTRAINT = "uk_document_generation_requests_user_type_key";

    private final DocumentGenerationRequestRepository documentGenerationRequestRepository;
    private final WorkplaceRepository workplaceRepository;
    private final WorkProofRepository workProofRepository;

    @Override
    @Transactional(readOnly = true)
    public WorkproofDocumentPreviewResult preview(Long userId, WorkproofDocumentPreviewQuery query) {
        validateQuery(query);

        Workplace workplace = getOwnedWorkplace(userId, query.workplaceId());

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
                workplace.getName(),
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

    @Override
    @Transactional
    public WorkproofDocumentAcceptedResult create(Long userId, CreateWorkproofDocumentCommand command) {
        validateCreateCommand(command);

        if (documentGenerationRequestRepository.existsByUserIdAndDocumentTypeAndIdempotencyKey(
                userId,
                DocumentType.WORKPROOF_STATEMENT,
                command.idempotencyKey()
        )) {
            throw new ApiException(ErrorCode.DOCUMENT_DUPLICATE_REQUEST);
        }

        Workplace workplace = getOwnedWorkplace(userId, command.workplaceId());
        DocumentGenerationRequest requestToSave = DocumentGenerationRequest.queueWorkproofStatement(
                workplace.getUser(),
                command.workplaceId(),
                command.startDate(),
                command.endDate(),
                command.idempotencyKey()
        );

        try {
            DocumentGenerationRequest saved = documentGenerationRequestRepository.saveAndFlush(requestToSave);
            return new WorkproofDocumentAcceptedResult(
                    saved.getRequestId(),
                    saved.getDocumentType(),
                    saved.getStatus(),
                    "/api/documents/requests/" + saved.getRequestId(),
                    "/api/documents/" + saved.getId() + "/download"
            );
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateRequestViolation(e)) {
                throw new ApiException(ErrorCode.DOCUMENT_DUPLICATE_REQUEST);
            }
            throw e;
        }
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

    private void validateCreateCommand(CreateWorkproofDocumentCommand command) {
        if (command == null
                || command.documentType() == null
                || command.workplaceId() == null
                || command.startDate() == null
                || command.endDate() == null
                || command.idempotencyKey() == null
                || command.idempotencyKey().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (command.documentType() != DocumentType.WORKPROOF_STATEMENT) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        validateQuery(new WorkproofDocumentPreviewQuery(
                command.workplaceId(),
                command.startDate(),
                command.endDate()
        ));
    }

    private Workplace getOwnedWorkplace(Long userId, Long workplaceId) {
        return workplaceRepository.findByIdAndUserId(workplaceId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPLACE_NOT_FOUND));
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

    private boolean isDuplicateRequestViolation(DataIntegrityViolationException e) {
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(e);
        String message = rootCause == null ? e.getMessage() : rootCause.getMessage();
        return message != null && message.contains(DUPLICATE_REQUEST_CONSTRAINT);
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
