package com.workproofpay.backend.advance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.advance.model.AdvancePayout;
import com.workproofpay.backend.advance.model.AdvancePayoutStatus;
import com.workproofpay.backend.advance.model.AdvanceRequest;
import com.workproofpay.backend.advance.repo.AdvancePayoutRepository;
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
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdvanceIntegrationTest extends PostgresIntegrationTestSupport {

    private static final long ELIGIBLE_AVAILABLE_ATOMIC = 103_448_275L;
    private static final long ELIGIBLE_AVAILABLE_DISPLAY_KRW = 150_000L;
    private static final long REQUEST_ATOMIC = 60_000_000L;
    private static final long REQUEST_DISPLAY_KRW = 87_000L;
    private static final long REPLAY_MISMATCH_ATOMIC = 61_000_000L;
    private static final long SECOND_WORKPLACE_REQUEST_ATOMIC = 50_000_000L;
    private static final long SAME_WORKPLACE_OPEN_REQUEST_ATOMIC = 70_000_000L;
    private static final long REDUCED_CAP_ATOMIC = 34_482_758L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdvanceRequestRepository advanceRequestRepository;

    @Autowired
    private AdvancePayoutRepository advancePayoutRepository;

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
        advancePayoutRepository.deleteAll();
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
                .andExpect(jsonPath("$.data.assetSymbol").value("dUSDC"))
                .andExpect(jsonPath("$.data.assetDecimals").value(6))
                .andExpect(jsonPath("$.data.exchangeRateSnapshot").value(1450))
                .andExpect(jsonPath("$.data.reflectedEarnedDisplayKrwAmount").value(800000))
                .andExpect(jsonPath("$.data.alreadyAdvancedDisplayKrwAmount").value(0))
                .andExpect(jsonPath("$.data.availableAmountAtomic").value(ELIGIBLE_AVAILABLE_ATOMIC))
                .andExpect(jsonPath("$.data.availableDisplayKrwAmount").value(ELIGIBLE_AVAILABLE_DISPLAY_KRW))
                .andExpect(jsonPath("$.data.currentTierName").value("B"))
                .andExpect(jsonPath("$.data.nextTierName").value("A"))
                .andExpect(jsonPath("$.data.progressToNextTier").value(0.6000))
                .andExpect(jsonPath("$.data.remainingWorkDaysToNextTier").value(8))
                .andExpect(jsonPath("$.data.nextTierExpectedCapDisplayKrw").value(300000))
                .andExpect(jsonPath("$.data.repaymentTier").value("B"))
                .andExpect(jsonPath("$.data.blockReasonCodes").isEmpty())
                .andExpect(jsonPath("$.data.noticeReasonCodes[0]").value("PENDING_WORKPROOF_REVIEW"))
                .andExpect(jsonPath("$.data.needsReviewRecordCount").value(1))
                .andExpect(jsonPath("$.data.pendingRecordCount").value(1))
                .andExpect(jsonPath("$.data.settlementDueDate").value(currentAdvanceCycleMonth().atDay(25).toString()))
                .andExpect(jsonPath("$.data.estimatedRepaymentDate").value(currentAdvanceCycleMonth().atDay(25).toString()));

        String requestJson = """
                {
                  "workplaceId": %d,
                  "requestedAmountAtomic": %d,
                  "requestedAt": "2030-01-10T09:00:00"
                }
                """.formatted(workplaceId, REQUEST_ATOMIC);

        String createdBody = mockMvc.perform(post("/api/advance/requests")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "advance-req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.assetSymbol").value("dUSDC"))
                .andExpect(jsonPath("$.data.assetDecimals").value(6))
                .andExpect(jsonPath("$.data.exchangeRateSnapshot").value(1450))
                .andExpect(jsonPath("$.data.approvedAmountAtomic").value(nullValue()))
                .andExpect(jsonPath("$.data.approvedDisplayKrwAmount").value(nullValue()))
                .andExpect(jsonPath("$.data.feeAmountAtomic").value(3448275))
                .andExpect(jsonPath("$.data.feeDisplayKrwAmount").value(5000))
                .andExpect(jsonPath("$.data.settlementStatus").value("SCHEDULED_FOR_PAYDAY"))
                .andExpect(jsonPath("$.data.settlementDueDate").value(currentAdvanceCycleMonth().atDay(25).toString()))
                .andExpect(jsonPath("$.data.eligibilitySnapshot.availableAmountAtomic").value(ELIGIBLE_AVAILABLE_ATOMIC))
                .andExpect(jsonPath("$.data.eligibilitySnapshot.availableDisplayKrwAmount").value(ELIGIBLE_AVAILABLE_DISPLAY_KRW))
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
                .andExpect(jsonPath("$.data.approvedAmountAtomic").value(nullValue()))
                .andExpect(jsonPath("$.data.approvedDisplayKrwAmount").value(nullValue()))
                .andExpect(jsonPath("$.data.settlementStatus").value("SCHEDULED_FOR_PAYDAY"))
                .andExpect(jsonPath("$.data.settlementDueDate").value(currentAdvanceCycleMonth().atDay(25).toString()));

        mockMvc.perform(get("/api/advance/requests")
                        .header("Authorization", bearer(token))
                        .param("month", currentAdvanceCycleMonth().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requests.length()").value(1))
                .andExpect(jsonPath("$.data.requests[0].requestId").value(requestId))
                .andExpect(jsonPath("$.data.requests[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.requests[0].requestedAmountAtomic").value(REQUEST_ATOMIC))
                .andExpect(jsonPath("$.data.requests[0].requestedDisplayKrwAmount").value(REQUEST_DISPLAY_KRW))
                .andExpect(jsonPath("$.data.requests[0].approvedAmountAtomic").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[0].approvedDisplayKrwAmount").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[0].settlementStatus").value("SCHEDULED_FOR_PAYDAY"))
                .andExpect(jsonPath("$.data.requests[0].settlementDueDate").value(currentAdvanceCycleMonth().atDay(25).toString()));

        mockMvc.perform(get("/api/advance/requests/{requestId}", requestId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestId").value(requestId))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.exchangeRateSnapshot").value(1450))
                .andExpect(jsonPath("$.data.requestedAmountAtomic").value(REQUEST_ATOMIC))
                .andExpect(jsonPath("$.data.requestedDisplayKrwAmount").value(REQUEST_DISPLAY_KRW))
                .andExpect(jsonPath("$.data.approvedAmountAtomic").value(nullValue()))
                .andExpect(jsonPath("$.data.approvedDisplayKrwAmount").value(nullValue()))
                .andExpect(jsonPath("$.data.settlementStatus").value("SCHEDULED_FOR_PAYDAY"))
                .andExpect(jsonPath("$.data.settlementDueDate").value(currentAdvanceCycleMonth().atDay(25).toString()))
                .andExpect(jsonPath("$.data.eligibilitySnapshot.exchangeRateSnapshot").value(1450))
                .andExpect(jsonPath("$.data.eligibilitySnapshot.needsReviewRecordCount").value(1));
    }

    @Test
    void eligibilityUsesRecognizedWorkedMinutesAfterAttendanceCorrection() throws Exception {
        User user = userRepository.save(User.register("advance-recognized@test.com", "hashed", "Advance"));
        String token = tokenFor(user);
        Long workplaceId = seedAdvanceEligibleScenario(user, 5, false);
        java.time.YearMonth targetMonth = currentAdvanceCycleMonth();

        WorkProof adjustedRecord = workProofRepository
                .findByUserIdAndWorkplaceIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(
                        user.getId(),
                        workplaceId,
                        targetMonth.atDay(1),
                        targetMonth.atEndOfMonth()
                ).stream()
                .filter(record -> record.getWorkDate().getDayOfMonth() == 5)
                .findFirst()
                .orElseThrow();
        adjustedRecord.updateRecognizedTimes(
                LocalDateTime.of(targetMonth.getYear(), targetMonth.getMonthValue(), 5, 9, 0),
                LocalDateTime.of(targetMonth.getYear(), targetMonth.getMonthValue(), 5, 13, 0)
        );
        workProofRepository.saveAndFlush(adjustedRecord);

        mockMvc.perform(get("/api/advance/eligibility")
                        .header("Authorization", bearer(token))
                        .param("workplaceId", workplaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workplaceId").value(workplaceId))
                .andExpect(jsonPath("$.data.availableDisplayKrwAmount").value(36000))
                .andExpect(jsonPath("$.data.repaymentTier").value("C"))
                .andExpect(jsonPath("$.data.reflectedWorkDays").value(5))
                .andExpect(jsonPath("$.data.reflectedWorkMinutes").value(2160))
                .andExpect(jsonPath("$.data.verifiedMinutes").value(2160))
                .andExpect(jsonPath("$.data.blockReasonCodes").isEmpty());
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
                  "requestedAmountAtomic": %d,
                  "requestedAt": "2030-01-10T09:00:00"
                }
                """.formatted(workplaceId, SECOND_WORKPLACE_REQUEST_ATOMIC);

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
                  "requestedAmountAtomic": %d,
                  "requestedAt": "2030-01-10T09:00:00"
                }
                """.formatted(workplaceId, REPLAY_MISMATCH_ATOMIC);

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
                  "requestedAmountAtomic": %d,
                  "requestedAt": "2030-01-10T09:00:00"
                }
                """.formatted(firstWorkplaceId, SECOND_WORKPLACE_REQUEST_ATOMIC);

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
                .andExpect(jsonPath("$.data.availableAmountAtomic").value(ELIGIBLE_AVAILABLE_ATOMIC))
                .andExpect(jsonPath("$.data.availableDisplayKrwAmount").value(ELIGIBLE_AVAILABLE_DISPLAY_KRW))
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
                  "requestedAmountAtomic": %d,
                  "requestedAt": "2030-01-10T09:00:00"
                }
                """.formatted(workplaceId, SAME_WORKPLACE_OPEN_REQUEST_ATOMIC);

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
                .andExpect(jsonPath("$.data.availableAmountAtomic").value(0))
                .andExpect(jsonPath("$.data.availableDisplayKrwAmount").value(0))
                .andExpect(jsonPath("$.data.blockReasonCodes[0]").value("EXISTING_OUTSTANDING_ADVANCE"))
                .andExpect(jsonPath("$.data.noticeReasonCodes").isEmpty());
    }

    @Test
    void deductsConfirmedAdvanceAmountFromSameCycleAvailability() throws Exception {
        User user = userRepository.save(User.register("advance-cycle-usage@test.com", "hashed", "Advance"));
        String token = tokenFor(user);
        Long workplaceId = seedAdvanceEligibleScenario(user, 10, false);

        long requestId = createAdvanceRequest(token, workplaceId, REDUCED_CAP_ATOMIC, "2030-01-10T09:00:00", "advance-used-1");
        approveRequest(requestId);
        attachPayout(requestId, user.getId(), REDUCED_CAP_ATOMIC, AdvancePayoutStatus.CONFIRMED);

        mockMvc.perform(get("/api/advance/eligibility")
                        .header("Authorization", bearer(token))
                        .param("workplaceId", workplaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alreadyAdvancedAmountAtomic").value(REDUCED_CAP_ATOMIC))
                .andExpect(jsonPath("$.data.alreadyAdvancedDisplayKrwAmount").value(50_000L))
                .andExpect(jsonPath("$.data.availableAmountAtomic").value(68_965_517L))
                .andExpect(jsonPath("$.data.availableDisplayKrwAmount").value(100_000L))
                .andExpect(jsonPath("$.data.blockReasonCodes").isEmpty());
    }

    @Test
    void allowsAdditionalAdvanceRequestWithinSameCycleAfterConfirmedPayoutWhenLimitRemains() throws Exception {
        User user = userRepository.save(User.register("advance-repeat@test.com", "hashed", "Advance"));
        String token = tokenFor(user);
        Long workplaceId = seedAdvanceEligibleScenario(user, 10, false);

        long firstRequestId = createAdvanceRequest(token, workplaceId, REDUCED_CAP_ATOMIC, "2030-01-10T09:00:00", "advance-repeat-1");
        approveRequest(firstRequestId);
        attachPayout(firstRequestId, user.getId(), REDUCED_CAP_ATOMIC, AdvancePayoutStatus.CONFIRMED);

        String secondRequestJson = """
                {
                  "workplaceId": %d,
                  "requestedAmountAtomic": %d,
                  "requestedAt": "2030-01-11T09:00:00"
                }
                """.formatted(workplaceId, 20_000_000L);

        mockMvc.perform(post("/api/advance/requests")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "advance-repeat-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondRequestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.eligibilitySnapshot.availableAmountAtomic").value(68_965_517L))
                .andExpect(jsonPath("$.data.eligibilitySnapshot.availableDisplayKrwAmount").value(100_000L));

        mockMvc.perform(get("/api/advance/requests")
                        .header("Authorization", bearer(token))
                        .param("month", currentAdvanceCycleMonth().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requests.length()").value(2))
                .andExpect(jsonPath("$.data.requests[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.requests[1].status").value("PAID"));
    }

    @Test
    void listsRequestsUnderResolvedAdvanceCycleMonth() throws Exception {
        User user = userRepository.save(User.register("advance-cycle@test.com", "hashed", "Advance"));
        String token = tokenFor(user);
        Long workplaceId = seedAdvanceEligibleScenario(user, 10, false);

        String requestJson = """
                {
                  "workplaceId": %d,
                  "requestedAmountAtomic": %d,
                  "requestedAt": "2030-01-10T09:00:00"
                }
                """.formatted(workplaceId, SECOND_WORKPLACE_REQUEST_ATOMIC);

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

    @Test
    void mapsWorkerResponsesAcrossRequestAndPayoutStatuses() throws Exception {
        User user = userRepository.save(User.register("advance-status@test.com", "hashed", "Advance"));
        String token = tokenFor(user);
        Long submittedWorkplaceId = seedAdvanceEligibleScenario(user, 10, false);
        Long approvedWorkplaceId = seedAdvanceEligibleScenario(user, 10, false);
        Long payingWorkplaceId = seedAdvanceEligibleScenario(user, 10, false);
        Long paidWorkplaceId = seedAdvanceEligibleScenario(user, 10, false);
        Long failedWorkplaceId = seedAdvanceEligibleScenario(user, 10, false);
        Long timedOutWorkplaceId = seedAdvanceEligibleScenario(user, 10, false);

        long submittedRequestId = createAdvanceRequest(token, submittedWorkplaceId, 45_000_000L, "2030-01-10T09:00:00", "advance-status-submitted");
        long approvedRequestId = createAdvanceRequest(token, approvedWorkplaceId, 46_000_000L, "2030-01-10T10:00:00", "advance-status-approved");
        long payingRequestId = createAdvanceRequest(token, payingWorkplaceId, 47_000_000L, "2030-01-10T11:00:00", "advance-status-paying");
        long paidRequestId = createAdvanceRequest(token, paidWorkplaceId, 48_000_000L, "2030-01-10T12:00:00", "advance-status-paid");
        long failedRequestId = createAdvanceRequest(token, failedWorkplaceId, 49_000_000L, "2030-01-10T13:00:00", "advance-status-failed");
        long timedOutRequestId = createAdvanceRequest(token, timedOutWorkplaceId, 50_000_000L, "2030-01-10T14:00:00", "advance-status-timeout");

        approveRequest(approvedRequestId);
        approveRequest(payingRequestId);
        approveRequest(paidRequestId);
        approveRequest(failedRequestId);
        approveRequest(timedOutRequestId);

        attachPayout(payingRequestId, user.getId(), 47_000_000L, AdvancePayoutStatus.BROADCASTED);
        attachPayout(paidRequestId, user.getId(), 48_000_000L, AdvancePayoutStatus.CONFIRMED);
        attachPayout(failedRequestId, user.getId(), 49_000_000L, AdvancePayoutStatus.FAILED);
        attachPayout(timedOutRequestId, user.getId(), 50_000_000L, AdvancePayoutStatus.TIMED_OUT);

        mockMvc.perform(get("/api/advance/requests")
                        .header("Authorization", bearer(token))
                        .param("month", currentAdvanceCycleMonth().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requests.length()").value(6))
                .andExpect(jsonPath("$.data.requests[0].requestId").value(timedOutRequestId))
                .andExpect(jsonPath("$.data.requests[0].status").value("PAYOUT_FAILED"))
                .andExpect(jsonPath("$.data.requests[0].requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.requests[0].payoutStatus").value("TIMED_OUT"))
                .andExpect(jsonPath("$.data.requests[0].approvedAmountAtomic").value(50_000_000L))
                .andExpect(jsonPath("$.data.requests[0].approvedDisplayKrwAmount").value(72_500L))
                .andExpect(jsonPath("$.data.requests[0].payoutTxHash").value("0x" + "%064x".formatted(timedOutRequestId)))
                .andExpect(jsonPath("$.data.requests[1].requestId").value(failedRequestId))
                .andExpect(jsonPath("$.data.requests[1].status").value("PAYOUT_FAILED"))
                .andExpect(jsonPath("$.data.requests[1].requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.requests[1].payoutStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.requests[1].approvedAmountAtomic").value(49_000_000L))
                .andExpect(jsonPath("$.data.requests[1].approvedDisplayKrwAmount").value(71_050L))
                .andExpect(jsonPath("$.data.requests[1].payoutTxHash").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[2].requestId").value(paidRequestId))
                .andExpect(jsonPath("$.data.requests[2].status").value("PAID"))
                .andExpect(jsonPath("$.data.requests[2].requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.requests[2].payoutStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.requests[2].approvedAmountAtomic").value(48_000_000L))
                .andExpect(jsonPath("$.data.requests[2].approvedDisplayKrwAmount").value(69_600L))
                .andExpect(jsonPath("$.data.requests[2].payoutTxHash").value("0x" + "%064x".formatted(paidRequestId)))
                .andExpect(jsonPath("$.data.requests[3].requestId").value(payingRequestId))
                .andExpect(jsonPath("$.data.requests[3].status").value("PAYING"))
                .andExpect(jsonPath("$.data.requests[3].requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.requests[3].payoutStatus").value("BROADCASTED"))
                .andExpect(jsonPath("$.data.requests[3].approvedAmountAtomic").value(47_000_000L))
                .andExpect(jsonPath("$.data.requests[3].approvedDisplayKrwAmount").value(68_150L))
                .andExpect(jsonPath("$.data.requests[3].payoutTxHash").value("0x" + "%064x".formatted(payingRequestId)))
                .andExpect(jsonPath("$.data.requests[4].requestId").value(approvedRequestId))
                .andExpect(jsonPath("$.data.requests[4].status").value("APPROVED"))
                .andExpect(jsonPath("$.data.requests[4].requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.requests[4].payoutStatus").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[4].approvedAmountAtomic").value(46_000_000L))
                .andExpect(jsonPath("$.data.requests[4].approvedDisplayKrwAmount").value(66_700L))
                .andExpect(jsonPath("$.data.requests[4].payoutTxHash").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[5].requestId").value(submittedRequestId))
                .andExpect(jsonPath("$.data.requests[5].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.requests[5].requestStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.requests[5].payoutStatus").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[5].approvedAmountAtomic").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[5].approvedDisplayKrwAmount").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[5].payoutTxHash").value(nullValue()));

        mockMvc.perform(get("/api/advance/requests/{requestId}", approvedRequestId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.payoutStatus").value(nullValue()))
                .andExpect(jsonPath("$.data.approvedAmountAtomic").value(46_000_000L))
                .andExpect(jsonPath("$.data.approvedDisplayKrwAmount").value(66_700L))
                .andExpect(jsonPath("$.data.payoutTxHash").value(nullValue()));

        mockMvc.perform(get("/api/advance/requests/{requestId}", payingRequestId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAYING"))
                .andExpect(jsonPath("$.data.requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.payoutStatus").value("BROADCASTED"))
                .andExpect(jsonPath("$.data.approvedAmountAtomic").value(47_000_000L))
                .andExpect(jsonPath("$.data.approvedDisplayKrwAmount").value(68_150L))
                .andExpect(jsonPath("$.data.payoutTxHash").value("0x" + "%064x".formatted(payingRequestId)));

        mockMvc.perform(get("/api/advance/requests/{requestId}", paidRequestId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.payoutStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.approvedAmountAtomic").value(48_000_000L))
                .andExpect(jsonPath("$.data.approvedDisplayKrwAmount").value(69_600L))
                .andExpect(jsonPath("$.data.payoutTxHash").value("0x" + "%064x".formatted(paidRequestId)));

        mockMvc.perform(get("/api/advance/requests/{requestId}", failedRequestId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAYOUT_FAILED"))
                .andExpect(jsonPath("$.data.requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.payoutStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.approvedAmountAtomic").value(49_000_000L))
                .andExpect(jsonPath("$.data.approvedDisplayKrwAmount").value(71_050L))
                .andExpect(jsonPath("$.data.payoutTxHash").value(nullValue()));

        mockMvc.perform(get("/api/advance/requests/{requestId}", timedOutRequestId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAYOUT_FAILED"))
                .andExpect(jsonPath("$.data.requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.payoutStatus").value("TIMED_OUT"))
                .andExpect(jsonPath("$.data.approvedAmountAtomic").value(50_000_000L))
                .andExpect(jsonPath("$.data.approvedDisplayKrwAmount").value(72_500L))
                .andExpect(jsonPath("$.data.payoutTxHash").value("0x" + "%064x".formatted(timedOutRequestId)));
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

    private long createAdvanceRequest(
            String token,
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

        String body = mockMvc.perform(post("/api/advance/requests")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readId(body, "requestId");
    }

    private void approveRequest(long requestId) {
        AdvanceRequest request = advanceRequestRepository.findById(requestId).orElseThrow();
        request.approve(999L);
        advanceRequestRepository.save(request);
    }

    private void attachPayout(long requestId, long userId, long amountAtomic, AdvancePayoutStatus targetStatus) {
        AdvancePayout payout = AdvancePayout.request(
                UUID.randomUUID().toString().replace("-", ""),
                requestId,
                userId,
                "0x" + "%040x".formatted(requestId),
                amountAtomic,
                "dUSDC",
                "advance-payout-" + requestId
        );
        advancePayoutRepository.save(payout);

        switch (targetStatus) {
            case REQUESTED -> {
            }
            case BROADCASTED -> {
                payout.markSigned("0x" + "%064x".formatted(requestId), "signed-" + requestId);
                payout.markBroadcasted();
            }
            case CONFIRMED -> {
                payout.markSigned("0x" + "%064x".formatted(requestId), "signed-" + requestId);
                payout.markBroadcasted();
                payout.markConfirmed();
            }
            case FAILED -> payout.markFailed("submit failed");
            case TIMED_OUT -> {
                payout.markSigned("0x" + "%064x".formatted(requestId), "signed-" + requestId);
                payout.markBroadcasted();
                payout.markTimedOut("receipt timeout");
            }
            default -> throw new IllegalArgumentException("Unsupported payout status for test: " + targetStatus);
        }

        advancePayoutRepository.save(payout);
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
