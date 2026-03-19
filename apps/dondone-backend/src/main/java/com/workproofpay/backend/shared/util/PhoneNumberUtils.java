package com.workproofpay.backend.shared.util;

import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;

public final class PhoneNumberUtils {

    private PhoneNumberUtils() {
    }

    public static String normalizeOrThrow(String rawPhoneNumber) {
        String normalized = rawPhoneNumber == null ? "" : rawPhoneNumber.replaceAll("[^0-9]", "");
        if (!normalized.matches("^01\\d{8,9}$")) {
            throw new ApiException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "phoneNumber must be a valid Korean mobile number"
            );
        }
        return normalized;
    }

    public static String mask(String normalizedPhoneNumber) {
        if (normalizedPhoneNumber == null || normalizedPhoneNumber.isBlank()) {
            return "";
        }
        if (normalizedPhoneNumber.length() == 11) {
            return normalizedPhoneNumber.substring(0, 3)
                    + "-****-"
                    + normalizedPhoneNumber.substring(7);
        }
        return normalizedPhoneNumber.substring(0, 3)
                + "-***-"
                + normalizedPhoneNumber.substring(6);
    }
}
