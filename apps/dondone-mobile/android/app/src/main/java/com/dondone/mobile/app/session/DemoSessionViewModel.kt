package com.dondone.mobile.app.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TRANSFER_CONFIRMATION_DELAY_MS = 1800L

class DemoSessionViewModel : ViewModel() {
    private val initialState = DemoSeedFactory.create()
    private var transferCompletionJob: Job? = null
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<DemoState> = _uiState.asStateFlow()

    fun shiftAsOfDay(delta: Int) {
        _uiState.update { state -> DemoSessionReducer.shiftAsOfDay(state, delta) }
    }

    fun selectAccount(accountId: String) {
        _uiState.update { state -> DemoSessionReducer.selectAccount(state, accountId) }
    }

    fun openTransferFlow() {
        if (_uiState.value.remittance.status == TransferStatus.SUBMITTED) {
            return
        }

        cancelTransferCompletion()
        _uiState.update { state -> DemoSessionReducer.openTransferFlow(state) }
    }

    fun showAccountStep() {
        _uiState.update { state -> DemoSessionReducer.showAccountStep(state) }
    }

    fun showRecipientStep() {
        _uiState.update { state -> DemoSessionReducer.showRecipientStep(state) }
    }

    fun showAmountStep() {
        _uiState.update { state -> DemoSessionReducer.showAmountStep(state) }
    }

    fun showAccountStepFromRecipient() {
        _uiState.update { state ->
            DemoSessionReducer.showAccountStepForReturn(state, TransferFlowStep.RECIPIENT)
        }
    }

    fun showAccountStepFromAmount() {
        _uiState.update { state ->
            DemoSessionReducer.showAccountStepForReturn(state, TransferFlowStep.AMOUNT)
        }
    }

    fun showRecipientStepFromAmount() {
        _uiState.update { state ->
            DemoSessionReducer.showRecipientStepForReturn(state, TransferFlowStep.AMOUNT)
        }
    }

    fun selectRecipient(recipientId: String) {
        _uiState.update { state -> DemoSessionReducer.selectRecipient(state, recipientId) }
    }

    fun updateTransferAmount(nextAmount: Int) {
        _uiState.update { state -> DemoSessionReducer.updateTransferAmount(state, nextAmount) }
    }

    fun clockIn() {
        _uiState.update { state -> DemoSessionReducer.clockIn(state) }
    }

    fun clockOut() {
        _uiState.update { state -> DemoSessionReducer.clockOut(state) }
    }

    fun submitTransfer() {
        _uiState.update { state -> DemoSessionReducer.submitTransfer(state) }
    }

    fun dismissTransferConfirmation() {
        _uiState.update { state -> DemoSessionReducer.dismissTransferConfirmation(state) }
    }

    fun confirmTransfer() {
        _uiState.update { state -> DemoSessionReducer.confirmTransfer(state) }
        scheduleTransferCompletion()
    }

    fun resetTransfer() {
        cancelTransferCompletion()
        _uiState.update { state -> DemoSessionReducer.resetTransfer(state) }
    }

    fun recordActualDeposit() {
        _uiState.update { state -> DemoSessionReducer.recordActualDeposit(state) }
    }

    fun adjustActualDeposit(delta: Int) {
        _uiState.update { state -> DemoSessionReducer.adjustActualDeposit(state, delta) }
    }

    fun setActualDeposit(amount: Int) {
        _uiState.update { state -> DemoSessionReducer.setActualDeposit(state, amount) }
    }

    fun saveWorkproofEdit(recordId: String, reason: String, memo: String, addAttachment: Boolean) {
        _uiState.update { state ->
            DemoSessionReducer.saveWorkproofEdit(
                state,
                recordId = recordId,
                reason = reason,
                memo = memo,
                addAttachment = addAttachment
            )
        }
    }

    fun resetSeed() {
        cancelTransferCompletion()
        _uiState.value = initialState
    }

    private fun scheduleTransferCompletion() {
        cancelTransferCompletion()
        transferCompletionJob = viewModelScope.launch {
            delay(TRANSFER_CONFIRMATION_DELAY_MS)
            _uiState.update { state -> DemoSessionReducer.completeTransfer(state) }
        }
    }

    private fun cancelTransferCompletion() {
        transferCompletionJob?.cancel()
        transferCompletionJob = null
    }
}
