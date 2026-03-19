package com.dondone.mobile.feature.home.presentation

import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.data.remittance.RemittanceRemotePayload
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload
import com.dondone.mobile.data.remittance.RemittanceWalletPayload
import com.dondone.mobile.domain.model.TransferStatus
import java.time.LocalDateTime
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

        assertEquals(HomeActionTarget.FINANCE, uiModel.money.nextAction.actionTarget)
        assertFalse(uiModel.money.showWorkActionCard)
        assertEquals("금융 보기", uiModel.money.nextAction.buttonText)
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

        assertEquals(HomeActionTarget.WAGE, uiModel.money.nextAction.actionTarget)
        assertTrue(uiModel.money.showWorkActionCard)
        assertEquals("보기", uiModel.money.nextAction.buttonText)
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

        assertEquals(HomeActionTarget.MENU, uiModel.money.nextAction.actionTarget)
        assertFalse(uiModel.money.showWorkActionCard)
        assertEquals("문서", uiModel.money.nextAction.buttonText)
    }

    @Test
    fun `unauthenticated home shows representative account info`() {
        val state = DemoSeedFactory.create()

        val uiModel = state.toHomeUiModel()

        assertEquals("대표 계좌", uiModel.account.titleText)
        assertEquals("₩1,740,000", uiModel.account.balanceText)
    }

    @Test
    fun `authenticated home shows representative wallet info`() {
        val state = DemoSeedFactory.create()
        val remittanceRemoteState = RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-19T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-19T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "128500000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = emptyList(),
                transfers = emptyList(),
                activeTransfer = null
            )
        )

        val uiModel = state.toHomeUiModel(
            remittanceRemoteState = remittanceRemoteState,
            isAuthenticated = true
        )

        assertEquals("대표 지갑", uiModel.account.titleText)
        assertEquals("128.5 dUSDC", uiModel.account.balanceText)
    }
}
