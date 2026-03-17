package com.workproofpay.remittance.dto;

import java.time.Instant;

public record PolicyResponse(
        String policyCode,
        Instant cooldownEndsAt
) {}
