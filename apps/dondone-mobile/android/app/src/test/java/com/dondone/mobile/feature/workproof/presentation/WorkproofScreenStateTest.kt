package com.dondone.mobile.feature.workproof.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkproofScreenStateTest {
    @Test
    fun `detail screen intercepts system back when edit sheet is closed`() {
        assertTrue(
            shouldInterceptWorkproofBack(
                showDetails = true,
                editingRecordId = null
            )
        )
    }

    @Test
    fun `edit sheet keeps system back with sheet dismiss flow`() {
        assertFalse(
            shouldInterceptWorkproofBack(
                showDetails = true,
                editingRecordId = "W-001"
            )
        )
    }
}
