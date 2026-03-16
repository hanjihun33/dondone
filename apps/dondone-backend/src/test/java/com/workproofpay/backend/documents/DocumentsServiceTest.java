package com.workproofpay.backend.documents;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.documents.api.dto.request.CreateProofPackRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentsServiceTest {

    @Mock
    private DocumentGenerationRequestRepository documentGenerationRequestRepository;

    @Mock
    private WageVerificationQueryService wageVerificationQueryService;

    private DocumentsService documentsService;

    @BeforeEach
    void setUp() {
        documentsService = new DocumentsService(documentGenerationRequestRepository, wageVerificationQueryService);
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

    private WageVerification mockVerification(long verificationId) {
        WageVerification verification = mock(WageVerification.class);
        User user = User.register("documents-service@test.com", "hashed", "Documents Service");

        when(verification.getUser()).thenReturn(user);
        when(verification.getId()).thenReturn(verificationId);
        when(verification.getMonth()).thenReturn("2026-03");
        when(verification.getWorkplaceId()).thenReturn(7L);
        return verification;
    }
}
