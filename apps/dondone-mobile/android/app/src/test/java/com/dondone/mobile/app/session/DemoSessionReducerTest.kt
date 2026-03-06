package com.dondone.mobile.app.session

import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.domain.model.TodayWork
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DemoSessionReducerTest {
    @Test
    fun `adjustActualDeposit preserves prior transfer deduction on selected account`() {
        val baseState = DemoSeedFactory.create()
        val confirmedState = DemoSessionReducer.confirmTransfer(
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
