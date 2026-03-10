package com.dondone.mobile.feature.remittance.presentation

import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.domain.model.TransferStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferUiModelTest {
    @Test
    fun `reviewing state shows only confirmation sheet`() {
        val uiModel = DemoSeedFactory.create()
            .copy(remittance = DemoSeedFactory.create().remittance.copy(status = TransferStatus.REVIEWING))
            .toTransferUiModel()

        assertTrue(uiModel.showConfirmationSheet)
        assertFalse(uiModel.showTrackerScreen)
    }

    @Test
    fun `submitted state shows only tracker screen`() {
        val uiModel = DemoSeedFactory.create()
            .copy(remittance = DemoSeedFactory.create().remittance.copy(status = TransferStatus.SUBMITTED))
            .toTransferUiModel()

        assertFalse(uiModel.showConfirmationSheet)
        assertTrue(uiModel.showTrackerScreen)
    }

    @Test
    fun `confirmed state keeps tracker visible and hides confirmation sheet`() {
        val uiModel = DemoSeedFactory.create()
            .copy(remittance = DemoSeedFactory.create().remittance.copy(status = TransferStatus.CONFIRMED))
            .toTransferUiModel()

        assertFalse(uiModel.showConfirmationSheet)
        assertTrue(uiModel.showTrackerScreen)
    }
}
