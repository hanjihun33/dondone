package com.workproofpay.backend.wage.api.dto.response;

import com.workproofpay.backend.wage.model.WageVerification;
import com.workproofpay.backend.wage.model.WageVerificationPossibleCauseSnapshot;
import com.workproofpay.backend.wage.model.WageVerificationResolutionStage;
import com.workproofpay.backend.wage.model.WageVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Documents/Claim가 다시 참조할 verification snapshot 상세 DTO다.
 * 계산 시점의 actual, estimated, threshold, evidence를 한 번에 읽을 수 있게 유지한다.
 */
@Schema(description = "Detailed wage verification snapshot response")
public record WageVerificationDetailResponse(
        @Schema(description = "Verification ID", example = "1")
        Long verificationId,
        @Schema(description = "Target month in YYYY-MM format", example = "2026-03")
        String month,
        @Schema(description = "Owned workplace ID used for this verification", example = "1")
        Long workplaceId,
        @Schema(description = "Verification status", example = "CHECK_REQUIRED")
        WageVerificationStatus status,
        @Schema(description = "Current resolution stage", example = "EMPLOYER_CONFIRMATION_RECOMMENDED")
        WageVerificationResolutionStage resolutionStage,
        @Schema(description = "Estimated wage snapshot used for this verification")
        EstimatedSnapshot estimated,
        @Schema(description = "Actual deposit snapshot submitted by the worker")
        ActualSnapshot actual,
        @Schema(description = "Difference snapshot captured at verification time")
        DifferenceSnapshot difference,
        @Schema(description = "Threshold snapshot captured at verification time")
        ThresholdSnapshot threshold,
        @Schema(description = "Possible causes derived from the current comparison result")
        List<PossibleCauseResponse> possibleCauses,
        @Schema(description = "Evidence snapshot stored for the verification")
        EvidenceSnapshot evidence,
        @Schema(description = "Employer support readiness for the current verification")
        EmployerSupportSnapshot employerSupport,
        @Schema(description = "Downstream readiness flags derived from the current verification")
        RelatedActionsSnapshot relatedActions
) {
    @Schema(description = "Estimated wage snapshot")
    public record EstimatedSnapshot(
            @Schema(description = "Estimated base amount in KRW", example = "1920000")
            long baseEstimate,
            @Schema(description = "Estimated overtime premium in KRW", example = "180000")
            long overtimePremium,
            @Schema(description = "Estimated night premium in KRW", example = "36000")
            long nightPremium,
            @Schema(description = "Estimated total amount in KRW", example = "2136000")
            long estimatedTotal
    ) {
        public static EstimatedSnapshot from(WageVerification verification) {
            return new EstimatedSnapshot(
                    verification.getBaseEstimate(),
                    verification.getOvertimePremium(),
                    verification.getNightPremium(),
                    verification.getEstimatedTotal()
            );
        }
    }

    @Schema(description = "Worker-submitted actual deposit snapshot")
    public record ActualSnapshot(
            @Schema(description = "Actual deposit amount in KRW", example = "1740000")
            long actualDepositAmount,
            @Schema(description = "Whether deductions are known for the submitted amount", example = "false")
            boolean deductionsKnown,
            @Schema(description = "Submission owner", example = "WORKER")
            String submittedBy
    ) {
        public static ActualSnapshot from(WageVerification verification) {
            return new ActualSnapshot(
                    verification.getActualDepositAmount(),
                    verification.isDeductionsKnown(),
                    "WORKER"
            );
        }
    }

    @Schema(description = "Difference snapshot")
    public record DifferenceSnapshot(
            @Schema(description = "Difference amount in KRW", example = "396000")
            long differenceAmount,
            @Schema(description = "Absolute difference rate compared to the estimated total", example = "0.1854")
            BigDecimal differenceRate,
            @Schema(description = "Threshold snapshot applied to this comparison")
            ThresholdSnapshot thresholdApplied
    ) {
        public static DifferenceSnapshot from(WageVerification verification) {
            ThresholdSnapshot thresholdSnapshot = ThresholdSnapshot.from(verification);
            return new DifferenceSnapshot(
                    verification.getDifferenceAmount(),
                    verification.getDifferenceRate(),
                    thresholdSnapshot
            );
        }
    }

    @Schema(description = "Threshold snapshot captured for the verification")
    public record ThresholdSnapshot(
            @Schema(description = "Absolute amount threshold in KRW", example = "50000")
            long absoluteWon,
            @Schema(description = "Relative ratio threshold", example = "0.03")
            BigDecimal relativePercent,
            @Schema(description = "Whether the relaxed threshold was used because deductions are not known", example = "true")
            boolean deductionRelaxed
    ) {
        public static ThresholdSnapshot from(WageVerification verification) {
            return new ThresholdSnapshot(
                    verification.getThresholdAbsoluteWon(),
                    verification.getThresholdRelativePercent(),
                    verification.isThresholdDeductionRelaxed()
            );
        }
    }

    @Schema(description = "Verification cause snapshot")
    public record PossibleCauseResponse(
            @Schema(description = "Cause code", example = "DIFFERENCE_OVER_THRESHOLD")
            String code,
            @Schema(description = "Cause title")
            String title,
            @Schema(description = "Human-readable cause detail")
            String detail
    ) {
        public static PossibleCauseResponse from(WageVerificationPossibleCauseSnapshot cause) {
            return new PossibleCauseResponse(
                    cause.getCode(),
                    cause.getTitle(),
                    cause.getDetail()
            );
        }
    }

    @Schema(description = "Evidence snapshot stored for the verification")
    public record EvidenceSnapshot(
            @Schema(description = "Overtime minutes reflected in the verification evidence", example = "600")
            long overtimeMinutes,
            @Schema(description = "Night minutes reflected in the verification evidence", example = "180")
            long nightMinutes,
            @Schema(description = "Modified WorkProof record count included in the evidence", example = "2")
            int modifiedRecordCount,
            @Schema(description = "WorkProof record IDs used as evidence")
            List<Long> recordIds
    ) {
        public static EvidenceSnapshot from(WageVerification verification) {
            return new EvidenceSnapshot(
                    verification.getOvertimeMinutes(),
                    verification.getNightMinutes(),
                    verification.getModifiedRecordCount(),
                    verification.getEvidenceRecordIds()
            );
        }
    }

    @Schema(description = "Employer-side follow-up readiness")
    public record EmployerSupportSnapshot(
            @Schema(description = "Whether employer support is currently available", example = "false")
            boolean available,
            @Schema(description = "Whether employer confirmation is recommended", example = "true")
            boolean recommended,
            @Schema(description = "Employer support status", example = "REQUEST_RECOMMENDED")
            String status
    ) {
    }

    @Schema(description = "Downstream readiness flags")
    public record RelatedActionsSnapshot(
            @Schema(description = "Whether a proof pack request can be started from this verification", example = "true")
            boolean proofPackReady,
            @Schema(description = "Whether a claim kit request can be started from this verification", example = "true")
            boolean claimKitReady,
            @Schema(description = "Whether Instant Claim preparation can start from this verification", example = "true")
            boolean instantClaimAvailable,
            @Schema(description = "Latest proof pack document ID linked to this verification", example = "12")
            Long proofPackDocumentId,
            @Schema(description = "Latest claim kit document ID linked to this verification", example = "13")
            Long claimKitDocumentId,
            @Schema(description = "Latest claim preparation ID linked to this verification", example = "7")
            Long preparationId
    ) {
    }

    public static WageVerificationDetailResponse from(WageVerification verification,
                                                      EmployerSupportSnapshot employerSupport,
                                                      RelatedActionsSnapshot relatedActions) {
        return new WageVerificationDetailResponse(
                verification.getId(),
                verification.getMonth(),
                verification.getWorkplaceId(),
                verification.getStatus(),
                verification.getResolutionStage(),
                EstimatedSnapshot.from(verification),
                ActualSnapshot.from(verification),
                DifferenceSnapshot.from(verification),
                ThresholdSnapshot.from(verification),
                verification.getPossibleCauses().stream()
                        .map(PossibleCauseResponse::from)
                        .toList(),
                EvidenceSnapshot.from(verification),
                employerSupport,
                relatedActions
        );
    }
}
