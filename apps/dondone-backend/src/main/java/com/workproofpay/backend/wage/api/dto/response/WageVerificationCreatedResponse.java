package com.workproofpay.backend.wage.api.dto.response;

import com.workproofpay.backend.wage.model.WageVerification;
import com.workproofpay.backend.wage.model.WageVerificationPossibleCauseSnapshot;
import com.workproofpay.backend.wage.model.WageVerificationResolutionStage;
import com.workproofpay.backend.wage.model.WageVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * verification 생성 직후 바로 보여줄 상태와 근거 요약 DTO다.
 * detail endpoint보다 작게 유지하되, 다음 행동을 정할 수 있는 정보는 바로 포함한다.
 */
@Schema(description = "Created wage verification summary response")
public record WageVerificationCreatedResponse(
        @Schema(description = "Created verification ID", example = "1")
        Long verificationId,
        @Schema(description = "Verification status", example = "CHECK_REQUIRED")
        WageVerificationStatus status,
        @Schema(description = "Current resolution stage", example = "EMPLOYER_CONFIRMATION_RECOMMENDED")
        WageVerificationResolutionStage resolutionStage,
        @Schema(description = "Reference-only estimated total amount in KRW", example = "2136000")
        long estimatedTotal,
        @Schema(description = "Worker-confirmed actual deposit amount in KRW", example = "1740000")
        long actualDepositAmount,
        @Schema(description = "Difference between estimate and actual deposit in KRW", example = "396000")
        long differenceAmount,
        @Schema(description = "Absolute difference rate compared to the estimated total", example = "0.1854")
        BigDecimal differenceRate,
        @Schema(description = "Threshold snapshot used for the current verification")
        ThresholdSnapshot threshold,
        @Schema(description = "Possible causes derived from the current comparison result")
        List<PossibleCauseResponse> possibleCauses,
        @Schema(description = "Evidence summary used for the current verification")
        EvidenceSnapshot evidence,
        @Schema(description = "Recommended next actions after the verification")
        List<String> nextActions
) {
    @Schema(description = "Threshold snapshot used to determine whether a difference needs follow-up")
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

    public static WageVerificationCreatedResponse from(WageVerification verification, List<String> nextActions) {
        return new WageVerificationCreatedResponse(
                verification.getId(),
                verification.getStatus(),
                verification.getResolutionStage(),
                verification.getEstimatedTotal(),
                verification.getActualDepositAmount(),
                verification.getDifferenceAmount(),
                verification.getDifferenceRate(),
                ThresholdSnapshot.from(verification),
                verification.getPossibleCauses().stream()
                        .map(PossibleCauseResponse::from)
                        .toList(),
                EvidenceSnapshot.from(verification),
                nextActions
        );
    }
}
