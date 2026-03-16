package com.workproofpay.backend.advance;

import com.workproofpay.backend.advance.api.dto.request.CreateAdvanceRequest;
import com.workproofpay.backend.advance.model.AdvanceRequest;
import com.workproofpay.backend.advance.service.AdvancePolicyEngine;
import com.workproofpay.backend.advance.service.AdvanceCreateResult;
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

    private final com.workproofpay.backend.advance.repo.AdvanceRequestRepository advanceRequestRepository = mock(com.workproofpay.backend.advance.repo.AdvanceRequestRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final WorkplaceRepository workplaceRepository = mock(WorkplaceRepository.class);
    private final WorkContractRepository workContractRepository = mock(WorkContractRepository.class);
    private final WorkProofRepository workProofRepository = mock(WorkProofRepository.class);
    private final WorkProofLane1Service workProofLane1Service = mock(WorkProofLane1Service.class);
    private final AdvancePolicyEngine advancePolicyEngine = new AdvancePolicyEngine();

    private final AdvanceService service = new AdvanceService(
            advanceRequestRepository,
            userRepository,
            workplaceRepository,
            workContractRepository,
            workProofRepository,
            workProofLane1Service,
            advancePolicyEngine
    );

    @Test
    void rejectsMissingIdempotencyKey() {
        CreateAdvanceRequest request = new CreateAdvanceRequest(
                1L,
                100_000L,
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
                120_000L,
                LocalDateTime.of(2026, 3, 16, 10, 0)
        );
        AdvanceRequest existing = mock(AdvanceRequest.class);
        when(advanceRequestRepository.findByUserIdAndIdempotencyKey(1L, "idem-1")).thenReturn(java.util.Optional.of(existing));
        when(existing.matches("idem-1", 1L, 120_000L, request.requestedAt())).thenReturn(true);
        when(existing.getId()).thenReturn(9L);
        when(existing.getStatus()).thenReturn(com.workproofpay.backend.advance.model.AdvanceRequestStatus.APPROVED);
        when(existing.getApprovedAmount()).thenReturn(120_000L);
        when(existing.getFeeAmount()).thenReturn(5_000L);
        when(existing.getRepaymentDueDate()).thenReturn(LocalDate.of(2026, 3, 25));
        when(existing.getSnapshotAvailableAmount()).thenReturn(150_000L);
        when(existing.getSnapshotMaxCap()).thenReturn(500_000L);
        when(existing.getSnapshotPolicyRate()).thenReturn(java.math.BigDecimal.valueOf(0.20));
        when(existing.getSnapshotReflectedWorkDays()).thenReturn(10);
        when(existing.getSnapshotReflectedWorkMinutes()).thenReturn(4_800L);
        when(existing.getSnapshotNeedsReviewRecordCount()).thenReturn(0);

        AdvanceCreateResult result = service.createRequest(1L, "idem-1", request);

        assertThat(result.replayed()).isTrue();
        assertThat(result.response().requestId()).isEqualTo(9L);
        verify(advanceRequestRepository, never()).save(any());
    }

    @Test
    void rejectsReusedIdempotencyKeyWithDifferentPayload() {
        CreateAdvanceRequest request = new CreateAdvanceRequest(
                1L,
                120_000L,
                LocalDateTime.of(2026, 3, 16, 10, 0)
        );
        AdvanceRequest existing = mock(AdvanceRequest.class);
        when(advanceRequestRepository.findByUserIdAndIdempotencyKey(1L, "idem-2")).thenReturn(java.util.Optional.of(existing));
        when(existing.matches("idem-2", 1L, 120_000L, request.requestedAt())).thenReturn(false);

        assertThatThrownBy(() -> service.createRequest(1L, "idem-2", request))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD);
    }
}
