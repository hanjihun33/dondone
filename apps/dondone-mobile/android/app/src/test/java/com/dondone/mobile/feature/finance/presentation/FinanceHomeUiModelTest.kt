package com.dondone.mobile.feature.finance.presentation

import com.dondone.mobile.app.session.VaultActionUiState
import com.dondone.mobile.app.session.VaultMessagePresentation
import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.data.remittance.RemittanceRemotePayload
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload
import com.dondone.mobile.data.remittance.RemittanceWalletPayload
import com.dondone.mobile.data.vault.VaultActionType
import com.dondone.mobile.data.vault.VaultInterestPreviewPayload
import com.dondone.mobile.data.vault.VaultRemotePayload
import com.dondone.mobile.data.vault.VaultRemoteState
import com.dondone.mobile.data.vault.VaultSummaryPayload
import com.dondone.mobile.data.vault.VaultTransactionDetailPayload
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceHomeUiModelTest {

    @Test
    fun `remote vault deposit preview uses selected amount when no stored balance`() {
        val uiModel = DemoSeedFactory.create().toFinanceHomeUiModel(
            vaultRemoteState = remoteVaultState(
                storedAmountAtomic = "0",
                availableToStoreAmountAtomic = "500000000",
                monthlyEstimatedYieldAtomic = "0",
                dailyEstimatedYieldAtomic = "0"
            ),
            selectedVaultAmount = 25,
            selectedVaultActionType = VaultActionType.DEPOSIT,
            vaultActionUiState = VaultActionUiState()
        )

        assertEquals("25 dUSDC", uiModel.vault.detail.selectedAmountText)
        assertEquals("0.1041 dUSDC", uiModel.vault.detail.monthlyInterestText)
        assertEquals("0.0034 dUSDC", uiModel.vault.detail.dailyInterestText)
    }

    @Test
    fun `remote vault withdraw preview uses remaining stored balance`() {
        val uiModel = DemoSeedFactory.create().toFinanceHomeUiModel(
            vaultRemoteState = remoteVaultState(
                storedAmountAtomic = "100000000",
                availableToStoreAmountAtomic = "400000000",
                monthlyEstimatedYieldAtomic = "416666",
                dailyEstimatedYieldAtomic = "13888"
            ),
            selectedVaultAmount = 25,
            selectedVaultActionType = VaultActionType.WITHDRAW,
            vaultActionUiState = VaultActionUiState()
        )

        assertEquals("0.3124 dUSDC", uiModel.vault.detail.monthlyInterestText)
        assertEquals("0.0104 dUSDC", uiModel.vault.detail.dailyInterestText)
    }

    @Test
    fun `finance wallet balance prefers remittance balance so it matches home`() {
        val uiModel = DemoSeedFactory.create().toFinanceHomeUiModel(
            remittanceRemoteState = remoteRemittanceState(tokenBalanceAtomic = "128500000"),
            vaultRemoteState = remoteVaultState(
                storedAmountAtomic = "100000000",
                availableToStoreAmountAtomic = "400000000",
                monthlyEstimatedYieldAtomic = "416666",
                dailyEstimatedYieldAtomic = "13888"
            )
        )

        assertEquals("128.5 dUSDC", uiModel.vault.detail.walletBalanceText)
    }

    @Test
    fun `confirmed remote vault transaction exposes completion banner copy`() {
        val uiModel = DemoSeedFactory.create().toFinanceHomeUiModel(
            vaultRemoteState = remoteVaultState(
                storedAmountAtomic = "100000000",
                availableToStoreAmountAtomic = "400000000",
                monthlyEstimatedYieldAtomic = "416666",
                dailyEstimatedYieldAtomic = "13888",
                latestTransaction = VaultTransactionDetailPayload(
                    requestId = "vtx-1",
                    txType = "DEPOSIT",
                    status = "CONFIRMED",
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    vaultAddress = "0x2222222222222222222222222222222222222222",
                    assetSymbol = "dUSDC",
                    amountAtomic = "100000000",
                    shareDelta = "100000000",
                    txHash = "0xabc",
                    failureCode = null,
                    createdAt = LocalDateTime.parse("2026-03-23T10:00:00"),
                    updatedAt = LocalDateTime.parse("2026-03-23T10:01:00"),
                    confirmedAt = LocalDateTime.parse("2026-03-23T10:01:00")
                )
            ),
            selectedVaultAmount = 25,
            selectedVaultActionType = VaultActionType.DEPOSIT,
            vaultActionUiState = VaultActionUiState()
        )

        assertEquals("예치 완료", uiModel.vault.latestStatusText)
        assertEquals("지갑 잔액이 예치 잔액으로 반영됐어요.", uiModel.vault.detail.statusBodyText)
        assertEquals("예치 잔액과 예상 이자를 바로 확인할 수 있어요.", uiModel.vault.helperText)
    }

    @Test
    fun `confirmed remote vault withdraw uses short completion copy`() {
        val uiModel = DemoSeedFactory.create().toFinanceHomeUiModel(
            vaultRemoteState = remoteVaultState(
                storedAmountAtomic = "100000000",
                availableToStoreAmountAtomic = "400000000",
                monthlyEstimatedYieldAtomic = "416666",
                dailyEstimatedYieldAtomic = "13888",
                latestTransaction = VaultTransactionDetailPayload(
                    requestId = "vtx-1w",
                    txType = "WITHDRAW",
                    status = "CONFIRMED",
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    vaultAddress = "0x2222222222222222222222222222222222222222",
                    assetSymbol = "dUSDC",
                    amountAtomic = "50000000",
                    shareDelta = "-50000000",
                    txHash = "0xdef",
                    failureCode = null,
                    createdAt = LocalDateTime.parse("2026-03-23T10:02:00"),
                    updatedAt = LocalDateTime.parse("2026-03-23T10:03:00"),
                    confirmedAt = LocalDateTime.parse("2026-03-23T10:03:00")
                )
            ),
            selectedVaultAmount = 25,
            selectedVaultActionType = VaultActionType.WITHDRAW,
            vaultActionUiState = VaultActionUiState()
        )

        assertEquals("출금 완료", uiModel.vault.latestStatusText)
        assertEquals("지갑 잔액으로 돌아왔어요.", uiModel.vault.detail.statusBodyText)
    }

    @Test
    fun `failed remote vault transaction exposes failure banner copy`() {
        val uiModel = DemoSeedFactory.create().toFinanceHomeUiModel(
            vaultRemoteState = remoteVaultState(
                storedAmountAtomic = "100000000",
                availableToStoreAmountAtomic = "400000000",
                monthlyEstimatedYieldAtomic = "416666",
                dailyEstimatedYieldAtomic = "13888",
                latestTransaction = VaultTransactionDetailPayload(
                    requestId = "vtx-2",
                    txType = "WITHDRAW",
                    status = "FAILED",
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    vaultAddress = "0x2222222222222222222222222222222222222222",
                    assetSymbol = "dUSDC",
                    amountAtomic = "50000000",
                    shareDelta = "-50000000",
                    txHash = null,
                    failureCode = "가스비 부족으로 출금이 실패했어요.",
                    createdAt = LocalDateTime.parse("2026-03-23T10:05:00"),
                    updatedAt = LocalDateTime.parse("2026-03-23T10:06:00"),
                    confirmedAt = null
                )
            ),
            selectedVaultAmount = 25,
            selectedVaultActionType = VaultActionType.WITHDRAW,
            vaultActionUiState = VaultActionUiState()
        )

        assertEquals("출금 실패", uiModel.vault.latestStatusText)
        assertEquals("가스비 부족으로 출금이 실패했어요.", uiModel.vault.detail.statusBodyText)
        assertEquals(true, uiModel.vault.latestStatusIsError)
    }

    @Test
    fun `toast only vault completion message is hidden from bottom sheet feedback`() {
        val uiModel = DemoSeedFactory.create().toFinanceHomeUiModel(
            vaultRemoteState = remoteVaultState(
                storedAmountAtomic = "100000000",
                availableToStoreAmountAtomic = "400000000",
                monthlyEstimatedYieldAtomic = "416666",
                dailyEstimatedYieldAtomic = "13888",
                latestTransaction = VaultTransactionDetailPayload(
                    requestId = "vtx-3",
                    txType = "DEPOSIT",
                    status = "CONFIRMED",
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    vaultAddress = "0x2222222222222222222222222222222222222222",
                    assetSymbol = "dUSDC",
                    amountAtomic = "100000000",
                    shareDelta = "100000000",
                    txHash = "0xabc",
                    failureCode = null,
                    createdAt = LocalDateTime.parse("2026-03-23T10:00:00"),
                    updatedAt = LocalDateTime.parse("2026-03-23T10:01:00"),
                    confirmedAt = LocalDateTime.parse("2026-03-23T10:01:00")
                )
            ),
            vaultActionUiState = VaultActionUiState(
                message = "예치가 완료됐어요.",
                messagePresentation = VaultMessagePresentation.TOAST_ONLY
            )
        )

        assertEquals(true, uiModel.vault.detail.statusIsTerminal)
        assertEquals("예치 완료", uiModel.vault.latestStatusText)
        assertEquals(true, uiModel.vault.shouldDismissDetailSheet)
    }

    @Test
    fun `remote vault disclaimer is normalized to korean copy`() {
        val uiModel = DemoSeedFactory.create().toFinanceHomeUiModel(
            vaultRemoteState = remoteVaultState(
                storedAmountAtomic = "100000000",
                availableToStoreAmountAtomic = "400000000",
                monthlyEstimatedYieldAtomic = "416666",
                dailyEstimatedYieldAtomic = "13888",
                disclaimer = "Valut value are demo-only estimates on testnet and do not guarantee real profit."
            )
        )

        assertEquals(
            "예상 이자는 테스트넷 기준 데모 추정치이며 실제 수익을 보장하지 않습니다.",
            uiModel.vault.detail.disclaimerText
        )
    }

    private fun remoteVaultState(
        storedAmountAtomic: String,
        availableToStoreAmountAtomic: String,
        monthlyEstimatedYieldAtomic: String,
        dailyEstimatedYieldAtomic: String,
        latestTransaction: VaultTransactionDetailPayload? = null,
        disclaimer: String = "Vault values are demo-only estimates on testnet and do not guarantee real profit."
    ): VaultRemoteState {
        return VaultRemoteState.content(
            VaultRemotePayload(
                summary = VaultSummaryPayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    vaultAddress = "0x2222222222222222222222222222222222222222",
                    network = "sepolia",
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    storedAmountAtomic = storedAmountAtomic,
                    accruedYieldAtomic = "0",
                    walletTokenBalanceAtomic = "500000000",
                    availableToStoreAmountAtomic = availableToStoreAmountAtomic,
                    shareBalance = storedAmountAtomic,
                    interestPreview = VaultInterestPreviewPayload(
                        dailyEstimatedYieldAtomic = dailyEstimatedYieldAtomic,
                        monthlyEstimatedYieldAtomic = monthlyEstimatedYieldAtomic,
                        yearlyEstimatedYieldAtomic = "5000000",
                        apyBps = 500
                    ),
                    disclaimer = disclaimer
                ),
                latestTransaction = latestTransaction
            )
        )
    }

    private fun remoteRemittanceState(tokenBalanceAtomic: String): RemittanceRemoteState {
        return RemittanceRemoteState.content(
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
                    tokenBalanceAtomic = tokenBalanceAtomic,
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = emptyList(),
                transfers = emptyList(),
                activeTransfer = null
            )
        )
    }
}
