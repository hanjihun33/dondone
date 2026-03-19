package com.dondone.mobile.feature.remittance.presentation

import com.dondone.mobile.app.session.RemittanceActionUiState
import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.domain.model.Recipient
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferUiModelTest {
    @Test
    fun `reviewing state shows only review screen`() {
        val uiModel = DemoSeedFactory.create()
            .copy(remittance = DemoSeedFactory.create().remittance.copy(status = TransferStatus.REVIEWING))
            .toTransferUiModel(
                remoteState = defaultRemoteState(),
                actionUiState = RemittanceActionUiState(),
                isAuthenticated = false
            )

        assertTrue(uiModel.showReviewScreen)
        assertFalse(uiModel.showTrackerScreen)
    }

    @Test
    fun `submitted state shows only tracker screen`() {
        val uiModel = DemoSeedFactory.create()
            .copy(remittance = DemoSeedFactory.create().remittance.copy(status = TransferStatus.SUBMITTED))
            .toTransferUiModel(
                remoteState = defaultRemoteState(),
                actionUiState = RemittanceActionUiState(),
                isAuthenticated = false
            )

        assertFalse(uiModel.showReviewScreen)
        assertTrue(uiModel.showTrackerScreen)
    }

    @Test
    fun `confirmed state keeps tracker visible and hides review screen`() {
        val uiModel = DemoSeedFactory.create()
            .copy(remittance = DemoSeedFactory.create().remittance.copy(status = TransferStatus.CONFIRMED))
            .toTransferUiModel(
                remoteState = defaultRemoteState(),
                actionUiState = RemittanceActionUiState(),
                isAuthenticated = false
            )

        assertFalse(uiModel.showReviewScreen)
        assertTrue(uiModel.showTrackerScreen)
    }

    @Test
    fun `recipient selector exposes reference style display fields`() {
        val uiModel = DemoSeedFactory.create().toTransferUiModel(
            remoteState = defaultRemoteState(),
            actionUiState = RemittanceActionUiState(),
            isAuthenticated = false
        )

        assertEquals("어디로 돈을 보낼까요?", uiModel.recipientScreenTitle)
        assertEquals("계좌번호 입력", uiModel.recipientSearchPlaceholderText)
        assertEquals(2, uiModel.recipientSections.size)
        assertEquals("자주 보내는 지갑", uiModel.recipientSections[0].title)
        assertEquals("최근 보낸 지갑", uiModel.recipientSections[1].title)
        assertTrue(uiModel.recipientSections.all { it.items.isNotEmpty() })
    }

    @Test
    fun `wallet mode exposes wallet detail labels in transfer ui model`() {
        val fullWalletAddress = "0x1234567890abcdef1234567890abcdef12345678"
        val uiModel = DemoSeedFactory.create()
            .copy(
                remittance = DemoSeedFactory.create().remittance.copy(
                    destinationMode = TransferDestinationMode.WALLET,
                    recipients = listOf(
                        Recipient(
                            id = "R-001",
                            name = "Minh Family",
                            relationship = "가족",
                            address = fullWalletAddress
                        )
                    )
                )
            )
            .toTransferUiModel(
                remoteState = defaultRemoteState(),
                actionUiState = RemittanceActionUiState(),
                isAuthenticated = false
            )

        assertEquals(TransferDestinationMode.WALLET, uiModel.destinationMode)
        assertEquals(
            "${fullWalletAddress.take(8)}...${fullWalletAddress.takeLast(6)}",
            uiModel.selectedRecipientWalletLabel
        )
        assertEquals(fullWalletAddress, uiModel.selectedRecipientWalletFullLabel)
        assertEquals("${'$'}360 USDC", uiModel.confirmationAmountText)
    }

    @Test
    fun `recipient display name override is reflected in transfer ui model`() {
        val uiModel = DemoSeedFactory.create()
            .copy(
                remittance = DemoSeedFactory.create().remittance.copy(
                    recipientDisplayNameOverride = "차지훈"
                )
            )
            .toTransferUiModel(
                remoteState = defaultRemoteState(),
                actionUiState = RemittanceActionUiState(),
                isAuthenticated = false
            )

        assertEquals("차지훈", uiModel.selectedRecipientName)
    }

    @Test
    fun `failed state keeps tracker visible`() {
        val uiModel = DemoSeedFactory.create()
            .copy(remittance = DemoSeedFactory.create().remittance.copy(status = TransferStatus.FAILED))
            .toTransferUiModel(
                remoteState = defaultRemoteState(),
                actionUiState = RemittanceActionUiState(),
                isAuthenticated = false
            )

        assertTrue(uiModel.showTrackerScreen)
    }

    @Test
    fun `submitting action is reflected in transfer ui model`() {
        val uiModel = DemoSeedFactory.create().toTransferUiModel(
            remoteState = defaultRemoteState(),
            actionUiState = RemittanceActionUiState(isSubmitting = true),
            isAuthenticated = false
        )

        assertTrue(uiModel.isActionSubmitting)
    }

    @Test
    fun `reviewing and submitting shows tracker instead of review`() {
        val uiModel = DemoSeedFactory.create()
            .copy(remittance = DemoSeedFactory.create().remittance.copy(status = TransferStatus.REVIEWING))
            .toTransferUiModel(
                remoteState = defaultRemoteState(),
                actionUiState = RemittanceActionUiState(isSubmitting = true),
                isAuthenticated = false
            )

        assertFalse(uiModel.showReviewScreen)
        assertTrue(uiModel.showTrackerScreen)
        assertEquals("송금 요청을 보내는 중이에요.", uiModel.trackerDetailText)
    }

    @Test
    fun `tracker takes precedence over remote gate`() {
        val uiModel = DemoSeedFactory.create()
            .copy(remittance = DemoSeedFactory.create().remittance.copy(status = TransferStatus.SUBMITTED))
            .toTransferUiModel(
                remoteState = RemittanceRemoteState.error("송금 정보를 불러오지 못했어요"),
                actionUiState = RemittanceActionUiState(),
                isAuthenticated = true
            )

        assertEquals(TransferScreenMode.TRACKER, resolveTransferScreenMode(uiModel))
    }
}

private fun defaultRemoteState(): RemittanceRemoteState =
    RemittanceRemoteState.unauthenticated("login required")
