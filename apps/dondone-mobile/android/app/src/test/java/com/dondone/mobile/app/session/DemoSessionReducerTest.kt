package com.dondone.mobile.app.session

import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.domain.model.TodayWork
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    fun `addRecipient prepends new local recipient and selects it`() {
        val baseState = DemoSeedFactory.create()

        val nextState = DemoSessionReducer.addRecipient(
            state = baseState,
            alias = "새 가족 지갑",
            relation = "가족",
            walletAddress = "0x7F4F0b8E8fA0d3B6bA91F5bEEfa2276c9168a20D"
        )

        assertEquals("새 가족 지갑", nextState.remittance.recipients.first().name)
        assertEquals("가족", nextState.remittance.recipients.first().relationship)
        assertEquals("0x7F4F0b8E8fA0d3B6bA91F5bEEfa2276c9168a20D", nextState.remittance.recipients.first().address)
        assertEquals(nextState.remittance.recipients.first().id, nextState.remittance.selectedRecipientId)
        assertEquals(baseState.remittance.recipients.size + 1, nextState.remittance.recipients.size)
        assertNotEquals(baseState.remittance.selectedRecipientId, nextState.remittance.selectedRecipientId)
    }

    @Test
    fun `addRecipient ignores duplicate wallet address`() {
        val baseState = DemoSeedFactory.create().copy(
            remittance = DemoSeedFactory.create().remittance.copy(
                recipients = listOf(
                    DemoSeedFactory.create().remittance.recipients.first().copy(
                        address = "0x1234567890abcdef1234567890abcdef12345678"
                    )
                )
            )
        )

        val nextState = DemoSessionReducer.addRecipient(
            state = baseState,
            alias = "중복 지갑",
            relation = "가족",
            walletAddress = "0x1234567890abcdef1234567890abcdef12345678"
        )

        assertEquals(baseState, nextState)
    }

    @Test
    fun `updateRecipient replaces matching local recipient fields`() {
        val baseState = DemoSeedFactory.create()

        val nextState = DemoSessionReducer.updateRecipient(
            state = baseState,
            recipientId = "R-001",
            alias = "수정된 이름",
            relation = "친구",
            walletAddress = "0x1111111111111111111111111111111111111111"
        )

        assertEquals("수정된 이름", nextState.remittance.recipients.first().name)
        assertEquals("친구", nextState.remittance.recipients.first().relationship)
        assertEquals("0x1111111111111111111111111111111111111111", nextState.remittance.recipients.first().address)
        assertEquals(TransferStatus.IDLE, nextState.remittance.status)
    }

    @Test
    fun `openTransferFlow starts at recipient step`() {
        val baseState = DemoSeedFactory.create().copy(
            remittance = DemoSeedFactory.create().remittance.copy(
                flowStep = TransferFlowStep.ACCOUNT,
                destinationMode = TransferDestinationMode.WALLET,
                selectedRecipientId = "R-002",
                recipientDisplayNameOverride = "이전 입력값",
                draftAmountUsd = 480,
                txHash = "0xold",
                status = TransferStatus.CONFIRMED,
                stepReturnTarget = TransferFlowStep.AMOUNT
            )
        )

        val nextState = DemoSessionReducer.openTransferFlow(baseState)

        assertEquals(TransferFlowStep.RECIPIENT, nextState.remittance.flowStep)
        assertEquals(TransferDestinationMode.ACCOUNT, nextState.remittance.destinationMode)
        assertEquals("R-001", nextState.remittance.selectedRecipientId)
        assertNull(nextState.remittance.recipientDisplayNameOverride)
        assertEquals(0, nextState.remittance.draftAmountUsd)
        assertEquals("", nextState.remittance.txHash)
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
