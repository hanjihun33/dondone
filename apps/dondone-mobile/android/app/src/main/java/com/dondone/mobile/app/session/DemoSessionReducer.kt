package com.dondone.mobile.app.session

import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.Recipient
import com.dondone.mobile.domain.model.TodayWork
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.domain.model.WorkAudit
import com.dondone.mobile.domain.model.WorkRecord
import java.time.LocalDate

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
            state.remittance.stepReturnTarget ?: TransferFlowStep.RECIPIENT
        } else {
            state.remittance.flowStep
        }
        return state.copy(
            remittance = state.remittance.copy(
                selectedAccountId = accountId,
                flowStep = nextStep,
                status = TransferStatus.IDLE,
                stepReturnTarget = null
            )
        )
    }

    fun openTransferFlow(state: DemoState): DemoState {
        return state.copy(
            remittance = state.remittance.copy(
                flowStep = TransferFlowStep.RECIPIENT,
                destinationMode = TransferDestinationMode.ACCOUNT,
                selectedRecipientId = state.remittance.recipients.firstOrNull()?.id ?: state.remittance.selectedRecipientId,
                recipientDisplayNameOverride = null,
                draftAmountUsd = 0,
                txHash = "",
                status = TransferStatus.IDLE,
                stepReturnTarget = null
            )
        )
    }

    fun selectTransferDestinationMode(state: DemoState, mode: TransferDestinationMode): DemoState {
        return state.copy(
            remittance = state.remittance.copy(destinationMode = mode)
        )
    }

    fun updateRecipientDisplayName(state: DemoState, displayName: String): DemoState {
        return state.copy(
            remittance = state.remittance.copy(
                recipientDisplayNameOverride = displayName.trim().ifBlank { null }
            )
        )
    }

    fun showAccountStep(state: DemoState): DemoState {
        return state.copy(
            remittance = state.remittance.copy(
                flowStep = TransferFlowStep.ACCOUNT,
                stepReturnTarget = null
            )
        )
    }

    fun showRecipientStep(state: DemoState): DemoState {
        return state.copy(
            remittance = state.remittance.copy(
                flowStep = TransferFlowStep.RECIPIENT,
                stepReturnTarget = null
            )
        )
    }

    fun showAmountStep(state: DemoState): DemoState {
        return state.copy(
            remittance = state.remittance.copy(
                flowStep = TransferFlowStep.AMOUNT,
                stepReturnTarget = null
            )
        )
    }

    fun showAccountStepForReturn(state: DemoState, returnTarget: TransferFlowStep): DemoState {
        return state.copy(
            remittance = state.remittance.copy(
                flowStep = TransferFlowStep.ACCOUNT,
                stepReturnTarget = returnTarget
            )
        )
    }

    fun showRecipientStepForReturn(state: DemoState, returnTarget: TransferFlowStep): DemoState {
        return state.copy(
            remittance = state.remittance.copy(
                flowStep = TransferFlowStep.RECIPIENT,
                stepReturnTarget = returnTarget
            )
        )
    }

    fun selectRecipient(state: DemoState, recipientId: String): DemoState {
        val nextStep = if (state.remittance.flowStep == TransferFlowStep.RECIPIENT) {
            state.remittance.stepReturnTarget ?: TransferFlowStep.AMOUNT
        } else {
            state.remittance.flowStep
        }
        return state.copy(
            remittance = state.remittance.copy(
                selectedRecipientId = recipientId,
                recipientDisplayNameOverride = null,
                flowStep = nextStep,
                stepReturnTarget = null
            )
        )
    }

    fun addRecipient(
        state: DemoState,
        alias: String,
        relation: String,
        walletAddress: String
    ): DemoState {
        if (state.remittance.recipients.any { it.address == walletAddress }) {
            return state
        }

        val nextId = buildLocalRecipientId(state)
        val nextRecipient = Recipient(
            id = nextId,
            name = alias,
            relationship = relation,
            address = walletAddress
        )

        return state.copy(
            remittance = state.remittance.copy(
                recipients = listOf(nextRecipient) + state.remittance.recipients,
                selectedRecipientId = nextId,
                recipientDisplayNameOverride = null,
                flowStep = if (state.remittance.flowStep == TransferFlowStep.RECIPIENT) {
                    state.remittance.stepReturnTarget ?: TransferFlowStep.AMOUNT
                } else {
                    state.remittance.flowStep
                },
                stepReturnTarget = null,
                status = TransferStatus.IDLE
            )
        )
    }

    fun updateRecipient(
        state: DemoState,
        recipientId: String,
        alias: String,
        relation: String,
        walletAddress: String
    ): DemoState {
        if (state.remittance.recipients.any { it.id != recipientId && it.address == walletAddress }) {
            return state
        }

        val updated = state.remittance.recipients.map { recipient ->
            if (recipient.id == recipientId) {
                recipient.copy(
                    name = alias,
                    relationship = relation,
                    address = walletAddress
                )
            } else {
                recipient
            }
        }

        return state.copy(
            remittance = state.remittance.copy(
                recipients = updated,
                recipientDisplayNameOverride = if (state.remittance.selectedRecipientId == recipientId) {
                    null
                } else {
                    state.remittance.recipientDisplayNameOverride
                },
                status = TransferStatus.IDLE
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

    fun saveWorkproofEdit(
        state: DemoState,
        recordId: String,
        reason: String,
        memo: String,
        addAttachment: Boolean
    ): DemoState {
        val record = state.workproof.records.firstOrNull { it.id == recordId } ?: return state
        if (reason.isBlank()) return state

        val nextAttachments = if (addAttachment) {
            maxOf(record.attachments, 1)
        } else {
            record.attachments
        }
        val nextReason = if (memo.isBlank()) {
            reason
        } else {
            "$reason / $memo"
        }
        val nextRecord = record.copy(
            modified = true,
            attachments = nextAttachments
        )
        val monthText = state.demo.month.toString().padStart(2, '0')
        val dayText = state.demo.asOfDay.toString().padStart(2, '0')
        val timeRange = "${record.inTime}-${record.outTime}"
        val nextAudit = WorkAudit(
            id = record.id,
            before = timeRange,
            after = timeRange,
            reason = nextReason,
            attachments = nextAttachments,
            at = "${state.demo.year}-$monthText-$dayText 12:34"
        )

        return state.copy(
            workproof = state.workproof.copy(
                records = state.workproof.records.map { current ->
                    if (current.id == recordId) nextRecord else current
                },
                audit = listOf(nextAudit) + state.workproof.audit
            )
        )
    }

    fun submitTransfer(state: DemoState): DemoState {
        return state.copy(remittance = state.remittance.copy(status = TransferStatus.REVIEWING))
    }

    fun dismissTransferConfirmation(state: DemoState): DemoState {
        return state.copy(remittance = state.remittance.copy(status = TransferStatus.IDLE))
    }

    fun confirmTransfer(state: DemoState): DemoState {
        if (state.remittance.status != TransferStatus.REVIEWING) return state

        return state.copy(remittance = state.remittance.copy(status = TransferStatus.SUBMITTED))
    }

    fun completeTransfer(state: DemoState): DemoState {
        if (state.remittance.status != TransferStatus.SUBMITTED) return state

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
                flowStep = TransferFlowStep.RECIPIENT,
                destinationMode = TransferDestinationMode.ACCOUNT,
                selectedRecipientId = state.remittance.recipients.firstOrNull()?.id ?: state.remittance.selectedRecipientId,
                recipientDisplayNameOverride = null,
                draftAmountUsd = 0,
                txHash = "",
                stepReturnTarget = null
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

    fun setActualDeposit(state: DemoState, amount: Int): DemoState {
        val nextDeposit = amount.coerceAtLeast(0)
        val balanceDelta = nextDeposit - state.wage.actualDeposit

        return state.copy(
            wage = state.wage.copy(
                actualDeposit = nextDeposit,
                actualDepositRecordedDay = state.demo.asOfDay
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
            workDate = LocalDate.of(state.demo.year, state.demo.month, day),
            day = day,
            inTime = clockIn,
            outTime = clockOut,
            modified = false,
            attachments = 0
        )
    }

    private fun buildLocalRecipientId(state: DemoState): String {
        val nextNumericId = state.remittance.recipients
            .mapNotNull { recipient -> recipient.id.removePrefix("R-LOCAL-").toIntOrNull() }
            .maxOrNull()
            ?.plus(1)
            ?: 1
        return "R-LOCAL-${nextNumericId.toString().padStart(3, '0')}"
    }

    private fun WorkRecord.toTodayWork(): TodayWork {
        return TodayWork(
            clockIn = inTime.takeIf { it != "-" },
            clockOut = outTime.takeIf { it != "-" }
        )
    }
}
