package com.workproofpay.backend.documents.repo;

import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentGenerationRequestRepository extends JpaRepository<DocumentGenerationRequest, Long> {

    boolean existsByUserIdAndDocumentTypeAndIdempotencyKey(Long userId, DocumentType documentType, String idempotencyKey);

    Optional<DocumentGenerationRequest> findByRequestIdAndUserId(String requestId, Long userId);

    Optional<DocumentGenerationRequest> findByIdAndUserId(Long id, Long userId);

    Optional<DocumentGenerationRequest> findByIdAndUserIdAndDocumentType(Long id, Long userId, DocumentType documentType);

    Optional<DocumentGenerationRequest> findFirstByUserIdAndWageVerificationIdAndDocumentTypeOrderByCreatedAtDesc(
            Long userId,
            Long wageVerificationId,
            DocumentType documentType
    );
}
