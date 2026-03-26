package com.dondone.mobile.feature.workproof.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkproofTimeInputTest {
    @Test
    fun `auto colon waits until four digits are entered`() {
        assertEquals("18", normalizeWorkproofTimeInput(previousInput = "", rawInput = "18"))
        assertEquals("180", normalizeWorkproofTimeInput(previousInput = "18", rawInput = "180"))
        assertEquals("18:00", normalizeWorkproofTimeInput(previousInput = "180", rawInput = "1800"))
    }

    @Test
    fun `four digit time input auto inserts colon`() {
        assertEquals("18:00", normalizeWorkproofTimeInput(previousInput = "180", rawInput = "1800"))
        assertEquals("09:05", normalizeWorkproofTimeInput(previousInput = "090", rawInput = "0905"))
    }

    @Test
    fun `manual colon input keeps partial editing shape`() {
        assertEquals("18:", normalizeWorkproofTimeInput(previousInput = "18", rawInput = "18:"))
        assertEquals("18:0", normalizeWorkproofTimeInput(previousInput = "18:", rawInput = "18:0"))
    }

    @Test
    fun `deleting from auto formatted time collapses colon without extra backspace`() {
        assertEquals("180", normalizeWorkproofTimeInput(previousInput = "18:00", rawInput = "18:0"))
        assertEquals("18", normalizeWorkproofTimeInput(previousInput = "18:0", rawInput = "18:"))
    }

    @Test
    fun `typing a new minute digit after deletion still restores expected time`() {
        val collapsed = normalizeWorkproofTimeInput(previousInput = "18:00", rawInput = "18:0")
        val restored = normalizeWorkproofTimeInput(previousInput = collapsed, rawInput = "1805")

        assertEquals("180", collapsed)
        assertEquals("18:05", restored)
    }

    @Test
    fun `editing minute tens digit keeps colon shape`() {
        val partiallyEdited = normalizeWorkproofTimeInput(previousInput = "18:05", rawInput = "18:5")
        val restored = normalizeWorkproofTimeInput(previousInput = partiallyEdited, rawInput = "18:35")

        assertEquals("18:5", partiallyEdited)
        assertEquals("18:35", restored)
    }

    @Test
    fun `normalized hhmm input becomes valid time`() {
        val normalized = normalizeWorkproofTimeInput(previousInput = "180", rawInput = "1800")

        assertEquals("18:00", normalized)
        assertTrue(normalized.isValidWorkproofTimeInput())
    }

    @Test
    fun `invalid hour minute pair stays invalid after normalization`() {
        val normalized = normalizeWorkproofTimeInput(previousInput = "256", rawInput = "2561")

        assertEquals("25:61", normalized)
        assertFalse(normalized.isValidWorkproofTimeInput())
    }
}
