package com.workproofpay.backend.documents.service;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentGenerationStatus;
import com.workproofpay.backend.documents.model.DocumentType;
import com.workproofpay.backend.documents.repo.DocumentGenerationRequestRepository;
import com.workproofpay.backend.employer.model.EmploymentMembership;
import com.workproofpay.backend.employer.model.EmploymentMembershipStatus;
import com.workproofpay.backend.employer.repo.EmploymentMembershipRepository;
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
    private UserRepository userRepository;

    @Mock
    private WorkplaceRepository workplaceRepository;

    @Mock
    private WorkProofRepository workProofRepository;

    @Mock
    private EmploymentMembershipRepository employmentMembershipRepository;

    private DefaultWorkproofDocumentService service;

    @BeforeEach
    void setUp() {
        service = new DefaultWorkproofDocumentService(
                documentGenerationRequestRepository,
                userRepository,
                workplaceRepository,
                workProofRepository,
                employmentMembershipRepository
        );
    }

    @Test
    void previewSummarizesPeriodRecords() {
        User user = User.register("preview@test.com", "hashed", "Preview User");
        setField(user, User.class, "id", 1L);
        Workplace workplace = Workplace.create(user, "Preview Cafe", "Seoul", "Gate", 37.5, 127.0, 150);
        setField(workplace, Workplace.class, "id", 2L);
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
                LocalDateTime.of(2026, 1, 10, 18, 23),
                "Correction",
                "Updated memo",
                2,
                "{\"attachments\":[]}"
        );
        reflected.updateRecognizedTimes(
                LocalDateTime.of(2026, 1, 10, 9, 0),
                LocalDateTime.of(2026, 1, 10, 18, 0)
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

        when(workplaceRepository.findById(2L)).thenReturn(Optional.of(workplace));
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
        setField(user, User.class, "id", 1L);
        Workplace workplace = Workplace.create(user, "Create Cafe", "Seoul", "Gate", 37.5, 127.0, 150);
        setField(workplace, Workplace.class, "id", 2L);

        when(workplaceRepository.findById(2L)).thenReturn(Optional.of(workplace));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentGenerationRequestRepository.existsByUserIdAndDocumentTypeAndIdempotencyKey(
                1L,
                DocumentType.WORKPROOF_STATEMENT,
                "workproof-1"
        )).thenReturn(false);
        when(documentGenerationRequestRepository.saveAndFlush(any(DocumentGenerationRequest.class)))
                .thenAnswer(invocation -> {
                    DocumentGenerationRequest saved = invocation.getArgument(0);
                    setId(saved, 42L);
                    return saved;
                });

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
        assertEquals("/api/documents/42/download", result.documentUrl());
    }

    @Test
    void createQueuesWorkproofStatementForWorkerEvenWhenWorkplaceOwnedByEmployer() {
        User employer = User.registerEmployer("boss@test.com", "hashed", "Boss");
        setField(employer, User.class, "id", 9L);
        User worker = User.register("worker@test.com", "hashed", "Worker");
        setField(worker, User.class, "id", 1L);
        Workplace workplace = Workplace.create(employer, 10L, "Member Cafe", "Seoul", "Gate", 37.5, 127.0, 150);
        setField(workplace, Workplace.class, "id", 2L);

        when(workplaceRepository.findById(2L)).thenReturn(Optional.of(workplace));
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(employmentMembershipRepository.findActiveWorkerMembershipByScope(
                1L,
                10L,
                2L,
                EmploymentMembershipStatus.ACTIVE,
                LocalDate.now()
        )).thenReturn(List.of(EmploymentMembership.create(1L, 10L, 2L, LocalDate.now().minusDays(10))));
        when(documentGenerationRequestRepository.existsByUserIdAndDocumentTypeAndIdempotencyKey(
                1L,
                DocumentType.WORKPROOF_STATEMENT,
                "workproof-2"
        )).thenReturn(false);
        when(documentGenerationRequestRepository.saveAndFlush(any(DocumentGenerationRequest.class)))
                .thenAnswer(invocation -> {
                    DocumentGenerationRequest saved = invocation.getArgument(0);
                    setId(saved, 43L);
                    return saved;
                });

        WorkproofDocumentAcceptedResult result = service.create(
                1L,
                new CreateWorkproofDocumentCommand(
                        DocumentType.WORKPROOF_STATEMENT,
                        2L,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        "workproof-2"
                )
        );

        assertEquals(DocumentType.WORKPROOF_STATEMENT, result.documentType());
        assertEquals(DocumentGenerationStatus.QUEUED, result.status());
        assertEquals("/api/documents/43/download", result.documentUrl());
    }

    @Test
    void previewAllowsMembershipAccessibleWorkplace() {
        User employer = User.registerEmployer("boss@test.com", "hashed", "Boss");
        setField(employer, User.class, "id", 9L);
        User worker = User.register("worker@test.com", "hashed", "Worker");
        setField(worker, User.class, "id", 1L);
        Workplace workplace = Workplace.create(employer, 10L, "Member Cafe", "Seoul", "Gate", 37.5, 127.0, 150);
        setField(workplace, Workplace.class, "id", 2L);
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
                worker,
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

        when(workplaceRepository.findById(2L)).thenReturn(Optional.of(workplace));
        when(employmentMembershipRepository.findActiveWorkerMembershipByScope(
                1L,
                10L,
                2L,
                EmploymentMembershipStatus.ACTIVE,
                LocalDate.now()
        )).thenReturn(List.of(EmploymentMembership.create(1L, 10L, 2L, LocalDate.now().minusDays(10))));
        when(workProofRepository.findByUserIdAndWorkplaceIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(
                1L,
                2L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        )).thenReturn(List.of(reflected));

        WorkproofDocumentPreviewResult result = service.preview(
                1L,
                new WorkproofDocumentPreviewQuery(
                        2L,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31)
                )
        );

        assertEquals("Member Cafe", result.workplaceName());
        assertEquals(1, result.totalRecordCount());
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

    private void setId(DocumentGenerationRequest request, Long id) {
        try {
            var field = DocumentGenerationRequest.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(request, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to set document request id for test", e);
        }
    }

    private void setField(Object target, Class<?> type, String fieldName, Object value) {
        try {
            var field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to set %s on %s for test".formatted(fieldName, type.getSimpleName()), e);
        }
    }
}
