package com.workproofpay.backend.wage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.support.PostgresIntegrationTestSupport;
import com.workproofpay.backend.wage.api.dto.request.CreateWageDepositRequest;
import com.workproofpay.backend.wage.repo.WageDepositRepository;
import com.workproofpay.backend.workproof.api.dto.request.CheckInWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CheckOutWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateContractRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkplaceRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkProofRequest;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
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

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WageDemoIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkProofRepository workProofRepository;

    @Autowired
    private WorkContractRepository workContractRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private WageDepositRepository wageDepositRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        wageDepositRepository.deleteAll();
        workProofRepository.deleteAll();
        workContractRepository.deleteAll();
        workplaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void returnsLane1MonthlySummaryAndEstimateFromWorkProofInputs() throws Exception {
        // 근무지 등록 -> 계약 생성 -> 출퇴근 2건 생성 -> Wage lane1 summary/estimate 조회까지 read 흐름을 검증한다.
        User user = userRepository.save(User.register("lane1-wage@test.com", "hashed", "Tester"));
        String token = tokenFor(user);
        Lane1Fixture fixture = createLane1WorkplaceAndContract(token);

        // 1. reflected 근무 2건을 만든다.
        Long firstRecordId = checkIn(token, fixture.workplaceId(), LocalDateTime.of(2026, 3, 10, 9, 0));
        checkOut(token, LocalDateTime.of(2026, 3, 10, 18, 0));
        Long secondRecordId = checkIn(token, fixture.workplaceId(), LocalDateTime.of(2026, 3, 12, 21, 0));
        checkOut(token, LocalDateTime.of(2026, 3, 12, 23, 0));

        // 2. WorkProof monthly summary를 입력으로 쓰는 Wage 월간 요약 응답을 검증한다.
        mockMvc.perform(get("/api/wage/monthly-summary")
                        .header("Authorization", bearer(token))
                        .param("month", "2026-03")
                        .param("workplaceId", fixture.workplaceId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month").value("2026-03"))
                .andExpect(jsonPath("$.data.workplaceId").value(fixture.workplaceId()))
                .andExpect(jsonPath("$.data.contractId").value(fixture.contractId()))
                .andExpect(jsonPath("$.data.payUnit").value("HOURLY"))
                .andExpect(jsonPath("$.data.normalizedHourlyWage").value(12000))
                .andExpect(jsonPath("$.data.workDayCount").value(2))
                .andExpect(jsonPath("$.data.verifiedWorkMinutes").value(660))
                .andExpect(jsonPath("$.data.overtimeMinutes").value(60))
                .andExpect(jsonPath("$.data.nightMinutes").value(60))
                .andExpect(jsonPath("$.data.modifiedRecordCount").value(0))
                .andExpect(jsonPath("$.data.excludedPendingRecordCount").value(0))
                .andExpect(jsonPath("$.data.includedRecordIds[*]").value(hasItems(
                        firstRecordId.intValue(),
                        secondRecordId.intValue()
                )));

        // 3. 같은 입력축으로 reference-only estimate 응답이 계산되는지 확인한다.
        mockMvc.perform(get("/api/wage/estimate")
                        .header("Authorization", bearer(token))
                        .param("month", "2026-03")
                        .param("workplaceId", fixture.workplaceId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month").value("2026-03"))
                .andExpect(jsonPath("$.data.workplaceId").value(fixture.workplaceId()))
                .andExpect(jsonPath("$.data.contract.contractId").value(fixture.contractId()))
                .andExpect(jsonPath("$.data.contract.payUnit").value("HOURLY"))
                .andExpect(jsonPath("$.data.contract.normalizedHourlyWage").value(12000))
                .andExpect(jsonPath("$.data.summary.workDayCount").value(2))
                .andExpect(jsonPath("$.data.summary.verifiedWorkMinutes").value(660))
                .andExpect(jsonPath("$.data.summary.overtimeMinutes").value(60))
                .andExpect(jsonPath("$.data.summary.nightMinutes").value(60))
                .andExpect(jsonPath("$.data.summary.modifiedRecordCount").value(0))
                .andExpect(jsonPath("$.data.estimate.baseEstimate").value(132000))
                .andExpect(jsonPath("$.data.estimate.overtimePremium").value(6000))
                .andExpect(jsonPath("$.data.estimate.nightPremium").value(6000))
                .andExpect(jsonPath("$.data.estimate.estimatedTotal").value(144000))
                .andExpect(jsonPath("$.data.ruleVersion").value("WAGE_LANE1_V0"))
                .andExpect(jsonPath("$.data.disclaimer").exists());
    }

    @Test
    void recordsDepositSummarizesWageAndFiltersDemoStateByAsOf() throws Exception {
        // 기존 wage summary/deposit 흐름이 lane1 read endpoint 추가 후에도 유지되는지 회귀 검증한다.
        User user = userRepository.save(User.register("wage@test.com", "hashed", "Tester"));
        String token = tokenFor(user);

        CreateWorkProofRequest marchTenth = new CreateWorkProofRequest(
                LocalDate.of(2026, 3, 10),
                LocalDateTime.of(2026, 3, 10, 9, 0),
                LocalDateTime.of(2026, 3, 10, 17, 0),
                LocalDateTime.of(2026, 3, 10, 8, 59),
                LocalDateTime.of(2026, 3, 10, 17, 1),
                37.1,
                127.1,
                37.1,
                127.1,
                null,
                null,
                0
        );
        CreateWorkProofRequest marchTwelveth = new CreateWorkProofRequest(
                LocalDate.of(2026, 3, 12),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                LocalDateTime.of(2026, 3, 12, 23, 0),
                LocalDateTime.of(2026, 3, 12, 20, 59),
                LocalDateTime.of(2026, 3, 12, 23, 1),
                37.2,
                127.2,
                37.2,
                127.2,
                null,
                null,
                0
        );

        mockMvc.perform(post("/api/workproof")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(marchTenth)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/workproof")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(marchTwelveth)))
                .andExpect(status().isCreated());

        CreateWageDepositRequest depositRequest = new CreateWageDepositRequest(
                "2026-03",
                LocalDate.of(2026, 3, 25),
                80_000L,
                false,
                "demo deposit"
        );

        mockMvc.perform(post("/api/wage/deposits")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.actualDepositAmount").value(80_000));

        mockMvc.perform(get("/api/wage/summary")
                        .header("Authorization", bearer(token))
                        .param("yearMonth", "2026-03")
                        .param("asOf", "2026-03-25")
                        .param("normalizedHourlyWage", "10000")
                        .param("paydayDay", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workDays").value(2))
                .andExpect(jsonPath("$.data.totalWorkedMinutes").value(600))
                .andExpect(jsonPath("$.data.nightMinutes").value(60))
                .andExpect(jsonPath("$.data.estimatedTotalAmount").value(105000))
                .andExpect(jsonPath("$.data.actualDepositAmount").value(80000))
                .andExpect(jsonPath("$.data.differenceAmount").value(25000))
                .andExpect(jsonPath("$.data.anomalyDetected").value(true))
                .andExpect(jsonPath("$.data.status").value("REVIEW_NEEDED"))
                .andExpect(jsonPath("$.data.reasons[*].code", hasItems(
                        "OVERTIME_INCLUDED",
                        "NIGHT_SHIFT_INCLUDED",
                        "DIFFERENCE_OVER_THRESHOLD"
                )))
                .andExpect(jsonPath("$.data.reasons[2].relatedWorkProofIds[0]").value(1))
                .andExpect(jsonPath("$.data.reasons[2].relatedWorkProofIds[1]").value(2));

        mockMvc.perform(get("/api/demo/state")
                        .header("Authorization", bearer(token))
                        .param("asOf", "2026-03-11")
                        .param("normalizedHourlyWage", "10000")
                        .param("paydayDay", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.yearMonth").value("2026-03"))
                .andExpect(jsonPath("$.data.workProofs.length()").value(1))
                .andExpect(jsonPath("$.data.workProofSummary.totalWorkDays").value(1))
                .andExpect(jsonPath("$.data.wageSummary.actualDepositAmount").doesNotExist())
                .andExpect(jsonPath("$.data.wageSummary.status").value("NOT_RECORDED"))
                .andExpect(jsonPath("$.data.wageSummary.reasons[*].code", hasItems("DEPOSIT_MISSING")));
    }

    @Test
    void returnsStructuredValidationDetailsForInvalidQueryParams() throws Exception {
        // 기존 summary와 신규 lane1 read endpoint가 모두 구조화된 validation 오류를 유지하는지 검증한다.
        User user = userRepository.save(User.register("validation@test.com", "hashed", "Tester"));
        String token = tokenFor(user);

        mockMvc.perform(get("/api/wage/summary")
                        .header("Authorization", bearer(token))
                        .param("yearMonth", "2026-03")
                        .param("normalizedHourlyWage", "10000")
                        .param("paydayDay", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.details[0].message").value("paydayDay must be between 1 and 31"));

        mockMvc.perform(get("/api/wage/monthly-summary")
                        .header("Authorization", bearer(token))
                        .param("month", "2026/03")
                        .param("workplaceId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));

        mockMvc.perform(get("/api/wage/estimate")
                        .header("Authorization", bearer(token))
                        .param("month", "2026-03")
                        .param("workplaceId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void usesLatestDepositVisibleAtAsOfAndDoesNotFlagZeroDifference() throws Exception {
        // 기존 summary 흐름에서 asOf 기준 최신 입금 선택과 임계값 판정이 유지되는지 확인한다.
        User user = userRepository.save(User.register("latest@test.com", "hashed", "Tester"));
        String token = tokenFor(user);

        CreateWorkProofRequest oneMinuteShift = new CreateWorkProofRequest(
                LocalDate.of(2026, 3, 10),
                LocalDateTime.of(2026, 3, 10, 9, 0),
                LocalDateTime.of(2026, 3, 10, 9, 1),
                LocalDateTime.of(2026, 3, 10, 8, 59),
                LocalDateTime.of(2026, 3, 10, 9, 2),
                37.1,
                127.1,
                37.1,
                127.1,
                null,
                null,
                0
        );

        mockMvc.perform(post("/api/workproof")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oneMinuteShift)))
                .andExpect(status().isCreated());

        CreateWageDepositRequest earlierDeposit = new CreateWageDepositRequest(
                "2026-03",
                LocalDate.of(2026, 3, 20),
                100L,
                false,
                "early deposit"
        );
        CreateWageDepositRequest laterDeposit = new CreateWageDepositRequest(
                "2026-03",
                LocalDate.of(2026, 3, 25),
                999L,
                false,
                "later deposit"
        );

        mockMvc.perform(post("/api/wage/deposits")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earlierDeposit)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/wage/deposits")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laterDeposit)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/wage/summary")
                        .header("Authorization", bearer(token))
                        .param("yearMonth", "2026-03")
                        .param("asOf", "2026-03-21")
                        .param("normalizedHourlyWage", "6000")
                        .param("paydayDay", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actualDepositAmount").value(100))
                .andExpect(jsonPath("$.data.actualDepositRecordedDay").value(20));

        mockMvc.perform(get("/api/wage/summary")
                        .header("Authorization", bearer(token))
                        .param("yearMonth", "2026-03")
                        .param("asOf", "2026-03-25")
                        .param("normalizedHourlyWage", "6000")
                        .param("paydayDay", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedTotalAmount").value(100))
                .andExpect(jsonPath("$.data.actualDepositAmount").value(999))
                .andExpect(jsonPath("$.data.actualDepositRecordedDay").value(25));

        mockMvc.perform(get("/api/wage/summary")
                        .header("Authorization", bearer(token))
                        .param("yearMonth", "2026-03")
                        .param("asOf", "2026-03-20")
                        .param("normalizedHourlyWage", "6000")
                        .param("paydayDay", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedTotalAmount").value(100))
                .andExpect(jsonPath("$.data.actualDepositAmount").value(100))
                .andExpect(jsonPath("$.data.differenceAmount").value(0))
                .andExpect(jsonPath("$.data.anomalyTriggerAmount").value(1))
                .andExpect(jsonPath("$.data.anomalyDetected").value(false))
                .andExpect(jsonPath("$.data.status").value("WITHIN_THRESHOLD"))
                .andExpect(jsonPath("$.data.reasons.length()").value(0));
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private Lane1Fixture createLane1WorkplaceAndContract(String token) throws Exception {
        String workplaceBody = mockMvc.perform(post("/api/workproof/workplaces")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWorkplaceRequest(
                                "DonDone Wage Cafe",
                                "Seoul Somewhere 1",
                                "Front door",
                                37.5,
                                127.0
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long workplaceId = readId(workplaceBody, "workplaceId");

        String contractBody = mockMvc.perform(post("/api/workproof/contracts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateContractRequest(
                                workplaceId,
                                WorkProofPayUnit.HOURLY,
                                BigDecimal.valueOf(12_000),
                                null,
                                null,
                                LocalDate.of(2026, 3, 1)
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long contractId = readId(contractBody, "contractId");
        return new Lane1Fixture(workplaceId, contractId);
    }

    private Long checkIn(String token, Long workplaceId, LocalDateTime deviceAt) throws Exception {
        String body = mockMvc.perform(post("/api/workproof/records/check-in")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckInWorkProofRequest(
                                workplaceId,
                                deviceAt,
                                37.5,
                                127.0,
                                "Front door"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readId(body, "recordId");
    }

    private void checkOut(String token, LocalDateTime deviceAt) throws Exception {
        mockMvc.perform(post("/api/workproof/records/check-out")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckOutWorkProofRequest(
                                deviceAt,
                                37.5,
                                127.0,
                                "Front door"
                        ))))
                .andExpect(status().isOk());
    }

    private Long readId(String json, String fieldName) throws Exception {
        return objectMapper.readTree(json).path("data").path(fieldName).asLong();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Lane1Fixture(Long workplaceId, Long contractId) {
    }
}
