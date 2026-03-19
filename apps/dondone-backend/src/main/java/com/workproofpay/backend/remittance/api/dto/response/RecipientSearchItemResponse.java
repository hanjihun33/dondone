package com.workproofpay.backend.remittance.api.dto.response;

public record RecipientSearchItemResponse(
        Long candidateUserId,
        String displayName,
        String maskedPhoneNumber,
        String walletAddressMasked,
        boolean alreadyRegistered
) {
}
