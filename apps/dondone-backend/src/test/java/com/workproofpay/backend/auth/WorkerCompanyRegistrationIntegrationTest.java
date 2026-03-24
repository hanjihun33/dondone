package com.workproofpay.backend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.admin.api.dto.request.AdminCreateEmployerCompanyRequest;
import com.workproofpay.backend.auth.api.dto.request.LoginRequest;
import com.workproofpay.backend.auth.api.dto.request.RedeemWorkerRegistrationCodeRequest;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.employer.model.EmploymentMembership;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.employer.repo.EmployerProfileRepository;
import com.workproofpay.backend.employer.repo.EmploymentMembershipRepository;
import com.workproofpay.backend.employer.repo.WorkerRegistrationCodeRepository;
import com.workproofpay.backend.employerauth.api.dto.request.EmployerSignupRequest;
import com.workproofpay.backend.employerauth.repo.EmployerSignupCodeRepository;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
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

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:worker-company-registration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "employer.signup-code-encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "worker.registration-code-encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "remittance.wallet.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkerCompanyRegistrationIntegrationTest {

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
    private EmployerSignupCodeRepository employerSignupCodeRepository;

    @Autowired
    private EmployerProfileRepository employerProfileRepository;

    @Autowired
    private WorkerRegistrationCodeRepository workerRegistrationCodeRepository;

    @Autowired
    private EmploymentMembershipRepository employmentMembershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        employmentMembershipRepository.deleteAll();
        workerRegistrationCodeRepository.deleteAll();
        employerProfileRepository.deleteAll();
        employerSignupCodeRepository.deleteAll();
        workContractRepository.deleteAll();
        workplaceRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void employerCanIssueAndWorkerCanRedeemRegistrationCode() throws Exception {
        String adminToken = createAdminAndLogin();
        CreatedCompanyFixture fixture = createCompanyAndEmployer(adminToken, "Acme Logistics", "DN-SEOUL-4101");
        String employerToken = fixture.employerToken();
        String workerToken = createWorkerAndLogin("worker1@dondone.test", "01011112222");

        MvcResult firstIssueResult = mockMvc.perform(post("/api/employer/worker-registration-codes")
                        .header("Authorization", bearer(employerToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.registrationCode").value(org.hamcrest.Matchers.startsWith("WORKER-")))
                .andReturn();

        JsonNode firstIssueBody = objectMapper.readTree(firstIssueResult.getResponse().getContentAsString());
        long firstCodeId = firstIssueBody.path("data").path("codeId").asLong();
        String firstCode = firstIssueBody.path("data").path("registrationCode").asText();

        MvcResult secondIssueResult = mockMvc.perform(post("/api/employer/worker-registration-codes")
                        .header("Authorization", bearer(employerToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn();

        JsonNode secondIssueBody = objectMapper.readTree(secondIssueResult.getResponse().getContentAsString());
        String activeCode = secondIssueBody.path("data").path("registrationCode").asText();

        mockMvc.perform(get("/api/employer/worker-registration-codes")
                        .header("Authorization", bearer(employerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("Acme Logistics"))
                .andExpect(jsonPath("$.data.codes[0].registrationCode").value(activeCode))
                .andExpect(jsonPath("$.data.codes[0].active").value(true))
                .andExpect(jsonPath("$.data.codes[1].codeId").value(firstCodeId))
                .andExpect(jsonPath("$.data.codes[1].registrationCode").value(firstCode))
                .andExpect(jsonPath("$.data.codes[1].active").value(false));

        mockMvc.perform(post("/api/auth/me/worker-registration-code")
                        .header("Authorization", bearer(workerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemWorkerRegistrationCodeRequest(activeCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("Acme Logistics"))
                .andExpect(jsonPath("$.data.companyCode").value("DN-SEOUL-4101"))
                .andExpect(jsonPath("$.data.workplaceName").value("Acme Logistics 기본 사업장"))
                .andExpect(jsonPath("$.data.membershipStatus").value("ACTIVE"));

        List<EmploymentMembership> memberships = employmentMembershipRepository.findAll();
        assertThat(memberships).hasSize(1);
        assertThat(memberships.get(0).getCompanyId()).isEqualTo(fixture.companyId());
        assertThat(memberships.get(0).getWorkplaceId()).isEqualTo(fixture.workplaceId());
        mockMvc.perform(post("/api/auth/me/worker-registration-code")
                        .header("Authorization", bearer(workerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemWorkerRegistrationCodeRequest(activeCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyCode").value("DN-SEOUL-4101"));

        assertThat(employmentMembershipRepository.findAll()).hasSize(1);
    }

    @Test
    void redeemedWorkerCanReadMembershipWorkplaceAndCurrentContract() throws Exception {
        String adminToken = createAdminAndLogin();
        CreatedCompanyFixture fixture = createCompanyAndEmployer(adminToken, "Access Logistics", "DN-SEOUL-4105");
        String workerToken = createWorkerAndLogin("worker5@dondone.test", "01099990000");

        String registrationCode = objectMapper.readTree(mockMvc.perform(post("/api/employer/worker-registration-codes")
                        .header("Authorization", bearer(fixture.employerToken())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString())
                .path("data")
                .path("registrationCode")
                .asText();

        mockMvc.perform(post("/api/auth/me/worker-registration-code")
                        .header("Authorization", bearer(workerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemWorkerRegistrationCodeRequest(registrationCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.membershipStatus").value("ACTIVE"));

        mockMvc.perform(get("/api/workproof/workplaces")
                        .header("Authorization", bearer(workerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workplaces.length()").value(1))
                .andExpect(jsonPath("$.data.workplaces[0].workplaceId").value(fixture.workplaceId()))
                .andExpect(jsonPath("$.data.workplaces[0].hasActiveContract").value(true));

        mockMvc.perform(get("/api/workproof/contracts/current")
                        .header("Authorization", bearer(workerToken))
                        .param("workplaceId", Long.toString(fixture.workplaceId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workplaceId").value(fixture.workplaceId()))
                .andExpect(jsonPath("$.data.payUnit").value("HOURLY"))
                .andExpect(jsonPath("$.data.basePayAmount").value(12000))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void revokedWorkerRegistrationCodeCannotBeRedeemed() throws Exception {
        String adminToken = createAdminAndLogin();
        CreatedCompanyFixture fixture = createCompanyAndEmployer(adminToken, "Beta Logistics", "DN-SEOUL-4102");
        String employerToken = fixture.employerToken();
        String workerToken = createWorkerAndLogin("worker2@dondone.test", "01033334444");

        MvcResult issueResult = mockMvc.perform(post("/api/employer/worker-registration-codes")
                        .header("Authorization", bearer(employerToken)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode issueBody = objectMapper.readTree(issueResult.getResponse().getContentAsString());
        long codeId = issueBody.path("data").path("codeId").asLong();
        String code = issueBody.path("data").path("registrationCode").asText();

        mockMvc.perform(post("/api/employer/worker-registration-codes/{codeId}/revoke", codeId)
                        .header("Authorization", bearer(employerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(post("/api/auth/me/worker-registration-code")
                        .header("Authorization", bearer(workerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemWorkerRegistrationCodeRequest(code))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WORKER_REGISTRATION_CODE"));
    }

    @Test
    void lowercaseWorkerRegistrationCodeIsRejectedByValidation() throws Exception {
        String adminToken = createAdminAndLogin();
        CreatedCompanyFixture fixture = createCompanyAndEmployer(adminToken, "Delta Logistics", "DN-SEOUL-4104");
        String employerToken = fixture.employerToken();
        String workerToken = createWorkerAndLogin("worker4@dondone.test", "01077778888");

        MvcResult issueResult = mockMvc.perform(post("/api/employer/worker-registration-codes")
                        .header("Authorization", bearer(employerToken)))
                .andExpect(status().isCreated())
                .andReturn();

        String code = objectMapper.readTree(issueResult.getResponse().getContentAsString())
                .path("data")
                .path("registrationCode")
                .asText()
                .toLowerCase(Locale.ROOT);

        mockMvc.perform(post("/api/auth/me/worker-registration-code")
                        .header("Authorization", bearer(workerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemWorkerRegistrationCodeRequest(code))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.details[0].field").value("registrationCode"));
    }

    @Test
    void workerCannotIssueAndAdminCannotRedeemWorkerRegistrationCode() throws Exception {
        String adminToken = createAdminAndLogin();
        CreatedCompanyFixture fixture = createCompanyAndEmployer(adminToken, "Gamma Logistics", "DN-SEOUL-4103");
        String employerToken = fixture.employerToken();
        String workerToken = createWorkerAndLogin("worker3@dondone.test", "01055556666");

        mockMvc.perform(post("/api/employer/worker-registration-codes")
                        .header("Authorization", bearer(workerToken)))
                .andExpect(status().isForbidden());

        MvcResult issueResult = mockMvc.perform(post("/api/employer/worker-registration-codes")
                        .header("Authorization", bearer(employerToken)))
                .andExpect(status().isCreated())
                .andReturn();

        String code = objectMapper.readTree(issueResult.getResponse().getContentAsString())
                .path("data")
                .path("registrationCode")
                .asText();

        mockMvc.perform(post("/api/auth/me/worker-registration-code")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemWorkerRegistrationCodeRequest(code))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminRejectsInvalidCompanyCodeBeforeWorkerRedeemFlow() throws Exception {
        String adminToken = createAdminAndLogin();

        AdminCreateEmployerCompanyRequest createRequest = new AdminCreateEmployerCompanyRequest(
                "Invalid Logistics",
                "돈던 서울"
        );

        mockMvc.perform(post("/api/admin/employers/companies")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.details[0].field").value("companyCode"))
                .andExpect(jsonPath("$.details[0].message").value("companyCode format is invalid"));
    }

    private String createAdminAndLogin() throws Exception {
        userRepository.save(User.registerAdmin(
                "admin@dondone.local",
                passwordEncoder.encode("qweqwe123"),
                "Service Admin"
        ));
        return loginAndReadAccessToken(new LoginRequest("admin@dondone.local", "qweqwe123"));
    }

    private CreatedCompanyFixture createCompanyAndEmployer(
            String adminToken,
            String companyName,
            String companyCode
    ) throws Exception {
        AdminCreateEmployerCompanyRequest createRequest = new AdminCreateEmployerCompanyRequest(companyName, companyCode);

        MvcResult createResult = mockMvc.perform(post("/api/admin/employers/companies")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long companyId = createBody.path("data").path("companyId").asLong();
        long workplaceId = createBody.path("data").path("defaultWorkplaceId").asLong();
        String employerSignupCode = createBody.path("data").path("employerSignupCode").asText();

        String employerEmail = "owner+" + companyCode.toLowerCase() + "@dondone.test";
        EmployerSignupRequest signupRequest = new EmployerSignupRequest(
                employerSignupCode,
                companyName + " Owner",
                employerEmail,
                "qweqwe123"
        );

        MvcResult signupResult = mockMvc.perform(post("/api/employer-auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode signupBody = objectMapper.readTree(signupResult.getResponse().getContentAsString());
        return new CreatedCompanyFixture(
                companyId,
                workplaceId,
                signupBody.path("data").path("accessToken").asText()
        );
    }

    private String createWorkerAndLogin(String email, String phoneNumber) throws Exception {
        userRepository.save(User.register(
                email,
                passwordEncoder.encode("qweqwe123"),
                "Worker",
                phoneNumber
        ));
        return loginAndReadAccessToken(new LoginRequest(email, "qweqwe123"));
    }

    private String loginAndReadAccessToken(LoginRequest request) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record CreatedCompanyFixture(
            long companyId,
            long workplaceId,
            String employerToken
    ) {
    }
}
