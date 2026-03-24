package com.workproofpay.backend.advance;

import com.workproofpay.backend.advance.api.dto.request.CreateAdvanceRequest;
import com.workproofpay.backend.advance.model.AdvanceRequest;
import com.workproofpay.backend.advance.service.AdvancePolicyEngine;
import com.workproofpay.backend.advance.service.AdvanceCreateResult;
import com.workproofpay.backend.advance.service.AdvanceRequestViewStatusResolver;
import com.workproofpay.backend.advance.service.AdvanceService;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import com.workproofpay.backend.workproof.service.WorkProofLane1Service;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AdvanceServiceTest {

    private static final long REQUEST_ATOMIC = 60_000_000L;
    private static final long APPROVED_ATOMIC = 60_000_000L;
    private static final long APPROVED_DISPLAY_KRW = 87_000L;
    private static final long FEE_ATOMIC = 3_448_275L;
    private static final long FEE_DISPLAY_KRW = 5_000L;
    private static final long SNAPSHOT_AVAILABLE_ATOMIC = 103_448_275L;
    private static final long SNAPSHOT_AVAILABLE_DISPLAY_KRW = 150_000L;
    private static final long SNAPSHOT_MAX_CAP_ATOMIC = 344_827_586L;
    private static final long SNAPSHOT_MAX_CAP_DISPLAY_KRW = 500_000L;

    private final com.workproofpay.backend.advance.repo.AdvancePayoutRepository advancePayoutRepository = mock(com.workproofpay.backend.advance.repo.AdvancePayoutRepository.class);
    private final com.workproofpay.backend.advance.repo.AdvanceRequestRepository advanceRequestRepository = mock(com.workproofpay.backend.advance.repo.AdvanceRequestRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final WorkplaceRepository workplaceRepository = mock(WorkplaceRepository.class);
    private final WorkContractRepository workContractRepository = mock(WorkContractRepository.class);
    private final WorkProofRepository workProofRepository = mock(WorkProofRepository.class);
    private final WorkProofLane1Service workProofLane1Service = mock(WorkProofLane1Service.class);
    private final AdvancePolicyEngine advancePolicyEngine = new AdvancePolicyEngine();
    private final AdvanceRequestViewStatusResolver advanceRequestViewStatusResolver = new AdvanceRequestViewStatusResolver();

    private final AdvanceService service = new AdvanceService(
            advanceRequestRepository,
            advancePayoutRepository,
            userRepository,
            workplaceRepository,
            workContractRepository,
            workProofRepository,
            workProofLane1Service,
            advancePolicyEngine,
            advanceRequestViewStatusResolver
    );

    @Test
    void rejectsMissingIdempotencyKey() {
        CreateAdvanceRequest request = new CreateAdvanceRequest(
                1L,
                REQUEST_ATOMIC,
                LocalDateTime.of(2026, 3, 16, 10, 0)
        );

        assertThatThrownBy(() -> service.createRequest(1L, " ", request))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    @Test
    void replaysExistingRequestWhenIdempotencyPayloadMatches() {
        CreateAdvanceRequest request = new CreateAdvanceRequest(
                1L,
                REQUEST_ATOMIC,
                LocalDateTime.of(2026, 3, 16, 10, 0)
        );
        AdvanceRequest existing = mock(AdvanceRequest.class);
        when(advanceRequestRepository.findByUserIdAndIdempotencyKey(1L, "idem-1")).thenReturn(java.util.Optional.of(existing));
        when(existing.matches("idem-1", 1L, REQUEST_ATOMIC, request.requestedAt())).thenReturn(true);
        when(existing.getId()).thenReturn(9L);
        when(existing.getAssetSymbol()).thenReturn("dUSDC");
        when(existing.getAssetDecimals()).thenReturn(6);
        when(existing.getExchangeRateSnapshot()).thenReturn(java.math.BigDecimal.valueOf(1_450));
        when(existing.getStatus()).thenReturn(com.workproofpay.backend.advance.model.AdvanceRequestStatus.APPROVED);
        when(existing.getApprovedAmountAtomic()).thenReturn(APPROVED_ATOMIC);
        when(existing.getApprovedDisplayKrwAmount()).thenReturn(APPROVED_DISPLAY_KRW);
        when(existing.getFeeAmountAtomic()).thenReturn(FEE_ATOMIC);
        when(existing.getFeeDisplayKrwAmount()).thenReturn(FEE_DISPLAY_KRW);
        when(existing.getRepaymentDueDate()).thenReturn(LocalDate.of(2026, 3, 25));
        when(existing.getSnapshotAvailableAmountAtomic()).thenReturn(SNAPSHOT_AVAILABLE_ATOMIC);
        when(existing.getSnapshotAvailableDisplayKrwAmount()).thenReturn(SNAPSHOT_AVAILABLE_DISPLAY_KRW);
        when(existing.getSnapshotMaxCapAmountAtomic()).thenReturn(SNAPSHOT_MAX_CAP_ATOMIC);
        when(existing.getSnapshotMaxCapDisplayKrwAmount()).thenReturn(SNAPSHOT_MAX_CAP_DISPLAY_KRW);
        when(existing.getSnapshotPolicyRate()).thenReturn(java.math.BigDecimal.valueOf(0.20));
        when(existing.getSnapshotReflectedWorkDays()).thenReturn(10);
        when(existing.getSnapshotReflectedWorkMinutes()).thenReturn(4_800L);
        when(existing.getSnapshotNeedsReviewRecordCount()).thenReturn(0);
        when(existing.getWorkplace()).thenReturn(mock(com.workproofpay.backend.workproof.model.Workplace.class));
        when(existing.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 3, 16, 10, 0));
        when(advancePayoutRepository.findByAdvanceRequestId(9L)).thenReturn(java.util.Optional.empty());

        AdvanceCreateResult result = service.createRequest(1L, "idem-1", request);

        assertThat(result.replayed()).isTrue();
        assertThat(result.response().requestId()).isEqualTo(9L);
        assertThat(result.response().approvedAmountAtomic()).isEqualTo(APPROVED_ATOMIC);
        assertThat(result.response().approvedDisplayKrwAmount()).isEqualTo(APPROVED_DISPLAY_KRW);
        assertThat(result.response().feeAmountAtomic()).isEqualTo(FEE_ATOMIC);
        assertThat(result.response().feeDisplayKrwAmount()).isEqualTo(FEE_DISPLAY_KRW);
        assertThat(result.response().eligibilitySnapshot().availableAmountAtomic()).isEqualTo(SNAPSHOT_AVAILABLE_ATOMIC);
        assertThat(result.response().eligibilitySnapshot().availableDisplayKrwAmount()).isEqualTo(SNAPSHOT_AVAILABLE_DISPLAY_KRW);
        verify(advanceRequestRepository, never()).save(any());
    }

    @Test
    void rejectsReusedIdempotencyKeyWithDifferentPayload() {
        CreateAdvanceRequest request = new CreateAdvanceRequest(
                1L,
                REQUEST_ATOMIC,
                LocalDateTime.of(2026, 3, 16, 10, 0)
        );
        AdvanceRequest existing = mock(AdvanceRequest.class);
        when(advanceRequestRepository.findByUserIdAndIdempotencyKey(1L, "idem-2")).thenReturn(java.util.Optional.of(existing));
        when(existing.matches("idem-2", 1L, REQUEST_ATOMIC, request.requestedAt())).thenReturn(false);

        assertThatThrownBy(() -> service.createRequest(1L, "idem-2", request))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD);
    }
}
