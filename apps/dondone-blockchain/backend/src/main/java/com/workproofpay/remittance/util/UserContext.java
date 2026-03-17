package com.workproofpay.remittance.util;

import org.springframework.stereotype.Component;

@Component
public class UserContext {
    public String resolveUserId(String xUserIdHeader) {
        if (xUserIdHeader == null || xUserIdHeader.isBlank()) {
            return "demo-user";
        }
        return xUserIdHeader;
    }

    public String resolveSenderAddress(String xSenderAddressHeader) {
        if (xSenderAddressHeader == null || xSenderAddressHeader.isBlank()) {
            return "0xDEMO_SENDER";
        }
        return xSenderAddressHeader;
    }
}
