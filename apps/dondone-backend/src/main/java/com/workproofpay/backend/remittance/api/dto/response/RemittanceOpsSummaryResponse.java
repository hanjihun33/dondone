package com.workproofpay.backend.remittance.api.dto.response;

import java.util.List;
import java.util.Map;

public record RemittanceOpsSummaryResponse(
        Map<String, Long> transferCounts,
        Map<String, Long> walletFundingCounts,
        Map<String, Long> jobCounts,
        List<String> recentFailureReasons
) {
}
