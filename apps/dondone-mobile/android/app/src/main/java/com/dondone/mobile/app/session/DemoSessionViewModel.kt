package com.dondone.mobile.app.session

import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dondone.mobile.core.ui.phoneDigits
import com.dondone.mobile.data.auth.AuthUnauthorizedException
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
import com.dondone.mobile.data.remittance.BackendRemittanceRepository
import com.dondone.mobile.data.remittance.RemittanceRemoteMode
import com.dondone.mobile.data.remittance.RemittanceRemotePayload
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.data.remittance.RemittanceRepository
import com.dondone.mobile.data.remittance.RemittanceTransferDetailPayload
import com.dondone.mobile.data.remittance.RemittanceUnauthorizedException
import com.dondone.mobile.data.wage.BackendWageRepository
import com.dondone.mobile.data.wage.WageRemotePayload
import com.dondone.mobile.data.wage.WageRemoteState
import com.dondone.mobile.data.wage.WageRepository
import com.dondone.mobile.data.wage.WageUnauthorizedException
import com.dondone.mobile.data.workproof.BackendWorkproofRepository
import com.dondone.mobile.data.workproof.WorkproofRemotePayload
import com.dondone.mobile.data.workproof.WorkproofRemoteMode
import com.dondone.mobile.data.workproof.WorkproofRemoteState
import com.dondone.mobile.data.workproof.WorkproofRepository
import com.dondone.mobile.data.workproof.WorkproofUnauthorizedException
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.Recipient
import com.dondone.mobile.domain.model.TodayWork
import com.dondone.mobile.domain.model.TransactionCategory
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfCreateUiState
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfFileAction
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfFileUiState
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfPreviewUiModel
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfPreviewUiState
import com.dondone.mobile.domain.model.WorkRecord
import com.dondone.mobile.domain.model.remittanceRelationCodeToLabel
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private const val TRANSFER_CONFIRMATION_DELAY_MS = 1800L
private const val REMITTANCE_STATUS_POLL_DELAY_MS = 1500L
private const val REMITTANCE_STATUS_POLL_ATTEMPTS = 120
private const val REMITTANCE_REMOTE_LOGIN_MESSAGE = "로그인 후 송금 실연동 데이터를 불러옵니다."
private const val ADVANCE_REMOTE_LOGIN_MESSAGE = "로그인 후 실연동 데이터를 불러옵니다."
private const val WORKPROOF_REMOTE_LOGIN_MESSAGE = "로그인 후 출퇴근 실연동을 불러옵니다."
private const val WAGE_REMOTE_LOGIN_MESSAGE = "로그인 후 급여 실연동 데이터를 불러옵니다."
private const val ATOMIC_UNITS_PER_USDC = 1_000_000L
private const val DOCUMENT_STATUS_READY = "READY"
private const val DOCUMENT_STATUS_NOT_CREATED = "NOT_CREATED"
private const val DOCUMENT_ID_PROOF = "PROOF"
private const val DOCUMENT_ID_CLAIM = "CLAIM"

class DemoSessionViewModel(
    private val appContext: Context,
    private val authRepository: AuthRepository,
    private val advanceRepository: AdvanceRepository = BackendAdvanceRepository(),
    private val workproofRepository: WorkproofRepository = BackendWorkproofRepository(),
    private val workproofDocumentRepository: WorkproofDocumentRepository = BackendWorkproofDocumentRepository(),
    private val remittanceRepository: RemittanceRepository = BackendRemittanceRepository(),
    private val wageRepository: WageRepository = BackendWageRepository()
) : ViewModel() {
    private val initialState = DemoSeedFactory.create()
    private var transferCompletionJob: Job? = null
    private var remittanceStatusPollingJob: Job? = null
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<DemoState> = _uiState.asStateFlow()
    private val _authUiState = MutableStateFlow(AuthUiState.restoring())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()
    private val _profileUpdateUiState = MutableStateFlow(ProfileUpdateUiState())
    val profileUpdateUiState: StateFlow<ProfileUpdateUiState> = _profileUpdateUiState.asStateFlow()
    private val _companyCodeUpdateUiState = MutableStateFlow(CompanyCodeUpdateUiState())
    val companyCodeUpdateUiState: StateFlow<CompanyCodeUpdateUiState> = _companyCodeUpdateUiState.asStateFlow()
    private val _recipientPhoneSearchUiState = MutableStateFlow(RecipientPhoneSearchUiState())
    val recipientPhoneSearchUiState: StateFlow<RecipientPhoneSearchUiState> = _recipientPhoneSearchUiState.asStateFlow()
    private val _advanceRemoteState =
        MutableStateFlow(AdvanceRemoteState.unauthenticated(ADVANCE_REMOTE_LOGIN_MESSAGE))
    val advanceRemoteState: StateFlow<AdvanceRemoteState> = _advanceRemoteState.asStateFlow()
    private val _workproofRemoteState =
        MutableStateFlow(WorkproofRemoteState.unauthenticated(WORKPROOF_REMOTE_LOGIN_MESSAGE))
    private val _wageRemoteState =
        MutableStateFlow(WageRemoteState.unauthenticated(WAGE_REMOTE_LOGIN_MESSAGE))
    private val _remittanceRemoteState =
        MutableStateFlow(RemittanceRemoteState.unauthenticated(REMITTANCE_REMOTE_LOGIN_MESSAGE))
    val wageRemoteState: StateFlow<WageRemoteState> = _wageRemoteState.asStateFlow()
    val remittanceRemoteState: StateFlow<RemittanceRemoteState> = _remittanceRemoteState.asStateFlow()
    private val _workproofActionUiState = MutableStateFlow(WorkproofActionUiState())
    val workproofActionUiState: StateFlow<WorkproofActionUiState> = _workproofActionUiState.asStateFlow()
    private val _workproofPdfPreviewUiState = MutableStateFlow(WorkproofPdfPreviewUiState())
    val workproofPdfPreviewUiState: StateFlow<WorkproofPdfPreviewUiState> = _workproofPdfPreviewUiState.asStateFlow()
    private val _workproofPdfCreateUiState = MutableStateFlow(WorkproofPdfCreateUiState())
    val workproofPdfCreateUiState: StateFlow<WorkproofPdfCreateUiState> = _workproofPdfCreateUiState.asStateFlow()
    private val _workproofPdfFileUiState = MutableStateFlow(WorkproofPdfFileUiState())
    val workproofPdfFileUiState: StateFlow<WorkproofPdfFileUiState> = _workproofPdfFileUiState.asStateFlow()
    private val _wageActionUiState = MutableStateFlow(WageActionUiState())
    val wageActionUiState: StateFlow<WageActionUiState> = _wageActionUiState.asStateFlow()
    private val _remittanceActionUiState = MutableStateFlow(RemittanceActionUiState())
    val remittanceActionUiState: StateFlow<RemittanceActionUiState> = _remittanceActionUiState.asStateFlow()
    private val _transactionMetadataOverrides = MutableStateFlow<Map<String, TransactionMetadataOverride>>(emptyMap())
    val transactionMetadataOverrides: StateFlow<Map<String, TransactionMetadataOverride>> =
        _transactionMetadataOverrides.asStateFlow()
    private val _selectedAdvanceAmount = MutableStateFlow<Int?>(null)
    val selectedAdvanceAmount: StateFlow<Int?> = _selectedAdvanceAmount.asStateFlow()
    private val _menuLaunchRequest = MutableStateFlow<MenuLaunchRequest?>(null)
    val menuLaunchRequest: StateFlow<MenuLaunchRequest?> = _menuLaunchRequest.asStateFlow()
    private val _advanceRequestUiState = MutableStateFlow(AdvanceRequestUiState())
    val advanceRequestUiState: StateFlow<AdvanceRequestUiState> = _advanceRequestUiState.asStateFlow()
    private val _advanceRequestDetailUiState = MutableStateFlow(AdvanceRequestDetailUiState())
    val advanceRequestDetailUiState: StateFlow<AdvanceRequestDetailUiState> = _advanceRequestDetailUiState.asStateFlow()
    private var nextMenuLaunchRequestId = 1L

    init {
        restoreAuthSession()
    }

    fun shiftAsOfDay(delta: Int) {
        _uiState.update { state -> DemoSessionReducer.shiftAsOfDay(state, delta) }
        if (_authUiState.value.isAuthenticated) {
            refreshWageRemoteState()
        }
    }

    fun selectAccount(accountId: String) {
        if (accountId == "remote-wallet") {
            return
        }
        _uiState.update { state -> DemoSessionReducer.selectAccount(state, accountId) }
    }

    fun openTransferFlow() {
        cancelTransferCompletion()
        _uiState.update { state -> DemoSessionReducer.openTransferFlow(state) }
        val session = _authUiState.value.session ?: return
        viewModelScope.launch {
            loadRemittanceRemoteState(session)
        }
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

    fun updateTransactionMetadata(
        transactionId: String,
        category: TransactionCategory,
        memo: String
    ) {
        _transactionMetadataOverrides.update { current ->
            current + (transactionId to TransactionMetadataOverride(category = category, memo = memo.trim()))
        }
    }

    fun createRemittanceRecipient(
        alias: String,
        relation: String,
        walletAddress: String
    ) {
        val session = _authUiState.value.session ?: run {
            _remittanceActionUiState.value = RemittanceActionUiState(
                message = "로그인 후 다시 시도해 주세요.",
                isError = true
            )
            return
        }

        submitRemoteRemittanceRecipientCreation(
            session = session,
            alias = alias,
            relation = relation,
            walletAddress = walletAddress
        )
    }

    fun addRecipientFromAccountManage(
        alias: String,
        relation: String,
        walletAddress: String,
        targetUserId: Long?
    ) {
        if (targetUserId == null && hasRecipientWallet(walletAddress)) {
            _remittanceActionUiState.value = RemittanceActionUiState(
                message = "이미 등록된 지갑이에요.",
                isError = true
            )
            return
        }

        val session = _authUiState.value.session
        if (session == null) {
            _uiState.update { state ->
                DemoSessionReducer.addRecipient(
                    state = state,
                    alias = alias,
                    relation = remittanceRelationCodeToLabel(relation),
                    walletAddress = walletAddress
                )
            }
            _remittanceActionUiState.value = RemittanceActionUiState(
                message = "수신 지갑을 추가했어요."
            )
            return
        }

        submitRemoteRemittanceRecipientCreation(
            session = session,
            alias = alias,
            relation = relation,
            walletAddress = walletAddress,
            targetUserId = targetUserId
        )
    }

    fun updateRecipientFromAccountManage(
        recipientId: String,
        alias: String,
        relation: String,
        walletAddress: String
    ) {
        if (hasOtherRecipientWallet(recipientId, walletAddress)) {
            _remittanceActionUiState.value = RemittanceActionUiState(
                message = "이미 등록된 지갑이에요.",
                isError = true
            )
            return
        }

        val session = _authUiState.value.session
        if (session == null) {
            _uiState.update { state ->
                DemoSessionReducer.updateRecipient(
                    state = state,
                    recipientId = recipientId,
                    alias = alias,
                    relation = remittanceRelationCodeToLabel(relation),
                    walletAddress = walletAddress
                )
            }
            _remittanceActionUiState.value = RemittanceActionUiState(
                message = "수신 지갑을 수정했어요."
            )
            return
        }

        submitRemoteRemittanceRecipientUpdate(
            session = session,
            recipientId = recipientId,
            alias = alias,
            relation = relation,
            walletAddress = walletAddress
        )
    }

    fun searchRecipientsByPhone(phoneNumber: String) {
        val session = _authUiState.value.session ?: run {
            _recipientPhoneSearchUiState.value = RecipientPhoneSearchUiState(
                errorMessage = "로그인 후 다시 시도해 주세요."
            )
            return
        }
        val normalizedPhoneNumber = phoneNumber.phoneDigits()
        if (normalizedPhoneNumber.length !in 10..11) {
            _recipientPhoneSearchUiState.value = RecipientPhoneSearchUiState(
                errorMessage = "휴대폰 번호를 다시 확인해 주세요."
            )
            return
        }

        viewModelScope.launch {
            _recipientPhoneSearchUiState.value = RecipientPhoneSearchUiState(isLoading = true)
            try {
                val results = remittanceRepository.searchRecipientsByPhone(
                    accessToken = session.accessToken,
                    phoneNumber = normalizedPhoneNumber
                )
                _recipientPhoneSearchUiState.value = RecipientPhoneSearchUiState(results = results)
            } catch (error: RemittanceUnauthorizedException) {
                expireSession(error.message)
                _recipientPhoneSearchUiState.value = RecipientPhoneSearchUiState(
                    errorMessage = error.message ?: "세션이 만료되어 다시 로그인해 주세요."
                )
            } catch (error: Exception) {
                _recipientPhoneSearchUiState.value = RecipientPhoneSearchUiState(
                    errorMessage = error.message ?: "휴대폰 번호 검색에 실패했어요."
                )
            }
        }
    }

    fun clearRecipientPhoneSearch() {
        _recipientPhoneSearchUiState.value = RecipientPhoneSearchUiState()
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
        if (_remittanceActionUiState.value.isSubmitting) {
            return
        }
        val session = _authUiState.value.session
        if (session == null) {
            _uiState.update { state -> DemoSessionReducer.submitTransfer(state) }
            return
        }

        val remittance = _uiState.value.remittance
        val selectedRecipient = remittance.selectedRecipientOrNull()
        if (selectedRecipient == null) {
            _remittanceActionUiState.value = RemittanceActionUiState(
                message = "송금할 수신자를 먼저 등록해 주세요.",
                isError = true
            )
            return
        }

        val amountAtomic = remittance.draftAmountUsd.toLong() * ATOMIC_UNITS_PER_USDC
        if (amountAtomic <= 0L) {
            _remittanceActionUiState.value = RemittanceActionUiState(
                message = "송금 금액을 입력해 주세요.",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            _remittanceActionUiState.value = RemittanceActionUiState(
                isSubmitting = true,
                submittingAction = RemittanceSubmittingAction.TRANSFER_PRECHECK
            )
            try {
                val precheck = remittanceRepository.precheck(
                    accessToken = session.accessToken,
                    recipientId = selectedRecipient.id,
                    amountAtomic = amountAtomic,
                    highAmountConfirmed = false,
                    recentRecipientConfirmed = false
                )
                if (!precheck.allowed && !precheck.isConfirmable()) {
                    _remittanceActionUiState.value = RemittanceActionUiState(
                        message = precheck.resolveBlockedMessage(),
                        isError = true
                    )
                    return@launch
                }

                _uiState.update { state -> DemoSessionReducer.submitTransfer(state) }
                _remittanceActionUiState.value = RemittanceActionUiState(precheck = precheck)
            } catch (error: RemittanceUnauthorizedException) {
                expireSession(error.message)
                _remittanceActionUiState.value = RemittanceActionUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                _remittanceActionUiState.value = RemittanceActionUiState(
                    message = error.message ?: "송금 가능 여부를 확인하지 못했어요.",
                    isError = true
                )
            }
        }
    }

    fun dismissTransferConfirmation() {
        _uiState.update { state -> DemoSessionReducer.dismissTransferConfirmation(state) }
        _remittanceActionUiState.value = RemittanceActionUiState()
    }

    fun confirmTransfer() {
        if (_remittanceActionUiState.value.isSubmitting) {
            return
        }
        val session = _authUiState.value.session
        if (session == null) {
            _uiState.update { state -> DemoSessionReducer.confirmTransfer(state) }
            scheduleTransferCompletion()
            return
        }

        val remittance = _uiState.value.remittance
        val selectedRecipient = remittance.selectedRecipientOrNull() ?: run {
            _remittanceActionUiState.value = RemittanceActionUiState(
                message = "송금할 수신자를 먼저 선택해 주세요.",
                isError = true
            )
            return
        }
        val amountAtomic = remittance.draftAmountUsd.toLong() * ATOMIC_UNITS_PER_USDC
        val precheck = _remittanceActionUiState.value.precheck

        viewModelScope.launch {
            _remittanceActionUiState.value = RemittanceActionUiState(
                isSubmitting = true,
                submittingAction = RemittanceSubmittingAction.TRANSFER_CREATE,
                precheck = precheck
            )
            try {
                val result = remittanceRepository.createTransfer(
                    accessToken = session.accessToken,
                    recipientId = selectedRecipient.id,
                    amountAtomic = amountAtomic,
                    highAmountConfirmed = precheck.requiresHighAmountConfirmation(),
                    recentRecipientConfirmed = precheck?.recentRecipientConfirmationRequired == true
                )
                _uiState.update { state -> DemoSessionReducer.confirmTransfer(state) }
                refreshRemittanceRemoteStateSilently(session)
                _remittanceActionUiState.value = RemittanceActionUiState()
                startRemittanceStatusPolling(session, result.transferId)
            } catch (error: RemittanceUnauthorizedException) {
                expireSession(error.message)
                _remittanceActionUiState.value = RemittanceActionUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                _remittanceActionUiState.value = RemittanceActionUiState(
                    message = error.message ?: "송금 요청을 보내지 못했어요.",
                    isError = true,
                    precheck = precheck
                )
            }
        }
    }

    fun resetTransfer() {
        cancelTransferCompletion()
        cancelRemittanceStatusPolling()
        _remittanceActionUiState.value = RemittanceActionUiState()
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

    fun submitWageDeposit(amount: Int) {
        val session = _authUiState.value.session ?: run {
            _wageActionUiState.value = WageActionUiState(
                message = "로그인 후 다시 시도해 주세요.",
                isError = true
            )
            return
        }
        if (amount <= 0) {
            _wageActionUiState.value = WageActionUiState(
                message = "입금 금액을 입력해 주세요.",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            _wageActionUiState.value = WageActionUiState(isSubmittingDeposit = true)
            try {
                wageRepository.createDeposit(
                    accessToken = session.accessToken,
                    month = currentWageMonth(),
                    depositDate = currentWageAsOfDate(),
                    actualDepositAmount = amount.toLong(),
                    deductionsKnown = _uiState.value.wage.deductionsKnown,
                    note = null
                )
                loadWageRemoteState(session)
                _wageActionUiState.value = WageActionUiState(message = "실제 입금액을 기록했어요.")
            } catch (error: WageUnauthorizedException) {
                expireSession(error.message)
                _wageActionUiState.value = WageActionUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                _wageActionUiState.value = WageActionUiState(
                    message = error.message ?: "실제 입금액을 저장하지 못했어요.",
                    isError = true
                )
            }
        }
    }

    fun createWageVerification() {
        val session = _authUiState.value.session ?: run {
            _wageActionUiState.value = WageActionUiState(
                message = "로그인 후 다시 시도해 주세요.",
                isError = true
            )
            return
        }
        val payload = _wageRemoteState.value.payload
        val workplaceId = payload?.workplaceId ?: _uiState.value.workproof.workplaceId ?: run {
            _wageActionUiState.value = WageActionUiState(
                message = "근무지 정보를 다시 불러와 주세요.",
                isError = true
            )
            return
        }
        val actualDepositAmount =
            payload?.summary?.actualDepositAmount ?: _uiState.value.wage.actualDeposit.toLong()
        if (actualDepositAmount <= 0L) {
            _wageActionUiState.value = WageActionUiState(
                message = "실제 입금액을 먼저 입력해 주세요.",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            _wageActionUiState.value = WageActionUiState(isSubmittingVerification = true)
            try {
                val created = wageRepository.createVerification(
                    accessToken = session.accessToken,
                    month = currentWageMonth(),
                    workplaceId = workplaceId,
                    actualDepositAmount = actualDepositAmount,
                    deductionsKnown = payload?.summary?.deductionsKnown ?: _uiState.value.wage.deductionsKnown,
                    memo = null
                )
                loadWageRemoteState(session, created.verificationId)
                _wageActionUiState.value = WageActionUiState(message = "급여 확인 결과를 생성했어요.")
            } catch (error: WageUnauthorizedException) {
                expireSession(error.message)
                _wageActionUiState.value = WageActionUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                _wageActionUiState.value = WageActionUiState(
                    message = error.message ?: "급여 확인 결과를 생성하지 못했어요.",
                    isError = true
                )
            }
        }
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
        cancelRemittanceStatusPolling()
        _uiState.value = initialState
        _advanceRequestUiState.value = AdvanceRequestUiState()
        _advanceRequestDetailUiState.value = AdvanceRequestDetailUiState()
        _workproofActionUiState.value = WorkproofActionUiState()
        _workproofPdfPreviewUiState.value = WorkproofPdfPreviewUiState()
        _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState()
        _workproofPdfFileUiState.value = WorkproofPdfFileUiState()
        _wageActionUiState.value = WageActionUiState()
        _remittanceActionUiState.value = RemittanceActionUiState()
        _menuLaunchRequest.value = null
        refreshAdvanceRemoteState()
        refreshWorkproofRemoteState()
        refreshWageRemoteState()
        refreshRemittanceRemoteState()
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
                    documentId = payload.documentId,
                    documentUrl = payload.documentUrl,
                    status = payload.status,
                    pollUrl = payload.pollUrl
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

    fun clearWageActionMessage() {
        if (!_wageActionUiState.value.isSubmitting && _wageActionUiState.value.message != null) {
            _wageActionUiState.value = WageActionUiState()
        }
    }

    fun openMenuForWageDocuments() {
        val relatedActions = _wageRemoteState.value.payload?.latestVerification?.relatedActions
        val target = when {
            relatedActions?.claimKitDocumentId != null -> MenuLaunchTarget.CLAIM_DOCUMENT
            relatedActions?.proofPackDocumentId != null -> MenuLaunchTarget.PROOF_DOCUMENT
            relatedActions != null -> MenuLaunchTarget.CLAIM_SHEET
            else -> null
        }
        _menuLaunchRequest.value = target?.let {
            MenuLaunchRequest(
                target = it,
                requestId = nextMenuLaunchRequestId++
            )
        }
    }

    fun consumeMenuLaunchRequest() {
        _menuLaunchRequest.value = null
    }

    fun clearRemittanceActionMessage() {
        if (!_remittanceActionUiState.value.isSubmitting && _remittanceActionUiState.value.message != null) {
            _remittanceActionUiState.value = _remittanceActionUiState.value.copy(
                message = null,
                isError = false
            )
        }
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
                _wageRemoteState.value = WageRemoteState.unauthenticated(WAGE_REMOTE_LOGIN_MESSAGE)
                _remittanceRemoteState.value = RemittanceRemoteState.unauthenticated(REMITTANCE_REMOTE_LOGIN_MESSAGE)
            }
        }
    }

    fun signup(name: String, email: String, password: String, phoneNumber: String) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val normalizedPhoneNumber = phoneNumber.phoneDigits()
        if (trimmedName.isBlank() || trimmedEmail.isBlank() || password.isBlank() || normalizedPhoneNumber.isBlank()) {
            _authUiState.value = AuthUiState.unauthenticated("이름, 이메일, 비밀번호, 전화번호를 모두 입력해 주세요.")
            return
        }
        if (password.length < 8) {
            _authUiState.value = AuthUiState.unauthenticated("비밀번호는 8자 이상이어야 해요.")
            return
        }
        if (normalizedPhoneNumber.length !in 10..11) {
            _authUiState.value = AuthUiState.unauthenticated("전화번호를 다시 확인해 주세요.")
            return
        }

        viewModelScope.launch {
            _authUiState.value = AuthUiState.submitting()
            try {
                val session = authRepository.signup(
                    name = trimmedName,
                    email = trimmedEmail,
                    password = password,
                    phoneNumber = normalizedPhoneNumber
                )
                onAuthenticated(session)
            } catch (error: Exception) {
                _authUiState.value = AuthUiState.unauthenticated(
                    error.message ?: "회원가입에 실패했어요. 잠시 후 다시 시도해 주세요."
                )
                _advanceRemoteState.value = AdvanceRemoteState.unauthenticated(ADVANCE_REMOTE_LOGIN_MESSAGE)
                _workproofRemoteState.value = WorkproofRemoteState.unauthenticated(WORKPROOF_REMOTE_LOGIN_MESSAGE)
                _wageRemoteState.value = WageRemoteState.unauthenticated(WAGE_REMOTE_LOGIN_MESSAGE)
                _remittanceRemoteState.value = RemittanceRemoteState.unauthenticated(REMITTANCE_REMOTE_LOGIN_MESSAGE)
            }
        }
    }

    fun updateProfile(name: String, phoneNumber: String) {
        val session = _authUiState.value.session ?: run {
            _profileUpdateUiState.value = ProfileUpdateUiState(
                message = "로그인 후 다시 시도해 주세요.",
                isError = true
            )
            return
        }
        val trimmedName = name.trim()
        val normalizedPhoneNumber = phoneNumber.phoneDigits()
        if (trimmedName.isBlank() || normalizedPhoneNumber.isBlank()) {
            _profileUpdateUiState.value = ProfileUpdateUiState(
                message = "이름과 전화번호를 모두 입력해 주세요.",
                isError = true
            )
            return
        }
        if (normalizedPhoneNumber.length !in 10..11) {
            _profileUpdateUiState.value = ProfileUpdateUiState(
                message = "전화번호를 다시 확인해 주세요.",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            _profileUpdateUiState.value = ProfileUpdateUiState(isSubmitting = true)
            try {
                val updatedSession = authRepository.updateProfile(
                    session = session,
                    name = trimmedName,
                    phoneNumber = normalizedPhoneNumber
                )
                _authUiState.value = AuthUiState.authenticated(updatedSession)
                _profileUpdateUiState.value = ProfileUpdateUiState(message = "내 정보를 수정했어요.")
            } catch (error: AuthUnauthorizedException) {
                expireSession(error.message)
                _profileUpdateUiState.value = ProfileUpdateUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                _profileUpdateUiState.value = ProfileUpdateUiState(
                    message = error.message ?: "내 정보를 수정하지 못했어요.",
                    isError = true
                )
            }
        }
    }

    fun updateCompanyCode(companyCode: String) {
        val session = _authUiState.value.session ?: run {
            _companyCodeUpdateUiState.value = CompanyCodeUpdateUiState(
                message = "로그인 후 다시 시도해 주세요.",
                isError = true
            )
            return
        }
        val normalizedCompanyCode = companyCode.trim().uppercase()
        if (normalizedCompanyCode.isBlank()) {
            _companyCodeUpdateUiState.value = CompanyCodeUpdateUiState(
                message = "회사코드를 입력해 주세요.",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            _companyCodeUpdateUiState.value = CompanyCodeUpdateUiState(isSubmitting = true)
            try {
                val updatedSession = authRepository.updateCompanyCode(
                    session = session,
                    companyCode = normalizedCompanyCode
                )
                _authUiState.value = AuthUiState.authenticated(updatedSession)
                _companyCodeUpdateUiState.value = CompanyCodeUpdateUiState(message = "회사코드를 저장했어요.")
            } catch (error: AuthUnauthorizedException) {
                expireSession(error.message)
                _companyCodeUpdateUiState.value = CompanyCodeUpdateUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                _companyCodeUpdateUiState.value = CompanyCodeUpdateUiState(
                    message = error.message ?: "회사코드를 저장하지 못했어요.",
                    isError = true
                )
            }
        }
    }

    fun clearCompanyCodeUpdateMessage() {
        if (!_companyCodeUpdateUiState.value.isSubmitting && _companyCodeUpdateUiState.value.message != null) {
            _companyCodeUpdateUiState.value = CompanyCodeUpdateUiState()
        }
    }

    fun clearProfileUpdateMessage() {
        if (!_profileUpdateUiState.value.isSubmitting && _profileUpdateUiState.value.message != null) {
            _profileUpdateUiState.value = ProfileUpdateUiState()
        }
    }

    fun clearAuthError() {
        if (_authUiState.value.errorMessage != null && !_authUiState.value.isSubmitting) {
            _authUiState.value = _authUiState.value.copy(errorMessage = null)
        }
    }

    fun logout() {
        cancelTransferCompletion()
        cancelRemittanceStatusPolling()
        _transactionMetadataOverrides.value = emptyMap()
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

    fun refreshWageRemoteState() {
        val session = _authUiState.value.session
        if (session == null) {
            _wageRemoteState.value = WageRemoteState.unauthenticated(WAGE_REMOTE_LOGIN_MESSAGE)
            return
        }

        viewModelScope.launch {
            loadWageRemoteState(session)
        }
    }

    fun refreshRemittanceRemoteState() {
        val session = _authUiState.value.session
        if (session == null) {
            _remittanceRemoteState.value = RemittanceRemoteState.unauthenticated(REMITTANCE_REMOTE_LOGIN_MESSAGE)
            return
        }

        viewModelScope.launch {
            loadRemittanceRemoteState(session)
        }
    }

    private fun restoreAuthSession() {
        viewModelScope.launch {
            val session = authRepository.restore()
            if (session == null) {
                _authUiState.value = AuthUiState.unauthenticated()
                _advanceRemoteState.value = AdvanceRemoteState.unauthenticated(ADVANCE_REMOTE_LOGIN_MESSAGE)
                _workproofRemoteState.value = WorkproofRemoteState.unauthenticated(WORKPROOF_REMOTE_LOGIN_MESSAGE)
                _wageRemoteState.value = WageRemoteState.unauthenticated(WAGE_REMOTE_LOGIN_MESSAGE)
                _remittanceRemoteState.value = RemittanceRemoteState.unauthenticated(REMITTANCE_REMOTE_LOGIN_MESSAGE)
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
        if (!_authUiState.value.isAuthenticated) {
            return
        }
        loadWageRemoteState(session)
        if (!_authUiState.value.isAuthenticated) {
            return
        }
        loadRemittanceRemoteState(session)
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

    private suspend fun loadWageRemoteState(session: AuthSession, verificationId: Long? = null) {
        _wageRemoteState.value = WageRemoteState.loading()
        val remoteState = wageRepository.load(
            accessToken = session.accessToken,
            month = currentWageMonth(),
            asOf = currentWageAsOfDate(),
            paydayDay = _uiState.value.wage.paydayDay
        )
        applyWageRemoteState(remoteState, verificationId)
    }

    private suspend fun loadRemittanceRemoteState(
        session: AuthSession
    ) {
        updateRemittanceRemoteState(
            session = session,
            showLoading = true,
            keepVisibleContentOnError = false
        )
    }

    private suspend fun refreshRemittanceRemoteStateSilently(
        session: AuthSession
    ) {
        updateRemittanceRemoteState(
            session = session,
            showLoading = false,
            keepVisibleContentOnError = true
        )
    }

    private suspend fun updateRemittanceRemoteState(
        session: AuthSession,
        showLoading: Boolean,
        keepVisibleContentOnError: Boolean
    ) {
        if (showLoading || _remittanceRemoteState.value.payload == null) {
            _remittanceRemoteState.value = RemittanceRemoteState.loading()
        }
        val remoteState = remittanceRepository.load(session.accessToken)
        if (
            keepVisibleContentOnError &&
            remoteState.mode == RemittanceRemoteMode.ERROR &&
            _remittanceRemoteState.value.payload != null
        ) {
            return
        }
        applyRemittanceRemoteState(remoteState)
        syncRemittanceStatusPolling(session, remoteState)
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

    private suspend fun applyWageRemoteState(
        remoteState: WageRemoteState,
        verificationId: Long? = null
    ) {
        if (!remoteState.isAuthenticated) {
            clearAuthenticatedState(
                AuthUiState.unauthenticated(
                    remoteState.errorMessage ?: "세션이 만료되어 다시 로그인해 주세요."
                )
            )
            return
        }

        val session = _authUiState.value.session
        val latestVerification = if (
            verificationId != null &&
            remoteState.payload != null &&
            session != null
        ) {
            try {
                wageRepository.getVerificationDetail(session.accessToken, verificationId)
            } catch (error: WageUnauthorizedException) {
                clearAuthenticatedState(
                    AuthUiState.unauthenticated(
                        error.message ?: "세션이 만료되어 다시 로그인해 주세요."
                    )
                )
                return
            } catch (_: Exception) {
                remoteState.payload.latestVerification
            }
        } else {
            remoteState.payload?.latestVerification
        }

        val nextState = if (remoteState.payload != null && latestVerification != null) {
            remoteState.copy(
                payload = remoteState.payload.copy(latestVerification = latestVerification)
            )
        } else {
            remoteState
        }

        _wageRemoteState.value = nextState
        val payload = nextState.payload ?: return
        _uiState.update { current -> current.syncRemoteWage(payload) }
    }

    private suspend fun applyRemittanceRemoteState(remoteState: RemittanceRemoteState) {
        if (!remoteState.isAuthenticated) {
            clearAuthenticatedState(
                AuthUiState.unauthenticated(
                    remoteState.errorMessage ?: "세션이 만료되어 다시 로그인해 주세요."
                )
            )
            return
        }

        _remittanceRemoteState.value = remoteState
        val payload = remoteState.payload ?: return
        _uiState.update { current -> current.syncRemoteRemittance(payload) }
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
        cancelRemittanceStatusPolling()
        authRepository.logout()
        _uiState.value = initialState
        _authUiState.value = unauthenticatedState
        _advanceRemoteState.value = AdvanceRemoteState.unauthenticated(
            unauthenticatedState.errorMessage ?: ADVANCE_REMOTE_LOGIN_MESSAGE
        )
        _workproofRemoteState.value = WorkproofRemoteState.unauthenticated(
            unauthenticatedState.errorMessage ?: WORKPROOF_REMOTE_LOGIN_MESSAGE
        )
        _wageRemoteState.value = WageRemoteState.unauthenticated(
            unauthenticatedState.errorMessage ?: WAGE_REMOTE_LOGIN_MESSAGE
        )
        _remittanceRemoteState.value = RemittanceRemoteState.unauthenticated(
            unauthenticatedState.errorMessage ?: REMITTANCE_REMOTE_LOGIN_MESSAGE
        )
        _selectedAdvanceAmount.value = null
        _advanceRequestUiState.value = AdvanceRequestUiState()
        _advanceRequestDetailUiState.value = AdvanceRequestDetailUiState()
        _workproofActionUiState.value = WorkproofActionUiState()
        _workproofPdfPreviewUiState.value = WorkproofPdfPreviewUiState()
        _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState()
        _workproofPdfFileUiState.value = WorkproofPdfFileUiState()
        _wageActionUiState.value = WageActionUiState()
        _remittanceActionUiState.value = RemittanceActionUiState()
        _profileUpdateUiState.value = ProfileUpdateUiState()
        _companyCodeUpdateUiState.value = CompanyCodeUpdateUiState()
        _recipientPhoneSearchUiState.value = RecipientPhoneSearchUiState()
        _menuLaunchRequest.value = null
    }

    private fun submitRemoteRemittanceRecipientCreation(
        session: AuthSession,
        alias: String,
        relation: String,
        walletAddress: String,
        targetUserId: Long? = null
    ) {
        viewModelScope.launch {
            _remittanceActionUiState.value = RemittanceActionUiState(
                isSubmitting = true,
                submittingAction = RemittanceSubmittingAction.RECIPIENT_CREATE
            )
            try {
                val created = remittanceRepository.createRecipient(
                    accessToken = session.accessToken,
                    alias = alias,
                    relation = relation,
                    walletAddress = walletAddress,
                    targetUserId = targetUserId
                )
                loadRemittanceRemoteState(session)
                _uiState.update { state -> DemoSessionReducer.selectRecipient(state, created.recipientId) }
                _remittanceActionUiState.value = RemittanceActionUiState(
                    message = "수신 지갑을 추가했어요."
                )
            } catch (error: RemittanceUnauthorizedException) {
                expireSession(error.message)
                _remittanceActionUiState.value = RemittanceActionUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                _remittanceActionUiState.value = RemittanceActionUiState(
                    message = error.message ?: "수신 지갑을 추가하지 못했어요.",
                    isError = true
                )
            }
        }
    }

    private fun submitRemoteRemittanceRecipientUpdate(
        session: AuthSession,
        recipientId: String,
        alias: String,
        relation: String,
        walletAddress: String
    ) {
        viewModelScope.launch {
            _remittanceActionUiState.value = RemittanceActionUiState(
                isSubmitting = true,
                submittingAction = RemittanceSubmittingAction.RECIPIENT_UPDATE
            )
            try {
                remittanceRepository.updateRecipient(
                    accessToken = session.accessToken,
                    recipientId = recipientId,
                    alias = alias,
                    relation = relation,
                    walletAddress = walletAddress
                )
                loadRemittanceRemoteState(session)
                _remittanceActionUiState.value = RemittanceActionUiState(
                    message = "수신 지갑을 수정했어요."
                )
            } catch (error: RemittanceUnauthorizedException) {
                expireSession(error.message)
                _remittanceActionUiState.value = RemittanceActionUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                _remittanceActionUiState.value = RemittanceActionUiState(
                    message = error.message ?: "수신 지갑을 수정하지 못했어요.",
                    isError = true
                )
            }
        }
    }

    private fun hasRecipientWallet(walletAddress: String): Boolean {
        return _uiState.value.remittance.recipients.any { recipient ->
            recipient.address.equals(walletAddress, ignoreCase = true)
        }
    }

    private fun hasOtherRecipientWallet(recipientId: String, walletAddress: String): Boolean {
        return _uiState.value.remittance.recipients.any { recipient ->
            recipient.id != recipientId && recipient.address.equals(walletAddress, ignoreCase = true)
        }
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

    private fun startRemittanceStatusPolling(session: AuthSession, transferId: String) {
        cancelRemittanceStatusPolling()
        remittanceStatusPollingJob = viewModelScope.launch {
            repeat(REMITTANCE_STATUS_POLL_ATTEMPTS) {
                delay(REMITTANCE_STATUS_POLL_DELAY_MS)
                try {
                    val detail = remittanceRepository.getTransferDetail(session.accessToken, transferId)
                    mergeRemoteTransferDetail(detail)
                    if (detail.isTerminalStatus()) {
                        refreshRemittanceRemoteStateSilently(session)
                        return@launch
                    }
                } catch (error: RemittanceUnauthorizedException) {
                    expireSession(error.message)
                    return@launch
                } catch (_: Exception) {
                    return@repeat
                }
            }
            refreshRemittanceRemoteStateSilently(session)
        }
    }

    private fun cancelRemittanceStatusPolling() {
        remittanceStatusPollingJob?.cancel()
        remittanceStatusPollingJob = null
    }

    private fun syncRemittanceStatusPolling(session: AuthSession, remoteState: RemittanceRemoteState) {
        val activeTransfer = remoteState.payload?.activeTransfer
        if (activeTransfer != null && !activeTransfer.isTerminalStatus()) {
            startRemittanceStatusPolling(session, activeTransfer.transferId)
            return
        }
        cancelRemittanceStatusPolling()
    }

    private fun mergeRemoteTransferDetail(detail: RemittanceTransferDetailPayload) {
        _remittanceRemoteState.update { current ->
            val payload = current.payload ?: return@update current
            val updatedTransfers = buildList {
                add(
                    payload.transfers.firstOrNull { it.transferId == detail.transferId }?.copy(
                        direction = detail.direction,
                        status = detail.status,
                        amountAtomic = detail.amountAtomic,
                        senderAddress = detail.senderAddress,
                        txHash = detail.txHash,
                        updatedAt = detail.updatedAt
                    ) ?: com.dondone.mobile.data.remittance.RemittanceTransferSummaryPayload(
                        transferId = detail.transferId,
                        direction = detail.direction,
                        status = detail.status,
                        assetSymbol = detail.assetSymbol,
                        amountAtomic = detail.amountAtomic,
                        senderAddress = detail.senderAddress,
                        recipientId = detail.recipientId,
                        recipientAlias = detail.recipientAlias,
                        recipientAddress = detail.recipientAddress,
                        txHash = detail.txHash,
                        updatedAt = detail.updatedAt
                    )
                )
                addAll(payload.transfers.filterNot { it.transferId == detail.transferId })
            }
            current.copy(
                payload = payload.copy(
                    transfers = updatedTransfers,
                    activeTransfer = detail
                )
            )
        }
        _uiState.update { current -> current.syncTransferStatus(detail) }
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

    private fun DemoState.syncRemoteWage(payload: WageRemotePayload): DemoState {
        val actualDepositAmount = payload.summary.actualDepositAmount?.toInt()
        return copy(
            wage = wage.copy(
                workDays = payload.summary.workDays,
                totalHours = (payload.summary.totalWorkedMinutes / 60L).toInt(),
                overtimeHours = (payload.summary.overtimeMinutes / 60L).toInt(),
                nightHours = (payload.summary.nightMinutes / 60L).toInt(),
                hourly = payload.monthlySummary.normalizedHourlyWage.toInt(),
                deductionsKnown = payload.summary.deductionsKnown,
                actualDepositRecordedDay = payload.summary.actualDepositRecordedDate?.dayOfMonth
                    ?: payload.summary.actualDepositRecordedDay,
                actualDeposit = actualDepositAmount ?: wage.actualDeposit,
                paydayDay = payload.summary.paydayDay
            ),
            workproof = workproof.copy(
                workplaceId = payload.workplaceId,
                workplaceName = payload.workplaceName
            ),
            documents = documents.syncWageDocuments(
                year = demo.year,
                month = demo.month,
                day = demo.asOfDay,
                latestVerification = payload.latestVerification
            )
        )
    }

    private fun DemoState.syncRemoteRemittance(payload: RemittanceRemotePayload): DemoState {
        val inFlightTransfer = payload.activeTransfer?.takeUnless { it.isTerminalStatus() }
        val nextRecipients = payload.recipients.map { recipient ->
            Recipient(
                id = recipient.recipientId,
                name = recipient.alias,
                relationship = remittanceRelationCodeToLabel(recipient.relation),
                address = recipient.walletAddress
            )
        }
        val selectedRecipientId = when {
            nextRecipients.isEmpty() -> remittance.selectedRecipientId
            nextRecipients.any { it.id == remittance.selectedRecipientId } -> remittance.selectedRecipientId
            else -> nextRecipients.first().id
        }

        return copy(
            remittance = remittance.copy(
                recipients = nextRecipients,
                selectedRecipientId = selectedRecipientId,
                destinationMode = TransferDestinationMode.WALLET,
                txHash = inFlightTransfer?.txHash ?: remittance.txHash,
                status = inFlightTransfer?.toUiTransferStatus() ?: remittance.status
            )
        )
    }

    private fun DemoState.syncTransferStatus(detail: RemittanceTransferDetailPayload): DemoState {
        return copy(
            remittance = remittance.copy(
                txHash = detail.txHash ?: remittance.txHash,
                status = detail.toUiTransferStatus()
            )
        )
    }

    private fun currentWageMonth(): YearMonth =
        YearMonth.of(_uiState.value.demo.year, _uiState.value.demo.month)

    private fun currentWageAsOfDate(): LocalDate =
        LocalDate.of(
            _uiState.value.demo.year,
            _uiState.value.demo.month,
            _uiState.value.demo.asOfDay
        )
}

private fun List<com.dondone.mobile.domain.model.DocumentItem>.syncWageDocuments(
    year: Int,
    month: Int,
    day: Int,
    latestVerification: com.dondone.mobile.data.wage.WageVerificationDetailPayload?
): List<com.dondone.mobile.domain.model.DocumentItem> {
    if (latestVerification == null) return this

    val relatedActions = latestVerification.relatedActions
    val fallbackUpdatedAt = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')} 18:00"

    return map { document ->
        when {
            document.id.contains(DOCUMENT_ID_PROOF) -> document.copy(
                status = if (relatedActions.proofPackDocumentId != null) DOCUMENT_STATUS_READY else DOCUMENT_STATUS_NOT_CREATED,
                updatedAt = if (relatedActions.proofPackDocumentId != null) document.updatedAt ?: fallbackUpdatedAt else null
            )

            document.id.contains(DOCUMENT_ID_CLAIM) -> document.copy(
                status = if (relatedActions.claimKitDocumentId != null) DOCUMENT_STATUS_READY else DOCUMENT_STATUS_NOT_CREATED,
                updatedAt = if (relatedActions.claimKitDocumentId != null) document.updatedAt ?: fallbackUpdatedAt else null
            )

            else -> document
        }
    }
}

private fun RemittanceTransferDetailPayload.toUiTransferStatus(): TransferStatus = when (status) {
    "CONFIRMED" -> TransferStatus.CONFIRMED
    "FAILED", "TIMED_OUT" -> TransferStatus.FAILED
    else -> TransferStatus.SUBMITTED
}

private fun RemittanceTransferDetailPayload.isTerminalStatus(): Boolean =
    status == "CONFIRMED" || status == "FAILED" || status == "TIMED_OUT"

private fun com.dondone.mobile.data.remittance.RemittanceTransferPrecheckPayload?.requiresHighAmountConfirmation(): Boolean =
    this?.policyCode == "HIGH_AMOUNT_CONFIRMATION_REQUIRED"

private fun com.dondone.mobile.data.remittance.RemittanceTransferPrecheckPayload.isConfirmable(): Boolean =
    policyCode == "RECENT_RECIPIENT_CONFIRMATION_REQUIRED" || policyCode == "HIGH_AMOUNT_CONFIRMATION_REQUIRED"

private fun com.dondone.mobile.data.remittance.RemittanceTransferPrecheckPayload.resolveBlockedMessage(): String {
    return when (policyCode) {
        "INSUFFICIENT_WALLET_BALANCE" -> "송금 지갑 잔액이 부족해요. 잠시 후 다시 확인해 주세요."
        "RECIPIENT_NOT_ALLOWED" -> "허용된 수신자만 송금할 수 있어요."
        "TRANSFER_ALREADY_IN_PROGRESS" -> "진행 중인 송금이 있어 잠시 후 다시 시도해 주세요."
        "SELF_TRANSFER_NOT_ALLOWED" -> "내 지갑으로는 송금할 수 없어요."
        else -> "현재는 이 송금을 진행할 수 없어요."
    }
}

private val WorkproofPdfPreviewDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
