package com.workproofpay.backend.shared.util;

import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompanyCodeUtilsTest {

    @Test
    void normalizeNullableOrThrowReturnsNullForNullInput() {
        assertNull(CompanyCodeUtils.normalizeNullableOrThrow(null));
    }

    @Test
    void normalizeNullableOrThrowRejectsBlankInput() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> CompanyCodeUtils.normalizeNullableOrThrow("   ")
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }
}
