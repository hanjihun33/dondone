package com.workproofpay.backend.shared;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.claim.model.ClaimPreparation;
import com.workproofpay.backend.claim.model.ClaimPreparationTone;
import com.workproofpay.backend.claim.repo.ClaimPreparationRepository;
import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.repo.DocumentGenerationRequestRepository;
import com.workproofpay.backend.support.PostgresIntegrationTestSupport;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuditingIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClaimPreparationRepository claimPreparationRepository;

    @Autowired
    private DocumentGenerationRequestRepository documentGenerationRequestRepository;

    @Autowired
    private WorkProofRepository workProofRepository;

    @BeforeEach
    void setUp() {
        documentGenerationRequestRepository.deleteAll();
        claimPreparationRepository.deleteAll();
        workProofRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void setsCreatedAtForAppendOnlyEntity() {
        User user = userRepository.saveAndFlush(User.register("append-only@test.com", "hashed", "AppendOnly"));

        ClaimPreparation claimPreparation = claimPreparationRepository.saveAndFlush(
                ClaimPreparation.ready(user, 100L, 200L, "ko-KR", ClaimPreparationTone.DEFAULT, "요약")
        );

        assertThat(claimPreparation.getCreatedAt()).isNotNull();
    }

    @Test
    void updatesLastModifiedAtForMutableEntity() throws InterruptedException {
        User user = userRepository.saveAndFlush(User.register("mutable@test.com", "hashed", "Mutable"));

        WorkProof workProof = workProofRepository.saveAndFlush(
                WorkProof.record(
                        user,
                        LocalDate.of(2026, 3, 1),
                        LocalDateTime.of(2026, 3, 1, 9, 0),
                        LocalDateTime.of(2026, 3, 1, 18, 0),
                        LocalDateTime.of(2026, 3, 1, 9, 0),
                        LocalDateTime.of(2026, 3, 1, 18, 0),
                        LocalDateTime.of(2026, 3, 1, 18, 1),
                        37.0,
                        127.0,
                        37.0,
                        127.0,
                        "초기 메모",
                        "초기 사유",
                        0
                )
        );

        LocalDateTime initialUpdatedAt = workProof.getUpdatedAt();
        Thread.sleep(5L);

        workProof.updateTimes(
                LocalDateTime.of(2026, 3, 1, 9, 10),
                LocalDateTime.of(2026, 3, 1, 18, 10),
                "수정 사유",
                "수정 메모",
                1,
                "{\"attachments\":[]}"
        );
        WorkProof updatedWorkProof = workProofRepository.saveAndFlush(workProof);

        assertThat(updatedWorkProof.getCreatedAt()).isNotNull();
        assertThat(updatedWorkProof.getUpdatedAt()).isNotNull();
        assertThat(updatedWorkProof.getUpdatedAt()).isAfter(initialUpdatedAt);
    }

    @Test
    void keepsRequestIdGenerationAlongsideAuditing() {
        User user = userRepository.saveAndFlush(User.register("document@test.com", "hashed", "Document"));

        DocumentGenerationRequest request = documentGenerationRequestRepository.saveAndFlush(
                DocumentGenerationRequest.queueProofPack(user, 300L, "2026-03", 400L, "proof-pack-key")
        );

        assertThat(request.getRequestId()).isNotBlank();
        assertThat(request.getCreatedAt()).isNotNull();
        assertThat(request.getUpdatedAt()).isNotNull();
    }
}
