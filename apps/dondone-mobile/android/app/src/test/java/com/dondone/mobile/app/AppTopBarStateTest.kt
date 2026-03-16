package com.dondone.mobile.app

import com.dondone.mobile.app.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppTopBarStateTest {
    @Test
    fun `home route keeps root layout and wordmark`() {
        val state = resolveAppTopBarState(
            currentRoute = Route.HOME,
            showRootTabs = true,
            showSettingsAction = true,
            headerTitle = null,
            headerDateText = null
        )

        assertEquals(AppTopBarLayout.ROOT, state.layout)
        assertTrue(state.showWordmark)
        assertTrue(state.showSettingsAction)
    }

    @Test
    fun `workproof detail collapses root top bar`() {
        val state = resolveAppTopBarState(
            currentRoute = Route.WORKPROOF,
            showRootTabs = true,
            showSettingsAction = false,
            headerTitle = null,
            headerDateText = null
        )

        assertEquals(AppTopBarLayout.COLLAPSED_ROOT, state.layout)
        assertFalse(state.showWordmark)
    }

    @Test
    fun `child route uses child layout`() {
        val state = resolveAppTopBarState(
            currentRoute = Route.ACCOUNT,
            showRootTabs = false,
            showSettingsAction = false,
            headerTitle = "계좌·지갑 관리",
            headerDateText = "2026.03.28"
        )

        assertEquals(AppTopBarLayout.CHILD, state.layout)
        assertEquals("계좌·지갑 관리", state.headerTitle)
        assertEquals("2026.03.28", state.headerDateText)
    }
}
