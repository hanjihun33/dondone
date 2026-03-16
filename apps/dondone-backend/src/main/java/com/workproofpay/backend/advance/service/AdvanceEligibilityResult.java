package com.workproofpay.backend.advance.service;

import com.workproofpay.backend.advance.api.dto.response.AdvanceEligibilityResponse;
import com.workproofpay.backend.workproof.model.WorkContract;
import com.workproofpay.backend.workproof.model.Workplace;

public record AdvanceEligibilityResult(
        Workplace workplace,
        WorkContract contract,
        String yearMonth,
        AdvanceEligibilityResponse response,
        boolean hardBlocked
) {
}
