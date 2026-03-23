package com.workproofpay.backend.shared.util;

import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;

import java.util.regex.Pattern;

public final class CompanyCodeUtils {

    public static final String DEFAULT_COMPANY_CODE = "DONDONE2026";
    private static final Pattern COMPANY_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{6,12}$");

    private CompanyCodeUtils() {
    }

    public static String normalizeNullableOrThrow(String companyCode) {
        if (companyCode == null) {
            return null;
        }
        return normalizeOrThrow(companyCode);
    }

    public static String normalizeOrThrow(String companyCode) {
        String normalized = companyCode == null ? "" : companyCode.trim().toUpperCase();
        if (!COMPANY_CODE_PATTERN.matcher(normalized).matches()) {
            throw new ApiException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "companyCode must contain 6 to 12 uppercase letters or digits without hyphens"
            );
        }
        return normalized;
    }
}
