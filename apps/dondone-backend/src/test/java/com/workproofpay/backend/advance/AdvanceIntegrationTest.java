package com.workproofpay.backend.advance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.advance.repo.AdvanceRequestRepository;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.support.PostgresIntegrationTestSupport;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdvanceIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdvanceRequestRepository advanceRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private WorkContractRepository workContractRepository;

    @Autowired
    private WorkProofRepository workProofRepository;

    @Autowired
    private WorkProofAuditLogRepository workProofAuditLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        advanceRequestRepository.deleteAll();
        workProofAuditLogRepository.deleteAll();
        workProofRepository.deleteAll();
        workContractRepository.deleteAll();
        workplaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void returnsEligibilityCreatesAdvanceAndReplaysSameIdempotencyKey() throws Exception {
        User user = userRepository.save(User.register("advance@test.com", "hashed", "Advance"));
        String token = tokenFor(user);
        Long workplaceId = seedAdvanceEligibleScenario(user, 10, true);

        mockMvc.perform(get("/api/advance/eligibility")
                        .header("Authorization", bearer(token))
                        .param("workplaceId", workplaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workplaceId").value(workplaceId))
                .andExpect(jsonPath("$.data.availableAmount").value(150000))
                .andExpect(jsonPath("$.data.repaymentTier").value("B"))
                .andExpect(jsonPath("$.data.blockReasonCodes[0]").value("PENDING_WORKPROOF_REVIEW"))
                .andExpect(jsonPath("$.data.needsReviewRecordCount").value(1))
                .andExpect(jsonPath("$.data.estimatedRepaymentDate").value(currentAdvanceCycleMonth().atDay(25).toString()));

        String requestJson = """
                {
                  "workplaceId": %d,
                  "requestedAmount": 100000,
                  "requestedAt": "2030-01-10T09:00:00"
                }
                """.formatted(workplaceId);

        String createdBody = mockMvc.perform(post("/api/advance/requests")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "advance-req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.approvedAmount").value(nullValue()))
                .andExpect(jsonPath("$.data.eligibilitySnapshot.availableAmount").value(150000))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long requestId = readId(createdBody, "requestId");

        mockMvc.perform(post("/api/advance/requests")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "advance-req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestId").value(requestId))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.approvedAmount").value(nullValue()));

        mockMvc.perform(get("/api/advance/requests")
                        .header("Authorization", bearer(token))
                        .param("month", currentAdvanceCycleMonth().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requests.length()").value(1))
                .andExpect(jsonPath("$.data.requests[0].requestId").value(requestId))
                .andExpect(jsonPath("$.data.requests[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.requests[0].approvedAmount").value(nullValue()));

        mockMvc.perform(get("/api/advance/requests/{requestId}", requestId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestId").value(requestId))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.approvedAmount").value(nullValue()))
                .andExpect(jsonPath("$.data.eligibilitySnapshot.needsReviewRecordCount").value(1));
    }

    @Test
    void hidesForeignAdvanceRequestAndRejectsMismatchedIdempotencyReplay() throws Exception {
        User owner = userRepository.save(User.register("advance-owner@test.com", "hashed", "Owner"));
        User other = userRepository.save(User.register("advance-other@test.com", "hashed", "Other"));
        String ownerToken = tokenFor(owner);
        String otherToken = tokenFor(other);
        Long workplaceId = seedAdvanceEligibleScenario(owner, 10, false);

        String requestJson = """
                {
                  "workplaceId": %d,
                  "requestedAmount": 50000,
                  "requestedAt": "2030-01-10T09:00:00"
                }
                """.formatted(workplaceId);

        String createdBody = mockMvc.perform(post("/api/advance/requests")
                        .header("Authorization", bearer(ownerToken))
                        .header("Idempotency-Key", "advance-req-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long requestId = readId(createdBody, "requestId");

        mockMvc.perform(get("/api/advance/requests/{requestId}", requestId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADVANCE_REQUEST_NOT_FOUND"));

        String mismatchedJson = """
                {
                  "workplaceId": %d,
                  "requestedAmount": 60000,
                  "requestedAt": "2030-01-10T09:00:00"
                }
                """.formatted(workplaceId);

        mockMvc.perform(post("/api/advance/requests")
                        .header("Authorization", bearer(ownerToken))
                        .header("Idempotency-Key", "advance-req-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mismatchedJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD"));
    }

    @Test
    void doesNotBlockDifferentWorkplaceWhenOutstandingAdvanceExistsElsewhere() throws Exception {
        User user = userRepository.save(User.register("advance-multi@test.com", "hashed", "Advance"));
        String token = tokenFor(user);
        Long firstWorkplaceId = seedAdvanceEligibleScenario(user, 10, false);
        Long secondWorkplaceId = seedAdvanceEligibleScenario(user, 10, false);

        String firstRequestJson = """
                {
                  "workplaceId": %d,
                  "requestedAmount": 50000,
                  "requestedAt": "2030-01-10T09:00:00"
                }
                """.formatted(firstWorkplaceId);

        mockMvc.perform(post("/api/advance/requests")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "advance-req-multi-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequestJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/advance/eligibility")
                        .header("Authorization", bearer(token))
                        .param("workplaceId", secondWorkplaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workplaceId").value(secondWorkplaceId))
                .andExpect(jsonPath("$.data.availableAmount").value(150000))
                .andExpect(jsonPath("$.data.blockReasonCodes").isEmpty());
    }

    @Test
    void blocksSecondAdvanceRequestForSameWorkplaceWhileSubmittedRequestIsOpen() throws Exception {
        User user = userRepository.save(User.register("advance-open@test.com", "hashed", "Advance"));
        String token = tokenFor(user);
        Long workplaceId = seedAdvanceEligibleScenario(user, 10, false);

        String firstRequestJson = """
                {
                  "workplaceId": %d,
                  "requestedAmount": 70000,
                  "requestedAt": "2030-01-10T09:00:00"
                }
                """.formatted(workplaceId);

        mockMvc.perform(post("/api/advance/requests")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "advance-req-open-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        mockMvc.perform(get("/api/advance/eligibility")
                        .header("Authorization", bearer(token))
                        .param("workplaceId", workplaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableAmount").value(0))
                .andExpect(jsonPath("$.data.blockReasonCodes[0]").value("EXISTING_OUTSTANDING_ADVANCE"));
    }

    @Test
    void listsRequestsUnderResolvedAdvanceCycleMonth() throws Exception {
        User user = userRepository.save(User.register("advance-cycle@test.com", "hashed", "Advance"));
        String token = tokenFor(user);
        Long workplaceId = seedAdvanceEligibleScenario(user, 10, false);

        String requestJson = """
                {
                  "workplaceId": %d,
                  "requestedAmount": 50000,
                  "requestedAt": "2030-01-10T09:00:00"
                }
                """.formatted(workplaceId);

        mockMvc.perform(post("/api/advance/requests")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "advance-req-cycle-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/advance/requests")
                        .header("Authorization", bearer(token))
                        .param("month", currentAdvanceCycleMonth().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month").value(currentAdvanceCycleMonth().toString()))
                .andExpect(jsonPath("$.data.requests.length()").value(1));

        mockMvc.perform(get("/api/advance/requests")
                        .header("Authorization", bearer(token))
                        .param("month", currentAdvanceCycleMonth().minusMonths(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month").value(currentAdvanceCycleMonth().minusMonths(1).toString()))
                .andExpect(jsonPath("$.data.requests.length()").value(0));
    }

    private Long seedAdvanceEligibleScenario(User user, int reflectedDayCount, boolean addPendingRecord) {
        Workplace workplace = workplaceRepository.save(Workplace.create(
                user,
                "Advance Farm",
                "Gumi",
                "Front gate",
                36.1,
                128.3,
                1_000
        ));
        WorkContract contract = workContractRepository.save(WorkContract.activate(
                workplace,
                WorkProofPayUnit.HOURLY,
                BigDecimal.valueOf(10_000),
                null,
                null,
                BigDecimal.valueOf(10_000),
                currentAdvanceCycleMonth().atDay(1)
        ));

        for (int day = 1; day <= reflectedDayCount; day++) {
            LocalDateTime checkIn = currentAdvanceCycleMonth().atDay(day).atTime(9, 0);
            WorkProof record = WorkProof.checkIn(
                    user,
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

        if (addPendingRecord) {
            LocalDateTime pendingCheckIn = currentAdvanceCycleMonth().atDay(reflectedDayCount + 1).atTime(9, 0);
            workProofRepository.save(WorkProof.checkIn(
                    user,
                    workplace,
                    contract,
                    pendingCheckIn,
                    pendingCheckIn.minusMinutes(1),
                    36.1,
                    128.3,
                    "Front gate"
            ));
        }

        return workplace.getId();
    }

    private Long readId(String json, String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        return root.path("data").path(fieldName).asLong();
    }

    private YearMonth currentAdvanceCycleMonth() {
        LocalDate today = LocalDate.now();
        YearMonth cycleMonth = YearMonth.from(today);
        return today.getDayOfMonth() > 25 ? cycleMonth.plusMonths(1) : cycleMonth;
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
