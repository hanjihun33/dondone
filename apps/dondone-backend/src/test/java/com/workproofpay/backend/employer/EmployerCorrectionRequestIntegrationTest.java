package com.workproofpay.backend.employer;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.correction.model.CorrectionDecisionAudit;
import com.workproofpay.backend.correction.model.CorrectionRequest;
import com.workproofpay.backend.correction.model.CorrectionRequestReasonCode;
import com.workproofpay.backend.correction.model.CorrectionReviewReasonCode;
import com.workproofpay.backend.correction.model.CorrectionRequestStatus;
import com.workproofpay.backend.correction.repo.CorrectionDecisionAuditRepository;
import com.workproofpay.backend.correction.repo.CorrectionRequestRepository;
import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.model.EmployerProfile;
import com.workproofpay.backend.employer.model.EmploymentMembership;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.employer.repo.EmployerProfileRepository;
import com.workproofpay.backend.employer.repo.EmploymentMembershipRepository;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.WorkProofAuditLog;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkProofAuditLogRepository;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:employer-correction-request;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "employer.signup-code-encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "remittance.wallet.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployerCorrectionRequestIntegrationTest {

    private static final DateTimeFormatter JSON_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private EmployerProfileRepository employerProfileRepository;

    @Autowired
    private EmploymentMembershipRepository employmentMembershipRepository;

    @Autowired
    private WorkProofRepository workProofRepository;

    @Autowired
    private WorkProofAuditLogRepository workProofAuditLogRepository;

    @Autowired
    private CorrectionRequestRepository correctionRequestRepository;

    @Autowired
    private CorrectionDecisionAuditRepository correctionDecisionAuditRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        correctionDecisionAuditRepository.deleteAll();
        correctionRequestRepository.deleteAll();
        workProofAuditLogRepository.deleteAll();
        workProofRepository.deleteAll();
        employmentMembershipRepository.deleteAll();
        employerProfileRepository.deleteAll();
        workplaceRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getCorrectionRequestsReturnsScopedQueueWithFilters() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/correction-requests")
                        .header("Authorization", bearer(fixture.employerUser()))
                        .queryParam("query", "late")
                        .queryParam("statuses", "PENDING")
                        .queryParam("page", "1")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.requests[0].requestId").value(fixture.approvableRequest().getId()))
                .andExpect(jsonPath("$.data.requests[0].workProofId").value(fixture.approvableWorkProof().getId()))
                .andExpect(jsonPath("$.data.requests[0].workerId").value(fixture.approvableWorker().getId()))
               .andExpect(jsonPath("$.data.requests[0].workerName").value("Approvable Worker"))
               .andExpect(jsonPath("$.data.requests[0].workerEmail").value("approvable-worker@acme.test"))
               .andExpect(jsonPath("$.data.requests[0].requestedClockInAt").value(formatDateTime(fixture.approvableRequest().getRequestedClockInAt())))
               .andExpect(jsonPath("$.data.requests[0].reasonCode").value("LATE_CLOCK_IN"))
               .andExpect(jsonPath("$.data.requests[0].reviewReasonCode").value("LATE_CLOCK_IN_AFTER_SCHEDULE"))
               .andExpect(jsonPath("$.data.requests[0].status").value("PENDING"));
    }

    @Test
    void getCorrectionRequestsKeepsNewestPendingRequestFirst() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/correction-requests")
                        .header("Authorization", bearer(fixture.employerUser()))
                        .queryParam("statuses", "PENDING")
                        .queryParam("page", "1")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.requests[0].requestId").value(fixture.rejectableRequest().getId()))
                .andExpect(jsonPath("$.data.requests[1].requestId").value(fixture.approvableRequest().getId()));
    }

    @Test
    void getCorrectionRequestReturnsScopedDetail() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/correction-requests/{requestId}", fixture.rejectableRequest().getId())
                        .header("Authorization", bearer(fixture.employerUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.requestId").value(fixture.rejectableRequest().getId()))
                .andExpect(jsonPath("$.data.workProofId").value(fixture.rejectableWorkProof().getId()))
               .andExpect(jsonPath("$.data.workerId").value(fixture.rejectableWorker().getId()))
               .andExpect(jsonPath("$.data.workerName").value("Rejectable Worker"))
               .andExpect(jsonPath("$.data.workerEmail").value("rejectable-worker@acme.test"))
               .andExpect(jsonPath("$.data.reasonCode").value("EARLY_CLOCK_OUT"))
               .andExpect(jsonPath("$.data.reviewReasonCode").value("EARLY_CLOCK_OUT_BEFORE_SCHEDULE"))
               .andExpect(jsonPath("$.data.reason").value("Manual correction request"))
               .andExpect(jsonPath("$.data.requestMemo").value("Detailed worker note"))
                .andExpect(jsonPath("$.data.attachmentCount").value(2))
                .andExpect(jsonPath("$.data.attachments[0].type").value("MEMO"))
                .andExpect(jsonPath("$.data.attachments[0].fileName").value("note.txt"))
                .andExpect(jsonPath("$.data.attachments[0].downloadAvailable").value(false))
                .andExpect(jsonPath("$.data.attachments[1].type").value("PHOTO"))
                .andExpect(jsonPath("$.data.attachments[1].fileName").value("photo.jpg"))
                .andExpect(jsonPath("$.data.attachments[1].downloadAvailable").value(false))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void approveCorrectionRequestUpdatesWorkProofAndAudits() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(post("/api/employer/correction-requests/{requestId}/approve", fixture.approvableRequest().getId())
                        .header("Authorization", bearer(fixture.employerUser()))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "decisionMemo": "Approved after evidence review"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.requestId").value(fixture.approvableRequest().getId()))
               .andExpect(jsonPath("$.data.status").value("APPROVED"))
               .andExpect(jsonPath("$.data.decisionByAccountId").value(fixture.employerUser().getId()))
               .andExpect(jsonPath("$.data.decisionByName").value("Acme HR"))
               .andExpect(jsonPath("$.data.recognizedClockInAt").value(formatDateTime(fixture.approvableRequest().getRequestedClockInAt())))
               .andExpect(jsonPath("$.data.recognizedClockOutAt").value(formatDateTime(fixture.approvableRequest().getRequestedClockOutAt())))
               .andExpect(jsonPath("$.data.decisionMemo").value("Approved after evidence review"));

        CorrectionRequest approvedRequest = correctionRequestRepository.findById(fixture.approvableRequest().getId()).orElseThrow();
        WorkProof updatedWorkProof = workProofRepository.findById(fixture.approvableWorkProof().getId()).orElseThrow();
        WorkProofAuditLog auditLog = workProofAuditLogRepository
                .findFirstByWorkProofIdOrderByCreatedAtDesc(fixture.approvableWorkProof().getId())
                .orElseThrow();
        List<CorrectionDecisionAudit> decisionAudits = correctionDecisionAuditRepository
                .findByCorrectionRequestIdOrderByCreatedAtDesc(fixture.approvableRequest().getId());

       assertThat(approvedRequest.getStatus()).isEqualTo(CorrectionRequestStatus.APPROVED);
       assertThat(approvedRequest.getDecisionByAccountId()).isEqualTo(fixture.employerUser().getId());
       assertThat(updatedWorkProof.getClockInAt()).isEqualTo(fixture.approvableRequest().getOriginalClockInAt());
       assertThat(updatedWorkProof.getClockOutAt()).isEqualTo(fixture.approvableRequest().getOriginalClockOutAt());
       assertThat(updatedWorkProof.resolveRecognizedClockInAt()).isEqualTo(fixture.approvableRequest().getRequestedClockInAt());
       assertThat(updatedWorkProof.resolveRecognizedClockOutAt()).isEqualTo(fixture.approvableRequest().getRequestedClockOutAt());
       assertThat(updatedWorkProof.getEditReason()).isEqualTo("Fix late subway arrival");
       assertThat(updatedWorkProof.getMemo()).isEqualTo("Subway delay memo");
       assertThat(updatedWorkProof.getAttachmentCount()).isEqualTo(1);
        assertThat(auditLog.getActorUserId()).isEqualTo(fixture.employerUser().getId());
        assertThat(auditLog.getBeforeClockInAt()).isEqualTo(fixture.approvableRequest().getOriginalClockInAt());
        assertThat(auditLog.getAfterClockInAt()).isEqualTo(fixture.approvableRequest().getRequestedClockInAt());
        assertThat(decisionAudits).hasSize(1);
        assertThat(decisionAudits.get(0).getBeforeStatus()).isEqualTo(CorrectionRequestStatus.PENDING);
        assertThat(decisionAudits.get(0).getAfterStatus()).isEqualTo(CorrectionRequestStatus.APPROVED);
        assertThat(decisionAudits.get(0).getDecisionMemo()).isEqualTo("Approved after evidence review");
    }

    @Test
    void rejectCorrectionRequestLeavesWorkProofUntouched() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(post("/api/employer/correction-requests/{requestId}/reject", fixture.rejectableRequest().getId())
                        .header("Authorization", bearer(fixture.employerUser()))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "decisionMemo": "Evidence is insufficient",
                                  "rejectReasonCode": "INSUFFICIENT_EVIDENCE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.requestId").value(fixture.rejectableRequest().getId()))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectReasonCode").value("INSUFFICIENT_EVIDENCE"))
                .andExpect(jsonPath("$.data.decisionByAccountId").value(fixture.employerUser().getId()))
                .andExpect(jsonPath("$.data.decisionMemo").value("Evidence is insufficient"));

        CorrectionRequest rejectedRequest = correctionRequestRepository.findById(fixture.rejectableRequest().getId()).orElseThrow();
        WorkProof untouchedWorkProof = workProofRepository.findById(fixture.rejectableWorkProof().getId()).orElseThrow();
        List<CorrectionDecisionAudit> decisionAudits = correctionDecisionAuditRepository
                .findByCorrectionRequestIdOrderByCreatedAtDesc(fixture.rejectableRequest().getId());

       assertThat(rejectedRequest.getStatus()).isEqualTo(CorrectionRequestStatus.REJECTED);
       assertThat(rejectedRequest.getRejectReasonCode()).isEqualTo("INSUFFICIENT_EVIDENCE");
       assertThat(untouchedWorkProof.getClockInAt()).isEqualTo(fixture.rejectableRequest().getOriginalClockInAt());
       assertThat(untouchedWorkProof.getClockOutAt()).isEqualTo(fixture.rejectableRequest().getOriginalClockOutAt());
       assertThat(untouchedWorkProof.resolveRecognizedClockInAt()).isEqualTo(fixture.rejectableRequest().getOriginalClockInAt());
       assertThat(untouchedWorkProof.resolveRecognizedClockOutAt()).isEqualTo(fixture.rejectableRequest().getOriginalClockOutAt());
       assertThat(workProofAuditLogRepository.countByWorkProofId(fixture.rejectableWorkProof().getId())).isZero();
        assertThat(decisionAudits).hasSize(1);
        assertThat(decisionAudits.get(0).getAfterStatus()).isEqualTo(CorrectionRequestStatus.REJECTED);
    }

    @Test
    void requestOutsideEmployerScopeIsForbidden() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/correction-requests/{requestId}", fixture.otherScopeRequest().getId())
                        .header("Authorization", bearer(fixture.employerUser())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void alreadyProcessedRequestCannotBeApprovedAgain() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(post("/api/employer/correction-requests/{requestId}/approve", fixture.processedRequest().getId())
                        .header("Authorization", bearer(fixture.employerUser()))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "decisionMemo": "Retry"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CORRECTION_REQUEST_ALREADY_PROCESSED"));
    }

    @Test
    void workerTokenCannotAccessEmployerCorrectionRequests() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/correction-requests")
                        .header("Authorization", bearer(fixture.approvableWorker())))
                .andExpect(status().isForbidden());
    }

    private Fixture createFixture() {
        Company company = companyRepository.save(Company.create("Acme Logistics", "ACME-SEOUL"));
        Company otherCompany = companyRepository.save(Company.create("Other Logistics", "OTHER-SEOUL"));
        User workplaceOwner = userRepository.save(User.register(
                "worker-owner@example.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker Owner"
        ));
        Workplace workplace = workplaceRepository.save(Workplace.create(
                workplaceOwner,
                company.getId(),
                "Seoul Hub",
                "Seoul Address 212",
                null,
                37.501274,
                127.039585,
                300
        ));
        Workplace otherWorkplace = workplaceRepository.save(Workplace.create(
                workplaceOwner,
                otherCompany.getId(),
                "Busan Hub",
                "Busan Address 48",
                null,
                35.171,
                129.131,
                300
        ));

        User employerUser = userRepository.save(User.registerEmployer(
                "manager@acme.test",
                passwordEncoder.encode("qweqwe123"),
                "Acme HR"
        ));
        employerProfileRepository.save(EmployerProfile.create(
                employerUser.getId(),
                company.getId(),
                workplace.getId(),
                "Acme HR"
        ));

        User approvableWorker = createScopedWorker("approvable-worker@acme.test", "Approvable Worker", company.getId(), workplace.getId());
        User rejectableWorker = createScopedWorker("rejectable-worker@acme.test", "Rejectable Worker", company.getId(), workplace.getId());
        User processedWorker = createScopedWorker("processed-worker@acme.test", "Processed Worker", company.getId(), workplace.getId());
        User otherScopeWorker = createScopedWorker("other-worker@other.test", "Other Scope Worker", otherCompany.getId(), otherWorkplace.getId());

       WorkProof approvableWorkProof = workProofRepository.save(completedRecord(
               approvableWorker,
               workplace,
               LocalDate.of(2026, 3, 17),
               9,
               20,
               18,
               10
       ));
       WorkProof rejectableWorkProof = workProofRepository.save(completedRecord(
               rejectableWorker,
               workplace,
               LocalDate.of(2026, 3, 16),
               9,
               0,
               17,
               40
       ));
        WorkProof processedWorkProof = workProofRepository.save(completedRecord(
                processedWorker,
                workplace,
                LocalDate.of(2026, 3, 15),
                8,
                45,
                17,
                45
        ));
        WorkProof otherScopeWorkProof = workProofRepository.save(completedRecord(
                otherScopeWorker,
                otherWorkplace,
                LocalDate.of(2026, 3, 15),
                9,
                0,
                18,
                0
        ));

       CorrectionRequest approvableRequest = CorrectionRequest.create(
               approvableWorkProof,
               approvableWorker.getId(),
               approvableWorker.getId(),
                company.getId(),
                workplace.getId(),
               approvableWorkProof.getWorkDate(),
               approvableWorkProof.getClockInAt(),
               approvableWorkProof.getClockOutAt(),
               LocalDateTime.of(2026, 3, 17, 9, 0),
               LocalDateTime.of(2026, 3, 17, 18, 0),
               CorrectionRequestReasonCode.LATE_CLOCK_IN,
               "Fix late subway arrival",
               "Subway delay memo",
               1,
               "[{\"type\":\"PHOTO\",\"fileName\":\"evidence.png\",\"fileRef\":\"storage://attachments/evidence.png\"}]"
       );
       approvableRequest.markNeedsReview(CorrectionReviewReasonCode.LATE_CLOCK_IN_AFTER_SCHEDULE);
       approvableRequest = correctionRequestRepository.save(approvableRequest);
       CorrectionRequest rejectableRequest = CorrectionRequest.create(
               rejectableWorkProof,
               rejectableWorker.getId(),
               rejectableWorker.getId(),
                company.getId(),
                workplace.getId(),
               rejectableWorkProof.getWorkDate(),
               rejectableWorkProof.getClockInAt(),
               rejectableWorkProof.getClockOutAt(),
               LocalDateTime.of(2026, 3, 16, 9, 0),
               LocalDateTime.of(2026, 3, 16, 18, 0),
               CorrectionRequestReasonCode.EARLY_CLOCK_OUT,
               "Manual correction request",
               "Detailed worker note",
               2,
               "[{\"type\":\"MEMO\",\"fileName\":\"note.txt\",\"fileRef\":\"storage://attachments/note.txt\"}," +
                       "{\"type\":\"PHOTO\",\"fileName\":\"photo.jpg\",\"fileRef\":\"storage://attachments/photo.jpg\"}]"
       );
       rejectableRequest.markNeedsReview(CorrectionReviewReasonCode.EARLY_CLOCK_OUT_BEFORE_SCHEDULE);
       rejectableRequest = correctionRequestRepository.save(rejectableRequest);
       CorrectionRequest processedRequest = CorrectionRequest.create(
               processedWorkProof,
               processedWorker.getId(),
                processedWorker.getId(),
                company.getId(),
                workplace.getId(),
                processedWorkProof.getWorkDate(),
               processedWorkProof.getClockInAt(),
               processedWorkProof.getClockOutAt(),
               LocalDateTime.of(2026, 3, 15, 9, 0),
               LocalDateTime.of(2026, 3, 15, 18, 0),
               CorrectionRequestReasonCode.OTHER,
               "Already handled",
               null,
               0,
                null
        );
        processedRequest.reject(employerUser.getId(), "Already reviewed", "DUPLICATE", LocalDateTime.of(2026, 3, 18, 9, 0));
        processedRequest = correctionRequestRepository.save(processedRequest);

       CorrectionRequest otherScopeRequest = correctionRequestRepository.save(CorrectionRequest.create(
               otherScopeWorkProof,
               otherScopeWorker.getId(),
                otherScopeWorker.getId(),
                otherCompany.getId(),
                otherWorkplace.getId(),
               otherScopeWorkProof.getWorkDate(),
               otherScopeWorkProof.getClockInAt(),
               otherScopeWorkProof.getClockOutAt(),
               LocalDateTime.of(2026, 3, 15, 9, 10),
               LocalDateTime.of(2026, 3, 15, 18, 10),
               CorrectionRequestReasonCode.OTHER,
               "Out of scope request",
               null,
               0,
                null
        ));

        return new Fixture(
                employerUser,
                approvableWorker,
                approvableWorkProof,
                approvableRequest,
                rejectableWorker,
                rejectableWorkProof,
                rejectableRequest,
                processedRequest,
                otherScopeRequest
        );
    }

    private User createScopedWorker(String email, String name, Long companyId, Long workplaceId) {
        User worker = userRepository.save(User.register(
                email,
                passwordEncoder.encode("qweqwe123"),
                name
        ));
        employmentMembershipRepository.save(EmploymentMembership.create(
                worker.getId(),
                companyId,
                workplaceId,
                LocalDate.now().minusDays(1)
        ));
        return worker;
    }

    private WorkProof completedRecord(User worker,
                                      Workplace workplace,
                                      LocalDate workDate,
                                      int checkInHour,
                                      int checkInMinute,
                                      int checkOutHour,
                                      int checkOutMinute) {
        WorkProof record = WorkProof.checkIn(
                worker,
                workplace,
                null,
                workDate.atTime(checkInHour, checkInMinute),
                LocalDateTime.now(),
                37.501274,
                127.039585,
                "Office"
        );
        record.completeCheckOut(
                workDate.atTime(checkOutHour, checkOutMinute),
                LocalDateTime.now(),
                37.501274,
                127.039585,
                "Office",
                false
        );
        return record;
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private String formatDateTime(LocalDateTime value) {
        return value.format(JSON_DATE_TIME);
    }

    private record Fixture(
            User employerUser,
            User approvableWorker,
            WorkProof approvableWorkProof,
            CorrectionRequest approvableRequest,
            User rejectableWorker,
            WorkProof rejectableWorkProof,
            CorrectionRequest rejectableRequest,
            CorrectionRequest processedRequest,
            CorrectionRequest otherScopeRequest
    ) {
    }
}
