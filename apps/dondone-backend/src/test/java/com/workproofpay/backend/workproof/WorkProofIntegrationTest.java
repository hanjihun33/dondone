package com.workproofpay.backend.workproof;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.support.PostgresIntegrationTestSupport;
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
class WorkProofIntegrationTest extends PostgresIntegrationTestSupport {

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
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
