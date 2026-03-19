package com.dondone.mobile.feature.finance.presentation

import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.data.remittance.RemittanceRemotePayload
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload
import com.dondone.mobile.data.remittance.RemittanceWalletPayload
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountManageUiModelTest {

    @Test
    fun `unauthenticated account manage keeps demo account list`() {
        val state = DemoSeedFactory.create()

        val uiModel = state.toAccountManageUiModel()

        assertEquals("총 계좌 잔액", uiModel.totalBalanceLabel)
        assertEquals("₩2,360,000", uiModel.totalBalanceText)
        assertEquals("₩2,360,000", uiModel.totalBalanceAmountText)
        assertNull(uiModel.totalBalanceUnitText)
        assertEquals("내 계좌", uiModel.accountSectionTitle)
        assertEquals("계좌 추가", uiModel.accountActionText)
        assertEquals("주 계좌", uiModel.accounts.first().name)
        assertEquals("****-3124", uiModel.accounts.first().number)
        assertNull(uiModel.accounts.first().copyNumber)
        assertEquals(4, uiModel.phoneDirectory.size)
        assertEquals("010-****-1183", uiModel.phoneDirectory.first().maskedPhoneNumber)
        assertTrue(!uiModel.supportsRemotePhoneSearch)
    }

    @Test
    fun `authenticated account manage shows remote wallet`() {
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

        val uiModel = state.toAccountManageUiModel(
            remittanceRemoteState = remittanceRemoteState,
            isAuthenticated = true
        )

        assertEquals("총 지갑 잔액", uiModel.totalBalanceLabel)
        assertEquals("128.5 dUSDC", uiModel.totalBalanceText)
        assertEquals("128.5", uiModel.totalBalanceAmountText)
        assertEquals("dUSDC", uiModel.totalBalanceUnitText)
        assertEquals("내 지갑", uiModel.accountSectionTitle)
        assertNull(uiModel.accountActionText)
        assertEquals("DonDone Wallet", uiModel.accounts.single().name)
        assertEquals("0x111111...111111", uiModel.accounts.single().number)
        assertEquals("0x1111111111111111111111111111111111111111", uiModel.accounts.single().copyNumber)
        assertEquals("128.5 dUSDC", uiModel.accounts.single().balanceText)
        assertTrue(uiModel.accounts.single().selected)
        assertEquals(0, uiModel.recipientWallets.size)
        assertTrue(uiModel.supportsRemotePhoneSearch)
        assertEquals(0, uiModel.phoneDirectory.size)
    }
}
