package com.workproofpay.backend.employer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.auth.api.dto.request.LoginRequest;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.employer.api.dto.request.UpdateEmployerWorkplaceSettingsRequest;
import com.workproofpay.backend.employer.model.AttendanceOvertimeRoundingUnit;
import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.model.EmployerProfile;
import com.workproofpay.backend.employer.model.EmploymentMembership;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.employer.repo.EmployerProfileRepository;
import com.workproofpay.backend.employer.repo.EmploymentMembershipRepository;
import com.workproofpay.backend.employerauth.api.dto.request.EmployerLoginRequest;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:employer-workplace-settings;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "employer.signup-code-encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "remittance.wallet.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployerWorkplaceSettingsIntegrationTest {

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
    private EmployerProfileRepository employerProfileRepository;

    @Autowired
    private EmploymentMembershipRepository employmentMembershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        employmentMembershipRepository.deleteAll();
        employerProfileRepository.deleteAll();
        workplaceRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getSettingsReturnsScopedDefaultWorkplaceAndCountsActiveMemberships() throws Exception {
        Company company = companyRepository.save(Company.create("Acme Logistics", "ACME-SEOUL"));
        User workplaceOwner = userRepository.save(User.register(
                "worker-owner@example.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker Owner"
        ));
        Workplace workplace = workplaceRepository.save(Workplace.create(
                workplaceOwner,
                company.getId(),
                "Seoul Hub",
                "212 Teheran-ro, Seoul",
                "Gate 1",
                37.501274,
                127.039585,
                300
        ));
        createEmployer(company.getId(), workplace.getId(), "manager@acme.test");
        User activeWorker = userRepository.save(User.register(
                "worker1@acme.test",
                passwordEncoder.encode("qweqwe123"),
                "Worker One"
        ));
        User futureWorker = userRepository.save(User.register(
                "worker2@acme.test",
                passwordEncoder.encode("qweqwe123"),
                "Worker Two"
        ));
        employmentMembershipRepository.save(EmploymentMembership.create(
                activeWorker.getId(),
                company.getId(),
                workplace.getId(),
                LocalDate.now().minusDays(1)
        ));
        employmentMembershipRepository.save(EmploymentMembership.create(
                futureWorker.getId(),
                company.getId(),
                workplace.getId(),
                LocalDate.now().plusDays(1)
        ));

        String accessToken = loginEmployer("manager@acme.test", "qweqwe123");

        mockMvc.perform(get("/api/employer/workplace-settings")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.workplaceId").value(workplace.getId()))
                .andExpect(jsonPath("$.data.workplaceName").value("Seoul Hub"))
                .andExpect(jsonPath("$.data.address").value("212 Teheran-ro, Seoul"))
                .andExpect(jsonPath("$.data.detailAddress").value("Gate 1"))
                .andExpect(jsonPath("$.data.allowedRadiusMeters").value(300))
                .andExpect(jsonPath("$.data.scheduledClockInTime").value("09:00"))
                .andExpect(jsonPath("$.data.scheduledClockOutTime").value("18:00"))
                .andExpect(jsonPath("$.data.overtimeRoundingUnit").value("FIFTEEN_MINUTES"))
                .andExpect(jsonPath("$.data.activeMembershipCount").value(1));
    }

    @Test
    void getSettingsFallsBackToDefaultsWhenLegacyAttendancePolicyColumnsAreNull() throws Exception {
        Company company = companyRepository.save(Company.create("Acme Logistics", "ACME-SEOUL"));
        User workplaceOwner = userRepository.save(User.register(
                "worker-owner@example.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker Owner"
        ));
        Workplace workplace = workplaceRepository.save(Workplace.create(
                workplaceOwner,
                company.getId(),
                "Seoul Hub",
                "212 Teheran-ro, Seoul",
                "Gate 1",
                37.501274,
                127.039585,
                300
        ));
        createEmployer(company.getId(), workplace.getId(), "manager@acme.test");
        jdbcTemplate.update(
                "update companies set scheduled_clock_in_time = null, scheduled_clock_out_time = null, overtime_rounding_unit = null where id = ?",
                company.getId()
        );

        String accessToken = loginEmployer("manager@acme.test", "qweqwe123");

        mockMvc.perform(get("/api/employer/workplace-settings")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.scheduledClockInTime").value("09:00"))
                .andExpect(jsonPath("$.data.scheduledClockOutTime").value("18:00"))
                .andExpect(jsonPath("$.data.overtimeRoundingUnit").value("FIFTEEN_MINUTES"));
    }

    @Test
    void updateSettingsUsesEmployerScopeInsteadOfLegacyWorkplaceOwner() throws Exception {
        Company company = companyRepository.save(Company.create("Acme Logistics", "ACME-SEOUL"));
        User workplaceOwner = userRepository.save(User.register(
                "worker-owner@example.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker Owner"
        ));
        Workplace workplace = workplaceRepository.save(Workplace.create(
                workplaceOwner,
                company.getId(),
                "Seoul Hub",
                "212 Teheran-ro, Seoul",
                "Gate 1",
                37.501274,
                127.039585,
                300
        ));
        User employerUser = createEmployer(company.getId(), workplace.getId(), "manager@acme.test");
        String accessToken = loginEmployer("manager@acme.test", "qweqwe123");

        UpdateEmployerWorkplaceSettingsRequest request = new UpdateEmployerWorkplaceSettingsRequest(
                "999 Teheran-ro, Seoul",
                "Gate 2",
                37.502,
                127.041,
                500,
                LocalTime.of(8, 30),
                LocalTime.of(17, 30),
                AttendanceOvertimeRoundingUnit.THIRTY_MINUTES
        );

        mockMvc.perform(put("/api/employer/workplace-settings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.address").value("999 Teheran-ro, Seoul"))
                .andExpect(jsonPath("$.data.detailAddress").value("Gate 2"))
                .andExpect(jsonPath("$.data.allowedRadiusMeters").value(500))
                .andExpect(jsonPath("$.data.scheduledClockInTime").value("08:30"))
                .andExpect(jsonPath("$.data.scheduledClockOutTime").value("17:30"))
                .andExpect(jsonPath("$.data.overtimeRoundingUnit").value("THIRTY_MINUTES"))
                .andExpect(jsonPath("$.data.updatedByAccountId").value(employerUser.getId()))
                .andExpect(jsonPath("$.data.effectiveFrom").isNotEmpty());

        Workplace saved = workplaceRepository.findById(workplace.getId()).orElseThrow();
        Company savedCompany = companyRepository.findById(company.getId()).orElseThrow();
        assertThat(saved.getAddress()).isEqualTo("999 Teheran-ro, Seoul");
        assertThat(saved.getMapLabel()).isEqualTo("Gate 2");
        assertThat(saved.getAllowedRadiusMeters()).isEqualTo(500);
        assertThat(saved.getSettingsUpdatedByAccountId()).isEqualTo(employerUser.getId());
        assertThat(saved.getSettingsEffectiveFrom()).isNotNull();
        assertThat(savedCompany.getScheduledClockInTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(savedCompany.getScheduledClockOutTime()).isEqualTo(LocalTime.of(17, 30));
        assertThat(savedCompany.getOvertimeRoundingUnit()).isEqualTo(AttendanceOvertimeRoundingUnit.THIRTY_MINUTES);
    }

    @Test
    void updateSettingsRejectsInvalidRadius() throws Exception {
        Company company = companyRepository.save(Company.create("Acme Logistics", "ACME-SEOUL"));
        User workplaceOwner = userRepository.save(User.register(
                "worker-owner@example.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker Owner"
        ));
        Workplace workplace = workplaceRepository.save(Workplace.create(
                workplaceOwner,
                company.getId(),
                "Seoul Hub",
                "212 Teheran-ro, Seoul",
                null,
                37.501274,
                127.039585,
                300
        ));
        User employerUser = createEmployer(company.getId(), workplace.getId(), "manager@acme.test");
        String accessToken = jwtTokenProvider.createAccessToken(
                employerUser.getId(),
                employerUser.getEmail(),
                employerUser.getRole().name()
        );

        UpdateEmployerWorkplaceSettingsRequest request = new UpdateEmployerWorkplaceSettingsRequest(
                "999 Teheran-ro, Seoul",
                "Gate 2",
                37.502,
                127.041,
                10,
                null,
                null,
                null
        );

        mockMvc.perform(put("/api/employer/workplace-settings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.details[0].field").value("allowedRadiusMeters"));
    }

    @Test
    void updateSettingsRejectsClockOutTimeEarlierThanClockInTime() throws Exception {
        Company company = companyRepository.save(Company.create("Acme Logistics", "ACME-SEOUL"));
        User workplaceOwner = userRepository.save(User.register(
                "worker-owner@example.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker Owner"
        ));
        Workplace workplace = workplaceRepository.save(Workplace.create(
                workplaceOwner,
                company.getId(),
                "Seoul Hub",
                "212 Teheran-ro, Seoul",
                null,
                37.501274,
                127.039585,
                300
        ));
        User employerUser = createEmployer(company.getId(), workplace.getId(), "manager@acme.test");
        String accessToken = jwtTokenProvider.createAccessToken(
                employerUser.getId(),
                employerUser.getEmail(),
                employerUser.getRole().name()
        );

        UpdateEmployerWorkplaceSettingsRequest request = new UpdateEmployerWorkplaceSettingsRequest(
                "999 Teheran-ro, Seoul",
                "Gate 2",
                37.502,
                127.041,
                500,
                LocalTime.of(18, 0),
                LocalTime.of(9, 0),
                AttendanceOvertimeRoundingUnit.FIFTEEN_MINUTES
        );

        mockMvc.perform(put("/api/employer/workplace-settings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.message").value("scheduledClockOutTime must be after scheduledClockInTime"));
    }

    @Test
    void workerTokenCannotAccessEmployerWorkplaceSettings() throws Exception {
        User workerUser = userRepository.save(User.register(
                "worker@example.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker User"
        ));

        String accessToken = loginWorker(workerUser.getEmail(), "qweqwe123");

        mockMvc.perform(get("/api/employer/workplace-settings")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSettingsRejectsWorkplaceOutsideCompanyScope() throws Exception {
        Company company = companyRepository.save(Company.create("Acme Logistics", "ACME-SEOUL"));
        Company otherCompany = companyRepository.save(Company.create("Other Logistics", "OTHER-SEOUL"));
        User workplaceOwner = userRepository.save(User.register(
                "worker-owner@example.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker Owner"
        ));
        Workplace workplace = workplaceRepository.save(Workplace.create(
                workplaceOwner,
                otherCompany.getId(),
                "Other Hub",
                "300 Teheran-ro, Seoul",
                null,
                37.501274,
                127.039585,
                300
        ));
        User employerUser = createEmployer(company.getId(), workplace.getId(), "manager@acme.test");
        String accessToken = jwtTokenProvider.createAccessToken(
                employerUser.getId(),
                employerUser.getEmail(),
                employerUser.getRole().name()
        );

        mockMvc.perform(get("/api/employer/workplace-settings")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMPLOYER_SCOPE_NOT_READY"));
    }

    private User createEmployer(Long companyId, Long defaultWorkplaceId, String email) {
        User employerUser = userRepository.save(User.registerEmployer(
                email,
                passwordEncoder.encode("qweqwe123"),
                "Acme HR"
        ));
        employerProfileRepository.save(EmployerProfile.create(
                employerUser.getId(),
                companyId,
                defaultWorkplaceId,
                "Acme HR"
        ));
        return employerUser;
    }

    private String loginEmployer(String email, String password) throws Exception {
        EmployerLoginRequest request = new EmployerLoginRequest(email, password);

        MvcResult loginResult = mockMvc.perform(post("/api/employer-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginBody.path("data").path("accessToken").asText();
    }

    private String loginWorker(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginBody.path("data").path("accessToken").asText();
    }
}
