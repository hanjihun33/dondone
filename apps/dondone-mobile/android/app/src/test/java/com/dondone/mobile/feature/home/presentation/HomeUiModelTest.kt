package com.dondone.mobile.feature.home.presentation

import com.dondone.mobile.app.session.RemittanceCompletionNoticeUiState
import com.dondone.mobile.core.designsystem.BadgeTone
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
    fun `confirmed transfer with no difference keeps finance next action when no completion banner`() {
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

        assertEquals(HomeActionTarget.FINANCE, uiModel.money.nextAction.actionTarget)
        assertFalse(uiModel.money.showWorkActionCard)
        assertEquals("금융 보기", uiModel.money.nextAction.buttonText)
        assertEquals(null, uiModel.completionBanner)
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

    @Test
    fun `completion notice shows success banner on home`() {
        val uiModel = DemoSeedFactory.create().toHomeUiModel(
            remittanceCompletionNoticeUiState = RemittanceCompletionNoticeUiState(
                transferId = "tx-1",
                status = TransferStatus.CONFIRMED,
                recipientName = "테스트 수신자",
                amountAtomic = 360_000_000L,
                assetSymbol = "dUSDC",
                txHash = "0xabc"
            )
        )

        assertEquals("송금 완료", uiModel.completionBanner?.title)
        assertEquals(
            "테스트 수신자에게 송금이 완료됐어요.",
            uiModel.completionBanner?.message
        )
        assertEquals(BadgeTone.Success, uiModel.completionBanner?.tone)
    }

    @Test
    fun `completion notice shows failure banner on home`() {
        val uiModel = DemoSeedFactory.create().toHomeUiModel(
            remittanceCompletionNoticeUiState = RemittanceCompletionNoticeUiState(
                transferId = "tx-2",
                status = TransferStatus.FAILED,
                recipientName = "테스트 수신자",
                amountAtomic = 360_000_000L,
                assetSymbol = "dUSDC",
                txHash = null
            )
        )

        assertEquals("송금 실패", uiModel.completionBanner?.title)
        assertEquals(
            "테스트 수신자에게 송금을 마치지 못했어요. 잠시 후 다시 시도해 주세요.",
            uiModel.completionBanner?.message
        )
        assertEquals(BadgeTone.Warning, uiModel.completionBanner?.tone)
    }
}
