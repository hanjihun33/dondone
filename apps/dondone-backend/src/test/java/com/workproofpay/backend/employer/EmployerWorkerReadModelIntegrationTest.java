package com.workproofpay.backend.employer;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:employer-worker-read-model;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "employer.signup-code-encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "remittance.wallet.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployerWorkerReadModelIntegrationTest {

    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 3, 18);
    private static final LocalDate FIXED_WEEK_START = FIXED_TODAY.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    private static final int FIXED_TODAY_INDEX =
            (int) java.time.temporal.ChronoUnit.DAYS.between(FIXED_WEEK_START, FIXED_TODAY);
    private static final int FIXED_PREVIOUS_DAY_INDEX = FIXED_TODAY_INDEX - 1;
    private static final int FIXED_NO_RECORD_DAY_INDEX = FIXED_TODAY_INDEX - 2;

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        workProofRepository.deleteAll();
        employmentMembershipRepository.deleteAll();
        employerProfileRepository.deleteAll();
        workplaceRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getWorkersReturnsScopedWorkersWithSearchAndStatusFilter() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/workers")
                        .header("Authorization", bearer(fixture.employerUser()))
                        .queryParam("query", "review")
                        .queryParam("statuses", "NEEDS_REVIEW")
                        .queryParam("page", "1")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.workers[0].workerId").value(fixture.reviewWorker().getId()))
                .andExpect(jsonPath("$.data.workers[0].name").value("Review Worker"))
                .andExpect(jsonPath("$.data.workers[0].email").value("review-worker@acme.test"))
                .andExpect(jsonPath("$.data.workers[0].recordStatus").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.data.workers[0].reflectionStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.workers[0].attendanceStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.workers[0].latestWorkDate").value(fixture.today().toString()));

        mockMvc.perform(get("/api/employer/workers")
                        .header("Authorization", bearer(fixture.employerUser()))
                        .queryParam("page", "1")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(jsonPath("$.data.workers[?(@.workerId == %s)].attendanceStatus".formatted(fixture.workingWorker().getId())).value("WORKING"))
                .andExpect(jsonPath("$.data.workers[?(@.workerId == %s)].recordStatus".formatted(fixture.workingWorker().getId())).value("CHECKED_IN"))
                .andExpect(jsonPath("$.data.workers[?(@.workerId == %s)].reflectionStatus".formatted(fixture.workingWorker().getId())).value("PENDING"))
                .andExpect(jsonPath("$.data.workers[?(@.workerId == %s)].attendanceStatus".formatted(fixture.completedWorker().getId())).value("COMPLETED"))
                .andExpect(jsonPath("$.data.workers[?(@.workerId == %s)].recordStatus".formatted(fixture.completedWorker().getId())).value("CHECKED_OUT"))
                .andExpect(jsonPath("$.data.workers[?(@.workerId == %s)].reflectionStatus".formatted(fixture.completedWorker().getId())).value("REFLECTED"))
                .andExpect(jsonPath("$.data.workers[?(@.workerId == %s)].attendanceStatus".formatted(fixture.reviewWorker().getId())).value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.workers[?(@.workerId == %s)].recordStatus".formatted(fixture.reviewWorker().getId())).value("CHECKED_OUT"))
                .andExpect(jsonPath("$.data.workers[?(@.workerId == %s)].reflectionStatus".formatted(fixture.reviewWorker().getId())).value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.workers[?(@.workerId == %s)].attendanceStatus".formatted(fixture.noRecordWorker().getId())).value("NO_RECORD"))
                .andExpect(jsonPath("$.data.workers[?(@.workerId == %s)][0].recordStatus".formatted(fixture.noRecordWorker().getId())).doesNotExist())
                .andExpect(jsonPath("$.data.workers[?(@.workerId == %s)][0].reflectionStatus".formatted(fixture.noRecordWorker().getId())).doesNotExist());
    }

    @Test
    void getDashboardSummaryCountsOnlyActiveScopedWorkers() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/dashboard/summary")
                        .header("Authorization", bearer(fixture.employerUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.activeWorkerCount").value(4))
                .andExpect(jsonPath("$.data.workingCount").value(1))
                .andExpect(jsonPath("$.data.completedCount").value(1))
                .andExpect(jsonPath("$.data.needsReviewCount").value(1))
                .andExpect(jsonPath("$.data.noRecordCount").value(1))
                .andExpect(jsonPath("$.data.asOf").value(fixture.today().toString()));
    }

    @Test
    void getWorkerDetailReturnsScopedLatestRecordAndRecentDays() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/workers/{workerId}", fixture.reviewWorker().getId())
                        .header("Authorization", bearer(fixture.employerUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.workerId").value(fixture.reviewWorker().getId()))
                .andExpect(jsonPath("$.data.name").value("Review Worker"))
                .andExpect(jsonPath("$.data.email").value("review-worker@acme.test"))
                .andExpect(jsonPath("$.data.membershipEffectiveFrom").value(fixture.today().minusDays(1).toString()))
                .andExpect(jsonPath("$.data.recordStatus").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.data.reflectionStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.attendanceStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.latestWorkDate").value(fixture.today().toString()))
                .andExpect(jsonPath("$.data.latestRecord.workDate").value(fixture.today().toString()))
                .andExpect(jsonPath("$.data.latestRecord.recordStatus").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.data.latestRecord.reflectionStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.latestRecord.attendanceStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.latestRecord.workedMinutes").value(550))
                .andExpect(jsonPath("$.data.latestRecord.needsReview").value(true))
                .andExpect(jsonPath("$.data.latestRecord.clockOutOutsideAllowedRadius").value(true))
                .andExpect(jsonPath("$.data.latestRecord.edited").value(false))
                .andExpect(jsonPath("$.data.latestRecord.workplaceName").value("Seoul Hub"))
                .andExpect(jsonPath("$.data.latestRecord.workplaceAddress").value("212 Teheran-ro, Gangnam-gu, Seoul"))
                .andExpect(jsonPath("$.data.latestRecord.workplaceMapLabel").doesNotExist())
                .andExpect(jsonPath("$.data.latestRecord.clockInLocationLabel").value("?類ｆ뻼"))
                .andExpect(jsonPath("$.data.latestRecord.clockOutLocationLabel").value("?袁ⓩ뻼"))
                .andExpect(jsonPath("$.data.recentDays.length()").value(7))
                .andExpect(jsonPath("$.data.recentDays[6].date").value(fixture.today().toString()))
                .andExpect(jsonPath("$.data.recentDays[6].attendanceStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.recentDays[6].workedMinutes").value(550));
    }

    @Test
    void getWorkerDetailRejectsWorkerOutsideEmployerScope() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/workers/{workerId}", fixture.otherScopeWorker().getId())
                        .header("Authorization", bearer(fixture.employerUser())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getAttendanceBoardReturnsWeeklyScopedSnapshots() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/employer/dashboard/attendance-board")
                        .header("Authorization", bearer(fixture.employerUser()))
                        .queryParam("weekStart", fixture.weekStart().toString())
                        .queryParam("query", "worker")
                        .queryParam("statuses", "WORKING")
                        .queryParam("page", "1")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.weekStart").value(fixture.weekStart().toString()))
                .andExpect(jsonPath("$.data.weekEnd").value(fixture.weekStart().plusDays(6).toString()))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.rows[0].workerId").value(fixture.workingWorker().getId()))
                .andExpect(jsonPath("$.data.rows[0].days[%s].date".formatted(fixture.previousDayIndex()))
                        .value(fixture.today().minusDays(1).toString()))
                .andExpect(jsonPath("$.data.rows[0].days[%s].attendanceStatus".formatted(fixture.previousDayIndex())).value("COMPLETED"))
                .andExpect(jsonPath("$.data.rows[0].days[%s].recordStatus".formatted(fixture.previousDayIndex())).value("CHECKED_OUT"))
                .andExpect(jsonPath("$.data.rows[0].days[%s].reflectionStatus".formatted(fixture.previousDayIndex())).value("REFLECTED"))
                .andExpect(jsonPath("$.data.rows[0].days[%s].workedMinutes".formatted(fixture.previousDayIndex())).value(480))
                .andExpect(jsonPath("$.data.rows[0].days[%s].attendanceStatus".formatted(fixture.noRecordDayIndex())).value("NO_RECORD"))
                .andExpect(jsonPath("$.data.rows[0].days[%s].recordStatus".formatted(fixture.noRecordDayIndex())).doesNotExist())
                .andExpect(jsonPath("$.data.rows[0].days[%s].attendanceStatus".formatted(fixture.todayIndex())).value("WORKING"))
                .andExpect(jsonPath("$.data.rows[0].days[%s].recordStatus".formatted(fixture.todayIndex())).value("CHECKED_IN"))
                .andExpect(jsonPath("$.data.rows[0].days[%s].reflectionStatus".formatted(fixture.todayIndex())).value("PENDING"))
                .andExpect(jsonPath("$.data.rows[0].days[%s].workedMinutes".formatted(fixture.todayIndex())).doesNotExist());
    }

    @Test
    void workerTokenCannotAccessEmployerWorkers() throws Exception {
        User workerUser = userRepository.save(User.register(
                "worker@example.com",
                passwordEncoder.encode("qweqwe123"),
                "Worker User"
        ));

        mockMvc.perform(get("/api/employer/workers")
                        .header("Authorization", bearer(workerUser)))
                .andExpect(status().isForbidden());
    }

    private Fixture createFixture() {
        LocalDate today = FIXED_TODAY;
        LocalDate weekStart = FIXED_WEEK_START;

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
                "212 Teheran-ro, Gangnam-gu, Seoul",
                null,
                37.501274,
                127.039585,
                300
        ));
        Workplace otherWorkplace = workplaceRepository.save(Workplace.create(
                workplaceOwner,
                otherCompany.getId(),
                "Busan Hub",
                "48 Suyeong-ro, Nam-gu, Busan",
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

        User workingWorker = createScopedWorker("working-worker@acme.test", "Working Worker", company.getId(), workplace.getId());
        User completedWorker = createScopedWorker("completed-worker@acme.test", "Completed Worker", company.getId(), workplace.getId());
        User reviewWorker = createScopedWorker("review-worker@acme.test", "Review Worker", company.getId(), workplace.getId());
        User noRecordWorker = createScopedWorker("no-record-worker@acme.test", "No Record Worker", company.getId(), workplace.getId());
        createScopedWorker("future-worker@acme.test", "Future Worker", company.getId(), workplace.getId(), FIXED_TODAY.plusDays(1));
        User otherScopeWorker = createScopedWorker("other-scope-worker@acme.test", "Other Scope Worker", otherCompany.getId(), otherWorkplace.getId());

        workProofRepository.save(completedRecord(
                workingWorker,
                workplace,
                today.minusDays(1),
                9,
                0,
                17,
                0,
                false
        ));

        workProofRepository.save(WorkProof.checkIn(
                workingWorker,
                workplace,
                null,
                today.atTime(9, 0),
                LocalDateTime.now(),
                37.501274,
                127.039585,
                "?類ｆ뻼"
        ));

        workProofRepository.save(completedRecord(
                completedWorker,
                workplace,
                today.minusDays(2),
                8,
                50,
                18,
                0,
                false
        ));
        workProofRepository.save(completedRecord(
                completedWorker,
                workplace,
                today,
                8,
                50,
                18,
                0,
                false
        ));

        workProofRepository.save(completedRecord(
                reviewWorker,
                workplace,
                today.minusDays(2),
                8,
                55,
                18,
                5,
                true
        ));
        workProofRepository.save(completedRecord(
                reviewWorker,
                workplace,
                today,
                8,
                55,
                18,
                5,
                true
        ));

        return new Fixture(
                employerUser,
                workingWorker,
                completedWorker,
                reviewWorker,
                noRecordWorker,
                otherScopeWorker,
                weekStart,
                today,
                FIXED_TODAY_INDEX,
                FIXED_PREVIOUS_DAY_INDEX,
                FIXED_NO_RECORD_DAY_INDEX
        );
    }

    private WorkProof completedRecord(User worker,
                                      Workplace workplace,
                                      LocalDate workDate,
                                      int checkInHour,
                                      int checkInMinute,
                                      int checkOutHour,
                                      int checkOutMinute,
                                      boolean outsideAllowedRadius) {
        WorkProof record = WorkProof.checkIn(
                worker,
                workplace,
                null,
                workDate.atTime(checkInHour, checkInMinute),
                LocalDateTime.now(),
                37.501274,
                127.039585,
                "?類ｆ뻼"
        );
        record.completeCheckOut(
                workDate.atTime(checkOutHour, checkOutMinute),
                LocalDateTime.now(),
                outsideAllowedRadius ? 37.506000 : 37.501274,
                outsideAllowedRadius ? 127.050000 : 127.039585,
                outsideAllowedRadius ? "?袁ⓩ뻼" : "?類ｆ뻼",
                outsideAllowedRadius
        );
        return record;
    }

    private User createScopedWorker(String email, String name, Long companyId, Long workplaceId) {
        return createScopedWorker(email, name, companyId, workplaceId, FIXED_TODAY.minusDays(1));
    }

    private User createScopedWorker(String email,
                                    String name,
                                    Long companyId,
                                    Long workplaceId,
                                    LocalDate effectiveFrom) {
        User worker = userRepository.save(User.register(
                email,
                passwordEncoder.encode("qweqwe123"),
                name
        ));
        employmentMembershipRepository.save(EmploymentMembership.create(
                worker.getId(),
                companyId,
                workplaceId,
                effectiveFrom
        ));
        return worker;
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private record Fixture(
            User employerUser,
            User workingWorker,
            User completedWorker,
            User reviewWorker,
            User noRecordWorker,
            User otherScopeWorker,
            LocalDate weekStart,
            LocalDate today,
            int todayIndex,
            int previousDayIndex,
            int noRecordDayIndex
    ) {
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                    Instant.parse("2026-03-17T15:00:00Z"),
                    ZoneId.of("Asia/Seoul")
            );
        }
    }
}

