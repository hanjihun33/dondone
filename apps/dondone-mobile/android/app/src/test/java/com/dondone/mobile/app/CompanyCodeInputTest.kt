package com.dondone.mobile.app

import org.junit.Assert.assertEquals
import org.junit.Test

class CompanyCodeInputTest {
    @Test
    fun `normalize company code keeps uppercase alphanumeric only`() {
        val normalized = normalizeCompanyCodeInput("don-done 2026!")

        assertEquals("DONDONE2026", normalized)
    }

    @Test
    fun `normalize company code enforces max length`() {
        val normalized = normalizeCompanyCodeInput("abcdefghijklmnop")

        assertEquals("ABCDEFGHIJKL", normalized)
    }
}
