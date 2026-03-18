package com.workproofpay.remittance.util;

import org.springframework.stereotype.Component;

@Component
public class UserContext {
    public Long resolveUserId(String xUserIdHeader) {
        if (xUserIdHeader == null || xUserIdHeader.isBlank()) {
            return 1L;
        }
        try {
            return Long.parseLong(xUserIdHeader);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("X-User-Id must be a numeric bigint user id");
        }
    }

    public String resolveSenderAddress(String xSenderAddressHeader) {
        if (xSenderAddressHeader == null || xSenderAddressHeader.isBlank()) {
            return "0xDEMO_SENDER";
        }
        return xSenderAddressHeader;
    }
}
