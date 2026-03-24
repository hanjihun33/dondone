package com.workproofpay.backend.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.advance.model.AdvanceRequest;
import com.workproofpay.backend.advance.model.AdvancePayoutStatus;
import com.workproofpay.backend.advance.repo.AdvanceRequestRepository;
import com.workproofpay.backend.advance.repo.AdvancePayoutRepository;
import com.workproofpay.backend.auth.api.dto.request.LoginRequest;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.jobs.model.JobReferenceKind;
import com.workproofpay.backend.jobs.model.JobStatus;
import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.repo.JobRepository;
import com.workproofpay.backend.remittance.repo.UserWalletRepository;
import com.workproofpay.backend.workproof.model.WorkContract;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
import com.workproofpay.backend.workproof.repo.WorkProofAuditLogRepository;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-advance-request;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "employer.signup-code-encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "remittance.wallet.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAdvanceRequestIntegrationTest {

    private static final long APPROVED_REQUEST_ATOMIC = 80_000_000L;
    private static final long APPROVED_REQUEST_DISPLAY_KRW = 116_000L;
    private static final long REJECTED_REQUEST_ATOMIC = 65_000_000L;
    private static final long REJECTED_REQUEST_DISPLAY_KRW = 94_250L;

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
    private WorkContractRepository workContractRepository;

    @Autowired
    private WorkProofRepository workProofRepository;

    @Autowired
    private WorkProofAuditLogRepository workProofAuditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdvancePayoutRepository advancePayoutRepository;

    @Autowired
    private AdvanceRequestRepository advanceRequestRepository;

    @Autowired
    private UserWalletRepository userWalletRepository;

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        advancePayoutRepository.deleteAll();
        advanceRequestRepository.deleteAll();
        jobRepository.deleteAll();
        userWalletRepository.deleteAll();
        workProofAuditLogRepository.deleteAll();
        workProofRepository.deleteAll();
        workContractRepository.deleteAll();
        workplaceRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminCanListApproveAndRejectAdvanceRequests() throws Exception {
        User admin = userRepository.save(User.registerAdmin(
                "admin@dondone.local",
                passwordEncoder.encode("qweqwe123"),
                "Service Admin"
        ));
        User workerOne = userRepository.save(User.register(
                "worker.one@test.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker One"
        ));
        User workerTwo = userRepository.save(User.register(
                "worker.two@test.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker Two"
        ));

        Workplace firstWorkplace = seedAdvanceEligibleScenario(workerOne, "Alpha Logistics", "DN-ALPHA-1001");
        Workplace secondWorkplace = seedAdvanceEligibleScenario(workerTwo, "Beta Logistics", "DN-BETA-2002");

        String adminToken = loginAndReadAccessToken(new LoginRequest("admin@dondone.local", "qweqwe123"));
        String workerOneToken = loginAndReadAccessToken(new LoginRequest("worker.one@test.com", "qweqwe123"));
        String workerTwoToken = loginAndReadAccessToken(new LoginRequest("worker.two@test.com", "qweqwe123"));

        long approvedCandidateId = createAdvanceRequest(
                workerOneToken,
                firstWorkplace.getId(),
                APPROVED_REQUEST_ATOMIC,
                "2030-01-10T09:00:00",
                "advance-admin-1"
        );
        long rejectedCandidateId = createAdvanceRequest(
                workerTwoToken,
                secondWorkplace.getId(),
                REJECTED_REQUEST_ATOMIC,
                "2030-01-10T10:00:00",
                "advance-admin-2"
        );

        mockMvc.perform(get("/api/admin/advance/requests")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requests.length()").value(2))
                .andExpect(jsonPath("$.data.requests[0].workerName").value("Worker Two"))
                .andExpect(jsonPath("$.data.requests[0].companyName").value("Beta Logistics"))
                .andExpect(jsonPath("$.data.requests[0].workplaceName").value("Beta Logistics Site"))
                .andExpect(jsonPath("$.data.requests[0].assetSymbol").value("dUSDC"))
                .andExpect(jsonPath("$.data.requests[0].assetDecimals").value(6))
                .andExpect(jsonPath("$.data.requests[0].exchangeRateSnapshot").value(1450))
                .andExpect(jsonPath("$.data.requests[0].requestedAmountAtomic").value(REJECTED_REQUEST_ATOMIC))
                .andExpect(jsonPath("$.data.requests[0].requestedDisplayKrwAmount").value(REJECTED_REQUEST_DISPLAY_KRW))
                .andExpect(jsonPath("$.data.requests[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.requests[0].requestStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.requests[0].payoutStatus").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[0].payoutTxHash").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[0].payoutFailureReason").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[0].approvedAmountAtomic").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[1].workerName").value("Worker One"))
                .andExpect(jsonPath("$.data.requests[1].companyName").value("Alpha Logistics"))
                .andExpect(jsonPath("$.data.requests[1].assetSymbol").value("dUSDC"))
                .andExpect(jsonPath("$.data.requests[1].assetDecimals").value(6))
                .andExpect(jsonPath("$.data.requests[1].exchangeRateSnapshot").value(1450))
                .andExpect(jsonPath("$.data.requests[1].requestedAmountAtomic").value(APPROVED_REQUEST_ATOMIC))
                .andExpect(jsonPath("$.data.requests[1].requestedDisplayKrwAmount").value(APPROVED_REQUEST_DISPLAY_KRW))
                .andExpect(jsonPath("$.data.requests[1].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.requests[1].requestStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.requests[1].payoutStatus").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[1].payoutTxHash").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[1].payoutFailureReason").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[1].feeAmountAtomic").value(3448275))
                .andExpect(jsonPath("$.data.requests[1].feeDisplayKrwAmount").value(5000));

        mockMvc.perform(post("/api/admin/advance/requests/{requestId}/approve", approvedCandidateId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        var payout = advancePayoutRepository.findByAdvanceRequestId(approvedCandidateId).orElseThrow();
        assertThat(payout.getUserId()).isEqualTo(workerOne.getId());
        assertThat(payout.getWalletAddress()).matches("^0x[a-f0-9]{40}$");
        assertThat(payout.getAmountAtomic()).isEqualTo(APPROVED_REQUEST_ATOMIC);
        assertThat(payout.getAssetSymbol()).isEqualTo("dUSDC");
        assertThat(payout.getStatus()).isEqualTo(AdvancePayoutStatus.REQUESTED);
        assertThat(userWalletRepository.findById(workerOne.getId())).isPresent();

        assertThat(jobRepository.findByReferenceKindAndStatusInOrderByUpdatedAtDescIdDesc(
                JobReferenceKind.ADVANCE_PAYOUT,
                List.of(JobStatus.QUEUED),
                org.springframework.data.domain.PageRequest.of(0, 10)
        )).anySatisfy(job -> {
            assertThat(job.getJobType()).isEqualTo(JobType.SUBMIT_ADVANCE_PAYOUT);
            assertThat(job.getReferenceId()).isEqualTo(payout.getAdvancePayoutId());
        });

        mockMvc.perform(post("/api/admin/advance/requests/{requestId}/reject", rejectedCandidateId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/advance/requests")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requests[0].requestId").value(rejectedCandidateId))
                .andExpect(jsonPath("$.data.requests[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.data.requests[0].requestStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.requests[0].payoutStatus").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[0].payoutTxHash").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[0].payoutFailureReason").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[0].approvedAmountAtomic").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[0].reviewedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.requests[1].requestId").value(approvedCandidateId))
                .andExpect(jsonPath("$.data.requests[1].status").value("PAYING"))
                .andExpect(jsonPath("$.data.requests[1].requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.requests[1].payoutStatus").value("REQUESTED"))
                .andExpect(jsonPath("$.data.requests[1].payoutTxHash").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[1].payoutFailureReason").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[1].assetSymbol").value("dUSDC"))
                .andExpect(jsonPath("$.data.requests[1].assetDecimals").value(6))
                .andExpect(jsonPath("$.data.requests[1].approvedAmountAtomic").value(APPROVED_REQUEST_ATOMIC))
                .andExpect(jsonPath("$.data.requests[1].approvedDisplayKrwAmount").value(APPROVED_REQUEST_DISPLAY_KRW))
                .andExpect(jsonPath("$.data.requests[1].feeAmountAtomic").value(3448275))
                .andExpect(jsonPath("$.data.requests[1].feeDisplayKrwAmount").value(5000))
                .andExpect(jsonPath("$.data.requests[1].reviewedAt").isNotEmpty());

        mockMvc.perform(get("/api/advance/requests/{requestId}", approvedCandidateId)
                        .header("Authorization", "Bearer " + workerOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAYING"))
                .andExpect(jsonPath("$.data.requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.payoutStatus").value("REQUESTED"))
                .andExpect(jsonPath("$.data.approvedAmountAtomic").value(APPROVED_REQUEST_ATOMIC))
                .andExpect(jsonPath("$.data.approvedDisplayKrwAmount").value(APPROVED_REQUEST_DISPLAY_KRW));

        mockMvc.perform(get("/api/advance/requests/{requestId}", rejectedCandidateId)
                        .header("Authorization", "Bearer " + workerTwoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.requestStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.payoutStatus").value(nullValue()))
                .andExpect(jsonPath("$.data.approvedAmountAtomic").value(nullValue()))
                .andExpect(jsonPath("$.data.approvedDisplayKrwAmount").value(nullValue()));

        mockMvc.perform(post("/api/admin/advance/requests/{requestId}/approve", approvedCandidateId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADVANCE_REQUEST_ALREADY_PROCESSED"));
    }

    @Test
    void workerCannotAccessAdminAdvanceRequests() throws Exception {
        User worker = userRepository.save(User.register(
                "worker.only@test.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker Only"
        ));
        String workerToken = loginAndReadAccessToken(new LoginRequest("worker.only@test.com", "qweqwe123"));

        mockMvc.perform(get("/api/admin/advance/requests")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminResponseSeparatesSubmittedApprovedPayingPaidAndPayoutFailedStates() throws Exception {
        User admin = userRepository.save(User.registerAdmin(
                "admin-status@dondone.local",
                passwordEncoder.encode("qweqwe123"),
                "Service Admin"
        ));
        User worker = userRepository.save(User.register(
                "worker.status@test.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker Status"
        ));
        Workplace submittedWorkplace = seedAdvanceEligibleScenario(worker, "Gamma Logistics A", "DN-GAMMA-3003");
        Workplace approvedWorkplace = seedAdvanceEligibleScenario(worker, "Gamma Logistics B", "DN-GAMMA-3004");
        Workplace payingWorkplace = seedAdvanceEligibleScenario(worker, "Gamma Logistics C", "DN-GAMMA-3005");
        Workplace paidWorkplace = seedAdvanceEligibleScenario(worker, "Gamma Logistics D", "DN-GAMMA-3006");
        Workplace failedWorkplace = seedAdvanceEligibleScenario(worker, "Gamma Logistics E", "DN-GAMMA-3007");

        String adminToken = loginAndReadAccessToken(new LoginRequest("admin-status@dondone.local", "qweqwe123"));
        String workerToken = loginAndReadAccessToken(new LoginRequest("worker.status@test.com", "qweqwe123"));

        long submittedRequestId = createAdvanceRequest(workerToken, submittedWorkplace.getId(), 40_000_000L, "2030-01-10T09:00:00", "advance-admin-status-submitted");
        long approvedRequestId = createAdvanceRequest(workerToken, approvedWorkplace.getId(), 41_000_000L, "2030-01-10T10:00:00", "advance-admin-status-approved");
        long payingRequestId = createAdvanceRequest(workerToken, payingWorkplace.getId(), 42_000_000L, "2030-01-10T11:00:00", "advance-admin-status-paying");
        long paidRequestId = createAdvanceRequest(workerToken, paidWorkplace.getId(), 43_000_000L, "2030-01-10T12:00:00", "advance-admin-status-paid");
        long failedRequestId = createAdvanceRequest(workerToken, failedWorkplace.getId(), 44_000_000L, "2030-01-10T13:00:00", "advance-admin-status-failed");

        approveRequest(approvedRequestId);
        approveRequest(payingRequestId);
        approveRequest(paidRequestId);
        approveRequest(failedRequestId);

        attachPayout(payingRequestId, AdvancePayoutStatus.REQUESTED);
        attachPayout(paidRequestId, AdvancePayoutStatus.CONFIRMED);
        attachPayout(failedRequestId, AdvancePayoutStatus.TIMED_OUT);

        mockMvc.perform(get("/api/admin/advance/requests")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requests.length()").value(5))
                .andExpect(jsonPath("$.data.requests[0].requestId").value(failedRequestId))
                .andExpect(jsonPath("$.data.requests[0].status").value("PAYOUT_FAILED"))
                .andExpect(jsonPath("$.data.requests[0].requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.requests[0].payoutStatus").value("TIMED_OUT"))
                .andExpect(jsonPath("$.data.requests[0].assetSymbol").value("dUSDC"))
                .andExpect(jsonPath("$.data.requests[0].assetDecimals").value(6))
                .andExpect(jsonPath("$.data.requests[0].requestedAmountAtomic").value(44_000_000L))
                .andExpect(jsonPath("$.data.requests[0].requestedDisplayKrwAmount").value(63_800L))
                .andExpect(jsonPath("$.data.requests[0].approvedAmountAtomic").value(44_000_000L))
                .andExpect(jsonPath("$.data.requests[0].approvedDisplayKrwAmount").value(63_800L))
                .andExpect(jsonPath("$.data.requests[0].payoutTxHash").value("0x" + "%064x".formatted(failedRequestId)))
                .andExpect(jsonPath("$.data.requests[0].payoutFailureReason").value("receipt timeout"))
                .andExpect(jsonPath("$.data.requests[1].requestId").value(paidRequestId))
                .andExpect(jsonPath("$.data.requests[1].status").value("PAID"))
                .andExpect(jsonPath("$.data.requests[1].requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.requests[1].payoutStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.requests[1].requestedAmountAtomic").value(43_000_000L))
                .andExpect(jsonPath("$.data.requests[1].requestedDisplayKrwAmount").value(62_350L))
                .andExpect(jsonPath("$.data.requests[1].approvedAmountAtomic").value(43_000_000L))
                .andExpect(jsonPath("$.data.requests[1].approvedDisplayKrwAmount").value(62_350L))
                .andExpect(jsonPath("$.data.requests[1].payoutTxHash").value("0x" + "%064x".formatted(paidRequestId)))
                .andExpect(jsonPath("$.data.requests[1].payoutFailureReason").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[2].requestId").value(payingRequestId))
                .andExpect(jsonPath("$.data.requests[2].status").value("PAYING"))
                .andExpect(jsonPath("$.data.requests[2].requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.requests[2].payoutStatus").value("REQUESTED"))
                .andExpect(jsonPath("$.data.requests[2].requestedAmountAtomic").value(42_000_000L))
                .andExpect(jsonPath("$.data.requests[2].requestedDisplayKrwAmount").value(60_900L))
                .andExpect(jsonPath("$.data.requests[2].approvedAmountAtomic").value(42_000_000L))
                .andExpect(jsonPath("$.data.requests[2].approvedDisplayKrwAmount").value(60_900L))
                .andExpect(jsonPath("$.data.requests[2].payoutTxHash").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[2].payoutFailureReason").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[3].requestId").value(approvedRequestId))
                .andExpect(jsonPath("$.data.requests[3].status").value("APPROVED"))
                .andExpect(jsonPath("$.data.requests[3].requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.requests[3].payoutStatus").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[3].requestedAmountAtomic").value(41_000_000L))
                .andExpect(jsonPath("$.data.requests[3].requestedDisplayKrwAmount").value(59_450L))
                .andExpect(jsonPath("$.data.requests[3].approvedAmountAtomic").value(41_000_000L))
                .andExpect(jsonPath("$.data.requests[3].approvedDisplayKrwAmount").value(59_450L))
                .andExpect(jsonPath("$.data.requests[3].payoutTxHash").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[3].payoutFailureReason").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[4].requestId").value(submittedRequestId))
                .andExpect(jsonPath("$.data.requests[4].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.requests[4].requestStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.requests[4].payoutStatus").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[4].requestedAmountAtomic").value(40_000_000L))
                .andExpect(jsonPath("$.data.requests[4].requestedDisplayKrwAmount").value(58_000L))
                .andExpect(jsonPath("$.data.requests[4].approvedAmountAtomic").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[4].approvedDisplayKrwAmount").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[4].payoutTxHash").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[4].payoutFailureReason").value(nullValue()));
    }

    private Workplace seedAdvanceEligibleScenario(User worker, String companyName, String companyCode) {
        Company company = companyRepository.save(Company.create(companyName, companyCode));
        Workplace workplace = workplaceRepository.save(Workplace.create(
                worker,
                company.getId(),
                companyName + " Site",
                companyName + " Address",
                "Front gate",
                36.1,
                128.3,
                1_000
        ));
        company.bindDefaultWorkplace(workplace.getId());
        companyRepository.save(company);

        WorkContract contract = workContractRepository.save(WorkContract.activate(
                workplace,
                WorkProofPayUnit.HOURLY,
                BigDecimal.valueOf(10_000),
                null,
                null,
                BigDecimal.valueOf(10_000),
                nextMonth().atDay(1)
        ));

        for (int day = 1; day <= 10; day++) {
            LocalDateTime checkIn = nextMonth().atDay(day).atTime(9, 0);
            WorkProof record = WorkProof.checkIn(
                    worker,
                    workplace,
                    contract,
                    checkIn,
                    checkIn.minusMinutes(1),
                    36.1,
                    128.3,
                    "Front gate"
            );
            record.completeCheckOut(
                    checkIn.plusHours(8),
                    checkIn.plusHours(8).plusMinutes(1),
                    36.1,
                    128.3,
                    "Front gate",
                    false
            );
            workProofRepository.save(record);
        }

        return workplace;
    }

    private long createAdvanceRequest(
            String accessToken,
            Long workplaceId,
            long requestedAmountAtomic,
            String requestedAt,
            String idempotencyKey
    ) throws Exception {
        String requestJson = """
                {
                  "workplaceId": %d,
                  "requestedAmountAtomic": %d,
                  "requestedAt": "%s"
                }
                """.formatted(workplaceId, requestedAmountAtomic, requestedAt);

        MvcResult createResult = mockMvc.perform(post("/api/advance/requests")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andReturn();

        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        return body.path("data").path("requestId").asLong();
    }

    private String loginAndReadAccessToken(LoginRequest request) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginBody.path("data").path("accessToken").asText();
    }

    private YearMonth nextMonth() {
        return YearMonth.now().plusMonths(1);
    }

    private void approveRequest(long requestId) {
        AdvanceRequest request = advanceRequestRepository.findById(requestId).orElseThrow();
        request.approve(999L);
        advanceRequestRepository.save(request);
    }

    private void attachPayout(long requestId, AdvancePayoutStatus targetStatus) {
        AdvanceRequest request = advanceRequestRepository.findById(requestId).orElseThrow();
        var payout = advancePayoutRepository.findByAdvanceRequestId(requestId).orElseGet(() -> advancePayoutRepository.save(
                com.workproofpay.backend.advance.model.AdvancePayout.request(
                        java.util.UUID.randomUUID().toString().replace("-", ""),
                        requestId,
                        request.getUser().getId(),
                        "0x" + "%040x".formatted(requestId),
                        request.getApprovedAmountAtomic(),
                        request.getAssetSymbol(),
                        "advance-admin-payout-" + requestId
                )
        ));

        switch (targetStatus) {
            case REQUESTED -> {
            }
            case CONFIRMED -> {
                payout.markSigned("0x" + "%064x".formatted(requestId), "signed-" + requestId);
                payout.markBroadcasted();
                payout.markConfirmed();
            }
            case TIMED_OUT -> {
                payout.markSigned("0x" + "%064x".formatted(requestId), "signed-" + requestId);
                payout.markBroadcasted();
                payout.markTimedOut("receipt timeout");
            }
            default -> throw new IllegalArgumentException("Unsupported payout status for admin response test: " + targetStatus);
        }

        advancePayoutRepository.save(payout);
    }
}
