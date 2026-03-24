package com.workproofpay.backend.advance;

import com.workproofpay.backend.advance.model.AdvancePayout;
import com.workproofpay.backend.advance.model.AdvancePayoutStatus;
import com.workproofpay.backend.advance.model.AdvanceRequest;
import com.workproofpay.backend.advance.repo.AdvancePayoutRepository;
import com.workproofpay.backend.advance.repo.AdvanceRequestRepository;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.support.PostgresIntegrationTestSupport;
import com.workproofpay.backend.workproof.model.WorkContract;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AdvancePayoutRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

    private static final BigDecimal EXCHANGE_RATE = BigDecimal.valueOf(1450);

    @Autowired
    private AdvancePayoutRepository advancePayoutRepository;

    @Autowired
    private AdvanceRequestRepository advanceRequestRepository;

    @Autowired
    private WorkContractRepository workContractRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        advancePayoutRepository.deleteAll();
        advanceRequestRepository.deleteAll();
        workContractRepository.deleteAll();
        workplaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void savesAndLoadsAdvancePayoutByAdvanceRequestAndUserReplayKey() {
        User user = userRepository.saveAndFlush(User.register("advance-payout@test.com", "hashed", "Advance Payout"));
        AdvanceRequest advanceRequest = saveAdvanceRequest(user, "advance-request-1", LocalDateTime.of(2030, 1, 10, 9, 0));

        AdvancePayout payout = advancePayoutRepository.saveAndFlush(AdvancePayout.request(
                "adv-payout-1",
                advanceRequest.getId(),
                user.getId(),
                "0x1111111111111111111111111111111111111111",
                60_000_000L,
                "dUSDC",
                "advance-payout-idem-1"
        ));

        assertThat(payout.getStatus()).isEqualTo(AdvancePayoutStatus.REQUESTED);
        assertThat(payout.getCreatedAt()).isNotNull();
        assertThat(payout.getUpdatedAt()).isNotNull();

        assertThat(advancePayoutRepository.findByAdvanceRequestId(advanceRequest.getId()))
                .get()
                .extracting(AdvancePayout::getAdvancePayoutId, AdvancePayout::getAmountAtomic, AdvancePayout::getAssetSymbol)
                .containsExactly("adv-payout-1", 60_000_000L, "dUSDC");

        assertThat(advancePayoutRepository.findByAdvancePayoutIdAndUserId("adv-payout-1", user.getId())).isPresent();
        assertThat(advancePayoutRepository.findByUserIdAndIdempotencyKey(user.getId(), "advance-payout-idem-1")).isPresent();
        assertThat(advancePayoutRepository.existsByAdvanceRequestId(advanceRequest.getId())).isTrue();

        List<AdvancePayout> payouts = advancePayoutRepository.findByUserIdOrderByCreatedAtDescAdvancePayoutIdDesc(
                user.getId(),
                PageRequest.of(0, 10)
        );
        assertThat(payouts).extracting(AdvancePayout::getAdvancePayoutId).containsExactly("adv-payout-1");
    }

    @Test
    void rejectsDuplicatePayoutForSameAdvanceRequest() {
        User user = userRepository.saveAndFlush(User.register("advance-payout-duplicate-request@test.com", "hashed", "Duplicate Request"));
        AdvanceRequest advanceRequest = saveAdvanceRequest(user, "advance-request-2", LocalDateTime.of(2030, 1, 10, 9, 0));

        advancePayoutRepository.saveAndFlush(AdvancePayout.request(
                "adv-payout-2-a",
                advanceRequest.getId(),
                user.getId(),
                "0x2222222222222222222222222222222222222222",
                50_000_000L,
                "dUSDC",
                "advance-payout-idem-2a"
        ));

        assertThatThrownBy(() -> advancePayoutRepository.saveAndFlush(AdvancePayout.request(
                "adv-payout-2-b",
                advanceRequest.getId(),
                user.getId(),
                "0x3333333333333333333333333333333333333333",
                50_000_000L,
                "dUSDC",
                "advance-payout-idem-2b"
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicatePayoutForSameUserAndIdempotencyKey() {
        User user = userRepository.saveAndFlush(User.register("advance-payout-duplicate-idem@test.com", "hashed", "Duplicate Idempotency"));
        AdvanceRequest firstRequest = saveAdvanceRequest(user, "advance-request-3a", LocalDateTime.of(2030, 1, 10, 9, 0));
        AdvanceRequest secondRequest = saveAdvanceRequest(user, "advance-request-3b", LocalDateTime.of(2030, 1, 11, 9, 0));

        advancePayoutRepository.saveAndFlush(AdvancePayout.request(
                "adv-payout-3-a",
                firstRequest.getId(),
                user.getId(),
                "0x4444444444444444444444444444444444444444",
                45_000_000L,
                "dUSDC",
                "advance-payout-idem-3"
        ));

        assertThatThrownBy(() -> advancePayoutRepository.saveAndFlush(AdvancePayout.request(
                "adv-payout-3-b",
                secondRequest.getId(),
                user.getId(),
                "0x5555555555555555555555555555555555555555",
                46_000_000L,
                "dUSDC",
                "advance-payout-idem-3"
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void persistsStatusTxHashAndFailureReasonAcrossStateTransitions() {
        User user = userRepository.saveAndFlush(User.register("advance-payout-status@test.com", "hashed", "Status Payout"));
        AdvanceRequest confirmedRequest = saveAdvanceRequest(user, "advance-request-4a", LocalDateTime.of(2030, 1, 10, 9, 0));
        AdvanceRequest failedRequest = saveAdvanceRequest(user, "advance-request-4b", LocalDateTime.of(2030, 1, 11, 9, 0));

        AdvancePayout confirmed = advancePayoutRepository.saveAndFlush(AdvancePayout.request(
                "adv-payout-4-a",
                confirmedRequest.getId(),
                user.getId(),
                "0x6666666666666666666666666666666666666666",
                40_000_000L,
                "dUSDC",
                "advance-payout-idem-4a"
        ));
        confirmed.markSigned("0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        confirmed.markBroadcasted();
        confirmed.markConfirmed();
        advancePayoutRepository.saveAndFlush(confirmed);

        AdvancePayout failed = advancePayoutRepository.saveAndFlush(AdvancePayout.request(
                "adv-payout-4-b",
                failedRequest.getId(),
                user.getId(),
                "0x7777777777777777777777777777777777777777",
                41_000_000L,
                "dUSDC",
                "advance-payout-idem-4b"
        ));
        failed.markSigned("0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        failed.markFailed("  treasury   balance   was   insufficient  ");
        advancePayoutRepository.saveAndFlush(failed);

        AdvancePayout confirmedPersisted = advancePayoutRepository.findByAdvanceRequestId(confirmedRequest.getId()).orElseThrow();
        assertThat(confirmedPersisted.getStatus()).isEqualTo(AdvancePayoutStatus.CONFIRMED);
        assertThat(confirmedPersisted.getTxHash()).isEqualTo("0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertThat(confirmedPersisted.getFailureReason()).isNull();

        AdvancePayout failedPersisted = advancePayoutRepository.findByAdvanceRequestId(failedRequest.getId()).orElseThrow();
        assertThat(failedPersisted.getStatus()).isEqualTo(AdvancePayoutStatus.FAILED);
        assertThat(failedPersisted.getTxHash()).isEqualTo("0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        assertThat(failedPersisted.getFailureReason()).isEqualTo("treasury balance was insufficient");
    }

    private AdvanceRequest saveAdvanceRequest(User user, String idempotencyKey, LocalDateTime requestedAt) {
        Workplace workplace = workplaceRepository.saveAndFlush(Workplace.create(
                user,
                "Advance Workplace " + idempotencyKey,
                "Seoul",
                "Main gate",
                37.5,
                127.0,
                100
        ));
        WorkContract contract = workContractRepository.saveAndFlush(WorkContract.activate(
                workplace,
                WorkProofPayUnit.HOURLY,
                BigDecimal.valueOf(10_000),
                null,
                null,
                BigDecimal.valueOf(10_000),
                LocalDate.of(2030, 1, 1)
        ));

        return advanceRequestRepository.saveAndFlush(AdvanceRequest.submit(
                user,
                workplace,
                contract,
                "2030-01",
                idempotencyKey,
                "dUSDC",
                6,
                EXCHANGE_RATE,
                60_000_000L,
                87_000L,
                3_448_275L,
                5_000L,
                LocalDate.of(2030, 1, 25),
                requestedAt,
                103_448_275L,
                150_000L,
                344_827_586L,
                500_000L,
                BigDecimal.valueOf(0.30),
                20,
                9_600L,
                0
        ));
    }
}
