package com.dondone.mobile.feature.home.presentation

import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.domain.model.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiModelTest {
    @Test
    fun `unrecorded deposit before payday keeps finance as next action`() {
        val baseState = DemoSeedFactory.create().copy(
            demo = DemoSeedFactory.create().demo.copy(asOfDay = 20),
            wage = DemoSeedFactory.create().wage.copy(actualDepositRecordedDay = null)
        )

        val uiModel = baseState.toHomeUiModel()

        assertEquals("입금 대기", uiModel.money.statusText)
        assertEquals(HomeActionTarget.FINANCE, uiModel.money.nextAction.actionTarget)
        assertFalse(uiModel.money.showWorkActionCard)
        assertFalse(uiModel.money.showPaydayCard)
    }

    @Test
    fun `difference state routes home next action to wage check`() {
        val baseState = DemoSeedFactory.create().copy(
            wage = DemoSeedFactory.create().wage.copy(
                actualDepositRecordedDay = 28,
                actualDeposit = 1_000_000
            )
        )

        val uiModel = baseState.toHomeUiModel()

        assertEquals("확인 필요한 차이", uiModel.money.statusText)
        assertEquals(HomeActionTarget.WAGE, uiModel.money.nextAction.actionTarget)
        assertTrue(uiModel.money.showWorkActionCard)
        assertFalse(uiModel.money.showPaydayCard)
    }

    @Test
    fun `confirmed transfer with no difference routes home next action to menu`() {
        val seed = DemoSeedFactory.create()
        val noDifferenceDeposit = seed.wage.hourly * seed.wage.totalHours +
            (seed.wage.hourly * seed.wage.overtimeHours * 0.5).toInt() +
            (seed.wage.hourly * seed.wage.nightHours * 0.5).toInt()
        val baseState = seed.copy(
            wage = seed.wage.copy(
                actualDepositRecordedDay = 28,
                actualDeposit = noDifferenceDeposit
            ),
            remittance = seed.remittance.copy(status = TransferStatus.CONFIRMED)
        )

        val uiModel = baseState.toHomeUiModel()

        assertEquals("이상 없음", uiModel.money.statusText)
        assertEquals(HomeActionTarget.MENU, uiModel.money.nextAction.actionTarget)
        assertFalse(uiModel.money.showWorkActionCard)
        assertTrue(uiModel.money.showPaydayCard)
    }
}
