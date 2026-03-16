package com.dondone.mobile.app.navigation

import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenChromeTest {
    @Test
    fun `home root route uses wordmark shell state`() {
        val chrome = resolveScreenChrome(
            route = Route.HOME,
            transferStep = TransferFlowStep.RECIPIENT,
            transferStatus = TransferStatus.IDLE,
            isWorkproofDetailVisible = false
        )

        assertNull(chrome.headerTitle)
        assertTrue(chrome.showRootTabs)
        assertTrue(chrome.showSettingsAction)
        assertFalse(chrome.showDate)
    }

    @Test
    fun `workproof detail hides root header title`() {
        val chrome = resolveScreenChrome(
            route = Route.WORKPROOF,
            transferStep = TransferFlowStep.RECIPIENT,
            transferStatus = TransferStatus.IDLE,
            isWorkproofDetailVisible = true
        )

        assertNull(chrome.headerTitle)
        assertTrue(chrome.showRootTabs)
        assertFalse(chrome.showSettingsAction)
        assertFalse(chrome.showDate)
    }

    @Test
    fun `transfer account step shows explicit child header`() {
        val chrome = resolveScreenChrome(
            route = Route.TRANSFER,
            transferStep = TransferFlowStep.ACCOUNT,
            transferStatus = TransferStatus.IDLE,
            isWorkproofDetailVisible = false
        )

        assertEquals("계좌 선택", chrome.headerTitle)
        assertFalse(chrome.showRootTabs)
        assertFalse(chrome.showSettingsAction)
        assertFalse(chrome.showDate)
    }

    @Test
    fun `transfer tracker state hides title and date`() {
        val chrome = resolveScreenChrome(
            route = Route.TRANSFER,
            transferStep = TransferFlowStep.ACCOUNT,
            transferStatus = TransferStatus.SUBMITTED,
            isWorkproofDetailVisible = false
        )

        assertNull(chrome.headerTitle)
        assertFalse(chrome.showRootTabs)
        assertFalse(chrome.showDate)
    }

    @Test
    fun `account child route keeps title and date`() {
        val chrome = resolveScreenChrome(
            route = Route.ACCOUNT,
            transferStep = TransferFlowStep.RECIPIENT,
            transferStatus = TransferStatus.IDLE,
            isWorkproofDetailVisible = false
        )

        assertEquals("계좌·지갑 관리", chrome.headerTitle)
        assertFalse(chrome.showRootTabs)
        assertFalse(chrome.showSettingsAction)
        assertTrue(chrome.showDate)
    }
}
