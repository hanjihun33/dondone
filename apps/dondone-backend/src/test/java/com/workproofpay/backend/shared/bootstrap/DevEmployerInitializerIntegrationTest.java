package com.workproofpay.backend.shared.bootstrap;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.employer.model.EmployerProfile;
import com.workproofpay.backend.employer.repo.EmployerProfileRepository;
import com.workproofpay.backend.workproof.model.WorkContract;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dev-employer-initializer;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "jwt.secret=test-dondone-secret-key-change-this-at-least-32chars",
        "employer.signup-code-encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "remittance.wallet.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@ActiveProfiles({"test", "demo"})
class DevEmployerInitializerIntegrationTest {

    private static final String EMPLOYER_EMAIL = "manager@gmail.com";
    private static final String EXTRA_WORKER_EMAIL = "worker.extra@acme.test";

    @Autowired
    private DevEmployerInitializer devEmployerInitializer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployerProfileRepository employerProfileRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private WorkContractRepository workContractRepository;

    @Autowired
    private WorkProofRepository workProofRepository;

    @Test
    void rerunDeletesWorkplaceScopedProofsBeforeDeletingContracts() {
        User employer = userRepository.findByEmailIgnoreCase(EMPLOYER_EMAIL).orElseThrow();
        EmployerProfile employerProfile = employerProfileRepository.findByAccountId(employer.getId()).orElseThrow();
        Workplace workplace = workplaceRepository.findById(employerProfile.getDefaultWorkplaceId()).orElseThrow();
        WorkContract contract = workContractRepository
                .findFirstByWorkplaceIdAndEffectiveToIsNullOrderByEffectiveFromDesc(workplace.getId())
                .orElseThrow();

        User extraWorker = userRepository.save(User.register(EXTRA_WORKER_EMAIL, "hashed", "Extra Worker"));
        WorkProof extraWorkProof = WorkProof.checkIn(
                extraWorker,
                workplace,
                contract,
                LocalDateTime.of(2026, 3, 24, 9, 0),
                LocalDateTime.of(2026, 3, 24, 9, 1),
                37.501274,
                127.039585,
                "Front door"
        );
        extraWorkProof.completeCheckOut(
                LocalDateTime.of(2026, 3, 24, 18, 0),
                LocalDateTime.of(2026, 3, 24, 18, 1),
                37.501274,
                127.039585,
                "Front door",
                false
        );
        workProofRepository.save(extraWorkProof);

        assertThat(workProofRepository.findByWorkplaceId(workplace.getId())).hasSize(7);

        assertThatCode(() -> devEmployerInitializer.run()).doesNotThrowAnyException();

        EmployerProfile reseededProfile = employerProfileRepository.findByAccountId(employer.getId()).orElseThrow();
        Workplace reseededWorkplace = workplaceRepository.findById(reseededProfile.getDefaultWorkplaceId()).orElseThrow();

        assertThat(workProofRepository.findByWorkplaceId(reseededWorkplace.getId())).hasSize(6);
        assertThat(userRepository.findByEmailIgnoreCase(EXTRA_WORKER_EMAIL)).isPresent();
    }
}
