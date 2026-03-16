package com.workproofpay.backend.wage.api.dto.response;

import com.workproofpay.backend.workproof.api.dto.response.CurrentContractResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryContractResponse;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Wage 화면이 WorkProof lane 1 월간 집계를 그대로 읽을 수 있게 만든 read-model 응답이다.
 * reflected record ID와 제외된 pending count를 함께 내려 verification 후속 흐름의 입력축으로 쓴다.
 */
@Schema(description = "WorkProof lane 1 monthly summary adapted for the wage monthly summary read model")
public record WageMonthlySummaryResponse(
        @Schema(description = "Target month in YYYY-MM format", example = "2026-03")
        String month,
        @Schema(description = "Owned workplace ID used for this summary", example = "1")
        Long workplaceId,
        @Schema(description = "Active contract ID used as the current wage basis", example = "10")
        Long contractId,
        @Schema(description = "Pay unit from the current contract", example = "HOURLY")
        WorkProofPayUnit payUnit,
        @Schema(description = "Normalized hourly wage used as the estimate basis", example = "12000")
        BigDecimal normalizedHourlyWage,
        @Schema(description = "Number of worked days included in the month summary", example = "20")
        int workDayCount,
        @Schema(description = "Total reflected worked minutes surfaced via the upstream monthly summary", example = "9600")
        long verifiedWorkMinutes,
        @Schema(description = "Total overtime minutes included in the month summary", example = "600")
        long overtimeMinutes,
        @Schema(description = "Total night minutes included in the month summary", example = "180")
        long nightMinutes,
        @Schema(description = "Count of modified WorkProof records included in the month summary", example = "2")
        int modifiedRecordCount,
        @Schema(description = "Reflected WorkProof record IDs included in this wage summary")
        List<Long> includedRecordIds,
        @Schema(description = "Pending WorkProof record count excluded from this summary", example = "1")
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
