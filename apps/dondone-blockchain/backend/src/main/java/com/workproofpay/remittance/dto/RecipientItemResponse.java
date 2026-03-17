package com.workproofpay.remittance.dto;

import java.time.Instant;

public record RecipientItemResponse(
        String recipientId,
        String alias,
        String walletAddress,
        String relation,
        boolean allowed,
        Instant updatedAt,
        Instant cooldownEndsAt
) {}
