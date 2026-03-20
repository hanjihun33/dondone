package com.workproofpay.backend.employer;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.correction.model.CorrectionRequest;
import com.workproofpay.backend.correction.repo.CorrectionRequestRepository;
import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.model.EmployerProfile;
import com.workproofpay.backend.employer.model.EmploymentMembership;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.employer.repo.EmployerProfileRepository;
import com.workproofpay.backend.employer.repo.EmploymentMembershipRepository;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:employer-issue-read-model;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "remittance.wallet.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployerIssueReadModelIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private WorkProofRepository workProofRepository;

    @Autowired
    private CorrectionRequestRepository correctionRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        correctionRequestRepository.deleteAll();
        workProofRepository.deleteAll();
        employmentMembershipRepository.deleteAll();
        employerProfileRepository.deleteAll();
        workplaceRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getIssuesReturnsPendingCorrectionRequestsAndReviewRequiredRecords() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/issues")
                        .header("Authorization", bearer(fixture.employerUser()))
                        .queryParam("page", "1")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.issues[?(@.itemType == 'CORRECTION_REQUEST')].requestId", hasItem(fixture.pendingRequest().getId().intValue())))
                .andExpect(jsonPath("$.data.issues[?(@.itemType == 'CORRECTION_REQUEST')].issueStatus", hasItem("PENDING")))
                .andExpect(jsonPath("$.data.issues[?(@.itemType == 'CORRECTION_REQUEST')].reason", hasItem("Fix late subway arrival")))
                .andExpect(jsonPath("$.data.issues[?(@.itemType == 'REVIEW_REQUIRED_RECORD')].workProofId", hasItem(fixture.reviewWorkProof().getId().intValue())))
                .andExpect(jsonPath("$.data.issues[?(@.itemType == 'REVIEW_REQUIRED_RECORD')].issueStatus", hasItem("NEEDS_REVIEW")))
                .andExpect(jsonPath("$.data.issues[?(@.itemType == 'REVIEW_REQUIRED_RECORD')].reviewReasonCode", hasItem("CLOCK_OUT_OUTSIDE_ALLOWED_RADIUS")));
    }

    @Test
    void getIssuesCanFilterByItemType() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/issues")
                        .header("Authorization", bearer(fixture.employerUser()))
                        .queryParam("itemTypes", "REVIEW_REQUIRED_RECORD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.issues[0].itemType").value("REVIEW_REQUIRED_RECORD"))
                .andExpect(jsonPath("$.data.issues[0].workProofId").value(fixture.reviewWorkProof().getId()));
    }

    @Test
    void getIssuesCanSearchAcrossReasonAndWorker() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/issues")
                        .header("Authorization", bearer(fixture.employerUser()))
                        .queryParam("query", "outside"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.issues[0].itemType").value("REVIEW_REQUIRED_RECORD"))
                .andExpect(jsonPath("$.data.issues[0].workerName").value("Review Worker"));
    }

    @Test
    void getReviewRecordReturnsScopedDetail() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/issues/review-records/{workProofId}", fixture.reviewWorkProof().getId())
                        .header("Authorization", bearer(fixture.employerUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.workProofId").value(fixture.reviewWorkProof().getId()))
                .andExpect(jsonPath("$.data.workerName").value("Review Worker"))
                .andExpect(jsonPath("$.data.recordStatus").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.data.reflectionStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.reviewReasonCode").value("CLOCK_OUT_OUTSIDE_ALLOWED_RADIUS"))
                .andExpect(jsonPath("$.data.clockOutOutsideAllowedRadius").value(true))
                .andExpect(jsonPath("$.data.workplace.name").value("Seoul Hub"))
                .andExpect(jsonPath("$.data.checkOut.locationLabel").value("Office"));
    }

    @Test
    void nonReviewRecordCannotBeOpenedAsReviewDetail() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/issues/review-records/{workProofId}", fixture.correctionWorkProof().getId())
                        .header("Authorization", bearer(fixture.employerUser())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORKPROOF_NOT_FOUND"));
    }

    @Test
    void workerTokenCannotAccessEmployerIssues() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/issues")
                        .header("Authorization", bearer(fixture.correctionWorker())))
                .andExpect(status().isForbidden());
    }

    private Fixture createFixture() {
        Company company = companyRepository.save(Company.create("Acme Logistics", "ACME-SEOUL"));
        Company otherCompany = companyRepository.save(Company.create("Other Logistics", "OTHER-SEOUL"));
        User workplaceOwner = userRepository.save(User.register(
                "worker-owner@example.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker Owner"
        ));
        Workplace workplace = workplaceRepository.save(Workplace.create(
                workplaceOwner,
                company.getId(),
                "Seoul Hub",
                "Seoul Address 212",
                null,
                37.501274,
                127.039585,
                300
        ));
        Workplace otherWorkplace = workplaceRepository.save(Workplace.create(
                workplaceOwner,
                otherCompany.getId(),
                "Busan Hub",
                "Busan Address 48",
                null,
                35.171,
                129.131,
                300
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

        User correctionWorker = createScopedWorker("correction-worker@acme.test", "Correction Worker", company.getId(), workplace.getId());
        User reviewWorker = createScopedWorker("review-worker@acme.test", "Review Worker", company.getId(), workplace.getId());
        User otherScopeWorker = createScopedWorker("other-worker@other.test", "Other Scope Worker", otherCompany.getId(), otherWorkplace.getId());

        WorkProof correctionWorkProof = workProofRepository.save(completedRecord(
                correctionWorker,
                workplace,
                LocalDate.of(2026, 3, 17),
                LocalDateTime.of(2026, 3, 17, 9, 0),
                LocalDateTime.of(2026, 3, 17, 18, 0),
                false
        ));
        WorkProof reviewWorkProof = workProofRepository.save(completedRecord(
                reviewWorker,
                workplace,
                LocalDate.of(2026, 3, 18),
                LocalDateTime.of(2026, 3, 18, 9, 10),
                LocalDateTime.of(2026, 3, 18, 18, 20),
                true
        ));
        WorkProof otherScopeWorkProof = workProofRepository.save(completedRecord(
                otherScopeWorker,
                otherWorkplace,
                LocalDate.of(2026, 3, 18),
                LocalDateTime.of(2026, 3, 18, 9, 0),
                LocalDateTime.of(2026, 3, 18, 18, 0),
                true
        ));

        CorrectionRequest pendingRequest = correctionRequestRepository.save(CorrectionRequest.create(
                correctionWorkProof,
                correctionWorker.getId(),
                correctionWorker.getId(),
                company.getId(),
                workplace.getId(),
                correctionWorkProof.getWorkDate(),
                correctionWorkProof.getClockInAt(),
                correctionWorkProof.getClockOutAt(),
                LocalDateTime.of(2026, 3, 17, 9, 20),
                LocalDateTime.of(2026, 3, 17, 18, 10),
                "Fix late subway arrival",
                "Subway delay memo",
                0,
                null
        ));
        correctionRequestRepository.save(CorrectionRequest.create(
                otherScopeWorkProof,
                otherScopeWorker.getId(),
                otherScopeWorker.getId(),
                otherCompany.getId(),
                otherWorkplace.getId(),
                otherScopeWorkProof.getWorkDate(),
                otherScopeWorkProof.getClockInAt(),
                otherScopeWorkProof.getClockOutAt(),
                LocalDateTime.of(2026, 3, 18, 9, 25),
                LocalDateTime.of(2026, 3, 18, 18, 15),
                "Out of scope request",
                null,
                0,
                null
        ));

        return new Fixture(
                employerUser,
                correctionWorker,
                correctionWorkProof,
                reviewWorkProof,
                pendingRequest
        );
    }

    private User createScopedWorker(String email, String name, Long companyId, Long workplaceId) {
        User worker = userRepository.save(User.register(
                email,
                passwordEncoder.encode("qweqwe123"),
                name
        ));
        employmentMembershipRepository.save(EmploymentMembership.create(
                worker.getId(),
                companyId,
                workplaceId,
                LocalDate.now().minusDays(1)
        ));
        return worker;
    }

    private WorkProof completedRecord(User worker,
                                      Workplace workplace,
                                      LocalDate workDate,
                                      LocalDateTime checkInAt,
                                      LocalDateTime checkOutAt,
                                      boolean outsideAllowedRadius) {
        WorkProof record = WorkProof.checkIn(
                worker,
                workplace,
                null,
                checkInAt,
                checkInAt.plusMinutes(1),
                37.501274,
                127.039585,
                "Office"
        );
        record.completeCheckOut(
                checkOutAt,
                checkOutAt.plusMinutes(1),
                37.501274,
                127.039585,
                "Office",
                outsideAllowedRadius
        );
        return record;
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private record Fixture(
            User employerUser,
            User correctionWorker,
            WorkProof correctionWorkProof,
            WorkProof reviewWorkProof,
            CorrectionRequest pendingRequest
    ) {
    }
}
