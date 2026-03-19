package com.workproofpay.backend.remittance.api.dto.response;

import com.workproofpay.backend.remittance.model.RecipientRelation;

import java.time.LocalDateTime;

public record RecipientItemResponse(
        String recipientId,
        String alias,
        RecipientRelation relation,
        String walletAddress,
        boolean allowed,
        boolean recentlyUpdated,
        LocalDateTime updatedAt
) {
}
