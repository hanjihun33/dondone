package com.workproofpay.backend.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.auth.api.dto.request.LoginRequest;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.repo.CompanyRepository;
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
    private static final long APPROVED_REQUEST_REFERENCE_KRW = 116_000L;
    private static final long REJECTED_REQUEST_ATOMIC = 65_000_000L;
    private static final long REJECTED_REQUEST_REFERENCE_KRW = 94_250L;

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

    @BeforeEach
    void setUp() {
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
                .andExpect(jsonPath("$.data.requests[0].requestedAmountAtomic").value(REJECTED_REQUEST_ATOMIC))
                .andExpect(jsonPath("$.data.requests[0].requestedReferenceKrw").value(REJECTED_REQUEST_REFERENCE_KRW))
                .andExpect(jsonPath("$.data.requests[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.requests[0].approvedAmountAtomic").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[1].workerName").value("Worker One"))
                .andExpect(jsonPath("$.data.requests[1].companyName").value("Alpha Logistics"))
                .andExpect(jsonPath("$.data.requests[1].requestedAmountAtomic").value(APPROVED_REQUEST_ATOMIC))
                .andExpect(jsonPath("$.data.requests[1].requestedReferenceKrw").value(APPROVED_REQUEST_REFERENCE_KRW))
                .andExpect(jsonPath("$.data.requests[1].status").value("SUBMITTED"));

        mockMvc.perform(post("/api/admin/advance/requests/{requestId}/approve", approvedCandidateId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/advance/requests/{requestId}/reject", rejectedCandidateId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/advance/requests")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requests[0].requestId").value(rejectedCandidateId))
                .andExpect(jsonPath("$.data.requests[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.data.requests[0].approvedAmountAtomic").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[0].reviewedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.requests[1].requestId").value(approvedCandidateId))
                .andExpect(jsonPath("$.data.requests[1].status").value("APPROVED"))
                .andExpect(jsonPath("$.data.requests[1].approvedAmountAtomic").value(APPROVED_REQUEST_ATOMIC))
                .andExpect(jsonPath("$.data.requests[1].approvedReferenceKrw").value(APPROVED_REQUEST_REFERENCE_KRW))
                .andExpect(jsonPath("$.data.requests[1].reviewedAt").isNotEmpty());

        mockMvc.perform(get("/api/advance/requests/{requestId}", approvedCandidateId)
                        .header("Authorization", "Bearer " + workerOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedAmountAtomic").value(APPROVED_REQUEST_ATOMIC))
                .andExpect(jsonPath("$.data.approvedReferenceKrw").value(APPROVED_REQUEST_REFERENCE_KRW));

        mockMvc.perform(get("/api/advance/requests/{requestId}", rejectedCandidateId)
                        .header("Authorization", "Bearer " + workerTwoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.approvedAmountAtomic").value(nullValue()))
                .andExpect(jsonPath("$.data.approvedReferenceKrw").value(nullValue()));

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
}
