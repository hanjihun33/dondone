package com.dondone.mobile.app.session

import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.domain.model.TodayWork
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DemoSessionReducerTest {
    @Test
    fun `confirm transfer moves reviewing state into submitted tracker state`() {
        val baseState = DemoSeedFactory.create().copy(
            remittance = DemoSeedFactory.create().remittance.copy(status = TransferStatus.REVIEWING)
        )

        val nextState = DemoSessionReducer.confirmTransfer(baseState)

        assertEquals(TransferStatus.SUBMITTED, nextState.remittance.status)
        assertEquals(
            baseState.remittance.selectedAccount().balance,
            nextState.remittance.selectedAccount().balance
        )
    }

    @Test
    fun `complete transfer confirms and deducts balance once`() {
        val baseState = DemoSeedFactory.create().copy(
            remittance = DemoSeedFactory.create().remittance.copy(status = TransferStatus.SUBMITTED)
        )

        val nextState = DemoSessionReducer.completeTransfer(baseState)

        assertEquals(TransferStatus.CONFIRMED, nextState.remittance.status)
        assertEquals(
            baseState.remittance.selectedAccount().balance - (baseState.remittance.draftAmountUsd * 1_450),
            nextState.remittance.selectedAccount().balance
        )
        assertEquals(nextState, DemoSessionReducer.completeTransfer(nextState))
    }

    @Test
    fun `adjustActualDeposit preserves prior transfer deduction on selected account`() {
        val baseState = DemoSeedFactory.create()
        val confirmedState = DemoSessionReducer.completeTransfer(
            baseState.copy(
                remittance = baseState.remittance.copy(
                    flowStep = TransferFlowStep.AMOUNT,
                    status = TransferStatus.SUBMITTED
                )
            )
        )

        val nextState = DemoSessionReducer.adjustActualDeposit(confirmedState, 50_000)

        assertEquals(
            confirmedState.remittance.selectedAccount().balance + 50_000,
            nextState.remittance.selectedAccount().balance
        )
        assertEquals(baseState.wage.actualDeposit + 50_000, nextState.wage.actualDeposit)
    }

    @Test
    fun `selectRecipient advances transfer flow to amount step`() {
        val baseState = DemoSeedFactory.create()

        val nextState = DemoSessionReducer.selectRecipient(baseState, "R-002")

        assertEquals("R-002", nextState.remittance.selectedRecipientId)
        assertEquals(TransferFlowStep.AMOUNT, nextState.remittance.flowStep)
    }

    @Test
    fun `openTransferFlow starts at recipient step`() {
        val baseState = DemoSeedFactory.create().copy(
            remittance = DemoSeedFactory.create().remittance.copy(
                flowStep = TransferFlowStep.ACCOUNT,
                destinationMode = TransferDestinationMode.WALLET,
                status = TransferStatus.CONFIRMED,
                stepReturnTarget = TransferFlowStep.AMOUNT
            )
        )

        val nextState = DemoSessionReducer.openTransferFlow(baseState)

        assertEquals(TransferFlowStep.RECIPIENT, nextState.remittance.flowStep)
        assertEquals(TransferDestinationMode.ACCOUNT, nextState.remittance.destinationMode)
        assertEquals(TransferStatus.IDLE, nextState.remittance.status)
        assertNull(nextState.remittance.stepReturnTarget)
    }

    @Test
    fun `selectTransferDestinationMode updates remittance mode`() {
        val baseState = DemoSeedFactory.create()

        val nextState = DemoSessionReducer.selectTransferDestinationMode(
            state = baseState,
            mode = TransferDestinationMode.WALLET
        )

        assertEquals(TransferDestinationMode.WALLET, nextState.remittance.destinationMode)
    }

    @Test
    fun `updateRecipientDisplayName stores trimmed override`() {
        val baseState = DemoSeedFactory.create()

        val nextState = DemoSessionReducer.updateRecipientDisplayName(baseState, "  차지훈  ")

        assertEquals("차지훈", nextState.remittance.recipientDisplayNameOverride)
    }

    @Test
    fun `shiftAsOfDay syncs today work from matching record or resets when absent`() {
        val baseState = DemoSeedFactory.create()

        val stateWithRecord = DemoSessionReducer.shiftAsOfDay(baseState, -1)
        val stateWithoutRecord = DemoSessionReducer.shiftAsOfDay(baseState, 3)

        assertEquals(27, stateWithRecord.demo.asOfDay)
        assertEquals("09:01", stateWithRecord.workproof.today.clockIn)
        assertEquals("18:03", stateWithRecord.workproof.today.clockOut)

        assertEquals(31, stateWithoutRecord.demo.asOfDay)
        assertEquals(TodayWork(), stateWithoutRecord.workproof.today)
        assertNull(stateWithoutRecord.workproof.today.clockIn)
    }
}
