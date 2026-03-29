package com.workproofpay.backend.wage.api.dto.response;

import com.workproofpay.backend.wage.service.WageSummaryCalculator;
import com.workproofpay.backend.workproof.api.dto.response.CurrentContractResponse;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 참고용 예상 급여 응답을 계약, 집계, 계산 결과 스냅샷으로 분리한 Wage lane 1 DTO다.
 * verification 도입 전까지는 read-only 근거 설명에 집중한다.
 */
@Schema(description = "Reference-only wage estimate response for a workplace and month")
public record WageEstimateResponse(
        @Schema(description = "Target month in YYYY-MM format", example = "2026-03")
        String month,
        @Schema(description = "Owned workplace ID used for this estimate", example = "1")
        Long workplaceId,
        @Schema(description = "Contract snapshot used for this estimate")
        ContractSnapshot contract,
        @Schema(description = "Monthly summary snapshot used as estimate input")
        SummarySnapshot summary,
        @Schema(description = "Reference-only estimated wage snapshot")
        EstimateSnapshot estimate,
        @Schema(description = "Reference-only disclaimer shown to clients")
        String disclaimer,
        @Schema(description = "Rule version label for the current estimate calculation", example = "WAGE_LANE1_V0")
        String ruleVersion
) {
    @Schema(description = "Contract snapshot captured for the current estimate")
    public record ContractSnapshot(
            @Schema(description = "Contract ID used for this estimate", example = "10")
            Long contractId,
            @Schema(description = "Pay unit from the contract", example = "HOURLY")
            WorkProofPayUnit payUnit,
            @Schema(description = "Base pay amount stored on the contract", example = "12000")
            BigDecimal basePayAmount,
            @Schema(description = "Configured daily work minutes on the contract", example = "480", nullable = true)
            Integer dailyWorkMinutes,
            @Schema(description = "Configured monthly work minutes on the contract", example = "10440", nullable = true)
            Integer monthlyWorkMinutes,
            @Schema(description = "Normalized hourly wage derived from the contract", example = "12000")
            BigDecimal normalizedHourlyWage,
            @Schema(description = "Configured payday day from the contract", example = "31")
            Integer paydayDay,
            @Schema(description = "Contract effective start date", example = "2026-03-01")
            LocalDate effectiveFrom,
            @Schema(description = "Whether the contract is currently active", example = "true")
            boolean isActive
    ) {
        public static ContractSnapshot from(CurrentContractResponse contract) {
            return new ContractSnapshot(
                    contract.contractId(),
                    contract.payUnit(),
                    contract.basePayAmount(),
                    contract.dailyWorkMinutes(),
                    contract.monthlyWorkMinutes(),
                    contract.normalizedHourlyWage(),
                    contract.paydayDay(),
                    contract.effectiveFrom(),
                    contract.isActive()
            );
        }
    }

    @Schema(description = "Monthly summary snapshot used as estimate input")
    public record SummarySnapshot(
            @Schema(description = "Number of worked days included in the summary", example = "20")
            int workDayCount,
            @Schema(description = "Total reflected worked minutes surfaced via the upstream monthly summary", example = "9600")
            long verifiedWorkMinutes,
            @Schema(description = "Total overtime minutes included in the summary", example = "600")
            long overtimeMinutes,
            @Schema(description = "Total night minutes included in the summary", example = "180")
            long nightMinutes,
            @Schema(description = "Count of modified WorkProof records included in the summary", example = "2")
            int modifiedRecordCount
    ) {
        public static SummarySnapshot from(WageMonthlySummaryResponse summary) {
            return new SummarySnapshot(
                    summary.workDayCount(),
                    summary.verifiedWorkMinutes(),
                    summary.overtimeMinutes(),
                    summary.nightMinutes(),
                    summary.modifiedRecordCount()
            );
        }
    }

    @Schema(description = "Reference-only wage estimate amounts")
    public record EstimateSnapshot(
            @Schema(description = "Estimated base wage amount in KRW", example = "1920000")
            long baseEstimate,
            @Schema(description = "Estimated overtime premium amount in KRW", example = "180000")
            long overtimePremium,
            @Schema(description = "Estimated night premium amount in KRW", example = "36000")
            long nightPremium,
            @Schema(description = "Estimated total wage amount in KRW", example = "2136000")
            long estimatedTotal
    ) {
        public static EstimateSnapshot from(WageSummaryCalculator.WageEstimateSnapshot snapshot) {
            return new EstimateSnapshot(
                    snapshot.baseEstimate(),
                    snapshot.overtimePremium(),
                    snapshot.nightPremium(),
                    snapshot.estimatedTotal()
            );
        }
    }

    public static WageEstimateResponse from(String month,
                                            Long workplaceId,
                                            CurrentContractResponse contract,
                                            WageMonthlySummaryResponse summary,
                                            WageSummaryCalculator.WageEstimateSnapshot estimate,
                                            String disclaimer,
                                            String ruleVersion) {
        return new WageEstimateResponse(
                month,
                workplaceId,
                ContractSnapshot.from(contract),
                SummarySnapshot.from(summary),
                EstimateSnapshot.from(estimate),
                disclaimer,
                ruleVersion
        );
    }
}
