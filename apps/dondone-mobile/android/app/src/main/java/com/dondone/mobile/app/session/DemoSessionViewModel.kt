package com.dondone.mobile.app.session

import android.content.Context
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dondone.mobile.core.location.AndroidCurrentLocationProvider
import com.dondone.mobile.core.location.CurrentLocationProvider
import com.dondone.mobile.core.location.UnavailableCurrentLocationProvider
import com.dondone.mobile.core.ui.phoneDigits
import com.dondone.mobile.data.auth.AuthUnauthorizedException
import com.dondone.mobile.data.advance.AdvanceRemoteMode
import com.dondone.mobile.data.advance.AdvanceRequestDetailPayload
import com.dondone.mobile.data.advance.AdvanceRequestItemPayload
import com.dondone.mobile.data.advance.AdvanceRemoteState
import com.dondone.mobile.data.advance.AdvanceRepository
import com.dondone.mobile.data.advance.BackendAdvanceRepository
import com.dondone.mobile.data.auth.AuthRepository
import com.dondone.mobile.data.auth.AuthSession
import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.data.documents.BackendWorkproofDocumentRepository
import com.dondone.mobile.data.documents.WorkproofDocumentRepository
import com.dondone.mobile.data.documents.WorkproofDocumentUnauthorizedException
import com.dondone.mobile.data.remittance.BackendRemittanceRepository
import com.dondone.mobile.data.remittance.InMemoryRemittanceCompletionNoticeStore
import com.dondone.mobile.data.remittance.RemittanceCompletionNoticeStore
import com.dondone.mobile.data.remittance.RemittanceRemoteMode
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.data.remittance.RemittanceRepository
import com.dondone.mobile.data.remittance.RemittanceTransferDetailPayload
import com.dondone.mobile.data.remittance.RemittanceUnauthorizedException
import com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload
import com.dondone.mobile.data.settings.AppLanguageStore
import com.dondone.mobile.data.settings.InMemoryAppLanguageStore
import com.dondone.mobile.data.vault.BackendVaultRepository
import com.dondone.mobile.data.vault.VaultActionType
import com.dondone.mobile.data.vault.VaultCreateTransactionPayload
import com.dondone.mobile.data.vault.VaultRemoteMode
import com.dondone.mobile.data.vault.VaultRemoteState
import com.dondone.mobile.data.vault.VaultRepository
import com.dondone.mobile.data.vault.VaultSummaryPayload
import com.dondone.mobile.data.vault.VaultTransactionDetailPayload
import com.dondone.mobile.data.vault.VaultUnauthorizedException
import com.dondone.mobile.data.wage.BackendWageRepository
import com.dondone.mobile.data.wage.WageRemoteState
import com.dondone.mobile.data.wage.WageRepository
import com.dondone.mobile.data.wage.WageUnauthorizedException
import com.dondone.mobile.data.workproof.BackendWorkproofRepository
import com.dondone.mobile.data.workproof.WorkproofCorrectionRequestMutation
import com.dondone.mobile.data.workproof.WorkproofRemoteMode
import com.dondone.mobile.data.workproof.WorkproofRemotePayload
import com.dondone.mobile.data.workproof.WorkproofRemoteState
import com.dondone.mobile.data.workproof.WorkproofRepository
import com.dondone.mobile.data.workproof.WorkproofUnauthorizedException
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TransactionCategory
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfCreateUiState
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfFileAction
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfFileUiState
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfPreviewUiState
import com.dondone.mobile.domain.model.remittanceRelationCodeToLabel
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private const val TRANSFER_CONFIRMATION_DELAY_MS = 1800L
private const val REMITTANCE_STATUS_POLL_DELAY_MS = 1500L
private const val REMITTANCE_STATUS_POLL_ATTEMPTS = 120
private const val VAULT_STATUS_POLL_DELAY_MS = 1500L
private const val VAULT_STATUS_POLL_ATTEMPTS = 16
private const val REMITTANCE_REMOTE_LOGIN_MESSAGE = "로그인 후 송금 실연동 데이터를 불러옵니다."
private const val VAULT_REMOTE_LOGIN_MESSAGE = "로그인 후 예치 실연동 데이터를 불러옵니다."
private const val ADVANCE_REMOTE_LOGIN_MESSAGE = "로그인 후 근무 정보를 불러옵니다."
private const val WORKPROOF_REMOTE_LOGIN_MESSAGE = "로그인 후 출퇴근 실연동을 불러옵니다."
private const val WAGE_REMOTE_LOGIN_MESSAGE = "로그인 후 급여 실연동 데이터를 불러옵니다."
private const val ATOMIC_UNITS_PER_USDC = 1_000_000L
private const val DEMO_SESSION_VIEW_MODEL_TAG = "DemoSessionViewModel"

private enum class RemittanceRemoteLoadMode {
    INITIAL,
    SILENT_REFRESH
}

private enum class VaultRemoteLoadMode {
    INITIAL,
    SILENT_REFRESH
}

class DemoSessionViewModel(
    private val appContext: Context? = null,
    private val authRepository: AuthRepository,
    private val advanceRepository: AdvanceRepository = BackendAdvanceRepository(),
    private val workproofRepository: WorkproofRepository = BackendWorkproofRepository(),
    private val workproofDocumentRepository: WorkproofDocumentRepository = BackendWorkproofDocumentRepository(),
    private val remittanceRepository: RemittanceRepository = BackendRemittanceRepository(),
    private val remittanceCompletionNoticeStore: RemittanceCompletionNoticeStore = InMemoryRemittanceCompletionNoticeStore(),
    private val appLanguageStore: AppLanguageStore = InMemoryAppLanguageStore(),
    private val vaultRepository: VaultRepository = BackendVaultRepository(),
    private val wageRepository: WageRepository = BackendWageRepository(),
    private val currentLocationProvider: CurrentLocationProvider = appContext?.let(::AndroidCurrentLocationProvider)
        ?: UnavailableCurrentLocationProvider()
) : ViewModel() {
    private val initialState = DemoSeedFactory.create()
    private var transferCompletionJob: Job? = null
    private var remittanceStatusPollingJob: Job? = null
    private var vaultStatusPollingJob: Job? = null
    private var activeVaultPollingRequestId: String? = null
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<DemoState> = _uiState.asStateFlow()
    private val _appLanguage = MutableStateFlow(appLanguageStore.read())
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()
    private val _authUiState = MutableStateFlow(AuthUiState.restoring())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()
    private val _profileUpdateUiState = MutableStateFlow(ProfileUpdateUiState())
    val profileUpdateUiState: StateFlow<ProfileUpdateUiState> = _profileUpdateUiState.asStateFlow()
    private val _workerRegistrationCodeUiState = MutableStateFlow(WorkerRegistrationCodeUiState())
    val workerRegistrationCodeUiState: StateFlow<WorkerRegistrationCodeUiState> = _workerRegistrationCodeUiState.asStateFlow()
    private val _recipientPhoneSearchUiState = MutableStateFlow(RecipientPhoneSearchUiState())
    val recipientPhoneSearchUiState: StateFlow<RecipientPhoneSearchUiState> = _recipientPhoneSearchUiState.asStateFlow()
    private val _advanceRemoteState =
        MutableStateFlow(AdvanceRemoteState.unauthenticated(ADVANCE_REMOTE_LOGIN_MESSAGE))
    val advanceRemoteState: StateFlow<AdvanceRemoteState> = _advanceRemoteState.asStateFlow()
    private val _workproofRemoteState =
        MutableStateFlow(WorkproofRemoteState.unauthenticated(WORKPROOF_REMOTE_LOGIN_MESSAGE))
    val workproofRemoteState: StateFlow<WorkproofRemoteState> = _workproofRemoteState.asStateFlow()
    private val _wageRemoteState =
        MutableStateFlow(WageRemoteState.unauthenticated(WAGE_REMOTE_LOGIN_MESSAGE))
    private val _remittanceRemoteState =
        MutableStateFlow(RemittanceRemoteState.unauthenticated(REMITTANCE_REMOTE_LOGIN_MESSAGE))
    private val _vaultRemoteState =
        MutableStateFlow(VaultRemoteState.unauthenticated(VAULT_REMOTE_LOGIN_MESSAGE))
    val wageRemoteState: StateFlow<WageRemoteState> = _wageRemoteState.asStateFlow()
    val remittanceRemoteState: StateFlow<RemittanceRemoteState> = _remittanceRemoteState.asStateFlow()
    val vaultRemoteState: StateFlow<VaultRemoteState> = _vaultRemoteState.asStateFlow()
    private val _workproofActionUiState = MutableStateFlow(WorkproofActionUiState())
    val workproofActionUiState: StateFlow<WorkproofActionUiState> = _workproofActionUiState.asStateFlow()
    private val _workproofCurrentLocationUiState = MutableStateFlow(WorkproofCurrentLocationUiState())
    val workproofCurrentLocationUiState: StateFlow<WorkproofCurrentLocationUiState> =
        _workproofCurrentLocationUiState.asStateFlow()
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
    private val _remittanceCompletionNoticeUiState = MutableStateFlow(RemittanceCompletionNoticeUiState())
    val remittanceCompletionNoticeUiState: StateFlow<RemittanceCompletionNoticeUiState> =
        _remittanceCompletionNoticeUiState.asStateFlow()
    private val _vaultActionUiState = MutableStateFlow(VaultActionUiState())
    val vaultActionUiState: StateFlow<VaultActionUiState> = _vaultActionUiState.asStateFlow()
    private val _transactionMetadataOverrides = MutableStateFlow<Map<String, TransactionMetadataOverride>>(emptyMap())
    val transactionMetadataOverrides: StateFlow<Map<String, TransactionMetadataOverride>> =
        _transactionMetadataOverrides.asStateFlow()
    private val _selectedAdvanceAmount = MutableStateFlow<Int?>(null)
    val selectedAdvanceAmount: StateFlow<Int?> = _selectedAdvanceAmount.asStateFlow()
    private val _selectedVaultAmount = MutableStateFlow<Int?>(null)
    val selectedVaultAmount: StateFlow<Int?> = _selectedVaultAmount.asStateFlow()
    private val _selectedVaultActionType = MutableStateFlow(VaultActionType.DEPOSIT)
    val selectedVaultActionType: StateFlow<VaultActionType> = _selectedVaultActionType.asStateFlow()
    private val _menuLaunchRequest = MutableStateFlow<MenuLaunchRequest?>(null)
    val menuLaunchRequest: StateFlow<MenuLaunchRequest?> = _menuLaunchRequest.asStateFlow()
    private val _remittanceLaunchRequest = MutableStateFlow<RemittanceLaunchRequest?>(null)
    val remittanceLaunchRequest: StateFlow<RemittanceLaunchRequest?> = _remittanceLaunchRequest.asStateFlow()
    private val _workproofLaunchRequest = MutableStateFlow<WorkproofLaunchRequest?>(null)
    val workproofLaunchRequest: StateFlow<WorkproofLaunchRequest?> = _workproofLaunchRequest.asStateFlow()
    private val _advanceRequestUiState = MutableStateFlow(AdvanceRequestUiState())
    val advanceRequestUiState: StateFlow<AdvanceRequestUiState> = _advanceRequestUiState.asStateFlow()
    private val _advanceRequestDetailUiState = MutableStateFlow(AdvanceRequestDetailUiState())
    val advanceRequestDetailUiState: StateFlow<AdvanceRequestDetailUiState> = _advanceRequestDetailUiState.asStateFlow()
    private var nextMenuLaunchRequestId = 1L
    private var nextRemittanceLaunchRequestId = 1L
    private var nextWorkproofLaunchRequestId = 1L
    private val advanceHandlers = DemoSessionAdvanceHandlers(
        scope = viewModelScope,
        advanceRepository = advanceRepository,
        authUiStateFlow = _authUiState,
        advanceRemoteStateFlow = _advanceRemoteState,
        selectedAdvanceAmountFlow = _selectedAdvanceAmount,
        advanceRequestUiStateFlow = _advanceRequestUiState,
        advanceRequestDetailUiStateFlow = _advanceRequestDetailUiState,
        unauthenticatedMessage = ADVANCE_REMOTE_LOGIN_MESSAGE,
        loadAdvanceRemoteState = ::loadAdvanceRemoteState,
        mergeAdvanceRequestDetail = ::mergeAdvanceRequestDetail,
        expireSession = ::expireSession
    )
    private val workproofHandlers = DemoSessionWorkproofHandlers(
        appContext = appContext,
        scope = viewModelScope,
        currentLocationProvider = currentLocationProvider,
        workproofRepository = workproofRepository,
        uiStateFlow = _uiState,
        authUiStateFlow = _authUiState,
        workproofRemoteStateFlow = _workproofRemoteState,
        workproofActionUiStateFlow = _workproofActionUiState,
        workproofCurrentLocationUiStateFlow = _workproofCurrentLocationUiState,
        applyWorkproofRemoteState = ::applyWorkproofRemoteState,
        expireSession = ::expireSession
    )

    init {
        restoreAuthSession()
    }

    fun shiftAsOfDay(delta: Int) {
        _uiState.update { state -> DemoSessionReducer.shiftAsOfDay(state, delta) }
        if (_authUiState.value.isAuthenticated) {
            refreshWageRemoteState()
        }
    }

    fun updateAppLanguage(languageCode: String) {
        val nextLanguage = AppLanguage.fromCode(languageCode)
        if (_appLanguage.value == nextLanguage) {
            return
        }
        appLanguageStore.save(nextLanguage)
        _appLanguage.value = nextLanguage
    }

    fun selectAccount(accountId: String) {
        if (accountId == "remote-wallet") {
            return
        }
        _uiState.update { state -> DemoSessionReducer.selectAccount(state, accountId) }
    }

    fun openTransferFlow(): Boolean {
        if (hasPendingRemittanceTransfer()) {
            _remittanceActionUiState.value = RemittanceActionUiState(
                message = "이전 송금 결과를 확인하고 있어요.",
                isError = true
            )
            return false
        }
        cancelTransferCompletion()
        _uiState.update { state -> DemoSessionReducer.openTransferFlow(state) }
        val session = _authUiState.value.session ?: return true
        viewModelScope.launch {
            loadRemittanceRemoteState(session)
        }
        return true
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

    fun addRecipientFromTransfer(
        alias: String,
        relation: String,
        walletAddress: String,
        targetUserId: Long?
    ) {
        val session = _authUiState.value.session ?: run {
            _remittanceActionUiState.value = RemittanceActionUiState(
                message = "로그인 후 다시 시도해 주세요.",
                isError = true
            )
            return
        }

        if (targetUserId == null && hasRecipientWallet(walletAddress)) {
            _remittanceActionUiState.value = RemittanceActionUiState(
                message = "이미 등록된 지갑이에요.",
                isError = true
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

    fun refreshWorkproofCurrentLocation() {
        workproofHandlers.refreshWorkproofCurrentLocation()
    }

    fun clockIn() {
        workproofHandlers.clockIn()
    }

    fun clockOut() {
        workproofHandlers.clockOut()
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
                _remittanceLaunchRequest.value = RemittanceLaunchRequest(
                    requestId = nextRemittanceLaunchRequestId++
                )
                _remittanceActionUiState.value = RemittanceActionUiState(
                    message = "송금 요청을 접수했어요."
                )
                refreshRemittanceRemoteStateSilently(session)
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

    fun dismissRemittanceCompletionNotice() {
        val transferId = _remittanceCompletionNoticeUiState.value.transferId
        val userId = _authUiState.value.session?.userId
        if (transferId != null && userId != null) {
            remittanceCompletionNoticeStore.saveDismissedTransferId(userId, transferId)
        }
        _remittanceCompletionNoticeUiState.value = RemittanceCompletionNoticeUiState()
    }

    fun consumeRemittanceLaunchRequest() {
        _remittanceLaunchRequest.value = null
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

    fun saveWorkproofEdit(
        recordId: String,
        requestedClockInText: String,
        requestedClockOutText: String,
        reasonCode: String,
        memo: String,
        addAttachment: Boolean
    ) {
        if (_workproofActionUiState.value.isSubmitting) {
            return
        }
        val session = _authUiState.value.session ?: run {
            _workproofActionUiState.value = WorkproofActionUiState(
                message = "로그인 후 수정 요청을 제출할 수 있어요.",
                isError = true
            )
            return
        }
        val targetRecord = _uiState.value.workproof.records.firstOrNull { it.id == recordId } ?: run {
            _workproofActionUiState.value = WorkproofActionUiState(
                message = "다시 불러온 뒤 수정 요청을 제출해 주세요.",
                isError = true
            )
            return
        }
        val workproofId = targetRecord.id.toLongOrNull() ?: run {
            _workproofActionUiState.value = WorkproofActionUiState(
                message = "실연동 기록만 수정 요청을 보낼 수 있어요.",
                isError = true
            )
            return
        }
        val requestedClockIn = requestedClockInText.toWorkproofLocalTimeOrNull() ?: run {
            _workproofActionUiState.value = WorkproofActionUiState(
                message = "출근 요청 시간을 HH:mm 형식으로 입력해 주세요.",
                isError = true
            )
            return
        }
        val requestedClockOut = requestedClockOutText.toWorkproofLocalTimeOrNull() ?: run {
            _workproofActionUiState.value = WorkproofActionUiState(
                message = "퇴근 요청 시간을 HH:mm 형식으로 입력해 주세요.",
                isError = true
            )
            return
        }
        val request = WorkproofCorrectionRequestMutation(
            workproofId = workproofId,
            requestedClockInAt = targetRecord.workDate.atTime(requestedClockIn),
            requestedClockOutAt = targetRecord.workDate.atTime(requestedClockOut),
            reasonCode = reasonCode,
            reason = reasonCode.toWorkproofReasonLabel(),
            memo = memo.ifBlank { null },
            attachmentCount = if (addAttachment) 1 else 0
        )

        viewModelScope.launch {
            _workproofActionUiState.value = WorkproofActionUiState(isSubmitting = true)
            try {
                val result = workproofRepository.createCorrectionRequest(session.accessToken, request)
                if (result.remoteState.mode != WorkproofRemoteMode.CONTENT) {
                    _workproofRemoteState.value = result.remoteState
                    _workproofActionUiState.value = WorkproofActionUiState(
                        message = result.remoteState.errorMessage ?: "수정 요청을 제출하지 못했어요.",
                        isError = true
                    )
                    return@launch
                }
                applyWorkproofRemoteState(result.remoteState)
                _workproofActionUiState.value = WorkproofActionUiState(
                    message = result.correctionRequest.status.toWorkproofSuccessMessage()
                )
            } catch (error: WorkproofUnauthorizedException) {
                expireSession(error.message)
                _workproofActionUiState.value = WorkproofActionUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                _workproofActionUiState.value = WorkproofActionUiState(
                    message = error.message ?: "수정 요청을 제출하지 못했어요.",
                    isError = true
                )
            }
        }
    }

    fun resetSeed() {
        cancelTransferCompletion()
        cancelRemittanceStatusPolling()
        _uiState.value = initialState
        _advanceRequestUiState.value = AdvanceRequestUiState()
        _advanceRequestDetailUiState.value = AdvanceRequestDetailUiState()
        _workproofActionUiState.value = WorkproofActionUiState()
        _workproofCurrentLocationUiState.value = WorkproofCurrentLocationUiState()
        _workproofPdfPreviewUiState.value = WorkproofPdfPreviewUiState()
        _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState()
        _workproofPdfFileUiState.value = WorkproofPdfFileUiState()
        _wageActionUiState.value = WageActionUiState()
        _remittanceActionUiState.value = RemittanceActionUiState()
        _menuLaunchRequest.value = null
        _workproofLaunchRequest.value = null
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

    fun openWorkproofPdfCreation() {
        _workproofLaunchRequest.value = WorkproofLaunchRequest(
            target = WorkproofLaunchTarget.PDF_CREATION,
            requestId = nextWorkproofLaunchRequestId++
        )
    }

    fun consumeWorkproofLaunchRequest() {
        _workproofLaunchRequest.value = null
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
                onAuthenticated(updatedSession)
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

    fun redeemWorkerRegistrationCode(registrationCode: String) {
        val session = _authUiState.value.session ?: run {
            _workerRegistrationCodeUiState.value = WorkerRegistrationCodeUiState(
                message = "로그인 후 다시 시도해 주세요.",
                isError = true
            )
            return
        }
        val normalizedRegistrationCode = registrationCode.trim().uppercase()
        if (normalizedRegistrationCode.isBlank()) {
            _workerRegistrationCodeUiState.value = WorkerRegistrationCodeUiState(
                message = "등록 코드를 입력해 주세요.",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            _workerRegistrationCodeUiState.value = WorkerRegistrationCodeUiState(isSubmitting = true)
            try {
                val updatedSession = authRepository.redeemWorkerRegistrationCode(
                    session = session,
                    registrationCode = normalizedRegistrationCode
                )
                onAuthenticated(updatedSession)
                val companyLabel = updatedSession.companyName ?: "회사"
                val workplaceLabel = ""
                _workerRegistrationCodeUiState.value = WorkerRegistrationCodeUiState(
                    message = "$companyLabel$workplaceLabel 등록이 완료됐어요."
                )
            } catch (error: AuthUnauthorizedException) {
                expireSession(error.message)
                _workerRegistrationCodeUiState.value = WorkerRegistrationCodeUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                _workerRegistrationCodeUiState.value = WorkerRegistrationCodeUiState(
                    message = error.message ?: "회사 등록을 완료하지 못했어요.",
                    isError = true
                )
            }
        }
    }

    fun clearWorkerRegistrationCodeMessage() {
        if (!_workerRegistrationCodeUiState.value.isSubmitting && _workerRegistrationCodeUiState.value.message != null) {
            _workerRegistrationCodeUiState.value = WorkerRegistrationCodeUiState()
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
        advanceHandlers.selectAdvanceAmount(amount)
    }

    fun clearAdvanceRequestMessage() {
        advanceHandlers.clearAdvanceRequestMessage()
    }

    fun openAdvanceRequestDetail(requestId: Long) {
        advanceHandlers.openAdvanceRequestDetail(requestId)
    }

    fun closeAdvanceRequestDetail() {
        advanceHandlers.closeAdvanceRequestDetail()
    }

    fun requestAdvance() {
        advanceHandlers.requestAdvance()
    }

    fun refreshAdvanceRemoteState() {
        advanceHandlers.refreshAdvanceRemoteState()
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

    fun refreshRemittanceRemoteStateSilentlyIfAuthenticated() {
        val session = _authUiState.value.session ?: return
        viewModelScope.launch {
            refreshRemittanceRemoteStateSilently(session)
        }
    }

    fun refreshVaultRemoteState() {
        val session = _authUiState.value.session
        if (session == null) {
            _vaultRemoteState.value = VaultRemoteState.unauthenticated(VAULT_REMOTE_LOGIN_MESSAGE)
            return
        }

        viewModelScope.launch {
            loadVaultRemoteState(session)
        }
    }

    fun selectVaultAction(actionType: VaultActionType) {
        if (_selectedVaultActionType.value == actionType) {
            return
        }
        _selectedVaultActionType.value = actionType
        syncSelectedVaultAmount(_vaultRemoteState.value)
    }

    fun selectVaultAmount(amount: Int) {
        _selectedVaultAmount.value = amount
    }

    fun submitVaultAction() {
        if (_vaultActionUiState.value.isSubmitting) {
            return
        }
        val session = _authUiState.value.session ?: run {
            _vaultActionUiState.value = VaultActionUiState(
                message = "로그인 후 다시 시도해 주세요.",
                isError = true
            )
            return
        }
        val summary = _vaultRemoteState.value.payload?.summary ?: run {
            _vaultActionUiState.value = VaultActionUiState(
                message = "예치 요약을 먼저 불러와 주세요.",
                isError = true
            )
            return
        }
        val selectedAmount = _selectedVaultAmount.value ?: 0
        if (selectedAmount <= 0) {
            _vaultActionUiState.value = VaultActionUiState(
                message = "금액을 먼저 선택해 주세요.",
                isError = true
            )
            return
        }

        val amountAtomic = selectedAmount.toAtomicAmount(summary.assetDecimals)
        if (amountAtomic <= 0L) {
            _vaultActionUiState.value = VaultActionUiState(
                message = "요청 금액이 올바르지 않아요.",
                isError = true
            )
            return
        }

        val actionType = _selectedVaultActionType.value
        viewModelScope.launch {
            _vaultActionUiState.value = VaultActionUiState(
                isSubmitting = true,
                submittingAction = actionType.toSubmittingAction()
            )
            try {
                val result = when (actionType) {
                    VaultActionType.DEPOSIT -> vaultRepository.createDeposit(
                        accessToken = session.accessToken,
                        amountAtomic = amountAtomic
                    )

                    VaultActionType.WITHDRAW -> vaultRepository.createWithdrawal(
                        accessToken = session.accessToken,
                        amountAtomic = amountAtomic
                    )
                }
                mergeVaultCreateResult(
                    summary = summary,
                    amountAtomic = amountAtomic.toString(),
                    result = result
                )
                _vaultActionUiState.value = VaultActionUiState(
                    message = actionType.toCreateSuccessMessage(),
                    messagePresentation = VaultMessagePresentation.TOAST_ONLY
                )
                startVaultStatusPolling(session, result.requestId)
            } catch (error: VaultUnauthorizedException) {
                expireSession(error.message)
                _vaultActionUiState.value = VaultActionUiState(
                    message = error.message,
                    isError = true
                )
            } catch (error: Exception) {
                _vaultActionUiState.value = VaultActionUiState(
                    message = error.message ?: actionType.toCreateFailureMessage(),
                    isError = true
                )
            }
        }
    }

    fun clearVaultActionMessage() {
        _vaultActionUiState.update { current ->
            if (current.message == null) current else current.copy(message = null, isError = false)
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
                _vaultRemoteState.value = VaultRemoteState.unauthenticated(VAULT_REMOTE_LOGIN_MESSAGE)
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
        if (!_authUiState.value.isAuthenticated) {
            return
        }
        loadVaultRemoteState(session)
    }

    private suspend fun loadAdvanceRemoteState(session: AuthSession) {
        val previousState = _advanceRemoteState.value
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
        syncRemittanceBalanceAfterAdvancePayout(previousState = previousState, currentState = remoteState, session = session)
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
            mode = RemittanceRemoteLoadMode.INITIAL
        )
    }

    private suspend fun loadVaultRemoteState(
        session: AuthSession
    ) {
        updateVaultRemoteState(
            session = session,
            mode = VaultRemoteLoadMode.INITIAL
        )
    }

    private suspend fun refreshRemittanceRemoteStateSilently(
        session: AuthSession
    ) {
        updateRemittanceRemoteState(
            session = session,
            mode = RemittanceRemoteLoadMode.SILENT_REFRESH
        )
    }

    private suspend fun refreshVaultRemoteStateSilently(
        session: AuthSession
    ) {
        updateVaultRemoteState(
            session = session,
            mode = VaultRemoteLoadMode.SILENT_REFRESH
        )
    }

    private suspend fun updateRemittanceRemoteState(
        session: AuthSession,
        mode: RemittanceRemoteLoadMode
    ) {
        if (mode == RemittanceRemoteLoadMode.INITIAL || _remittanceRemoteState.value.payload == null) {
            _remittanceRemoteState.value = RemittanceRemoteState.loading()
        }
        val remoteState = remittanceRepository.load(session.accessToken)
        if (
            mode == RemittanceRemoteLoadMode.SILENT_REFRESH &&
            remoteState.mode == RemittanceRemoteMode.ERROR &&
            _remittanceRemoteState.value.payload != null
        ) {
            return
        }
        applyRemittanceRemoteState(remoteState)
        syncRemittanceStatusPolling(session, remoteState)
    }

    private suspend fun updateVaultRemoteState(
        session: AuthSession,
        mode: VaultRemoteLoadMode
    ) {
        if (mode == VaultRemoteLoadMode.INITIAL || _vaultRemoteState.value.payload == null) {
            _vaultRemoteState.value = VaultRemoteState.loading()
        }
        val remoteState = vaultRepository.load(session.accessToken)
        if (
            mode == VaultRemoteLoadMode.SILENT_REFRESH &&
            remoteState.mode == VaultRemoteMode.ERROR &&
            _vaultRemoteState.value.payload != null
        ) {
            return
        }
        applyVaultRemoteState(remoteState)
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
        syncAuthenticatedOrganization(payload)
        _uiState.update { current ->
            val synced = current.syncRemoteWorkproof(payload)
            if (_workproofCurrentLocationUiState.value.status == WorkproofCurrentLocationStatus.IDLE) {
                synced.copy(
                    workproof = synced.workproof.copy(
                        currentLatitude = payload.workplace.latitude,
                        currentLongitude = payload.workplace.longitude
                    )
                )
            } else {
                synced
            }
        }
    }

    private fun syncAuthenticatedOrganization(payload: WorkproofRemotePayload) {
        val session = _authUiState.value.session ?: return
        val nextSession = session.copy(
            companyName = session.companyName ?: session.companyCode,
            workplaceName = payload.workplace.name
        )
        if (nextSession != session) {
            _authUiState.value = AuthUiState.authenticated(nextSession)
        }
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
            } catch (error: IOException) {
                Log.w(
                    DEMO_SESSION_VIEW_MODEL_TAG,
                    "Failed to refresh wage verification detail. Keeping previous payload.",
                    error
                )
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
        syncRemittanceCompletionNotice(detail = payload.activeTransfer, shouldAnnounce = false)
    }

    private suspend fun applyVaultRemoteState(remoteState: VaultRemoteState) {
        if (!remoteState.isAuthenticated) {
            clearAuthenticatedState(
                AuthUiState.unauthenticated(
                    remoteState.errorMessage ?: "세션이 만료되어 다시 로그인해 주세요."
                )
            )
            return
        }

        _vaultRemoteState.value = remoteState
        syncRemittanceBalanceFromVaultSummary(remoteState.payload?.summary)
        syncSelectedVaultAmount(remoteState)

        val session = _authUiState.value.session
        val latestTransaction = remoteState.payload?.latestTransaction
        if (session != null && latestTransaction != null && !latestTransaction.isTerminalStatus()) {
            startVaultStatusPolling(session, latestTransaction.requestId)
        } else {
            cancelVaultStatusPolling()
        }
    }

    private fun syncSelectedAdvanceAmount(remoteState: AdvanceRemoteState) {
        val availableAmount = remoteState.eligibility?.availableAmountInWholeAssetUnits ?: 0
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

    private fun syncSelectedVaultAmount(remoteState: VaultRemoteState) {
        val summary = remoteState.payload?.summary ?: run {
            _selectedVaultAmount.value = null
            return
        }
        val currentActionType = _selectedVaultActionType.value
        val availableAmount = summary.availableAmountFor(currentActionType)
        if (currentActionType == VaultActionType.WITHDRAW && availableAmount <= 0) {
            _selectedVaultActionType.value = VaultActionType.DEPOSIT
        }
        val effectiveAvailableAmount = summary.availableAmountFor(_selectedVaultActionType.value)
        if (effectiveAvailableAmount <= 0) {
            _selectedVaultAmount.value = null
            return
        }
        val current = _selectedVaultAmount.value
        if (current != null && current in 1..effectiveAvailableAmount) {
            return
        }
        _selectedVaultAmount.value = pickDefaultVaultAmount(effectiveAvailableAmount)
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
                        workplaceId = detail.workplaceId,
                        assetSymbol = detail.assetSymbol,
                        assetDecimals = detail.assetDecimals,
                        exchangeRateSnapshot = detail.exchangeRateSnapshot,
                        requestedAmountAtomic = detail.requestedAmountAtomic,
                        requestedDisplayKrwAmount = detail.requestedDisplayKrwAmount,
                        approvedAmountAtomic = detail.approvedAmountAtomic,
                        approvedDisplayKrwAmount = detail.approvedDisplayKrwAmount,
                        status = detail.status,
                        requestStatus = detail.requestStatus,
                        payoutStatus = detail.payoutStatus,
                        payoutTxHash = detail.payoutTxHash,
                        repaymentDueDate = detail.repaymentDueDate,
                        requestedAt = detail.createdAt
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

    private suspend fun syncRemittanceBalanceAfterAdvancePayout(
        previousState: AdvanceRemoteState,
        currentState: AdvanceRemoteState,
        session: AuthSession
    ) {
        if (currentState.mode != AdvanceRemoteMode.CONTENT) {
            return
        }

        val previousPaidKeys = previousState.requests
            .asSequence()
            .filter { it.status == "PAID" && !it.payoutTxHash.isNullOrBlank() }
            .map { "${it.requestId}:${it.payoutTxHash}" }
            .toSet()

        val hasNewPaidPayout = currentState.requests.any { request ->
            request.status == "PAID" &&
                !request.payoutTxHash.isNullOrBlank() &&
                "${request.requestId}:${request.payoutTxHash}" !in previousPaidKeys
        }

        if (hasNewPaidPayout) {
            refreshRemittanceRemoteStateSilently(session)
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
        _authUiState.value.session?.userId?.let(remittanceCompletionNoticeStore::clear)
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
        _vaultRemoteState.value = VaultRemoteState.unauthenticated(
            unauthenticatedState.errorMessage ?: VAULT_REMOTE_LOGIN_MESSAGE
        )
        _selectedAdvanceAmount.value = null
        _selectedVaultAmount.value = null
        _selectedVaultActionType.value = VaultActionType.DEPOSIT
        _advanceRequestUiState.value = AdvanceRequestUiState()
        _advanceRequestDetailUiState.value = AdvanceRequestDetailUiState()
        _workproofActionUiState.value = WorkproofActionUiState()
        _workproofPdfPreviewUiState.value = WorkproofPdfPreviewUiState()
        _workproofPdfCreateUiState.value = WorkproofPdfCreateUiState()
        _workproofPdfFileUiState.value = WorkproofPdfFileUiState()
        _wageActionUiState.value = WageActionUiState()
        _remittanceActionUiState.value = RemittanceActionUiState()
        _remittanceCompletionNoticeUiState.value = RemittanceCompletionNoticeUiState()
        _vaultActionUiState.value = VaultActionUiState()
        _profileUpdateUiState.value = ProfileUpdateUiState()
        _workerRegistrationCodeUiState.value = WorkerRegistrationCodeUiState()
        _recipientPhoneSearchUiState.value = RecipientPhoneSearchUiState()
        _menuLaunchRequest.value = null
        _remittanceLaunchRequest.value = null
        _workproofLaunchRequest.value = null
        cancelRemittanceStatusPolling()
        cancelVaultStatusPolling()
    }

    private fun hasPendingRemittanceTransfer(): Boolean {
        if (_uiState.value.remittance.status == TransferStatus.SUBMITTED) {
            return true
        }
        val remoteActiveTransfer = _remittanceRemoteState.value.payload?.activeTransfer
        return remoteActiveTransfer != null && !remoteActiveTransfer.isTerminalStatus()
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
        val context = requireNotNull(appContext) { "Workproof PDF download requires appContext" }
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
                    context,
                    "${context.packageName}.fileprovider",
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
        val context = requireNotNull(appContext) { "Workproof PDF download requires appContext" }
        val documentsDir = File(context.cacheDir, "documents").apply { mkdirs() }
        return File(documentsDir, fileName).apply {
            writeBytes(bytes)
        }
    }

    private fun startRemittanceStatusPolling(session: AuthSession, transferId: String) {
        cancelRemittanceStatusPolling()
        remittanceStatusPollingJob = viewModelScope.launch {
            try {
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
                    } catch (error: IOException) {
                        Log.w(
                            DEMO_SESSION_VIEW_MODEL_TAG,
                            "Failed to poll remittance transfer detail for $transferId.",
                            error
                        )
                        return@repeat
                    } catch (_: Exception) {
                        // Keep polling so a single network failure does not freeze the tracker UI.
                    }
                }
                refreshRemittanceRemoteStateSilently(session)
            } finally {
                remittanceStatusPollingJob = null
            }
        }
    }

    private fun cancelRemittanceStatusPolling() {
        remittanceStatusPollingJob?.cancel()
        remittanceStatusPollingJob = null
    }

    private fun startVaultStatusPolling(session: AuthSession, requestId: String) {
        if (activeVaultPollingRequestId == requestId && vaultStatusPollingJob?.isActive == true) {
            return
        }
        cancelVaultStatusPolling()
        activeVaultPollingRequestId = requestId
        vaultStatusPollingJob = viewModelScope.launch {
            try {
                repeat(VAULT_STATUS_POLL_ATTEMPTS) {
                    delay(VAULT_STATUS_POLL_DELAY_MS)
                    try {
                        val detail = vaultRepository.getTransactionDetail(session.accessToken, requestId)
                        mergeVaultTransactionDetail(detail)
                        if (detail.isTerminalStatus()) {
                            refreshRemittanceRemoteStateSilently(session)
                            if (_authUiState.value.isAuthenticated) {
                                refreshVaultRemoteStateSilently(session)
                            }
                            _vaultActionUiState.value = detail.toCompletionUiState()
                            return@launch
                        }
                    } catch (error: VaultUnauthorizedException) {
                        expireSession(error.message)
                        return@launch
                    } catch (_: Exception) {
                        return@launch
                    }
                }
            } finally {
                if (activeVaultPollingRequestId == requestId) {
                    activeVaultPollingRequestId = null
                    vaultStatusPollingJob = null
                }
            }
        }
    }

    private fun cancelVaultStatusPolling() {
        vaultStatusPollingJob?.cancel()
        vaultStatusPollingJob = null
        activeVaultPollingRequestId = null
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
                        senderName = detail.senderName,
                        txHash = detail.txHash,
                        networkFeeWei = detail.networkFeeWei,
                        networkFeeAssetSymbol = detail.networkFeeAssetSymbol,
                        updatedAt = detail.updatedAt
                    ) ?: com.dondone.mobile.data.remittance.RemittanceTransferSummaryPayload(
                        transferId = detail.transferId,
                        direction = detail.direction,
                        status = detail.status,
                        assetSymbol = detail.assetSymbol,
                        amountAtomic = detail.amountAtomic,
                        senderAddress = detail.senderAddress,
                        senderName = detail.senderName,
                        recipientId = detail.recipientId,
                        recipientAlias = detail.recipientAlias,
                        recipientAddress = detail.recipientAddress,
                        txHash = detail.txHash,
                        networkFeeWei = detail.networkFeeWei,
                        networkFeeAssetSymbol = detail.networkFeeAssetSymbol,
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
        syncRemittanceCompletionNotice(detail = detail, shouldAnnounce = detail.isTerminalStatus())
    }

    private fun syncRemittanceCompletionNotice(
        detail: RemittanceTransferDetailPayload?,
        shouldAnnounce: Boolean
    ) {
        val session = _authUiState.value.session ?: return
        if (detail == null || !detail.isTerminalStatus()) {
            return
        }

        val dismissedTransferId = remittanceCompletionNoticeStore.readDismissedTransferId(session.userId)
        if (dismissedTransferId == detail.transferId) {
            if (_remittanceCompletionNoticeUiState.value.transferId == detail.transferId) {
                _remittanceCompletionNoticeUiState.value = RemittanceCompletionNoticeUiState()
            }
            return
        }

        val currentNoticeTransferId = _remittanceCompletionNoticeUiState.value.transferId
        _remittanceCompletionNoticeUiState.value = detail.toCompletionNoticeUiState()
        if (shouldAnnounce && currentNoticeTransferId != detail.transferId) {
            _remittanceActionUiState.value = RemittanceActionUiState(
                message = detail.toCompletionToastMessage(),
                isError = detail.toUiTransferStatus() == TransferStatus.FAILED
            )
        }
    }

    private fun mergeVaultCreateResult(
        summary: VaultSummaryPayload,
        amountAtomic: String,
        result: VaultCreateTransactionPayload
    ) {
        _vaultRemoteState.update { current ->
            val payload = current.payload ?: return@update current
            current.copy(
                mode = VaultRemoteMode.CONTENT,
                payload = payload.copy(
                    latestTransaction = VaultTransactionDetailPayload(
                        requestId = result.requestId,
                        txType = result.txType,
                        status = result.status,
                        walletAddress = summary.walletAddress,
                        vaultAddress = summary.vaultAddress,
                        assetSymbol = summary.assetSymbol,
                        amountAtomic = amountAtomic,
                        shareDelta = null,
                        txHash = null,
                        failureCode = null,
                        createdAt = result.createdAt,
                        updatedAt = result.createdAt,
                        confirmedAt = null
                    )
                )
            )
        }
    }

    private fun mergeVaultTransactionDetail(detail: VaultTransactionDetailPayload) {
        _vaultRemoteState.update { current ->
            val payload = current.payload ?: return@update current
            current.copy(
                mode = VaultRemoteMode.CONTENT,
                payload = payload.copy(latestTransaction = detail)
            )
        }
    }

    private fun syncRemittanceBalanceFromVaultSummary(summary: VaultSummaryPayload?) {
        if (summary == null) {
            return
        }
        _remittanceRemoteState.update { current ->
            val payload = current.payload ?: return@update current
            val currentBalance = payload.balance
            current.copy(
                mode = RemittanceRemoteMode.CONTENT,
                payload = payload.copy(
                    balance = RemittanceWalletBalancePayload(
                        walletAddress = summary.walletAddress,
                        assetSymbol = summary.assetSymbol,
                        assetDecimals = summary.assetDecimals,
                        tokenBalanceAtomic = summary.walletTokenBalanceAtomic,
                        nativeBalanceWei = currentBalance?.nativeBalanceWei ?: "0"
                    )
                )
            )
        }
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
