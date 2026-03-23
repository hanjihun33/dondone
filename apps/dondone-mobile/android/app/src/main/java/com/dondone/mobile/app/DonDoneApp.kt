package com.dondone.mobile.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dondone.mobile.app.navigation.AppBackAction
import com.dondone.mobile.app.navigation.DonDoneNavGraph
import com.dondone.mobile.app.navigation.Route
import com.dondone.mobile.app.navigation.navigateToRootTab
import com.dondone.mobile.app.navigation.resolveAppBackAction
import com.dondone.mobile.app.navigation.resolveScreenChrome
import com.dondone.mobile.app.navigation.shouldResetWorkproofUiState
import com.dondone.mobile.app.navigation.showTransferStep
import com.dondone.mobile.app.session.DemoSessionViewModel
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.designsystem.DawnSurface
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.DawnWarning
import com.dondone.mobile.core.designsystem.DonDoneToastHost
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SecondaryActionButton
import com.dondone.mobile.core.designsystem.rememberDonDoneToastState
import com.dondone.mobile.domain.model.DemoInfo
import com.dondone.mobile.feature.auth.presentation.LoginLoadingScreen
import com.dondone.mobile.feature.auth.presentation.LoginScreen

private const val WORKER_REGISTRATION_CODE_MAX_LENGTH = 32
private const val WORKER_REGISTRATION_CODE_MIN_LENGTH = 8

@Composable
fun DonDoneApp(
    viewModel: DemoSessionViewModel
) {
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()

    if (authUiState.isRestoring) {
        LoginLoadingScreen()
        return
    }

    if (!authUiState.isAuthenticated) {
        LoginScreen(
            uiState = authUiState,
            onLogin = viewModel::login,
            onSignup = viewModel::signup,
            onFieldEdited = viewModel::clearAuthError
        )
        return
    }

    AuthenticatedDonDoneAppShell(viewModel = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedDonDoneAppShell(
    viewModel: DemoSessionViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val remittanceActionUiState by viewModel.remittanceActionUiState.collectAsStateWithLifecycle()
    val workerRegistrationCodeUiState by viewModel.workerRegistrationCodeUiState.collectAsStateWithLifecycle()
    val toastState = rememberDonDoneToastState()
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentRoute = currentDestination?.route.orEmpty()
    val remittance = uiState.remittance
    val workproofShellState = rememberWorkproofShellState(currentRoute)
    var isWorkerRegistrationSheetVisible by rememberSaveable { mutableStateOf(false) }
    var workerRegistrationCodeInput by rememberSaveable { mutableStateOf("") }
    val workerRegistrationCodeValidationMessage =
        workerRegistrationCodeValidationMessage(workerRegistrationCodeInput)
    val isWorkerRegistrationSavable =
        workerRegistrationCodeInput.isNotBlank() && workerRegistrationCodeValidationMessage == null

    val chrome = resolveScreenChrome(
        route = currentRoute,
        transferStep = remittance.flowStep,
        transferStatus = remittance.status,
        isWorkproofDetailVisible = workproofShellState.isDetailVisible
    )
    val topBarState = resolveAppTopBarState(
        currentRoute = currentRoute,
        showRootTabs = chrome.showRootTabs,
        showSettingsAction = chrome.showSettingsAction,
        headerTitle = chrome.headerTitle,
        headerDateText = uiState.demo.toHeaderDateTextOrNull(showDate = chrome.showDate)
    )
    val rootTabs = currentDestination.toRootTabUiStates(currentRoute)

    fun handleBack() {
        when (
            val action = resolveAppBackAction(
                currentRoute = currentRoute,
                remittance = remittance,
                isRemittanceSubmitting = remittanceActionUiState.isSubmitting,
                remittanceSubmittingAction = remittanceActionUiState.submittingAction
            )
        ) {
            AppBackAction.NavigateUp -> navController.navigateUp()
            AppBackAction.DismissTransferConfirmation -> {
                viewModel.dismissTransferConfirmation()
            }

            AppBackAction.Ignore -> Unit

            is AppBackAction.ShowTransferStep -> {
                viewModel.showTransferStep(action.step)
            }
        }
    }

    BackHandler(
        enabled = currentRoute == Route.TRANSFER,
        onBack = ::handleBack
    )

    LaunchedEffect(workerRegistrationCodeUiState.message, workerRegistrationCodeUiState.isError) {
        val message = workerRegistrationCodeUiState.message ?: return@LaunchedEffect
        toastState.show(
            message = message,
            tone = if (workerRegistrationCodeUiState.isError) BadgeTone.Warning else BadgeTone.Success
        )
        if (!workerRegistrationCodeUiState.isError) {
            workerRegistrationCodeInput = ""
            isWorkerRegistrationSheetVisible = false
        }
        viewModel.clearWorkerRegistrationCodeMessage()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ChromeSurface,
            topBar = {
                AppTopBar(
                    state = topBarState,
                    onBack = ::handleBack,
                    onMenuClick = {
                        workerRegistrationCodeInput = ""
                        isWorkerRegistrationSheetVisible = true
                    }
                )
            },
            bottomBar = {
                RootBottomBar(
                    visible = chrome.showRootTabs,
                    tabs = rootTabs,
                    onTabClick = { route -> navController.navigateToRootTab(route) }
                )
            }
        ) { innerPadding ->
            DonDoneNavGraph(
                modifier = Modifier.padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                ),
                navController = navController,
                viewModel = viewModel,
                workproofResetVersion = workproofShellState.resetVersion,
                onNavigateToRootTab = { route -> navController.navigateToRootTab(route) },
                onWorkproofDetailVisibilityChange = workproofShellState.onDetailVisibilityChange,
                onShowToast = { message, tone ->
                    toastState.show(message = message, tone = tone)
                }
            )
        }

        DonDoneToastHost(
            state = toastState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 28.dp)
        )

        if (isWorkerRegistrationSheetVisible) {
            WorkerRegistrationCodeSheet(
                registrationCode = workerRegistrationCodeInput,
                validationMessage = workerRegistrationCodeValidationMessage,
                onRegistrationCodeChange = { nextValue ->
                    workerRegistrationCodeInput = normalizeWorkerRegistrationCodeInput(nextValue)
                },
                isSubmitting = workerRegistrationCodeUiState.isSubmitting,
                isSaveEnabled = isWorkerRegistrationSavable,
                onDismiss = {
                    if (!workerRegistrationCodeUiState.isSubmitting) {
                        isWorkerRegistrationSheetVisible = false
                        viewModel.clearWorkerRegistrationCodeMessage()
                    }
                },
                onSave = {
                    viewModel.redeemWorkerRegistrationCode(workerRegistrationCodeInput)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerRegistrationCodeSheet(
    registrationCode: String,
    validationMessage: String?,
    onRegistrationCodeChange: (String) -> Unit,
    isSubmitting: Boolean,
    isSaveEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DawnSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "근로자 등록 코드 입력",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                value = registrationCode,
                onValueChange = onRegistrationCodeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                singleLine = true,
                isError = validationMessage != null,
                label = { Text("등록 코드") },
                placeholder = { Text("예: WORKER-AB12-CD34") },
                supportingText = {
                    Text(
                        text = validationMessage ?: "영문 대문자, 숫자, 하이픈(-)만 입력할 수 있어요.",
                        color = if (validationMessage != null) DawnWarning else DawnTextSubtle
                    )
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                SecondaryActionButton(
                    text = "닫기",
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f)
                )
                PrimaryActionButton(
                    text = if (isSubmitting) "등록 중.." else "등록",
                    onClick = onSave,
                    enabled = !isSubmitting && isSaveEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }
        }
    }
}

internal fun normalizeWorkerRegistrationCodeInput(
    rawValue: String
): String {
    return rawValue
        .uppercase()
        .take(WORKER_REGISTRATION_CODE_MAX_LENGTH)
}

internal fun workerRegistrationCodeValidationMessage(
    registrationCode: String
): String? {
    if (registrationCode.isBlank()) {
        return null
    }
    if (registrationCode.any { !it.isDigit() && it !in 'A'..'Z' && it != '-' }) {
        return "영문 대문자, 숫자, 하이픈(-)만 입력할 수 있어요."
    }
    if (registrationCode.length < WORKER_REGISTRATION_CODE_MIN_LENGTH) {
        return "등록 코드는 8자 이상이어야 해요."
    }
    if (registrationCode.length > WORKER_REGISTRATION_CODE_MAX_LENGTH) {
        return "등록 코드는 32자 이하여야 해요."
    }
    return null
}

@Composable
private fun rememberWorkproofShellState(
    currentRoute: String
): WorkproofShellState {
    var isDetailVisible by rememberSaveable { mutableStateOf(false) }
    var resetVersion by rememberSaveable { mutableStateOf(0) }
    var previousRoute by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(currentRoute) {
        if (shouldResetWorkproofUiState(previousRoute, currentRoute)) {
            isDetailVisible = false
            resetVersion += 1
        }
        previousRoute = currentRoute
    }

    return WorkproofShellState(
        isDetailVisible = isDetailVisible,
        resetVersion = resetVersion,
        onDetailVisibilityChange = { visible ->
            isDetailVisible = visible
        }
    )
}

private fun DemoInfo.toHeaderDateTextOrNull(showDate: Boolean): String? {
    if (!showDate) {
        return null
    }

    return "$year.${month.toString().padStart(2, '0')}.${asOfDay.toString().padStart(2, '0')}"
}

private data class WorkproofShellState(
    val isDetailVisible: Boolean,
    val resetVersion: Int,
    val onDetailVisibilityChange: (Boolean) -> Unit
)
