package com.dondone.mobile.app

import org.junit.Assert.assertEquals
import org.junit.Test

class CompanyCodeInputTest {
    @Test
    fun `normalize company code uppercases input and keeps visible characters`() {
        val normalized = normalizeCompanyCodeInput("don-done!")

        assertEquals("DON-DONE!", normalized)
    }

    @Test
    fun `normalize company code enforces max length`() {
        val normalized = normalizeCompanyCodeInput("abcdefghijklmnop")

        assertEquals("ABCDEFGHIJKL", normalized)
    }

    @Test
    fun `company code validation rejects non alphanumeric characters`() {
        val message = companyCodeValidationMessage("DON-DONE")

        assertEquals("영문 대문자와 숫자만 입력할 수 있어요.", message)
    }

    @Test
    fun `company code validation rejects short value`() {
        val message = companyCodeValidationMessage("DON12")

        assertEquals("회사코드는 6자 이상이어야 해요.", message)
    }

    @Test
    fun `company code validation accepts six to twelve uppercase alphanumerics`() {
        val message = companyCodeValidationMessage("DONDONE2026")

        assertEquals(null, message)
    }
}
