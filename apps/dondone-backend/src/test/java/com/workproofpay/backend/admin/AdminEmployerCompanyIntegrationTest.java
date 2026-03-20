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
                "서비스 관리자"
        ));

        String adminToken = loginAndReadAccessToken(new LoginRequest("admin@dondone.local", "qweqwe123"));

        AdminCreateEmployerCompanyRequest createRequest = new AdminCreateEmployerCompanyRequest(
                "돈던 물류",
                "DN-SEOUL-3001"
        );

        MvcResult createResult = mockMvc.perform(post("/api/admin/employers/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.companyName").value("돈던 물류"))
                .andExpect(jsonPath("$.data.companyCode").value("DN-SEOUL-3001"))
                .andExpect(jsonPath("$.data.defaultWorkplaceName").value("돈던 물류 기본 사업장"))
                .andExpect(jsonPath("$.data.workplaceSettingsConfigured").value(false))
                .andExpect(jsonPath("$.data.hasJoinedEmployer").value(false))
                .andExpect(jsonPath("$.data.employerCount").value(0))
                .andExpect(jsonPath("$.data.employerSignupCode").isNotEmpty())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String employerSignupCode = createBody.path("data").path("employerSignupCode").asText();

        mockMvc.perform(get("/api/admin/employers/companies")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companies[0].companyName").value("돈던 물류"))
                .andExpect(jsonPath("$.data.companies[0].companyCode").value("DN-SEOUL-3001"))
                .andExpect(jsonPath("$.data.companies[0].workplaceSettingsConfigured").value(false))
                .andExpect(jsonPath("$.data.companies[0].hasJoinedEmployer").value(false))
                .andExpect(jsonPath("$.data.companies[0].employerCount").value(0))
                .andExpect(jsonPath("$.data.companies[0].hasActiveEmployerSignupCode").value(true));

        mockMvc.perform(get("/api/admin/employers/companies/{companyId}/signup-code", createBody.path("data").path("companyId").asLong())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("돈던 물류"))
                .andExpect(jsonPath("$.data.employerSignupCode").value(employerSignupCode));

        EmployerSignupRequest signupRequest = new EmployerSignupRequest(
                employerSignupCode,
                "돈던 담당자",
                "owner@dondone.test",
                "qweqwe123"
        );

        MvcResult signupResult = mockMvc.perform(post("/api/employer-auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.companyName").value("돈던 물류"))
                .andExpect(jsonPath("$.data.defaultWorkplaceName").value("돈던 물류 기본 사업장"))
                .andReturn();

        JsonNode signupBody = objectMapper.readTree(signupResult.getResponse().getContentAsString());
        String employerToken = signupBody.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/employer/profile")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("돈던 담당자"))
                .andExpect(jsonPath("$.data.companyCode").value("DN-SEOUL-3001"));

        mockMvc.perform(get("/api/admin/employers/companies")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companies[0].hasJoinedEmployer").value(true))
                .andExpect(jsonPath("$.data.companies[0].employerCount").value(1));
    }

    @Test
    void employerTokenCannotCreateAdminCompany() throws Exception {
        User employer = userRepository.save(User.registerEmployer(
                "manager@dondone.test",
                passwordEncoder.encode("qweqwe123"),
                "돈던 담당자"
        ));

        String employerToken = loginAndReadAccessToken(new LoginRequest(employer.getEmail(), "qweqwe123"));

        AdminCreateEmployerCompanyRequest createRequest = new AdminCreateEmployerCompanyRequest(
                "돈던 물류",
                "DN-SEOUL-3002"
        );

        mockMvc.perform(post("/api/admin/employers/companies")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
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
