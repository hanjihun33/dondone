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
        User user = userRepository.saveAndFlush(User.register("pdf-assembler@test.com", "hashed", "Assembler User", "010-1111-2222"));
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
                LocalDateTime.of(2026, 3, 10, 18, 23),
                "Correction",
                "Updated memo",
                2,
                "{\"attachments\":[]}"
        );
        reflected.updateRecognizedTimes(
                LocalDateTime.of(2026, 3, 10, 9, 0),
                LocalDateTime.of(2026, 3, 10, 18, 0)
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
        assertThat(snapshot.worker().phoneNumber()).isEqualTo("010-1111-2222");
        assertThat(snapshot.workplace().name()).isEqualTo("Proof Pack Cafe");
        assertThat(snapshot.statement().title()).isEqualTo("근무 기록 문서");
        assertThat(snapshot.statement().subtitle()).contains("출퇴근 기록");
        assertThat(snapshot.period().periodLabel()).isEqualTo("대상 기간: 2026-03-01 ~ 2026-03-31");
        assertThat(snapshot.summary().totalWorkDayCount()).isEqualTo(2);
        assertThat(snapshot.summary().totalWorkDayCountLabel()).isEqualTo("2일");
        assertThat(snapshot.summary().editedCount()).isEqualTo(1);
        assertThat(snapshot.summary().issueCount()).isEqualTo(1);
        assertThat(snapshot.summary().issueCountLabel()).isEqualTo("1건");
        assertThat(snapshot.summary().totalWorkedMinutes()).isEqualTo(1_140L);
        assertThat(snapshot.records()).hasSize(2);
        assertThat(snapshot.audits()).hasSize(1);
        assertThat(snapshot.records())
                .anyMatch(item -> item.clockInAt().equals("09:00") && item.clockOutAt().equals("18:00"));
        assertThat(snapshot.records())
                .anyMatch(item -> item.remarks().contains("수정 기록 있음"))
                .anyMatch(item -> item.remarks().contains("기록 확인 필요"));
        assertThat(snapshot.audits().get(0).changeSummary()).contains("출근 09:00→09:10");
        assertThat(snapshot.audits().get(0).workDate()).isEqualTo("2026-03-10");
        assertThat(snapshot.audits().get(0).editReason()).isEqualTo("Correction");
    }

    @Test
    void assemblesWorkproofStatementSnapshotFromPeriodRequest() {
        User user = userRepository.saveAndFlush(User.register("pdf-statement@test.com", "hashed", "Statement User", "010-3333-4444"));
        Workplace workplace = workplaceRepository.saveAndFlush(Workplace.create(
                user,
                "Statement Cafe",
                "Seoul Somewhere 2",
                "Side door",
                37.5,
                127.0,
                150
        ));
        WorkContract contract = workContractRepository.saveAndFlush(WorkContract.activate(
                workplace,
                WorkProofPayUnit.HOURLY,
                BigDecimal.valueOf(11_000),
                480,
                10_560,
                BigDecimal.valueOf(11_000),
                LocalDate.of(2026, 1, 1)
        ));

        WorkProof record = WorkProof.checkIn(
                user,
                workplace,
                contract,
                LocalDateTime.of(2026, 2, 1, 9, 0),
                LocalDateTime.of(2026, 2, 1, 9, 1),
                37.5,
                127.0,
                "Side door"
        );
        record.completeCheckOut(
                LocalDateTime.of(2026, 2, 1, 18, 0),
                LocalDateTime.of(2026, 2, 1, 18, 1),
                37.5,
                127.0,
                "Side door",
                false
        );
        workProofRepository.saveAndFlush(record);

        DocumentGenerationRequest request = documentGenerationRequestRepository.saveAndFlush(
                DocumentGenerationRequest.queueWorkproofStatement(
                        user,
                        workplace.getId(),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 2, 23),
                        "workproof-statement-key"
                )
        );

        WorkProofPdfSnapshot snapshot = assembler.assemble(new WorkProofPdfAssembleCommand(
                user.getId(),
                request.getId(),
                request.getRequestId(),
                workplace.getId(),
                request.getStartDate(),
                request.getEndDate(),
                ZoneId.of("Asia/Seoul"),
                Locale.KOREA
        ));

        assertThat(snapshot.meta().documentType()).isEqualTo("WORKPROOF_STATEMENT");
        assertThat(snapshot.meta().documentNumber()).startsWith("WS-");
        assertThat(snapshot.statement().title()).isEqualTo("근무 기록 문서");
        assertThat(snapshot.period().startDate()).isEqualTo("2026-01-01");
        assertThat(snapshot.period().endDate()).isEqualTo("2026-02-23");
        assertThat(snapshot.period().yearMonth()).isEqualTo("2026-01-01 ~ 2026-02-23");
        assertThat(snapshot.period().periodLabel()).isEqualTo("대상 기간: 2026-01-01 ~ 2026-02-23");
        assertThat(snapshot.summary().totalWorkDayCount()).isEqualTo(1);
        assertThat(snapshot.summary().totalWorkDayCountLabel()).isEqualTo("1일");
        assertThat(snapshot.records()).hasSize(1);
        assertThat(snapshot.worker().phoneNumber()).isEqualTo("010-3333-4444");
        assertThat(snapshot.records().get(0).remarks()).isEqualTo("-");
    }

    @Test
    void keepsHistoricalWorkplaceInfoAfterWorkplaceSettingsChange() {
        User user = userRepository.saveAndFlush(User.register("pdf-snapshot@test.com", "hashed", "Snapshot User"));
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

        WorkProof record = WorkProof.checkIn(
                user,
                workplace,
                contract,
                LocalDateTime.of(2026, 3, 12, 9, 0),
                LocalDateTime.of(2026, 3, 12, 9, 1),
                37.5,
                127.0,
                "Front door"
        );
        record.completeCheckOut(
                LocalDateTime.of(2026, 3, 12, 18, 0),
                LocalDateTime.of(2026, 3, 12, 18, 1),
                37.5,
                127.0,
                "Front door",
                false
        );
        workProofRepository.saveAndFlush(record);

        workplace.updateEmployerSettings(
                "Seoul Updated 99",
                "Back door",
                37.6,
                127.1,
                500,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                user.getId()
        );
        workplaceRepository.saveAndFlush(workplace);

        DocumentGenerationRequest request = documentGenerationRequestRepository.saveAndFlush(
                DocumentGenerationRequest.queueProofPack(user, 500L, "2026-03", workplace.getId(), "proof-pack-snapshot-key")
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

        assertThat(snapshot.workplace().address()).isEqualTo("Seoul Somewhere 1");
        assertThat(snapshot.workplace().name()).isEqualTo("Proof Pack Cafe");
    }
}
