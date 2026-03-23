package com.dondone.mobile.app

import org.junit.Assert.assertEquals
import org.junit.Test

class CompanyCodeInputTest {
    @Test
    fun `normalize worker registration code uppercases input and keeps visible characters`() {
        val normalized = normalizeWorkerRegistrationCodeInput("don-done!")

        assertEquals("DON-DONE!", normalized)
    }

    @Test
    fun `normalize worker registration code enforces max length`() {
        val normalized = normalizeWorkerRegistrationCodeInput("abcdefghijklmnopqrstuvwxyz123456")

        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456", normalized)
    }

    @Test
    fun `worker registration code validation rejects invalid characters`() {
        val message = workerRegistrationCodeValidationMessage("DON_DONE")

        assertEquals("영문 대문자, 숫자, 하이픈(-)만 입력할 수 있어요.", message)
    }

    @Test
    fun `worker registration code validation rejects short value`() {
        val message = workerRegistrationCodeValidationMessage("DON12")

        assertEquals("등록 코드는 8자 이상이어야 해요.", message)
    }

    @Test
    fun `worker registration code validation accepts uppercase alphanumerics and hyphen`() {
        val message = workerRegistrationCodeValidationMessage("DONDONE-2026")

        assertEquals(null, message)
    }
}
