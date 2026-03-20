package com.workproofpay.backend.employerauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.auth.api.dto.request.LoginRequest;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.model.EmployerProfile;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.employer.repo.EmployerProfileRepository;
import com.workproofpay.backend.employerauth.api.dto.request.EmployerInvitationAcceptRequest;
import com.workproofpay.backend.employerauth.api.dto.request.EmployerLoginRequest;
import com.workproofpay.backend.employerauth.model.EmployerInvitationToken;
import com.workproofpay.backend.employerauth.repo.EmployerInvitationTokenRepository;
import com.workproofpay.backend.workproof.model.Workplace;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:employer-auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "remittance.wallet.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployerAuthIntegrationTest {

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
    private EmployerInvitationTokenRepository employerInvitationTokenRepository;

    @Autowired
    private EmployerProfileRepository employerProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        employerInvitationTokenRepository.deleteAll();
        employerProfileRepository.deleteAll();
        workplaceRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void acceptInvitationCreatesEmployerAccount() throws Exception {
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
                "Seoul",
                null,
                37.5665,
                126.9780,
                100
        ));
        employerInvitationTokenRepository.save(EmployerInvitationToken.create(
                "invite-token-1",
                "manager@acme.test",
                company.getId(),
                workplace.getId(),
                LocalDateTime.now().plusDays(1),
                null
        ));

        EmployerInvitationAcceptRequest request = new EmployerInvitationAcceptRequest(
                "invite-token-1",
                "manager@acme.test",
                "qweqwe123",
                "Acme HR"
        );

        mockMvc.perform(post("/api/employer-auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.companyId").value(company.getId()))
                .andExpect(jsonPath("$.data.defaultWorkplaceId").value(workplace.getId()))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void invitationTokenIsStoredHashed() {
        EmployerInvitationToken token = employerInvitationTokenRepository.save(EmployerInvitationToken.create(
                "invite-token-plain",
                "manager@acme.test",
                1L,
                2L,
                LocalDateTime.now().plusDays(1),
                null
        ));

        assertNotEquals("invite-token-plain", token.getTokenHash());
        assertEquals(64, token.getTokenHash().length());
    }

    @Test
    void acceptInvitationRejectsUsedToken() throws Exception {
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
                "Seoul",
                null,
                37.5665,
                126.9780,
                100
        ));
        EmployerInvitationToken usedToken = EmployerInvitationToken.create(
                "invite-token-used",
                "manager@acme.test",
                company.getId(),
                workplace.getId(),
                LocalDateTime.now().plusDays(1),
                null
        );
        usedToken.markUsed(LocalDateTime.now());
        employerInvitationTokenRepository.save(usedToken);

        EmployerInvitationAcceptRequest request = new EmployerInvitationAcceptRequest(
                "invite-token-used",
                "manager@acme.test",
                "qweqwe123",
                "Acme HR"
        );

        mockMvc.perform(post("/api/employer-auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_EMPLOYER_INVITATION"));
    }

    @Test
    void acceptInvitationRejectsCaseOnlyDuplicateWorkerEmail() throws Exception {
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
                "Seoul",
                null,
                37.5665,
                126.9780,
                100
        ));
        userRepository.save(User.register(
                "Worker@Acme.Test",
                passwordEncoder.encode("qweqwe123"),
                "Existing Worker"
        ));
        employerInvitationTokenRepository.save(EmployerInvitationToken.create(
                "invite-token-dup-case",
                "worker@acme.test",
                company.getId(),
                workplace.getId(),
                LocalDateTime.now().plusDays(1),
                null
        ));

        EmployerInvitationAcceptRequest request = new EmployerInvitationAcceptRequest(
                "invite-token-dup-case",
                "worker@acme.test",
                "qweqwe123",
                "Acme HR"
        );

        mockMvc.perform(post("/api/employer-auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void acceptInvitationRejectsWorkplaceOutsideCompanyScope() throws Exception {
        Company invitedCompany = companyRepository.save(Company.create("Acme Logistics", "ACME-SEOUL"));
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
                "Seoul",
                null,
                37.5665,
                126.9780,
                100
        ));
        employerInvitationTokenRepository.save(EmployerInvitationToken.create(
                "invite-token-mismatch",
                "manager@acme.test",
                invitedCompany.getId(),
                workplace.getId(),
                LocalDateTime.now().plusDays(1),
                null
        ));

        EmployerInvitationAcceptRequest request = new EmployerInvitationAcceptRequest(
                "invite-token-mismatch",
                "manager@acme.test",
                "qweqwe123",
                "Acme HR"
        );

        mockMvc.perform(post("/api/employer-auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMPLOYER_SCOPE_NOT_READY"));
    }

    @Test
    void employerLoginAndProfileBootstrapSucceed() throws Exception {
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
                "Seoul",
                null,
                37.5665,
                126.9780,
                100
        ));
        User employerUser = userRepository.save(User.registerEmployer(
                "manager@acme.test",
                passwordEncoder.encode("qweqwe123"),
                "Acme HR"
        ));
        employerProfileRepository.save(EmployerProfile.create(
                employerUser.getId(),
                company.getId(),
                workplace.getId(),
                "Acme HR"
        ));

        EmployerLoginRequest loginRequest = new EmployerLoginRequest("manager@acme.test", "qweqwe123");

        MvcResult loginResult = mockMvc.perform(post("/api/employer-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.companyName").value("Acme Logistics"))
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/employer/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Acme HR"))
                .andExpect(jsonPath("$.data.companyCode").value("ACME-SEOUL"))
                .andExpect(jsonPath("$.data.defaultWorkplaceName").value("Seoul Hub"));
    }

    @Test
    void workerTokenCannotAccessEmployerProfile() throws Exception {
        User workerUser = userRepository.save(User.register(
                "worker@example.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker User"
        ));

        LoginRequest loginRequest = new LoginRequest(workerUser.getEmail(), "qweqwe123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/employer/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }
}
