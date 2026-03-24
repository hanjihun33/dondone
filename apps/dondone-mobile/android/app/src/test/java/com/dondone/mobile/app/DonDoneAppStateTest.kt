package com.dondone.mobile.app

import com.dondone.mobile.app.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DonDoneAppStateTest {
    @Test
    fun `home route keeps root top bar so wordmark remains visible`() {
        val state = resolveAppTopBarState(
            currentRoute = Route.HOME,
            showRootTabs = true,
            showSettingsAction = false,
            headerTitle = null,
            headerDateText = null
        )

        assertEquals(AppTopBarLayout.ROOT, state.layout)
        assertTrue(state.showWordmark)
        assertNull(state.headerTitle)
        assertNull(state.headerDateText)
    }

    @Test
    fun `root route with title uses root top bar renderer state`() {
        val state = resolveAppTopBarState(
            currentRoute = Route.WORKPROOF,
            showRootTabs = true,
            showSettingsAction = false,
            headerTitle = "근무 일지",
            headerDateText = null
        )

        assertEquals(AppTopBarLayout.ROOT, state.layout)
        assertFalse(state.showWordmark)
        assertEquals("근무 일지", state.rootState().headerTitle)
    }

    @Test
    fun `child route uses child top bar renderer state`() {
        val state = resolveAppTopBarState(
            currentRoute = Route.ACCOUNT,
            showRootTabs = false,
            showSettingsAction = false,
            headerTitle = "계좌·지갑 관리",
            headerDateText = "2026.03.28"
        )

        assertEquals(AppTopBarLayout.CHILD, state.layout)
        assertFalse(state.showWordmark)
        assertEquals("계좌·지갑 관리", state.headerTitle)
        assertEquals("2026.03.28", state.headerDateText)
    }
}
