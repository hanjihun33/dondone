package com.dondone.mobile.data.remittance

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackendRemittanceRepositoryParsingTest {

    @Test
    fun `normalize api string treats literal null as absent`() {
        assertNull(normalizeApiString(null))
        assertNull(normalizeApiString("null"))
        assertNull(normalizeApiString(" NULL "))
        assertNull(normalizeApiString("   "))
        assertEquals("0xabc", normalizeApiString(" 0xabc "))
    }

    @Test
    fun `parse optional date time skips literal null strings`() {
        assertNull(parseOptionalDateTime(null))
        assertNull(parseOptionalDateTime("null"))
        assertNull(parseOptionalDateTime(" NULL "))
        assertEquals(
            LocalDateTime.parse("2026-03-19T13:37:00"),
            parseOptionalDateTime("2026-03-19T13:37:00")
        )
    }
}
