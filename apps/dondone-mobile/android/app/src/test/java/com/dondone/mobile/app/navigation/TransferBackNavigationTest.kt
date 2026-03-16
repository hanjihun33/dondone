package com.dondone.mobile.app.navigation

import com.dondone.mobile.domain.model.Account
import com.dondone.mobile.domain.model.Recipient
import com.dondone.mobile.domain.model.RemittanceData
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TransferBackNavigationTest {
    @Test
    fun `non transfer route navigates up`() {
        val action = resolveAppBackAction(
            currentRoute = Route.HOME,
            remittance = createRemittanceData()
        )

        assertSame(AppBackAction.NavigateUp, action)
    }

    @Test
    fun `reviewing transfer dismisses confirmation first`() {
        val action = resolveAppBackAction(
            currentRoute = Route.TRANSFER,
            remittance = createRemittanceData(status = TransferStatus.REVIEWING)
        )

        assertSame(AppBackAction.DismissTransferConfirmation, action)
    }

    @Test
    fun `submitted transfer navigates up`() {
        val action = resolveAppBackAction(
            currentRoute = Route.TRANSFER,
            remittance = createRemittanceData(status = TransferStatus.SUBMITTED)
        )

        assertSame(AppBackAction.NavigateUp, action)
    }

    @Test
    fun `idle transfer returns to previous step when available`() {
        val action = resolveAppBackAction(
            currentRoute = Route.TRANSFER,
            remittance = createRemittanceData(
                flowStep = TransferFlowStep.RECIPIENT,
                stepReturnTarget = TransferFlowStep.ACCOUNT
            )
        )

        assertEquals(
            AppBackAction.ShowTransferStep(TransferFlowStep.ACCOUNT),
            action
        )
    }

    @Test
    fun `idle transfer without previous step falls back to navigate up`() {
        val action = resolveAppBackAction(
            currentRoute = Route.TRANSFER,
            remittance = createRemittanceData(
                flowStep = TransferFlowStep.RECIPIENT,
                stepReturnTarget = null
            )
        )

        assertSame(AppBackAction.NavigateUp, action)
    }
}

private fun createRemittanceData(
    status: TransferStatus = TransferStatus.IDLE,
    flowStep: TransferFlowStep = TransferFlowStep.AMOUNT,
    stepReturnTarget: TransferFlowStep? = null
): RemittanceData {
    return RemittanceData(
        accounts = listOf(Account("A-001", "주 계좌", "****-3124", 1_740_000)),
        selectedAccountId = "A-001",
        recipients = listOf(Recipient("R-001", "Minh Family", "가족", "0x2Aa3...17F9")),
        selectedRecipientId = "R-001",
        draftAmountUsd = 360,
        txHash = "0x9f2e3d8b1c0a4e7f6d5c4b3a20e1f0d9c8b7a6f5e4d3c2b1a0",
        status = status,
        flowStep = flowStep,
        destinationMode = TransferDestinationMode.ACCOUNT,
        stepReturnTarget = stepReturnTarget
    )
}
