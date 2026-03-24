package com.workproofpay.backend.advance.service;

import com.workproofpay.backend.advance.model.AdvancePayout;
import com.workproofpay.backend.advance.model.AdvancePayoutStatus;
import com.workproofpay.backend.advance.model.AdvanceRequest;
import com.workproofpay.backend.advance.model.AdvanceRequestStatus;
import org.springframework.stereotype.Component;

@Component
public class AdvanceRequestViewStatusResolver {

    public AdvanceRequestViewStatus resolve(AdvanceRequest request, AdvancePayout payout) {
        String requestStatus = request.getStatus().name();
        String payoutStatus = payout != null ? payout.getStatus().name() : null;

        String status = switch (request.getStatus()) {
            case SUBMITTED -> AdvanceViewStatus.SUBMITTED.name();
            case REJECTED -> AdvanceViewStatus.REJECTED.name();
            case NEEDS_REVIEW -> AdvanceViewStatus.NEEDS_REVIEW.name();
            case APPROVED -> resolveApprovedStatus(payout);
        };

        return new AdvanceRequestViewStatus(status, requestStatus, payoutStatus);
    }

    private String resolveApprovedStatus(AdvancePayout payout) {
        if (payout == null) {
            return AdvanceViewStatus.APPROVED.name();
        }

        AdvancePayoutStatus payoutStatus = payout.getStatus();
        return switch (payoutStatus) {
            case REQUESTED, SIGNED, BROADCASTED -> AdvanceViewStatus.PAYING.name();
            case CONFIRMED -> AdvanceViewStatus.PAID.name();
            case FAILED, TIMED_OUT -> AdvanceViewStatus.PAYOUT_FAILED.name();
        };
    }

    public enum AdvanceViewStatus {
        SUBMITTED,
        APPROVED,
        PAYING,
        PAID,
        PAYOUT_FAILED,
        REJECTED,
        NEEDS_REVIEW
    }

    public record AdvanceRequestViewStatus(
            String status,
            String requestStatus,
            String payoutStatus
    ) {
    }
}
