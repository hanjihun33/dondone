package com.dondone.mobile.feature.remittance.presentation

import com.dondone.mobile.data.demo.DemoSeedFactory
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
            .toTransferUiModel()

        assertTrue(uiModel.showReviewScreen)
        assertFalse(uiModel.showTrackerScreen)
    }

    @Test
    fun `submitted state shows only tracker screen`() {
        val uiModel = DemoSeedFactory.create()
            .copy(remittance = DemoSeedFactory.create().remittance.copy(status = TransferStatus.SUBMITTED))
            .toTransferUiModel()

        assertFalse(uiModel.showReviewScreen)
        assertTrue(uiModel.showTrackerScreen)
    }

    @Test
    fun `confirmed state keeps tracker visible and hides review screen`() {
        val uiModel = DemoSeedFactory.create()
            .copy(remittance = DemoSeedFactory.create().remittance.copy(status = TransferStatus.CONFIRMED))
            .toTransferUiModel()

        assertFalse(uiModel.showReviewScreen)
        assertTrue(uiModel.showTrackerScreen)
    }

    @Test
    fun `recipient selector exposes reference style display fields`() {
        val uiModel = DemoSeedFactory.create().toTransferUiModel()

        assertEquals("어디로 돈을 보낼까요?", uiModel.recipientScreenTitle)
        assertEquals("계좌번호 입력", uiModel.recipientSearchPlaceholderText)
        assertEquals(2, uiModel.recipientSections.size)
        assertEquals("자주 보내는 지갑", uiModel.recipientSections[0].title)
        assertEquals("최근 보낸 지갑", uiModel.recipientSections[1].title)
        assertTrue(uiModel.recipientSections.all { it.items.isNotEmpty() })
    }

    @Test
    fun `wallet mode exposes wallet detail labels in transfer ui model`() {
        val uiModel = DemoSeedFactory.create()
            .copy(
                remittance = DemoSeedFactory.create().remittance.copy(
                    destinationMode = TransferDestinationMode.WALLET
                )
            )
            .toTransferUiModel()

        assertEquals(TransferDestinationMode.WALLET, uiModel.destinationMode)
        assertEquals(
            DemoSeedFactory.create().remittance.recipients.first().address,
            uiModel.selectedRecipientWalletLabel
        )
        assertEquals("$360 USDC", uiModel.confirmationAmountText)
    }

    @Test
    fun `recipient display name override is reflected in transfer ui model`() {
        val uiModel = DemoSeedFactory.create()
            .copy(
                remittance = DemoSeedFactory.create().remittance.copy(
                    recipientDisplayNameOverride = "차지훈"
                )
            )
            .toTransferUiModel()

        assertEquals("차지훈", uiModel.selectedRecipientName)
    }
}
