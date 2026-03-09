package com.dondone.mobile.app.session

import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TodayWork
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.domain.model.WorkRecord

private const val CLOCK_IN_TIME = "09:02"
private const val CLOCK_OUT_TIME = "18:03"

object DemoSessionReducer {
    fun shiftAsOfDay(state: DemoState, delta: Int): DemoState {
        val nextDay = (state.demo.asOfDay + delta).coerceIn(1, state.demo.monthLength)
        val nextTodayRecord = state.workproof.records.firstOrNull { it.day == nextDay }

        return state.copy(
            demo = state.demo.copy(asOfDay = nextDay),
            workproof = state.workproof.copy(
                today = nextTodayRecord?.toTodayWork() ?: TodayWork()
            )
        )
    }

    fun selectAccount(state: DemoState, accountId: String): DemoState {
        val nextStep = if (state.remittance.flowStep == TransferFlowStep.ACCOUNT) {
            TransferFlowStep.RECIPIENT
        } else {
            state.remittance.flowStep
        }
        return state.copy(
            remittance = state.remittance.copy(
                selectedAccountId = accountId,
                flowStep = nextStep,
                status = TransferStatus.IDLE
            )
        )
    }

    fun openTransferFlow(state: DemoState): DemoState {
        return state.copy(
            remittance = state.remittance.copy(
                flowStep = TransferFlowStep.ACCOUNT,
                status = TransferStatus.IDLE
            )
        )
    }

    fun showAccountStep(state: DemoState): DemoState {
        return state.copy(remittance = state.remittance.copy(flowStep = TransferFlowStep.ACCOUNT))
    }

    fun showRecipientStep(state: DemoState): DemoState {
        return state.copy(remittance = state.remittance.copy(flowStep = TransferFlowStep.RECIPIENT))
    }

    fun selectRecipient(state: DemoState, recipientId: String): DemoState {
        return state.copy(
            remittance = state.remittance.copy(
                selectedRecipientId = recipientId,
                flowStep = TransferFlowStep.AMOUNT
            )
        )
    }

    fun updateTransferAmount(state: DemoState, nextAmount: Int): DemoState {
        return state.copy(
            remittance = state.remittance.copy(
                draftAmountUsd = nextAmount.coerceAtLeast(0),
                status = TransferStatus.IDLE
            )
        )
    }

    fun clockIn(state: DemoState): DemoState {
        if (state.workproof.today.clockIn != null) return state

        val day = state.demo.asOfDay
        val nextRecords = upsertTodayRecord(
            state = state,
            recordBuilder = { current ->
                (current ?: createTodayRecord(state, day, CLOCK_IN_TIME, "-")).copy(
                    inTime = CLOCK_IN_TIME,
                    outTime = current?.outTime ?: "-"
                )
            }
        )

        return state.copy(
            workproof = state.workproof.copy(
                today = TodayWork(clockIn = CLOCK_IN_TIME, clockOut = null),
                records = nextRecords
            )
        )
    }

    fun clockOut(state: DemoState): DemoState {
        val currentClockIn = state.workproof.today.clockIn ?: return state
        if (state.workproof.today.clockOut != null) return state

        val day = state.demo.asOfDay
        val nextRecords = upsertTodayRecord(
            state = state,
            recordBuilder = { current ->
                (current ?: createTodayRecord(state, day, currentClockIn, CLOCK_OUT_TIME)).copy(
                    inTime = current?.inTime?.takeIf { it != "-" } ?: currentClockIn,
                    outTime = CLOCK_OUT_TIME
                )
            }
        )

        return state.copy(
            workproof = state.workproof.copy(
                today = state.workproof.today.copy(clockOut = CLOCK_OUT_TIME),
                records = nextRecords
            )
        )
    }

    fun submitTransfer(state: DemoState): DemoState {
        return state.copy(remittance = state.remittance.copy(status = TransferStatus.SUBMITTED))
    }

    fun confirmTransfer(state: DemoState): DemoState {
        val debitAmount = state.remittance.draftAmountUsd * 1_450
        return state.copy(
            remittance = state.remittance
                .copy(status = TransferStatus.CONFIRMED)
                .changeSelectedAccountBalanceBy(-debitAmount)
        )
    }

    fun resetTransfer(state: DemoState): DemoState {
        return state.copy(
            remittance = state.remittance.copy(
                status = TransferStatus.IDLE,
                flowStep = TransferFlowStep.ACCOUNT
            )
        )
    }

    fun recordActualDeposit(state: DemoState): DemoState {
        return if (state.wage.actualDepositRecordedDay != null) {
            state
        } else {
            state.copy(wage = state.wage.copy(actualDepositRecordedDay = state.demo.asOfDay))
        }
    }

    fun adjustActualDeposit(state: DemoState, delta: Int): DemoState {
        val nextDeposit = (state.wage.actualDeposit + delta).coerceAtLeast(0)
        val balanceDelta = nextDeposit - state.wage.actualDeposit

        return state.copy(
            wage = state.wage.copy(
                actualDeposit = nextDeposit,
                actualDepositRecordedDay = state.wage.actualDepositRecordedDay ?: state.demo.asOfDay
            ),
            remittance = state.remittance.changeSelectedAccountBalanceBy(balanceDelta)
        )
    }

    private fun upsertTodayRecord(
        state: DemoState,
        recordBuilder: (WorkRecord?) -> WorkRecord
    ): List<WorkRecord> {
        val day = state.demo.asOfDay
        val current = state.workproof.records.firstOrNull { it.day == day }
        val nextRecord = recordBuilder(current)

        return if (current == null) {
            listOf(nextRecord) + state.workproof.records
        } else {
            state.workproof.records.map { record ->
                if (record.day == day) nextRecord else record
            }
        }
    }

    private fun createTodayRecord(
        state: DemoState,
        day: Int,
        clockIn: String,
        clockOut: String
    ): WorkRecord {
        return WorkRecord(
            id = "WP-${state.demo.month.toString().padStart(2, '0')}${day.toString().padStart(2, '0')}-TODAY",
            day = day,
            inTime = clockIn,
            outTime = clockOut,
            modified = false,
            attachments = 0
        )
    }

    private fun WorkRecord.toTodayWork(): TodayWork {
        return TodayWork(
            clockIn = inTime.takeIf { it != "-" },
            clockOut = outTime.takeIf { it != "-" }
        )
    }
}
