package com.workproofpay.backend.workproof;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.correction.model.CorrectionRequest;
import com.workproofpay.backend.correction.model.CorrectionRequestStatus;
import com.workproofpay.backend.correction.repo.CorrectionRequestRepository;
import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.model.EmploymentMembership;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.employer.repo.EmploymentMembershipRepository;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.support.PostgresIntegrationTestSupport;
import com.workproofpay.backend.wage.repo.WageDepositRepository;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkProofCorrectionRequest;
import com.workproofpay.backend.workproof.api.dto.request.UpdateWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.WorkProofAttachmentMetadataRequest;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.WorkProofAuditLog;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkProofAuditLogRepository;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkProofIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private EmploymentMembershipRepository employmentMembershipRepository;

    @Autowired
    private WorkProofRepository workProofRepository;

    @Autowired
    private CorrectionRequestRepository correctionRequestRepository;

    @Autowired
    private WageDepositRepository wageDepositRepository;

    @Autowired
    private WorkProofAuditLogRepository workProofAuditLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        correctionRequestRepository.deleteAll();
        workProofAuditLogRepository.deleteAll();
        wageDepositRepository.deleteAll();
        workProofRepository.deleteAll();
        employmentMembershipRepository.deleteAll();
        workplaceRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createListDetailAndMonthlySummary() throws Exception {
        User user = userRepository.save(User.register("workproof@test.com", "hashed", "Tester"));
        String token = tokenFor(user);

        CreateWorkProofRequest reflectedOne = new CreateWorkProofRequest(
                LocalDate.of(2026, 3, 3),
                LocalDateTime.of(2026, 3, 3, 9, 0),
                LocalDateTime.of(2026, 3, 3, 19, 30),
                LocalDateTime.of(2026, 3, 3, 8, 59),
                LocalDateTime.of(2026, 3, 3, 19, 31),
                37.1,
                127.1,
                37.11,
                127.11,
                "regular shift",
                null,
                0
        );
        CreateWorkProofRequest reflectedTwo = new CreateWorkProofRequest(
                LocalDate.of(2026, 3, 4),
                LocalDateTime.of(2026, 3, 4, 21, 30),
                LocalDateTime.of(2026, 3, 4, 23, 30),
                LocalDateTime.of(2026, 3, 4, 21, 29),
                LocalDateTime.of(2026, 3, 4, 23, 31),
                37.2,
                127.2,
                37.21,
                127.21,
                "night shift",
                "corrected from paper roster",
                1
        );
        CreateWorkProofRequest pending = new CreateWorkProofRequest(
                LocalDate.of(2026, 3, 6),
                LocalDateTime.of(2026, 3, 6, 9, 0),
                null,
                LocalDateTime.of(2026, 3, 6, 8, 59),
                null,
                37.3,
                127.3,
                null,
                null,
                "clock-in only",
                null,
                0
        );

        String firstResponse = mockMvc.perform(post("/api/workproof")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reflectedOne)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.financialStatus").value("REFLECTED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(post("/api/workproof")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reflectedTwo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.edited").value(true));

        mockMvc.perform(post("/api/workproof")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pending)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.financialStatus").value("PENDING"));

        Long workProofId = objectMapper.readTree(firstResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/workproof")
                        .header("Authorization", bearer(token))
                        .param("yearMonth", "2026-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));

        mockMvc.perform(get("/api/workproof/{workProofId}", workProofId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(workProofId))
                .andExpect(jsonPath("$.data.workedMinutes").value(630));

        mockMvc.perform(get("/api/workproof/monthly-summary")
                        .header("Authorization", bearer(token))
                        .param("yearMonth", "2026-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalWorkDays").value(2))
                .andExpect(jsonPath("$.data.totalWorkedMinutes").value(750))
                .andExpect(jsonPath("$.data.totalOvertimeMinutes").value(150))
                .andExpect(jsonPath("$.data.totalNightMinutes").value(90))
                .andExpect(jsonPath("$.data.editedRecordCount").value(1))
                .andExpect(jsonPath("$.data.reflectedRecordCount").value(2))
                .andExpect(jsonPath("$.data.pendingRecordCount").value(1));
    }

    @Test
    void updateExistingWorkProofMarksItEditedAndReturnsUpdatedTimes() throws Exception {
        User user = userRepository.save(User.register("editor@test.com", "hashed", "Editor"));
        String token = tokenFor(user);

        CreateWorkProofRequest originalRequest = new CreateWorkProofRequest(
                LocalDate.of(2026, 3, 8),
                LocalDateTime.of(2026, 3, 8, 9, 0),
                LocalDateTime.of(2026, 3, 8, 18, 0),
                LocalDateTime.of(2026, 3, 8, 8, 58),
                LocalDateTime.of(2026, 3, 8, 18, 1),
                37.0,
                127.0,
                37.0,
                127.0,
                "before edit",
                null,
                0
        );

        String created = mockMvc.perform(post("/api/workproof")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(originalRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long workProofId = objectMapper.readTree(created).path("data").path("id").asLong();

        UpdateWorkProofRequest updateRequest = new UpdateWorkProofRequest(
                LocalDateTime.of(2026, 3, 8, 9, 30),
                LocalDateTime.of(2026, 3, 8, 18, 30),
                "출근/퇴근 탭을 늦게 눌렀어요",
                "paper roster checked",
                null,
                List.of(
                        new WorkProofAttachmentMetadataRequest(
                                WorkProofAttachmentMetadataRequest.AttachmentType.PHOTO,
                                "roster.jpg",
                                "storage://workproof/roster.jpg"
                        ),
                        new WorkProofAttachmentMetadataRequest(
                                WorkProofAttachmentMetadataRequest.AttachmentType.MEMO,
                                "note.txt",
                                "memo://late-tap"
                        )
                )
        );

        mockMvc.perform(patch("/api/workproof/{workProofId}", workProofId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(workProofId))
                .andExpect(jsonPath("$.data.clockInAt").value("2026-03-08T09:30:00"))
                .andExpect(jsonPath("$.data.clockOutAt").value("2026-03-08T18:30:00"))
                .andExpect(jsonPath("$.data.editReason").value("출근/퇴근 탭을 늦게 눌렀어요"))
                .andExpect(jsonPath("$.data.memo").value("paper roster checked"))
                .andExpect(jsonPath("$.data.attachmentCount").value(2))
                .andExpect(jsonPath("$.data.edited").value(true))
                .andExpect(jsonPath("$.data.deviceClockInAt").doesNotExist())
                .andExpect(jsonPath("$.data.deviceClockOutAt").doesNotExist())
                .andExpect(jsonPath("$.data.serverClockInAt").doesNotExist())
                .andExpect(jsonPath("$.data.serverClockOutAt").doesNotExist())
                .andExpect(jsonPath("$.data.clockInLatitude").doesNotExist())
                .andExpect(jsonPath("$.data.clockInLongitude").doesNotExist())
                .andExpect(jsonPath("$.data.clockOutLatitude").doesNotExist())
                .andExpect(jsonPath("$.data.clockOutLongitude").doesNotExist())
                .andExpect(jsonPath("$.data.workedMinutes").value(540))
                .andExpect(jsonPath("$.data.overtimeMinutes").value(60))
                .andExpect(jsonPath("$.data.nightMinutes").value(0))
                .andExpect(jsonPath("$.data.financialStatus").value("REFLECTED"));

        Assertions.assertEquals(1L, workProofAuditLogRepository.countByWorkProofId(workProofId));
        WorkProofAuditLog auditLog = workProofAuditLogRepository.findFirstByWorkProofIdOrderByCreatedAtDesc(workProofId)
                .orElseThrow();
        Assertions.assertEquals(LocalDateTime.of(2026, 3, 8, 9, 0), auditLog.getBeforeClockInAt());
        Assertions.assertEquals(LocalDateTime.of(2026, 3, 8, 18, 0), auditLog.getBeforeClockOutAt());
        Assertions.assertEquals(LocalDateTime.of(2026, 3, 8, 9, 30), auditLog.getAfterClockInAt());
        Assertions.assertEquals(LocalDateTime.of(2026, 3, 8, 18, 30), auditLog.getAfterClockOutAt());
        Assertions.assertNull(auditLog.getBeforeEditReason());
        Assertions.assertEquals("출근/퇴근 탭을 늦게 눌렀어요", auditLog.getAfterEditReason());
        Assertions.assertEquals("before edit", auditLog.getBeforeMemo());
        Assertions.assertEquals("paper roster checked", auditLog.getAfterMemo());
        Assertions.assertEquals(0, auditLog.getBeforeAttachmentCount());
        Assertions.assertEquals(2, auditLog.getAfterAttachmentCount());
        Assertions.assertNull(auditLog.getBeforeAttachmentMetadataJson());
        Assertions.assertTrue(auditLog.getAfterAttachmentMetadataJson().contains("\"type\":\"PHOTO\""));
        Assertions.assertTrue(auditLog.getAfterAttachmentMetadataJson().contains("\"fileRef\":\"storage://workproof/roster.jpg\""));
        WorkProof savedWorkProof = workProofRepository.findById(workProofId).orElseThrow();
        Assertions.assertEquals(2, savedWorkProof.getAttachmentCount());
        Assertions.assertTrue(savedWorkProof.getAttachmentMetadataJson().contains("\"type\":\"PHOTO\""));
    }

    @Test
    void createCorrectionRequestForEmployerScopedWorkProof() throws Exception {
        User worker = userRepository.save(User.register("correction-worker@test.com", "hashed", "Correction Worker"));
        Company company = companyRepository.save(Company.create("Acme Logistics", "ACME-SEOUL"));
        Workplace workplace = workplaceRepository.save(Workplace.create(
                worker,
                company.getId(),
                "Seoul Hub",
                "Seoul Address 212",
                null,
                37.501274,
                127.039585,
                300
        ));
        employmentMembershipRepository.save(EmploymentMembership.create(
                worker.getId(),
                company.getId(),
                workplace.getId(),
                LocalDate.of(2026, 3, 1)
        ));

        WorkProof workProof = WorkProof.checkIn(
                worker,
                workplace,
                null,
                LocalDateTime.of(2026, 3, 8, 9, 0),
                LocalDateTime.of(2026, 3, 8, 9, 1),
                37.501274,
                127.039585,
                "Front door"
        );
        workProof.completeCheckOut(
                LocalDateTime.of(2026, 3, 8, 18, 0),
                LocalDateTime.of(2026, 3, 8, 18, 1),
                37.501274,
                127.039585,
                "Front door",
                false
        );
        workProof = workProofRepository.save(workProof);
        String token = tokenFor(worker);

        CreateWorkProofCorrectionRequest request = new CreateWorkProofCorrectionRequest(
                LocalDateTime.of(2026, 3, 8, 9, 20),
                LocalDateTime.of(2026, 3, 8, 18, 10),
                "Late subway arrival",
                "Train delay screenshot attached",
                null,
                List.of(
                        new WorkProofAttachmentMetadataRequest(
                                WorkProofAttachmentMetadataRequest.AttachmentType.PHOTO,
                                "subway.png",
                                "storage://corrections/subway.png"
                        )
                )
        );

        mockMvc.perform(post("/api/workproof/{workProofId}/correction-requests", workProof.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.workProofId").value(workProof.getId()))
                .andExpect(jsonPath("$.data.reason").value("Late subway arrival"))
                .andExpect(jsonPath("$.data.memo").value("Train delay screenshot attached"))
                .andExpect(jsonPath("$.data.attachmentCount").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        CorrectionRequest savedRequest = correctionRequestRepository.findAll().get(0);
        Assertions.assertEquals(worker.getId(), savedRequest.getRequestedByAccountId());
        Assertions.assertEquals(company.getId(), savedRequest.getCompanyId());
        Assertions.assertEquals(workplace.getId(), savedRequest.getWorkplaceId());
        Assertions.assertEquals("Train delay screenshot attached", savedRequest.getRequestMemo());
        Assertions.assertTrue(savedRequest.getAttachmentMetadataJson().contains("\"fileRef\":\"storage://corrections/subway.png\""));
    }

    @Test
    void rejectsCorrectionRequestWithoutEmployerScopedWorkProofOrWhenPendingAlreadyExists() throws Exception {
        User worker = userRepository.save(User.register("pending-correction@test.com", "hashed", "Pending Worker"));
        String token = tokenFor(worker);

        CreateWorkProofRequest personalRequest = new CreateWorkProofRequest(
                LocalDate.of(2026, 3, 12),
                LocalDateTime.of(2026, 3, 12, 9, 0),
                LocalDateTime.of(2026, 3, 12, 18, 0),
                LocalDateTime.of(2026, 3, 12, 8, 59),
                LocalDateTime.of(2026, 3, 12, 18, 1),
                37.0,
                127.0,
                37.0,
                127.0,
                "personal record",
                null,
                0
        );

        String createdPersonal = mockMvc.perform(post("/api/workproof")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(personalRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long personalWorkProofId = objectMapper.readTree(createdPersonal).path("data").path("id").asLong();

        CreateWorkProofCorrectionRequest correctionRequest = new CreateWorkProofCorrectionRequest(
                LocalDateTime.of(2026, 3, 12, 9, 10),
                LocalDateTime.of(2026, 3, 12, 18, 10),
                "Need correction",
                null,
                0,
                null
        );

        mockMvc.perform(post("/api/workproof/{workProofId}/correction-requests", personalWorkProofId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctionRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CORRECTION_REQUEST_SCOPE_NOT_READY"));

        Company company = companyRepository.save(Company.create("Acme Logistics", "ACME-WORKER"));
        Workplace workplace = workplaceRepository.save(Workplace.create(
                worker,
                company.getId(),
                "Employer Hub",
                "Employer Address 1",
                null,
                37.5,
                127.0,
                300
        ));
        employmentMembershipRepository.save(EmploymentMembership.create(
                worker.getId(),
                company.getId(),
                workplace.getId(),
                LocalDate.of(2026, 3, 1)
        ));

        WorkProof scopedWorkProof = WorkProof.checkIn(
                worker,
                workplace,
                null,
                LocalDateTime.of(2026, 3, 13, 9, 0),
                LocalDateTime.of(2026, 3, 13, 9, 1),
                37.5,
                127.0,
                "Front door"
        );
        scopedWorkProof.completeCheckOut(
                LocalDateTime.of(2026, 3, 13, 18, 0),
                LocalDateTime.of(2026, 3, 13, 18, 1),
                37.5,
                127.0,
                "Front door",
                false
        );
        scopedWorkProof = workProofRepository.save(scopedWorkProof);

        correctionRequestRepository.save(CorrectionRequest.create(
                scopedWorkProof,
                worker.getId(),
                worker.getId(),
                company.getId(),
                workplace.getId(),
                scopedWorkProof.getWorkDate(),
                scopedWorkProof.getClockInAt(),
                scopedWorkProof.getClockOutAt(),
                LocalDateTime.of(2026, 3, 13, 9, 10),
                LocalDateTime.of(2026, 3, 13, 18, 10),
                "Existing pending correction",
                null,
                0,
                null
        ));

        mockMvc.perform(post("/api/workproof/{workProofId}/correction-requests", scopedWorkProof.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWorkProofCorrectionRequest(
                                LocalDateTime.of(2026, 3, 13, 9, 30),
                                LocalDateTime.of(2026, 3, 13, 18, 30),
                                "Second correction attempt",
                                null,
                                0,
                                null
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CORRECTION_REQUEST_PENDING_EXISTS"));

        WorkProof unchangedWorkProof = WorkProof.checkIn(
                worker,
                workplace,
                null,
                LocalDateTime.of(2026, 3, 14, 9, 0),
                LocalDateTime.of(2026, 3, 14, 9, 1),
                37.5,
                127.0,
                "Front door"
        );
        unchangedWorkProof.completeCheckOut(
                LocalDateTime.of(2026, 3, 14, 18, 0),
                LocalDateTime.of(2026, 3, 14, 18, 1),
                37.5,
                127.0,
                "Front door",
                false
        );
        unchangedWorkProof = workProofRepository.save(unchangedWorkProof);

        mockMvc.perform(post("/api/workproof/{workProofId}/correction-requests", unchangedWorkProof.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWorkProofCorrectionRequest(
                                unchangedWorkProof.getClockInAt(),
                                unchangedWorkProof.getClockOutAt(),
                                "No actual change",
                                null,
                                0,
                                null
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CORRECTION_REQUEST_NO_CHANGES"));
    }

    @Test
    void updateAllowsClearingMemoWithBlankString() throws Exception {
        User user = userRepository.save(User.register("clear@test.com", "hashed", "Clear"));
        String token = tokenFor(user);

        CreateWorkProofRequest originalRequest = new CreateWorkProofRequest(
                LocalDate.of(2026, 3, 9),
                LocalDateTime.of(2026, 3, 9, 9, 0),
                LocalDateTime.of(2026, 3, 9, 18, 0),
                LocalDateTime.of(2026, 3, 9, 8, 59),
                LocalDateTime.of(2026, 3, 9, 18, 1),
                37.0,
                127.0,
                37.0,
                127.0,
                "memo to clear",
                null,
                0
        );

        String created = mockMvc.perform(post("/api/workproof")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(originalRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long workProofId = objectMapper.readTree(created).path("data").path("id").asLong();

        UpdateWorkProofRequest updateRequest = new UpdateWorkProofRequest(
                LocalDateTime.of(2026, 3, 9, 9, 5),
                LocalDateTime.of(2026, 3, 9, 18, 0),
                "메모를 비워둡니다",
                "",
                0,
                List.of()
        );

        mockMvc.perform(patch("/api/workproof/{workProofId}", workProofId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memo").value(""));

        WorkProofAuditLog auditLog = workProofAuditLogRepository.findFirstByWorkProofIdOrderByCreatedAtDesc(workProofId)
                .orElseThrow();
        Assertions.assertEquals("memo to clear", auditLog.getBeforeMemo());
        Assertions.assertEquals("", auditLog.getAfterMemo());
    }

    @Test
    void updateWithCountOnlyClearsAttachmentMetadataButKeepsAuditHistory() throws Exception {
        User user = userRepository.save(User.register("meta@test.com", "hashed", "Meta"));
        String token = tokenFor(user);

        CreateWorkProofRequest originalRequest = new CreateWorkProofRequest(
                LocalDate.of(2026, 3, 11),
                LocalDateTime.of(2026, 3, 11, 9, 0),
                LocalDateTime.of(2026, 3, 11, 18, 0),
                LocalDateTime.of(2026, 3, 11, 8, 58),
                LocalDateTime.of(2026, 3, 11, 18, 1),
                37.0,
                127.0,
                37.0,
                127.0,
                "seed",
                null,
                0
        );

        String created = mockMvc.perform(post("/api/workproof")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(originalRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long workProofId = objectMapper.readTree(created).path("data").path("id").asLong();

        UpdateWorkProofRequest withMetadata = new UpdateWorkProofRequest(
                LocalDateTime.of(2026, 3, 11, 9, 10),
                LocalDateTime.of(2026, 3, 11, 18, 10),
                "증거를 첨부했어요",
                null,
                1,
                List.of(
                        new WorkProofAttachmentMetadataRequest(
                                WorkProofAttachmentMetadataRequest.AttachmentType.DOCUMENT,
                                "timesheet.pdf",
                                "storage://workproof/timesheet.pdf"
                        )
                )
        );

        mockMvc.perform(patch("/api/workproof/{workProofId}", workProofId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withMetadata)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachmentCount").value(1));

        UpdateWorkProofRequest countOnly = new UpdateWorkProofRequest(
                LocalDateTime.of(2026, 3, 11, 9, 15),
                LocalDateTime.of(2026, 3, 11, 18, 15),
                "개수만 다시 적었어요",
                null,
                2,
                null
        );

        mockMvc.perform(patch("/api/workproof/{workProofId}", workProofId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(countOnly)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachmentCount").value(2));

        WorkProof savedWorkProof = workProofRepository.findById(workProofId).orElseThrow();
        Assertions.assertEquals(2, savedWorkProof.getAttachmentCount());
        Assertions.assertNull(savedWorkProof.getAttachmentMetadataJson());

        WorkProofAuditLog latestAuditLog = workProofAuditLogRepository.findFirstByWorkProofIdOrderByCreatedAtDesc(workProofId)
                .orElseThrow();
        Assertions.assertTrue(latestAuditLog.getBeforeAttachmentMetadataJson().contains("\"fileRef\":\"storage://workproof/timesheet.pdf\""));
        Assertions.assertNull(latestAuditLog.getAfterAttachmentMetadataJson());
    }

    @Test
    void requiresAuthAndValidationAndOwnership() throws Exception {
        User owner = userRepository.save(User.register("owner@test.com", "hashed", "Owner"));
        User other = userRepository.save(User.register("other@test.com", "hashed", "Other"));
        String ownerToken = tokenFor(owner);
        String otherToken = tokenFor(other);

        CreateWorkProofRequest invalidRequest = new CreateWorkProofRequest(
                LocalDate.of(2026, 3, 3),
                LocalDateTime.of(2026, 3, 3, 18, 0),
                LocalDateTime.of(2026, 3, 3, 9, 0),
                LocalDateTime.of(2026, 3, 3, 18, 0),
                LocalDateTime.of(2026, 3, 3, 9, 0),
                37.0,
                127.0,
                37.0,
                127.0,
                null,
                null,
                0
        );

        mockMvc.perform(post("/api/workproof")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"));

        mockMvc.perform(post("/api/workproof")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WORKPROOF_TIME"))
                .andExpect(jsonPath("$.message").value("clockOutAt must be after clockInAt"));

        CreateWorkProofRequest validRequest = new CreateWorkProofRequest(
                LocalDate.of(2026, 3, 7),
                LocalDateTime.of(2026, 3, 7, 9, 0),
                LocalDateTime.of(2026, 3, 7, 17, 0),
                LocalDateTime.of(2026, 3, 7, 8, 59),
                LocalDateTime.of(2026, 3, 7, 17, 1),
                37.0,
                127.0,
                37.0,
                127.0,
                null,
                null,
                0
        );

        String created = mockMvc.perform(post("/api/workproof")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long workProofId = objectMapper.readTree(created).path("data").path("id").asLong();

        mockMvc.perform(get("/api/workproof/{workProofId}", workProofId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORKPROOF_NOT_FOUND"));

        mockMvc.perform(get("/api/workproof")
                        .header("Authorization", bearer(ownerToken))
                        .param("yearMonth", "2026-031"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.details[*].message").value(hasItem("yearMonth must be exactly 7 characters")));

        UpdateWorkProofRequest invalidUpdateRequest = new UpdateWorkProofRequest(
                LocalDateTime.of(2026, 3, 7, 18, 0),
                LocalDateTime.of(2026, 3, 7, 9, 0),
                "연장근무가 있었어요",
                null,
                1,
                null
        );

        mockMvc.perform(patch("/api/workproof/{workProofId}", workProofId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdateRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(patch("/api/workproof/{workProofId}", workProofId)
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORKPROOF_NOT_FOUND"));

        mockMvc.perform(patch("/api/workproof/{workProofId}", workProofId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WORKPROOF_TIME"));

        UpdateWorkProofRequest missingReasonRequest = new UpdateWorkProofRequest(
                LocalDateTime.of(2026, 3, 7, 9, 0),
                LocalDateTime.of(2026, 3, 7, 18, 0),
                " ",
                null,
                0,
                null
        );

        mockMvc.perform(patch("/api/workproof/{workProofId}", workProofId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingReasonRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.details[0].message").value("editReason is required"));

        UpdateWorkProofRequest invalidDateRequest = new UpdateWorkProofRequest(
                LocalDateTime.of(2026, 3, 8, 9, 0),
                LocalDateTime.of(2026, 3, 8, 18, 0),
                "기록이 누락됐어요",
                null,
                0,
                null
        );

        mockMvc.perform(patch("/api/workproof/{workProofId}", workProofId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WORK_DATE"));

        UpdateWorkProofRequest invalidAttachmentRequest = new UpdateWorkProofRequest(
                LocalDateTime.of(2026, 3, 7, 9, 0),
                LocalDateTime.of(2026, 3, 7, 18, 0),
                "기록이 누락됐어요",
                null,
                -1,
                null
        );

        mockMvc.perform(patch("/api/workproof/{workProofId}", workProofId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAttachmentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.details[0].message").value("attachmentCount must be 0 or greater"));

        CreateWorkProofRequest pendingRequest = new CreateWorkProofRequest(
                LocalDate.of(2026, 3, 10),
                LocalDateTime.of(2026, 3, 10, 9, 0),
                null,
                LocalDateTime.of(2026, 3, 10, 8, 58),
                null,
                37.0,
                127.0,
                null,
                null,
                null,
                null,
                0
        );

        String pendingCreated = mockMvc.perform(post("/api/workproof")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pendingRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long pendingWorkProofId = objectMapper.readTree(pendingCreated).path("data").path("id").asLong();

        UpdateWorkProofRequest pendingEditRequest = new UpdateWorkProofRequest(
                LocalDateTime.of(2026, 3, 10, 9, 10),
                LocalDateTime.of(2026, 3, 10, 18, 0),
                "기록이 누락됐어요",
                null,
                0,
                null
        );

        mockMvc.perform(patch("/api/workproof/{workProofId}", pendingWorkProofId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pendingEditRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORKPROOF_EDIT_NOT_ALLOWED"));

        UpdateWorkProofRequest mismatchedAttachmentRequest = new UpdateWorkProofRequest(
                LocalDateTime.of(2026, 3, 7, 9, 10),
                LocalDateTime.of(2026, 3, 7, 18, 10),
                "증거를 다시 정리했어요",
                null,
                2,
                List.of(
                        new WorkProofAttachmentMetadataRequest(
                                WorkProofAttachmentMetadataRequest.AttachmentType.DOCUMENT,
                                "timesheet.pdf",
                                "storage://workproof/timesheet.pdf"
                        )
                )
        );

        mockMvc.perform(patch("/api/workproof/{workProofId}", workProofId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchedAttachmentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.message").value("attachmentCount must match attachments size"));
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
