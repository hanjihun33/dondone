package com.dondone.mobile.app.session

import androidx.lifecycle.ViewModel
import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.domain.model.DemoState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DemoSessionViewModel : ViewModel() {
    private val initialState = DemoSeedFactory.create()
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<DemoState> = _uiState.asStateFlow()

    fun shiftAsOfDay(delta: Int) {
        _uiState.update { state -> DemoSessionReducer.shiftAsOfDay(state, delta) }
    }

    fun selectAccount(accountId: String) {
        _uiState.update { state -> DemoSessionReducer.selectAccount(state, accountId) }
    }

    fun openTransferFlow() {
        _uiState.update { state -> DemoSessionReducer.openTransferFlow(state) }
    }

    fun showRecipientStep() {
        _uiState.update { state -> DemoSessionReducer.showRecipientStep(state) }
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

    fun confirmTransfer() {
        _uiState.update { state -> DemoSessionReducer.confirmTransfer(state) }
    }

    fun resetTransfer() {
        _uiState.update { state -> DemoSessionReducer.resetTransfer(state) }
    }

    fun recordActualDeposit() {
        _uiState.update { state -> DemoSessionReducer.recordActualDeposit(state) }
    }

    fun adjustActualDeposit(delta: Int) {
        _uiState.update { state -> DemoSessionReducer.adjustActualDeposit(state, delta) }
    }

    fun resetSeed() {
        _uiState.value = initialState
    }
}
