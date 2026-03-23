package com.workproofpay.backend.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.admin.api.dto.request.AdminCreateEmployerCompanyRequest;
import com.workproofpay.backend.auth.api.dto.request.LoginRequest;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.employer.repo.EmployerProfileRepository;
import com.workproofpay.backend.employerauth.api.dto.request.EmployerSignupRequest;
import com.workproofpay.backend.employerauth.repo.EmployerSignupCodeRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-employer-company;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "employer.signup-code-encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "remittance.wallet.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminEmployerCompanyIntegrationTest {

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
    private EmployerSignupCodeRepository employerSignupCodeRepository;

    @Autowired
    private EmployerProfileRepository employerProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        employerProfileRepository.deleteAll();
        employerSignupCodeRepository.deleteAll();
        workplaceRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminCanCreateCompanyAndIssuedCodeCanBeUsedForEmployerSignup() throws Exception {
        User admin = userRepository.save(User.registerAdmin(
                "admin@dondone.local",
                passwordEncoder.encode("qweqwe123"),
                "Service Admin"
        ));

        String adminToken = loginAndReadAccessToken(new LoginRequest("admin@dondone.local", "qweqwe123"));

        AdminCreateEmployerCompanyRequest createRequest = new AdminCreateEmployerCompanyRequest(
                "Acme Logistics",
                "DN-SEOUL-3001"
        );

        MvcResult createResult = mockMvc.perform(post("/api/admin/employers/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.companyName").value(createRequest.companyName()))
                .andExpect(jsonPath("$.data.companyCode").value("DN-SEOUL-3001"))
                .andExpect(jsonPath("$.data.defaultWorkplaceName").value("Acme Logistics 기본 사업장"))
                .andExpect(jsonPath("$.data.workplaceSettingsConfigured").value(false))
                .andExpect(jsonPath("$.data.hasJoinedEmployer").value(false))
                .andExpect(jsonPath("$.data.employerCount").value(0))
                .andExpect(jsonPath("$.data.employerSignupCode").isNotEmpty())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long companyId = createBody.path("data").path("companyId").asLong();
        String employerSignupCode = createBody.path("data").path("employerSignupCode").asText();

        mockMvc.perform(get("/api/admin/employers/companies")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companies[0].companyName").value(createRequest.companyName()))
                .andExpect(jsonPath("$.data.companies[0].companyCode").value("DN-SEOUL-3001"))
                .andExpect(jsonPath("$.data.companies[0].workplaceSettingsConfigured").value(false))
                .andExpect(jsonPath("$.data.companies[0].hasJoinedEmployer").value(false))
                .andExpect(jsonPath("$.data.companies[0].employerCount").value(0))
                .andExpect(jsonPath("$.data.companies[0].hasActiveEmployerSignupCode").value(true));

        mockMvc.perform(get("/api/admin/employers/companies/{companyId}/signup-code", companyId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value(createRequest.companyName()))
                .andExpect(jsonPath("$.data.employerSignupCode").value(employerSignupCode));

        mockMvc.perform(get("/api/admin/employers/companies/{companyId}/employers", companyId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyId").value(companyId))
                .andExpect(jsonPath("$.data.companyName").value(createRequest.companyName()))
                .andExpect(jsonPath("$.data.employers").isArray())
                .andExpect(jsonPath("$.data.employers").isEmpty());

        EmployerSignupRequest signupRequest = new EmployerSignupRequest(
                employerSignupCode,
                "Acme Owner",
                "owner@dondone.test",
                "qweqwe123"
        );

        MvcResult signupResult = mockMvc.perform(post("/api/employer-auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.companyName").value(createRequest.companyName()))
                .andExpect(jsonPath("$.data.defaultWorkplaceName").value("Acme Logistics 기본 사업장"))
                .andReturn();

        JsonNode signupBody = objectMapper.readTree(signupResult.getResponse().getContentAsString());
        String employerToken = signupBody.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/employer/profile")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value(signupRequest.displayName()))
                .andExpect(jsonPath("$.data.companyCode").value("DN-SEOUL-3001"));

        mockMvc.perform(get("/api/admin/employers/companies")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companies[0].hasJoinedEmployer").value(true))
                .andExpect(jsonPath("$.data.companies[0].employerCount").value(1));

        mockMvc.perform(get("/api/admin/employers/companies/{companyId}/employers", companyId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyId").value(companyId))
                .andExpect(jsonPath("$.data.companyName").value(createRequest.companyName()))
                .andExpect(jsonPath("$.data.employers[0].displayName").value(signupRequest.displayName()))
                .andExpect(jsonPath("$.data.employers[0].email").value("owner@dondone.test"))
                .andExpect(jsonPath("$.data.employers[0].profileStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.employers[0].workplaceSettingsConfigured").value(false));
    }

    @Test
    void employerTokenCannotCreateAdminCompany() throws Exception {
        User employer = userRepository.save(User.registerEmployer(
                "manager@dondone.test",
                passwordEncoder.encode("qweqwe123"),
                "Acme Owner"
        ));

        String employerToken = loginAndReadAccessToken(new LoginRequest(employer.getEmail(), "qweqwe123"));

        AdminCreateEmployerCompanyRequest createRequest = new AdminCreateEmployerCompanyRequest(
                "Acme Logistics",
                "DN-SEOUL-3002"
        );

        mockMvc.perform(post("/api/admin/employers/companies")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreateCompanyRejectsInvalidCompanyCodeFormat() throws Exception {
        userRepository.save(User.registerAdmin(
                "admin@dondone.local",
                passwordEncoder.encode("qweqwe123"),
                "Service Admin"
        ));

        String adminToken = loginAndReadAccessToken(new LoginRequest("admin@dondone.local", "qweqwe123"));

        AdminCreateEmployerCompanyRequest createRequest = new AdminCreateEmployerCompanyRequest(
                "Acme Logistics",
                "돈던 서울"
        );

        mockMvc.perform(post("/api/admin/employers/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.details[0].field").value("companyCode"))
                .andExpect(jsonPath("$.details[0].message").value("companyCode format is invalid"));
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
}
