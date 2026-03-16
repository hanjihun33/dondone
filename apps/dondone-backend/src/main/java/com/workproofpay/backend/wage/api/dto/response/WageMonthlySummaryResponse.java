package com.workproofpay.backend.wage.api.dto.response;

import com.workproofpay.backend.workproof.api.dto.response.CurrentContractResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryContractResponse;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;

import java.math.BigDecimal;
import java.util.List;

public record WageMonthlySummaryResponse(
        String month,
        Long workplaceId,
        Long contractId,
        WorkProofPayUnit payUnit,
        BigDecimal normalizedHourlyWage,
        int workDayCount,
        long verifiedWorkMinutes,
        long overtimeMinutes,
        long nightMinutes,
        int modifiedRecordCount,
        List<Long> includedRecordIds,
        int excludedPendingRecordCount
) {
    public static WageMonthlySummaryResponse from(CurrentContractResponse contract,
                                                  WorkProofMonthlySummaryContractResponse summary,
                                                  List<Long> includedRecordIds,
                                                  int excludedPendingRecordCount) {
        return new WageMonthlySummaryResponse(
                summary.month(),
                summary.workplaceId(),
                contract.contractId(),
                contract.payUnit(),
                contract.normalizedHourlyWage(),
                summary.workDayCount(),
                summary.integrity().verifiedMinutes(),
                summary.overtimeMinutes(),
                summary.nightMinutes(),
                summary.modifiedRecordCount(),
                List.copyOf(includedRecordIds),
                excludedPendingRecordCount
        );
    }
}
