package com.workproofpay.backend.wage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.workproof.api.dto.request.CheckInWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CheckOutWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateContractRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkplaceRequest;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:wageperf;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "jwt.secret=test-dondone-secret-key-change-this-at-least-32chars"
})
@AutoConfigureMockMvc
class WagePerformanceIntegrationTest {

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
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        workProofRepository.deleteAll();
        workContractRepository.deleteAll();
        workplaceRepository.deleteAll();
        userRepository.deleteAll();
        queryStatistics().clear();
    }

    @Test
    void keepsEstimateQueryCountAtThreeOrLess() throws Exception {
        User user = userRepository.save(User.register("wage-perf@test.com", "hashed", "Perf"));
        String token = tokenFor(user);
        Long workplaceId = createWorkplace(token);
        createContract(token, workplaceId);

        for (int day = 1; day <= 10; day++) {
            LocalDateTime checkInAt = LocalDateTime.of(2026, 3, day, 9, 0);
            checkIn(token, workplaceId, checkInAt);
            checkOut(token, LocalDateTime.of(2026, 3, day, 18, 0));
        }

        queryStatistics().clear();

        mockMvc.perform(get("/api/wage/estimate")
                        .header("Authorization", bearer(token))
                        .param("month", "2026-03")
                        .param("workplaceId", workplaceId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month").value("2026-03"))
                .andExpect(jsonPath("$.data.workplaceId").value(workplaceId))
                .andExpect(jsonPath("$.data.summary.workDayCount").value(10));

        long preparedStatementCount = queryStatistics().getPrepareStatementCount();
        assertTrue(preparedStatementCount <= 3, "expected wage estimate query count to stay at or below 3");
        System.out.println("Wage estimate prepared statements: " + preparedStatementCount);
    }

    private Statistics queryStatistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private Long createWorkplace(String token) throws Exception {
        String body = mockMvc.perform(post("/api/workproof/workplaces")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWorkplaceRequest(
                                "Perf Wage Cafe",
                                "Seoul Somewhere 1",
                                "Front door",
                                37.5,
                                127.0
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body).path("data").path("workplaceId").asLong();
    }

    private void createContract(String token, Long workplaceId) throws Exception {
        mockMvc.perform(post("/api/workproof/contracts")
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
                .andExpect(status().isCreated());
    }

    private void checkIn(String token, Long workplaceId, LocalDateTime deviceAt) throws Exception {
        mockMvc.perform(post("/api/workproof/records/check-in")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckInWorkProofRequest(
                                workplaceId,
                                deviceAt,
                                37.5,
                                127.0,
                                "Front door"
                        ))))
                .andExpect(status().isCreated());
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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
