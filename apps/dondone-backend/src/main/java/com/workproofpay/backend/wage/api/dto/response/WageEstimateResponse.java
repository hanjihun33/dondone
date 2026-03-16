package com.workproofpay.backend.wage.api.dto.response;

import com.workproofpay.backend.wage.service.WageSummaryCalculator;
import com.workproofpay.backend.workproof.api.dto.response.CurrentContractResponse;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WageEstimateResponse(
        String month,
        Long workplaceId,
        ContractSnapshot contract,
        SummarySnapshot summary,
        EstimateSnapshot estimate,
        String disclaimer,
        String ruleVersion
) {
    public record ContractSnapshot(
            Long contractId,
            WorkProofPayUnit payUnit,
            BigDecimal basePayAmount,
            Integer dailyWorkMinutes,
            Integer monthlyWorkMinutes,
            BigDecimal normalizedHourlyWage,
            LocalDate effectiveFrom,
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
                    contract.effectiveFrom(),
                    contract.isActive()
            );
        }
    }

    public record SummarySnapshot(
            int workDayCount,
            long verifiedWorkMinutes,
            long overtimeMinutes,
            long nightMinutes,
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

    public record EstimateSnapshot(
            long baseEstimate,
            long overtimePremium,
            long nightPremium,
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
