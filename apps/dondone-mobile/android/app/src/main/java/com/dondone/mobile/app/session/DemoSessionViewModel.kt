package com.dondone.mobile.app.session

import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dondone.mobile.data.advance.AdvanceRemoteMode
import com.dondone.mobile.data.advance.AdvanceRequestDetailPayload
import com.dondone.mobile.data.advance.AdvanceRequestItemPayload
import com.dondone.mobile.data.advance.AdvanceRemoteState
import com.dondone.mobile.data.advance.AdvanceRepository
import com.dondone.mobile.data.advance.AdvanceUnauthorizedException
import com.dondone.mobile.data.advance.BackendAdvanceRepository
import com.dondone.mobile.data.auth.AuthRepository
import com.dondone.mobile.data.auth.AuthSession
import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.data.documents.BackendWorkproofDocumentRepository
import com.dondone.mobile.data.documents.WorkproofDocumentPreviewPayload
import com.dondone.mobile.data.documents.WorkproofDocumentRepository
import com.dondone.mobile.data.documents.WorkproofDocumentUnauthorizedException
import com.dondone.mobile.data.workproof.BackendWorkproofRepository
import com.dondone.mobile.data.workproof.WorkproofRemotePayload
import com.dondone.mobile.data.workproof.WorkproofRemoteMode
import com.dondone.mobile.data.workproof.WorkproofRemoteState
import com.dondone.mobile.data.workproof.WorkproofRepository
import com.dondone.mobile.data.workproof.WorkproofUnauthorizedException
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TodayWork
import com.dondone.mobile.domain.model.WorkRecord
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfCreateUiState
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfFileAction
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfFileUiState
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfPreviewUiModel
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfPreviewUiState
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TRANSFER_CONFIRMATION_DELAY_MS = 1800L
private const val ADVANCE_REMOTE_LOGIN_MESSAGE = "로그인 후 실연동 데이터를 불러옵니다."
private const val WORKPROOF_REMOTE_LOGIN_MESSAGE = "로그인 후 출퇴근 실연동을 불러옵니다."
private const val WORKPROOF_PDF_POLL_INTERVAL_MS = 1000L
private const val WORKPROOF_PDF_POLL_MAX_ATTEMPTS = 10

class DemoSessionViewModel(
    private val appContext: Context,
    private val authRepository: AuthRepository,
    private val advanceRepository: AdvanceRepository = BackendAdvanceRepository(),
    private val workproofRepository: WorkproofRepository = BackendWorkproofRepository(),
    private val workproofDocumentRepository: WorkproofDocumentRepository = BackendWorkproofDocumentRepository()
) : ViewModel() {
    private val initialState = DemoSeedFactory.create()
    private var transferCompletionJob: Job? = null
    private var workproofPdfPollingJob: Job? = null
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<DemoState> = _uiState.asStateFlow()
    private val _authUiState = MutableStateFlow(AuthUiState.restoring())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()
    private val _advanceRemoteState =
        MutableStateFlow(AdvanceRemoteState.unauthenticated(ADVANCE_REMOTE_LOGIN_MESSAGE))
    val advanceRemoteState: StateFlow<AdvanceRemoteState> = _advanceRemoteState.asStateFlow()
    private val _workproofRemoteState =
        MutableStateFlow(WorkproofRemoteState.unauthenticated(WORKPROOF_REMOTE_LOGIN_MESSAGE))
    private val _workproofActionUiState = MutableStateFlow(WorkproofActionUiState())
    val workproofActionUiState: StateFlow<WorkproofActionUiState> = _workproofActionUiState.asStateFlow()
    private val _workproofPdfPreviewUiState = MutableStateFlow(WorkproofPdfPreviewUiState())
    val workproofPdfPreviewUiState: StateFlow<WorkproofPdfPreviewUiState> = _workproofPdfPreviewUiState.asStateFlow()
    private val _workproofPdfCreateUiState = MutableStateFlow(WorkproofPdfCreateUiState())
    val workproofPdfCreateUiState: StateFlow<WorkproofPdfCreateUiState> = _workproofPdfCreateUiState.asStateFlow()
    private val _workproofPdfFileUiState = MutableStateFlow(WorkproofPdfFileUiState())
    val workproofPdfFileUiState: StateFlow<WorkproofPdfFileUiState> = _workproofPdfFileUiState.asStateFlow()
    private val _selectedAdvanceAmount = MutableStateFlow<Int?>(null)
    val selectedAdvanceAmount: StateFlow<Int?> = _selectedAdvanceAmount.asStateFlow()
    private val _advanceRequestUiState = MutableStateFlow(AdvanceRequestUiState())
    val advanceRequestUiState: StateFlow<AdvanceRequestUiState> = _advanceRequestUiState.asStateFlow()
    private val _advanceRequestDetailUiState = MutableStateFlow(AdvanceRequestDetailUiState())
    val advanceRequestDetailUiState: StateFlow<AdvanceRequestDetailUiState> = _advanceRequestDetailUiState.asStateFlow()

    init {
        restoreAuthSession()
    }

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

    fun selectTransferDestinationMode(mode: TransferDestinationMode) {
        _uiState.update { state -> DemoSessionReducer.selectTransferDestinationMode(state, mode) }
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

    fun updateRecipientDisplayName(displayName: String) {
        _uiState.update { state -> DemoSessionReducer.updateRecipientDisplayName(state, displayName) }
    }

    fun updateTransferAmount(nextAmount: Int) {
        _uiState.update { state -> DemoSessionReducer.updateTransferAmount(state, nextAmount) }
    }

    fun clockIn() {
        submitWorkproofAction(
            fallback = { state -> DemoSessionReducer.clockIn(state) },
            remoteCall = { session -> workproofRepository.clockIn(session.accessToken, _uiState.value.workproof) },
            successMessage = "출근 기록이 백엔드에 저장됐어요.",
            failureMessage = "출근 기록을 저장하지 못했어요."
        )
    }

    fun clockOut() {
        submitWorkproofAction(
            fallback = { state -> DemoSessionReducer.clockOut(state) },
            remoteCall = { session -> workproofRepository.clockOut(session.accessToken, _uiState.value.workproof) },
            successMessage = "퇴근 기록이 백엔드에 저장됐어요.",
            failureMessage = "퇴근 기록을 저장하지 못했어요."
        )
    }

    private fun submitWorkproofAction(
        fallback: (DemoState) -> DemoState,
        remoteCall: suspend (AuthSession) -> WorkproofRemoteState,
        successMessage: String,
        failureMessage: String
    ) {
        val session = _authUiState.value.session
        if (session == null) {
            _uiState.update { state -> fallback(state) }
            return
        }

        viewModelScope.launch {
            _workproofActionUiState.value = WorkproofActionUiState(isSubmitting = true)
            try {
                val remoteState = remoteCall(session)
                if (remoteState.mode != WorkproofRemoteMode.CONTENT) {
                    _workproofRemoteState.value = remoteState
                    _workproofActionUiState.value = WorkproofActionUiState(
                        message = remoteState.errorMessage ?: failureMessage,
                        isError = true
                    )
                    return@launch
                }
                applyWorkproofRemoteState(remoteState)
                _workproofActionUiState.value = WorkproofActionUiState(message = successMessage)
            } catch (error: WorkproofUnauthorizedException) {
                expireSession(error.message)
                _workproofActionUiState.value = WorkproofActionUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                _workproofActionUiState.value = WorkproofActionUiState(
                    message = error.message ?: failureMessage,
                    isError = true
                )
            }
        }
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
        cancelWorkproofPdfPolling()
        _uiState.value = initialState
        _advanceRequestUiState.value = AdvanceRequestUiState()
        _advanceRequestDetailUiState.value = AdvanceRequestDetailUiState()
        _workproofActionUiState.value = WorkproofActionUiState()
        _workproofPdfPreviewUiState.value = WorkproofPdfPreviewUiState()
        _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState()
        _workproofPdfFileUiState.value = WorkproofPdfFileUiState()
        refreshAdvanceRemoteState()
        refreshWorkproofRemoteState()
    }

    fun clearWorkproofActionMessage() {
        if (!_workproofActionUiState.value.isSubmitting && _workproofActionUiState.value.message != null) {
            _workproofActionUiState.value = WorkproofActionUiState()
        }
    }

    fun previewWorkproofPdf(startDate: LocalDate, endDate: LocalDate) {
        val session = _authUiState.value.session ?: run {
            _workproofPdfPreviewUiState.value = WorkproofPdfPreviewUiState(
                errorMessage = "로그인 후 문서 미리보기를 확인할 수 있어요."
            )
            return
        }
        val workplaceId = _uiState.value.workproof.workplaceId ?: run {
            _workproofPdfPreviewUiState.value = WorkproofPdfPreviewUiState(
                errorMessage = "연결된 근무지 정보를 다시 불러와 주세요."
            )
            return
        }
        if (startDate.isAfter(endDate)) {
            _workproofPdfPreviewUiState.value = WorkproofPdfPreviewUiState(
                errorMessage = "종료일은 시작일보다 빠를 수 없어요."
            )
            return
        }

        viewModelScope.launch {
            _workproofPdfPreviewUiState.value = WorkproofPdfPreviewUiState(isLoading = true)
            try {
                val payload = workproofDocumentRepository.preview(
                    accessToken = session.accessToken,
                    workplaceId = workplaceId,
                    startDate = startDate,
                    endDate = endDate
                )
                _workproofPdfPreviewUiState.value = WorkproofPdfPreviewUiState(
                    preview = payload.toUiModel()
                )
            } catch (error: WorkproofDocumentUnauthorizedException) {
                expireSession(error.message)
                _workproofPdfPreviewUiState.value = WorkproofPdfPreviewUiState(
                    errorMessage = error.message ?: "세션이 만료되어 다시 로그인해 주세요."
                )
            } catch (error: Exception) {
                _workproofPdfPreviewUiState.value = WorkproofPdfPreviewUiState(
                    errorMessage = error.message ?: "문서 미리보기를 불러오지 못했어요."
                )
            }
        }
    }

    fun clearWorkproofPdfPreview() {
        _workproofPdfPreviewUiState.value = WorkproofPdfPreviewUiState()
    }

    fun createWorkproofPdf(startDate: LocalDate, endDate: LocalDate) {
        val session = _authUiState.value.session ?: run {
            _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState(
                errorMessage = "로그인 후 문서를 생성할 수 있어요."
            )
            return
        }
        val workplaceId = _uiState.value.workproof.workplaceId ?: run {
            _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState(
                errorMessage = "연결된 근무지 정보를 다시 불러와 주세요."
            )
            return
        }
        if (startDate.isAfter(endDate)) {
            _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState(
                errorMessage = "종료일은 시작일보다 빠를 수 없어요."
            )
            return
        }

        viewModelScope.launch {
            _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState(isSubmitting = true)
            try {
                val payload = workproofDocumentRepository.create(
                    accessToken = session.accessToken,
                    workplaceId = workplaceId,
                    startDate = startDate,
                    endDate = endDate
                )
                _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState(
                    requestId = payload.requestId,
                    status = payload.status,
                    pollUrl = payload.pollUrl
                )
                startWorkproofPdfPolling(
                    accessToken = session.accessToken,
                    requestId = payload.requestId
                )
            } catch (error: WorkproofDocumentUnauthorizedException) {
                expireSession(error.message)
                _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState(
                    errorMessage = error.message ?: "세션이 만료되어 다시 로그인해 주세요."
                )
            } catch (error: Exception) {
                _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState(
                    errorMessage = error.message ?: "문서 생성 요청을 접수하지 못했어요."
                )
            }
        }
    }

    fun clearWorkproofPdfCreateState() {
        cancelWorkproofPdfPolling()
        _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState()
    }

    fun openWorkproofPdf(documentId: Long) {
        downloadWorkproofPdf(documentId, WorkproofPdfFileAction.OPEN)
    }

    fun shareWorkproofPdf(documentId: Long) {
        downloadWorkproofPdf(documentId, WorkproofPdfFileAction.SHARE)
    }

    fun clearWorkproofPdfFileState() {
        _workproofPdfFileUiState.value = WorkproofPdfFileUiState()
    }

    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) {
            _authUiState.value = AuthUiState.unauthenticated("이메일과 비밀번호를 입력해 주세요.")
            return
        }

        viewModelScope.launch {
            _authUiState.value = AuthUiState.submitting()
            try {
                val session = authRepository.login(trimmedEmail, password)
                onAuthenticated(session)
            } catch (error: Exception) {
                _authUiState.value = AuthUiState.unauthenticated(
                    error.message ?: "로그인에 실패했어요. 잠시 후 다시 시도해 주세요."
                )
                _advanceRemoteState.value = AdvanceRemoteState.unauthenticated(ADVANCE_REMOTE_LOGIN_MESSAGE)
                _workproofRemoteState.value = WorkproofRemoteState.unauthenticated(WORKPROOF_REMOTE_LOGIN_MESSAGE)
            }
        }
    }

    fun signup(name: String, email: String, password: String) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        if (trimmedName.isBlank() || trimmedEmail.isBlank() || password.isBlank()) {
            _authUiState.value = AuthUiState.unauthenticated("이름, 이메일, 비밀번호를 모두 입력해 주세요.")
            return
        }
        if (password.length < 8) {
            _authUiState.value = AuthUiState.unauthenticated("비밀번호는 8자 이상이어야 해요.")
            return
        }

        viewModelScope.launch {
            _authUiState.value = AuthUiState.submitting()
            try {
                val session = authRepository.signup(trimmedName, trimmedEmail, password)
                onAuthenticated(session)
            } catch (error: Exception) {
                _authUiState.value = AuthUiState.unauthenticated(
                    error.message ?: "회원가입에 실패했어요. 잠시 후 다시 시도해 주세요."
                )
                _advanceRemoteState.value = AdvanceRemoteState.unauthenticated(ADVANCE_REMOTE_LOGIN_MESSAGE)
                _workproofRemoteState.value = WorkproofRemoteState.unauthenticated(WORKPROOF_REMOTE_LOGIN_MESSAGE)
            }
        }
    }

    fun clearAuthError() {
        if (_authUiState.value.errorMessage != null && !_authUiState.value.isSubmitting) {
            _authUiState.value = _authUiState.value.copy(errorMessage = null)
        }
    }

    fun logout() {
        cancelTransferCompletion()
        cancelWorkproofPdfPolling()
        viewModelScope.launch {
            clearAuthenticatedState(AuthUiState.unauthenticated())
        }
    }

    fun selectAdvanceAmount(amount: Int) {
        _selectedAdvanceAmount.value = amount
        if (_advanceRequestUiState.value.message != null) {
            _advanceRequestUiState.value = AdvanceRequestUiState()
        }
    }

    fun clearAdvanceRequestMessage() {
        if (!_advanceRequestUiState.value.isSubmitting && _advanceRequestUiState.value.message != null) {
            _advanceRequestUiState.value = AdvanceRequestUiState()
        }
    }

    fun openAdvanceRequestDetail(requestId: Long) {
        val cached = _advanceRemoteState.value.requestDetailsById[requestId]
        if (cached != null) {
            _advanceRequestDetailUiState.value = AdvanceRequestDetailUiState(detail = cached)
            return
        }

        val session = _authUiState.value.session ?: run {
            _advanceRequestDetailUiState.value = AdvanceRequestDetailUiState(
                errorMessage = "로그인 후 다시 시도해 주세요."
            )
            return
        }

        viewModelScope.launch {
            _advanceRequestDetailUiState.value = AdvanceRequestDetailUiState(isLoading = true)
            try {
                val detail = advanceRepository.getRequestDetail(session.accessToken, requestId)
                mergeAdvanceRequestDetail(detail)
                _advanceRequestDetailUiState.value = AdvanceRequestDetailUiState(detail = detail)
            } catch (error: AdvanceUnauthorizedException) {
                expireSession(error.message)
                _advanceRequestDetailUiState.value = AdvanceRequestDetailUiState(
                    errorMessage = error.message ?: "세션이 만료되어 다시 로그인해 주세요."
                )
            } catch (error: Exception) {
                _advanceRequestDetailUiState.value = AdvanceRequestDetailUiState(
                    errorMessage = error.message ?: "미리받기 상세를 불러오지 못했어요."
                )
            }
        }
    }

    fun closeAdvanceRequestDetail() {
        _advanceRequestDetailUiState.value = AdvanceRequestDetailUiState()
    }

    fun requestAdvance() {
        val session = _authUiState.value.session ?: run {
            _advanceRequestUiState.value = AdvanceRequestUiState(
                message = "로그인 후 다시 시도해 주세요.",
                isError = true
            )
            return
        }
        val eligibility = _advanceRemoteState.value.eligibility ?: run {
            _advanceRequestUiState.value = AdvanceRequestUiState(
                message = "실연동 한도를 다시 불러온 뒤 시도해 주세요.",
                isError = true
            )
            return
        }
        val requestedAmount = (_selectedAdvanceAmount.value ?: eligibility.availableAmount.toInt())
            .coerceAtMost(eligibility.availableAmount.toInt())
        if (requestedAmount <= 0) {
            _advanceRequestUiState.value = AdvanceRequestUiState(
                message = "신청 가능한 금액이 없어요.",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            _advanceRequestUiState.value = AdvanceRequestUiState(isSubmitting = true)
            try {
                val result = advanceRepository.createRequest(
                    accessToken = session.accessToken,
                    workplaceId = eligibility.workplaceId,
                    requestedAmount = requestedAmount.toLong()
                )
                val detail = advanceRepository.getRequestDetail(session.accessToken, result.requestId)
                loadAdvanceRemoteState(session)
                mergeAdvanceRequestDetail(detail)
                _advanceRequestDetailUiState.value = AdvanceRequestDetailUiState(detail = detail)
                _advanceRequestUiState.value = AdvanceRequestUiState(
                    message = "미리받기 신청이 반영됐어요. ${result.status} · ${result.approvedAmount}원",
                    isError = false
                )
            } catch (error: AdvanceUnauthorizedException) {
                expireSession(error.message)
                _advanceRequestUiState.value = AdvanceRequestUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                _advanceRequestUiState.value = AdvanceRequestUiState(
                    message = error.message ?: "미리받기 신청에 실패했어요. 잠시 후 다시 시도해 주세요.",
                    isError = true
                )
            }
        }
    }

    fun refreshAdvanceRemoteState() {
        val session = _authUiState.value.session
        if (session == null) {
            _advanceRemoteState.value = AdvanceRemoteState.unauthenticated(ADVANCE_REMOTE_LOGIN_MESSAGE)
            return
        }

        viewModelScope.launch {
            loadAdvanceRemoteState(session)
        }
    }

    fun refreshWorkproofRemoteState() {
        val session = _authUiState.value.session
        if (session == null) {
            _workproofRemoteState.value = WorkproofRemoteState.unauthenticated(WORKPROOF_REMOTE_LOGIN_MESSAGE)
            return
        }

        viewModelScope.launch {
            loadWorkproofRemoteState(session)
        }
    }

    private fun restoreAuthSession() {
        viewModelScope.launch {
            val session = authRepository.restore()
            if (session == null) {
                _authUiState.value = AuthUiState.unauthenticated()
                _advanceRemoteState.value = AdvanceRemoteState.unauthenticated(ADVANCE_REMOTE_LOGIN_MESSAGE)
                _workproofRemoteState.value = WorkproofRemoteState.unauthenticated(WORKPROOF_REMOTE_LOGIN_MESSAGE)
            } else {
                onAuthenticated(session)
            }
        }
    }

    private suspend fun onAuthenticated(session: AuthSession) {
        _authUiState.value = AuthUiState.authenticated(session)
        loadAdvanceRemoteState(session)
        if (!_authUiState.value.isAuthenticated) {
            return
        }
        loadWorkproofRemoteState(session)
    }

    private suspend fun loadAdvanceRemoteState(session: AuthSession) {
        _advanceRemoteState.value = AdvanceRemoteState.loading()
        val remoteState = advanceRepository.load(session.accessToken)
        if (!remoteState.isAuthenticated) {
            clearAuthenticatedState(
                AuthUiState.unauthenticated(
                    remoteState.errorMessage ?: "세션이 만료되어 다시 로그인해 주세요."
                )
            )
            return
        }
        _advanceRemoteState.value = remoteState
        syncSelectedAdvanceAmount(remoteState)
    }

    private suspend fun loadWorkproofRemoteState(session: AuthSession) {
        _workproofRemoteState.value = WorkproofRemoteState.loading()
        val remoteState = workproofRepository.load(session.accessToken)
        applyWorkproofRemoteState(remoteState)
    }

    private suspend fun applyWorkproofRemoteState(remoteState: WorkproofRemoteState) {
        if (!remoteState.isAuthenticated) {
            clearAuthenticatedState(
                AuthUiState.unauthenticated(
                    remoteState.errorMessage ?: "세션이 만료되어 다시 로그인해 주세요."
                )
            )
            return
        }

        _workproofRemoteState.value = remoteState
        val payload = remoteState.payload ?: return
        _uiState.update { current -> current.syncRemoteWorkproof(payload) }
    }

    private fun syncSelectedAdvanceAmount(remoteState: AdvanceRemoteState) {
        val availableAmount = remoteState.eligibility?.availableAmount?.toInt() ?: 0
        if (availableAmount <= 0) {
            _selectedAdvanceAmount.value = null
            return
        }
        val current = _selectedAdvanceAmount.value
        if (current != null && current in 1..availableAmount) {
            return
        }
        _selectedAdvanceAmount.value = availableAmount
    }

    private fun mergeAdvanceRequestDetail(detail: AdvanceRequestDetailPayload) {
        _advanceRemoteState.update { current ->
            if (current.mode != AdvanceRemoteMode.CONTENT) {
                return@update current
            }

            val mergedRequests = buildList {
                add(
                    AdvanceRequestItemPayload(
                        requestId = detail.requestId,
                        requestedAmount = detail.requestedAmount,
                        approvedAmount = detail.approvedAmount,
                        status = detail.status,
                        repaymentDueDate = detail.repaymentDueDate
                    )
                )
                addAll(current.requests.filterNot { it.requestId == detail.requestId })
            }.sortedByDescending { it.requestId }

            current.copy(
                requests = mergedRequests,
                requestDetailsById = current.requestDetailsById + (detail.requestId to detail)
            )
        }
    }

    private suspend fun expireSession(message: String?) {
        clearAuthenticatedState(
            AuthUiState.unauthenticated(
                message ?: "세션이 만료되어 다시 로그인해 주세요."
            )
        )
    }

    private suspend fun clearAuthenticatedState(unauthenticatedState: AuthUiState) {
        cancelWorkproofPdfPolling()
        authRepository.logout()
        _uiState.value = initialState
        _authUiState.value = unauthenticatedState
        _advanceRemoteState.value = AdvanceRemoteState.unauthenticated(
            unauthenticatedState.errorMessage ?: ADVANCE_REMOTE_LOGIN_MESSAGE
        )
        _workproofRemoteState.value = WorkproofRemoteState.unauthenticated(
            unauthenticatedState.errorMessage ?: WORKPROOF_REMOTE_LOGIN_MESSAGE
        )
        _selectedAdvanceAmount.value = null
        _advanceRequestUiState.value = AdvanceRequestUiState()
        _advanceRequestDetailUiState.value = AdvanceRequestDetailUiState()
        _workproofActionUiState.value = WorkproofActionUiState()
        _workproofPdfPreviewUiState.value = WorkproofPdfPreviewUiState()
        _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState()
        _workproofPdfFileUiState.value = WorkproofPdfFileUiState()
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

    private fun startWorkproofPdfPolling(
        accessToken: String,
        requestId: String
    ) {
        cancelWorkproofPdfPolling()
        workproofPdfPollingJob = viewModelScope.launch {
            repeat(WORKPROOF_PDF_POLL_MAX_ATTEMPTS) {
                try {
                    val payload = workproofDocumentRepository.getRequestStatus(accessToken, requestId)
                    val isTerminal = payload.status == "READY" || payload.status == "FAILED"
                    _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState(
                        isPolling = !isTerminal,
                        requestId = payload.requestId,
                        documentId = payload.documentId,
                        documentUrl = payload.documentUrl,
                        status = payload.status,
                        pollUrl = payload.pollUrl,
                        errorMessage = if (payload.status == "FAILED") {
                            "근무 기록 문서 생성에 실패했어요."
                        } else {
                            null
                        }
                    )
                    if (isTerminal) {
                        return@launch
                    }
                } catch (error: WorkproofDocumentUnauthorizedException) {
                    expireSession(error.message)
                    _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState(
                        requestId = requestId,
                        errorMessage = error.message ?: "세션이 만료되어 다시 로그인해 주세요."
                    )
                    return@launch
                } catch (_: Exception) {
                    // Keep polling within the retry window before surfacing a delay message.
                }
                delay(WORKPROOF_PDF_POLL_INTERVAL_MS)
            }

            _workproofPdfCreateUiState.update { current ->
                if (current.isReady || current.isFailed) {
                    current
                } else {
                    current.copy(
                        isPolling = false,
                        errorMessage = "문서 생성이 지연되고 있어요. 잠시 후 다시 확인해 주세요."
                    )
                }
            }
        }
    }

    private fun cancelWorkproofPdfPolling() {
        workproofPdfPollingJob?.cancel()
        workproofPdfPollingJob = null
    }

    private fun downloadWorkproofPdf(
        documentId: Long,
        action: WorkproofPdfFileAction
    ) {
        val session = _authUiState.value.session ?: run {
            _workproofPdfFileUiState.value = WorkproofPdfFileUiState(
                errorMessage = "로그인 후 문서를 내려받을 수 있어요."
            )
            return
        }

        viewModelScope.launch {
            _workproofPdfFileUiState.value = WorkproofPdfFileUiState(
                isDownloading = true,
                pendingAction = action
            )
            try {
                val payload = workproofDocumentRepository.download(session.accessToken, documentId)
                val file = writeWorkproofPdfFile(payload.fileName, payload.bytes)
                val uri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    file
                )
                _workproofPdfFileUiState.value = WorkproofPdfFileUiState(
                    pendingAction = action,
                    fileUri = uri.toString(),
                    fileName = payload.fileName
                )
            } catch (error: WorkproofDocumentUnauthorizedException) {
                expireSession(error.message)
                _workproofPdfFileUiState.value = WorkproofPdfFileUiState(
                    errorMessage = error.message ?: "세션이 만료되어 다시 로그인해 주세요."
                )
            } catch (error: Exception) {
                _workproofPdfFileUiState.value = WorkproofPdfFileUiState(
                    errorMessage = error.message ?: "근무 기록 문서를 내려받지 못했어요."
                )
            }
        }
    }

    private fun writeWorkproofPdfFile(fileName: String, bytes: ByteArray): File {
        val documentsDir = File(appContext.cacheDir, "documents").apply { mkdirs() }
        return File(documentsDir, fileName).apply {
            writeBytes(bytes)
        }
    }

    private fun DemoState.syncRemoteWorkproof(payload: WorkproofRemotePayload): DemoState {
        val systemToday = java.time.LocalDate.now()
        val activeRecord = payload.records.firstOrNull { record ->
            record.status == "CHECKED_IN" && record.checkOutDeviceAt == null
        }
        val selectedTodayRecord = activeRecord
            ?: payload.records.firstOrNull { it.workDate == systemToday }
            ?: payload.records.firstOrNull()
        val selectedDate = selectedTodayRecord?.workDate ?: systemToday
        val nextRecords = payload.records
            .sortedByDescending { it.workDate }
            .map { record ->
                WorkRecord(
                    id = record.recordId.toString(),
                    day = record.workDate.dayOfMonth,
                    inTime = record.checkInDeviceAt.toLocalTime().toString().take(5),
                    outTime = record.checkOutDeviceAt?.toLocalTime()?.toString()?.take(5) ?: "-",
                    modified = record.modified,
                    attachments = 0
                )
            }

        return copy(
            demo = demo.copy(
                year = selectedDate.year,
                month = selectedDate.monthValue,
                monthLength = selectedDate.lengthOfMonth(),
                asOfDay = selectedDate.dayOfMonth
            ),
            workproof = workproof.copy(
                workplaceName = payload.workplace.name,
                workplaceAddress = payload.workplace.address,
                workplaceLatitude = payload.workplace.latitude,
                workplaceLongitude = payload.workplace.longitude,
                today = TodayWork(
                    clockIn = selectedTodayRecord?.checkInDeviceAt?.toLocalTime()?.toString()?.take(5),
                    clockOut = selectedTodayRecord?.checkOutDeviceAt?.toLocalTime()?.toString()?.take(5)
                ),
                records = nextRecords,
                audit = emptyList(),
                workplaceId = payload.workplace.workplaceId,
                allowedRadiusMeters = payload.workplace.allowedRadiusMeters
            )
        )
    }

    private fun WorkproofDocumentPreviewPayload.toUiModel(): WorkproofPdfPreviewUiModel {
        return WorkproofPdfPreviewUiModel(
            workplaceName = workplaceName,
            periodText = "${startDate.format(WorkproofPdfPreviewDateFormatter)} - ${endDate.format(WorkproofPdfPreviewDateFormatter)}",
            totalRecordCountText = "${totalRecordCount}건",
            editedCountText = "${editedCount}건",
            attachmentCountText = "${attachmentCount}건",
            totalWorkedHoursText = totalWorkedHoursText,
            sectionSummaryText = "출퇴근 기록, 수정 이력, 기간 요약"
        )
    }
}

private val WorkproofPdfPreviewDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
