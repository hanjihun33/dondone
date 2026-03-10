package com.workproofpay.backend.demo.api.dto.response;

import com.workproofpay.backend.wage.api.dto.response.WageSummaryResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofResponse;

import java.time.LocalDate;
import java.util.List;

public record DemoStateResponse(
        LocalDate asOf,
        String yearMonth,
        WorkProofMonthlySummaryResponse workProofSummary,
        List<WorkProofResponse> workProofs,
        WageSummaryResponse wageSummary
) {
}
