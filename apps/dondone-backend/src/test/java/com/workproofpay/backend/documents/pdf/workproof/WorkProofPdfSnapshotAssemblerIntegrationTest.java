package com.workproofpay.backend.documents.pdf.workproof;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.repo.DocumentGenerationRequestRepository;
import com.workproofpay.backend.support.PostgresIntegrationTestSupport;
import com.workproofpay.backend.workproof.model.WorkContract;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.WorkProofAuditLog;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
import com.workproofpay.backend.workproof.repo.WorkProofAuditLogRepository;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WorkProofPdfSnapshotAssemblerIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private WorkProofPdfSnapshotAssembler assembler;

    @Autowired
    private DocumentGenerationRequestRepository documentGenerationRequestRepository;

    @Autowired
    private WorkProofAuditLogRepository workProofAuditLogRepository;

    @Autowired
    private WorkProofRepository workProofRepository;

    @Autowired
    private WorkContractRepository workContractRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        documentGenerationRequestRepository.deleteAll();
        workProofAuditLogRepository.deleteAll();
        workProofRepository.deleteAll();
        workContractRepository.deleteAll();
        workplaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void assemblesProofPackSnapshotFromOwnedRecords() {
        User user = userRepository.saveAndFlush(User.register("pdf-assembler@test.com", "hashed", "Assembler User"));
        Workplace workplace = workplaceRepository.saveAndFlush(Workplace.create(
                user,
                "Proof Pack Cafe",
                "Seoul Somewhere 1",
                "Front door",
                37.5,
                127.0,
                150
        ));
        WorkContract contract = workContractRepository.saveAndFlush(WorkContract.activate(
                workplace,
                WorkProofPayUnit.HOURLY,
                BigDecimal.valueOf(12_000),
                480,
                10_560,
                BigDecimal.valueOf(12_000),
                LocalDate.of(2026, 3, 1)
        ));

        WorkProof reflected = WorkProof.checkIn(
                user,
                workplace,
                contract,
                LocalDateTime.of(2026, 3, 10, 9, 0),
                LocalDateTime.of(2026, 3, 10, 9, 1),
                37.5,
                127.0,
                "Front door"
        );
        reflected.completeCheckOut(
                LocalDateTime.of(2026, 3, 10, 18, 0),
                LocalDateTime.of(2026, 3, 10, 18, 1),
                37.5,
                127.0,
                "Front door",
                false
        );
        reflected.updateTimes(
                LocalDateTime.of(2026, 3, 10, 9, 10),
                LocalDateTime.of(2026, 3, 10, 18, 10),
                "Correction",
                "Updated memo",
                2,
                "{\"attachments\":[]}"
        );
        reflected = workProofRepository.saveAndFlush(reflected);

        WorkProof review = WorkProof.checkIn(
                user,
                workplace,
                contract,
                LocalDateTime.of(2026, 3, 11, 9, 0),
                LocalDateTime.of(2026, 3, 11, 9, 1),
                37.5,
                127.0,
                "Front door"
        );
        review.completeCheckOut(
                LocalDateTime.of(2026, 3, 11, 18, 0),
                LocalDateTime.of(2026, 3, 11, 18, 1),
                37.51,
                127.01,
                "Parking",
                true
        );
        review = workProofRepository.saveAndFlush(review);

        workProofAuditLogRepository.saveAndFlush(WorkProofAuditLog.record(
                reflected,
                user.getId(),
                LocalDateTime.of(2026, 3, 10, 9, 0),
                LocalDateTime.of(2026, 3, 10, 18, 0),
                LocalDateTime.of(2026, 3, 10, 9, 10),
                LocalDateTime.of(2026, 3, 10, 18, 10),
                null,
                "Correction",
                null,
                "Updated memo",
                0,
                2,
                null,
                "{\"attachments\":[]}"
        ));

        DocumentGenerationRequest request = documentGenerationRequestRepository.saveAndFlush(
                DocumentGenerationRequest.queueProofPack(user, 500L, "2026-03", workplace.getId(), "proof-pack-assembler-key")
        );

        WorkProofPdfSnapshot snapshot = assembler.assemble(new WorkProofPdfAssembleCommand(
                user.getId(),
                request.getId(),
                request.getRequestId(),
                workplace.getId(),
                null,
                null,
                ZoneId.of("Asia/Seoul"),
                Locale.KOREA
        ));

        assertThat(snapshot.worker().name()).isEqualTo("Assembler User");
        assertThat(snapshot.workplace().name()).isEqualTo("Proof Pack Cafe");
        assertThat(snapshot.contract().payUnit()).isEqualTo("시급");
        assertThat(snapshot.summary().totalRecordCount()).isEqualTo(2);
        assertThat(snapshot.summary().reflectedCount()).isEqualTo(1);
        assertThat(snapshot.summary().needsReviewCount()).isEqualTo(1);
        assertThat(snapshot.summary().editedCount()).isEqualTo(1);
        assertThat(snapshot.summary().totalAttachmentCount()).isEqualTo(2);
        assertThat(snapshot.summary().totalWorkedMinutes()).isEqualTo(1_140L);
        assertThat(snapshot.records()).hasSize(2);
        assertThat(snapshot.audits()).hasSize(1);
        assertThat(snapshot.notices())
                .anyMatch(notice -> notice.contains("검토 필요"))
                .anyMatch(notice -> notice.contains("허용 반경 밖"))
                .anyMatch(notice -> notice.contains("수정된 기록"));
        assertThat(snapshot.records())
                .anyMatch(item -> item.financialStatus().equals("NEEDS_REVIEW"))
                .anyMatch(WorkProofPdfSnapshot.WorkProofRecordItem::edited);
    }
}
