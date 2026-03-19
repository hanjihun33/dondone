package com.workproofpay.backend.documents;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.documents.api.dto.request.CreateProofPackRequest;
import com.workproofpay.backend.documents.pdf.PdfRenderer;
import com.workproofpay.backend.documents.pdf.RenderedPdf;
import com.workproofpay.backend.documents.pdf.workproof.WorkProofPdfSnapshot;
import com.workproofpay.backend.documents.pdf.workproof.WorkProofPdfSnapshotAssembler;
import com.workproofpay.backend.documents.model.DocumentGenerationStatus;
import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentType;
import com.workproofpay.backend.documents.repo.DocumentGenerationRequestRepository;
import com.workproofpay.backend.documents.service.DocumentsService;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.wage.model.WageVerification;
import com.workproofpay.backend.wage.service.WageVerificationQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentsServiceTest {

    @Mock
    private DocumentGenerationRequestRepository documentGenerationRequestRepository;

    @Mock
    private WageVerificationQueryService wageVerificationQueryService;

    @Mock
    private WorkProofPdfSnapshotAssembler workProofPdfSnapshotAssembler;

    @Mock
    private PdfRenderer pdfRenderer;

    private DocumentsService documentsService;

    @BeforeEach
    void setUp() {
        documentsService = new DocumentsService(
                documentGenerationRequestRepository,
                wageVerificationQueryService,
                workProofPdfSnapshotAssembler,
                pdfRenderer
        );
    }

    @Test
    void mapsUniqueConstraintViolationToDuplicateRequestError() {
        long userId = 1L;
        long verificationId = 10L;
        String idempotencyKey = "proof-pack-1";
        WageVerification verification = mockVerification(verificationId);

        when(documentGenerationRequestRepository.existsByUserIdAndDocumentTypeAndIdempotencyKey(
                userId,
                DocumentType.PROOF_PACK,
                idempotencyKey
        )).thenReturn(false);
        when(wageVerificationQueryService.getOwnedVerification(userId, verificationId)).thenReturn(verification);
        when(documentGenerationRequestRepository.saveAndFlush(any(DocumentGenerationRequest.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"" +
                                "uk_document_generation_requests_user_type_key\""
                ));

        ApiException exception = assertThrows(ApiException.class, () ->
                documentsService.createProofPack(userId, idempotencyKey, new CreateProofPackRequest(verificationId))
        );

        assertEquals(ErrorCode.DOCUMENT_DUPLICATE_REQUEST, exception.getErrorCode());
    }

    @Test
    void rethrowsNonDuplicateDataIntegrityViolation() {
        long userId = 1L;
        long verificationId = 10L;
        String idempotencyKey = "proof-pack-1";
        WageVerification verification = mockVerification(verificationId);
        DataIntegrityViolationException exception = new DataIntegrityViolationException("other constraint");

        when(documentGenerationRequestRepository.existsByUserIdAndDocumentTypeAndIdempotencyKey(
                userId,
                DocumentType.PROOF_PACK,
                idempotencyKey
        )).thenReturn(false);
        when(wageVerificationQueryService.getOwnedVerification(userId, verificationId)).thenReturn(verification);
        when(documentGenerationRequestRepository.saveAndFlush(any(DocumentGenerationRequest.class)))
                .thenThrow(exception);

        DataIntegrityViolationException thrown = assertThrows(DataIntegrityViolationException.class, () ->
                documentsService.createProofPack(userId, idempotencyKey, new CreateProofPackRequest(verificationId))
        );

        assertSame(exception, thrown);
    }

    @Test
    void generatesProofPackPdfAndMarksRequestReady() {
        long userId = 1L;
        long documentId = 11L;
        User user = User.register("pdf@test.com", "hashed", "Pdf User");
        DocumentGenerationRequest request = DocumentGenerationRequest.queueProofPack(user, 300L, "2026-03", 7L, "proof-pack-key");
        WorkProofPdfSnapshot snapshot = sampleSnapshot();
        RenderedPdf renderedPdf = new RenderedPdf(new byte[]{1, 2, 3}, "proof-pack.pdf", "application/pdf", "abc123");

        when(documentGenerationRequestRepository.findByIdAndUserIdAndDocumentType(documentId, userId, DocumentType.PROOF_PACK))
                .thenReturn(Optional.of(request));
        when(documentGenerationRequestRepository.saveAndFlush(any(DocumentGenerationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workProofPdfSnapshotAssembler.assemble(any())).thenReturn(snapshot);
        when(pdfRenderer.render("pdf/workproof-statement", snapshot)).thenReturn(renderedPdf);

        RenderedPdf result = documentsService.generateProofPackPdf(userId, documentId);

        assertSame(renderedPdf, result);
        assertEquals(DocumentGenerationStatus.READY, request.getStatus());
        verify(workProofPdfSnapshotAssembler).assemble(any());
        verify(pdfRenderer).render("pdf/workproof-statement", snapshot);
    }

    @Test
    void marksRequestFailedWhenProofPackRenderingFails() {
        long userId = 1L;
        long documentId = 12L;
        User user = User.register("pdf-fail@test.com", "hashed", "Pdf Fail");
        DocumentGenerationRequest request = DocumentGenerationRequest.queueProofPack(user, 301L, "2026-03", 7L, "proof-pack-fail-key");
        WorkProofPdfSnapshot snapshot = sampleSnapshot();

        when(documentGenerationRequestRepository.findByIdAndUserIdAndDocumentType(documentId, userId, DocumentType.PROOF_PACK))
                .thenReturn(Optional.of(request));
        when(documentGenerationRequestRepository.saveAndFlush(any(DocumentGenerationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workProofPdfSnapshotAssembler.assemble(any())).thenReturn(snapshot);
        when(pdfRenderer.render("pdf/workproof-statement", snapshot))
                .thenThrow(new IllegalStateException("render failed"));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> documentsService.generateProofPackPdf(userId, documentId));

        assertEquals("render failed", thrown.getMessage());
        assertEquals(DocumentGenerationStatus.FAILED, request.getStatus());
    }

    private WageVerification mockVerification(long verificationId) {
        WageVerification verification = mock(WageVerification.class);
        User user = User.register("documents-service@test.com", "hashed", "Documents Service");

        when(verification.getUser()).thenReturn(user);
        when(verification.getId()).thenReturn(verificationId);
        when(verification.getMonth()).thenReturn("2026-03");
        when(verification.getWorkplaceId()).thenReturn(7L);
        return verification;
    }

    private WorkProofPdfSnapshot sampleSnapshot() {
        return new WorkProofPdfSnapshot(
                new WorkProofPdfSnapshot.DocumentMeta("PROOF_PACK", "PP-202603-000001", "workproof-statement-v1", "2026-03-19 10:00:00", "Asia/Seoul", Locale.KOREA.toLanguageTag()),
                new WorkProofPdfSnapshot.WorkerInfo(1L, "Test User", "test@example.com"),
                new WorkProofPdfSnapshot.WorkplaceInfo(7L, "DunDone Factory", "Seoul", "Main Gate"),
                new WorkProofPdfSnapshot.ContractInfo("시급", "10,030원", "10,030원", 480, 10560, "2026-03-01", "현재"),
                new WorkProofPdfSnapshot.PeriodInfo("2026-03-01", "2026-03-31", "2026-03"),
                new WorkProofPdfSnapshot.SummaryInfo(1, 1, 0, 0, 0, 540L, "9시간 00분"),
                List.of(new WorkProofPdfSnapshot.WorkProofRecordItem(
                        100L,
                        "2026-03-10",
                        "09:00",
                        "18:00",
                        540L,
                        "9시간 00분",
                        "Main Gate",
                        "Main Gate",
                        "REFLECTED",
                        "반영 완료",
                        "success",
                        false,
                        false,
                        "-",
                        "-",
                        "-",
                        0,
                        "2026-03-10 09:00:00",
                        "2026-03-10 18:00:00"
                )),
                List.of(),
                List.of("생성 테스트용 notice")
        );
    }
}
