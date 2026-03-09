package com.workproofpay.backend.wage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.support.PostgresIntegrationTestSupport;
import com.workproofpay.backend.wage.api.dto.request.CreateWageDepositRequest;
import com.workproofpay.backend.wage.repo.WageDepositRepository;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkProofRequest;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private WageDepositRepository wageDepositRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        wageDepositRepository.deleteAll();
        workProofRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void recordsDepositSummarizesWageAndFiltersDemoStateByAsOf() throws Exception {
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
                .andExpect(jsonPath("$.data.status").value("REVIEW_NEEDED"));

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
                .andExpect(jsonPath("$.data.wageSummary.status").value("NOT_RECORDED"));
    }

    @Test
    void returnsStructuredValidationDetailsForInvalidQueryParams() throws Exception {
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
    }

    @Test
    void usesLatestDepositVisibleAtAsOfAndDoesNotFlagZeroDifference() throws Exception {
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
                .andExpect(jsonPath("$.data.status").value("WITHIN_THRESHOLD"));
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
