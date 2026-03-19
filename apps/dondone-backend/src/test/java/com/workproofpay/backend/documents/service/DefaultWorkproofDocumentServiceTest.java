package com.workproofpay.backend.documents.service;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentGenerationStatus;
import com.workproofpay.backend.documents.model.DocumentType;
import com.workproofpay.backend.documents.repo.DocumentGenerationRequestRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.model.WorkContract;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultWorkproofDocumentServiceTest {

    @Mock
    private DocumentGenerationRequestRepository documentGenerationRequestRepository;

    @Mock
    private WorkplaceRepository workplaceRepository;

    @Mock
    private WorkProofRepository workProofRepository;

    private DefaultWorkproofDocumentService service;

    @BeforeEach
    void setUp() {
        service = new DefaultWorkproofDocumentService(
                documentGenerationRequestRepository,
                workplaceRepository,
                workProofRepository
        );
    }

    @Test
    void previewSummarizesPeriodRecords() {
        User user = User.register("preview@test.com", "hashed", "Preview User");
        Workplace workplace = Workplace.create(user, "Preview Cafe", "Seoul", "Gate", 37.5, 127.0, 150);
        WorkContract contract = WorkContract.activate(
                workplace,
                WorkProofPayUnit.HOURLY,
                BigDecimal.valueOf(12_000),
                480,
                10_560,
                BigDecimal.valueOf(12_000),
                LocalDate.of(2026, 1, 1)
        );
        WorkProof reflected = WorkProof.checkIn(
                user,
                workplace,
                contract,
                LocalDateTime.of(2026, 1, 10, 9, 0),
                LocalDateTime.of(2026, 1, 10, 9, 0),
                37.5,
                127.0,
                "Gate"
        );
        reflected.completeCheckOut(
                LocalDateTime.of(2026, 1, 10, 18, 0),
                LocalDateTime.of(2026, 1, 10, 18, 0),
                37.5,
                127.0,
                "Gate",
                false
        );
        reflected.updateTimes(
                LocalDateTime.of(2026, 1, 10, 9, 10),
                LocalDateTime.of(2026, 1, 10, 18, 10),
                "Correction",
                "Updated memo",
                2,
                "{\"attachments\":[]}"
        );

        WorkProof review = WorkProof.checkIn(
                user,
                workplace,
                contract,
                LocalDateTime.of(2026, 1, 11, 9, 0),
                LocalDateTime.of(2026, 1, 11, 9, 0),
                37.5,
                127.0,
                "Gate"
        );
        review.completeCheckOut(
                LocalDateTime.of(2026, 1, 11, 18, 0),
                LocalDateTime.of(2026, 1, 11, 18, 0),
                37.6,
                127.1,
                "Parking",
                true
        );

        when(workplaceRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(workplace));
        when(workProofRepository.findByUserIdAndWorkplaceIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(
                1L,
                2L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        )).thenReturn(List.of(reflected, review));

        WorkproofDocumentPreviewResult result = service.preview(
                1L,
                new WorkproofDocumentPreviewQuery(
                        2L,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31)
                )
        );

        assertEquals("WORKPROOF_STATEMENT", result.documentType());
        assertEquals("Preview Cafe", result.workplaceName());
        assertEquals(2, result.totalRecordCount());
        assertEquals(1, result.reflectedCount());
        assertEquals(1, result.needsReviewCount());
        assertEquals(1, result.editedCount());
        assertEquals(2, result.attachmentCount());
        assertEquals(1_080L, result.totalWorkedMinutes());
        assertEquals("18시간 00분", result.totalWorkedHoursText());
    }

    @Test
    void createQueuesWorkproofStatementRequest() {
        User user = User.register("create@test.com", "hashed", "Create User");
        Workplace workplace = Workplace.create(user, "Create Cafe", "Seoul", "Gate", 37.5, 127.0, 150);

        when(workplaceRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(workplace));
        when(documentGenerationRequestRepository.existsByUserIdAndDocumentTypeAndIdempotencyKey(
                1L,
                DocumentType.WORKPROOF_STATEMENT,
                "workproof-1"
        )).thenReturn(false);
        when(documentGenerationRequestRepository.saveAndFlush(any(DocumentGenerationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkproofDocumentAcceptedResult result = service.create(
                1L,
                new CreateWorkproofDocumentCommand(
                        DocumentType.WORKPROOF_STATEMENT,
                        2L,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        "workproof-1"
                )
        );

        assertEquals(DocumentType.WORKPROOF_STATEMENT, result.documentType());
        assertEquals(DocumentGenerationStatus.QUEUED, result.status());
    }

    @Test
    void rejectsInvalidPreviewDateRange() {
        ApiException exception = assertThrows(ApiException.class, () -> service.preview(
                1L,
                new WorkproofDocumentPreviewQuery(
                        2L,
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 1, 31)
                )
        ));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }
}
