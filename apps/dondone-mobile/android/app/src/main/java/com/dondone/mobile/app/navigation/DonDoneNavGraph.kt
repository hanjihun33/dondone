package com.dondone.mobile.app.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dondone.mobile.app.session.DemoSessionViewModel
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.LocalAppLanguage
import com.dondone.mobile.core.i18n.translate
import com.dondone.mobile.feature.finance.presentation.AccountManageScreen
import com.dondone.mobile.feature.finance.presentation.FinanceHomeScreen
import com.dondone.mobile.feature.finance.presentation.TransactionHistoryDetailScreen
import com.dondone.mobile.feature.finance.presentation.TransactionHistoryEditScreen
import com.dondone.mobile.feature.finance.presentation.TransactionHistoryMainScreen
import com.dondone.mobile.feature.finance.presentation.toAccountManageUiModel
import com.dondone.mobile.feature.finance.presentation.toFinanceHomeUiModel
import com.dondone.mobile.feature.finance.presentation.toTransactionHistoryDetailUiModel
import com.dondone.mobile.feature.finance.presentation.toTransactionHistoryEditUiModel
import com.dondone.mobile.feature.finance.presentation.toTransactionHistoryMainUiModel
import com.dondone.mobile.feature.home.presentation.HomeScreen
import com.dondone.mobile.feature.home.presentation.toHomeUiModel
import com.dondone.mobile.feature.menu.presentation.MenuScreen
import com.dondone.mobile.feature.menu.presentation.toMenuUiModel
import com.dondone.mobile.feature.remittance.presentation.TransferScreen
import com.dondone.mobile.feature.remittance.presentation.toTransferUiModel
import com.dondone.mobile.feature.wage.presentation.WageScreen
import com.dondone.mobile.feature.wage.presentation.toWageUiModel
import com.dondone.mobile.feature.workproof.presentation.WorkproofScreen
import com.dondone.mobile.feature.workproof.presentation.toWorkproofUiModel

@Composable
fun DonDoneNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: DemoSessionViewModel,
    appLanguage: AppLanguage,
    workproofResetVersion: Int,
    onNavigateToRootTab: (String) -> Unit,
    onWorkproofDetailVisibilityChange: (Boolean) -> Unit,
    onOpenWorkerRegistrationCode: () -> Unit,
    onShowToast: (String, BadgeTone) -> Unit
) {
    val appLanguage = LocalAppLanguage.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val advanceRemoteState by viewModel.advanceRemoteState.collectAsStateWithLifecycle()
    val workproofRemoteState by viewModel.workproofRemoteState.collectAsStateWithLifecycle()
    val wageRemoteState by viewModel.wageRemoteState.collectAsStateWithLifecycle()
    val remittanceRemoteState by viewModel.remittanceRemoteState.collectAsStateWithLifecycle()
    val vaultRemoteState by viewModel.vaultRemoteState.collectAsStateWithLifecycle()
    val wageActionUiState by viewModel.wageActionUiState.collectAsStateWithLifecycle()
    val remittanceActionUiState by viewModel.remittanceActionUiState.collectAsStateWithLifecycle()
    val remittanceCompletionNoticeUiState by viewModel.remittanceCompletionNoticeUiState.collectAsStateWithLifecycle()
    val vaultActionUiState by viewModel.vaultActionUiState.collectAsStateWithLifecycle()
    val workproofActionUiState by viewModel.workproofActionUiState.collectAsStateWithLifecycle()
    val workproofCurrentLocationUiState by viewModel.workproofCurrentLocationUiState.collectAsStateWithLifecycle()
    val workproofPdfPreviewUiState by viewModel.workproofPdfPreviewUiState.collectAsStateWithLifecycle()
    val workproofPdfCreateUiState by viewModel.workproofPdfCreateUiState.collectAsStateWithLifecycle()
    val workproofPdfFileUiState by viewModel.workproofPdfFileUiState.collectAsStateWithLifecycle()
    val selectedAdvanceAmount by viewModel.selectedAdvanceAmount.collectAsStateWithLifecycle()
    val selectedVaultAmount by viewModel.selectedVaultAmount.collectAsStateWithLifecycle()
    val selectedVaultActionType by viewModel.selectedVaultActionType.collectAsStateWithLifecycle()
    val advanceRequestUiState by viewModel.advanceRequestUiState.collectAsStateWithLifecycle()
    val advanceRequestDetailUiState by viewModel.advanceRequestDetailUiState.collectAsStateWithLifecycle()
    val profileUpdateUiState by viewModel.profileUpdateUiState.collectAsStateWithLifecycle()
    val recipientPhoneSearchUiState by viewModel.recipientPhoneSearchUiState.collectAsStateWithLifecycle()
    val transactionMetadataOverrides by viewModel.transactionMetadataOverrides.collectAsStateWithLifecycle()
    val menuLaunchRequest by viewModel.menuLaunchRequest.collectAsStateWithLifecycle()
    val remittanceLaunchRequest by viewModel.remittanceLaunchRequest.collectAsStateWithLifecycle()
    val workproofLaunchRequest by viewModel.workproofLaunchRequest.collectAsStateWithLifecycle()

    LaunchedEffect(workproofActionUiState.message, workproofActionUiState.isError) {
        val message = workproofActionUiState.message ?: return@LaunchedEffect
        onShowToast(
            message,
            if (workproofActionUiState.isError) BadgeTone.Warning else BadgeTone.Success
        )
        viewModel.clearWorkproofActionMessage()
    }

    LaunchedEffect(remittanceActionUiState.message, remittanceActionUiState.isError) {
        val message = remittanceActionUiState.message ?: return@LaunchedEffect
        onShowToast(
            appLanguage.translate(message),
            if (remittanceActionUiState.isError) BadgeTone.Warning else BadgeTone.Success
        )
        viewModel.clearRemittanceActionMessage()
    }

    LaunchedEffect(wageActionUiState.message, wageActionUiState.isError) {
        val message = wageActionUiState.message ?: return@LaunchedEffect
        onShowToast(
            message,
            if (wageActionUiState.isError) BadgeTone.Warning else BadgeTone.Success
        )
        viewModel.clearWageActionMessage()
    }

    LaunchedEffect(vaultActionUiState.message, vaultActionUiState.isError) {
        val message = vaultActionUiState.message ?: return@LaunchedEffect
        onShowToast(
            message,
            if (vaultActionUiState.isError) BadgeTone.Warning else BadgeTone.Success
        )
        viewModel.clearVaultActionMessage()
    }

    LaunchedEffect(remittanceLaunchRequest?.requestId) {
        remittanceLaunchRequest ?: return@LaunchedEffect
        navController.navigate(Route.HOME) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = false
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
        viewModel.consumeRemittanceLaunchRequest()
    }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Route.HOME,
        // Navigation Compose applies a default crossfade/size transform. Disable it so
        // route changes render as immediate swaps instead of overlapping old/new screens.
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
        sizeTransform = { null }
    ) {
        composable(Route.HOME) {
            HomeScreen(
                uiModel = uiState.toHomeUiModel(
                    language = appLanguage,
                    workproofActionUiState = workproofActionUiState,
                    wageRemoteState = wageRemoteState,
                    remittanceRemoteState = remittanceRemoteState,
                    remittanceCompletionNoticeUiState = remittanceCompletionNoticeUiState,
                    isAuthenticated = authUiState.isAuthenticated,
                    session = authUiState.session,
                    workproofRemoteState = workproofRemoteState
                ),
                onOpenTransfer = {
                    if (viewModel.openTransferFlow()) {
                        navigateWithinApp(Route.TRANSFER, onNavigateToRootTab) { target ->
                            navController.navigate(target)
                        }
                    }
                },
                onOpenAccount = { navigateWithinApp(Route.ACCOUNT, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenFinance = { navigateWithinApp(Route.FINANCE_HOME, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenWage = { navigateWithinApp(Route.WAGE, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenMenu = { navigateWithinApp(Route.MENU, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenWorkproof = { navigateWithinApp(Route.WORKPROOF, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onDismissRemittanceCompletionNotice = viewModel::dismissRemittanceCompletionNotice,
                onClockIn = viewModel::clockIn,
                onClockOut = viewModel::clockOut
            )
        }
        composable(Route.WORKPROOF) {
            WorkproofScreen(
                uiModel = uiState.toWorkproofUiModel(
                    language = appLanguage,
                    actionUiState = workproofActionUiState,
                    remoteState = workproofRemoteState,
                    isAuthenticated = authUiState.isAuthenticated,
                    currentLocationUiState = workproofCurrentLocationUiState
                ),
                pdfPreviewUiState = workproofPdfPreviewUiState,
                pdfCreateUiState = workproofPdfCreateUiState,
                pdfFileUiState = workproofPdfFileUiState,
                onClockIn = viewModel::clockIn,
                onClockOut = viewModel::clockOut,
                onRefreshCurrentLocation = viewModel::refreshWorkproofCurrentLocation,
                onSaveEdit = viewModel::saveWorkproofEdit,
                onRefreshPdfPreview = viewModel::previewWorkproofPdf,
                onClearPdfPreview = viewModel::clearWorkproofPdfPreview,
                onCreateWorkproofPdf = viewModel::createWorkproofPdf,
                onClearPdfCreateState = viewModel::clearWorkproofPdfCreateState,
                onOpenWorkproofPdf = viewModel::openWorkproofPdf,
                onShareWorkproofPdf = viewModel::shareWorkproofPdf,
                onClearPdfFileState = viewModel::clearWorkproofPdfFileState,
                launchRequest = workproofLaunchRequest,
                onConsumeLaunchRequest = viewModel::consumeWorkproofLaunchRequest,
                resetVersion = workproofResetVersion,
                onDetailVisibilityChange = onWorkproofDetailVisibilityChange
            )
        }
        composable(Route.FINANCE_HOME) {
            FinanceHomeScreen(
                uiModel = uiState.toFinanceHomeUiModel(
                    remoteState = advanceRemoteState,
                    wageRemoteState = wageRemoteState,
                    workproofRemoteState = workproofRemoteState,
                    remittanceRemoteState = remittanceRemoteState,
                    vaultRemoteState = vaultRemoteState,
                    language = appLanguage,
                    selectedAdvanceAmount = selectedAdvanceAmount,
                    selectedVaultAmount = selectedVaultAmount,
                    selectedVaultActionType = selectedVaultActionType,
                    advanceRequestUiState = advanceRequestUiState,
                    advanceRequestDetailUiState = advanceRequestDetailUiState,
                    vaultActionUiState = vaultActionUiState
                ),
                onSelectAdvanceAmount = viewModel::selectAdvanceAmount,
                onRequestAdvance = viewModel::requestAdvance,
                onClearAdvanceMessage = viewModel::clearAdvanceRequestMessage,
                onSelectVaultAction = viewModel::selectVaultAction,
                onSelectVaultAmount = viewModel::selectVaultAmount,
                onSubmitVaultAction = viewModel::submitVaultAction,
                onClearVaultMessage = viewModel::clearVaultActionMessage,
                onOpenAdvanceRequestDetail = viewModel::openAdvanceRequestDetail,
                onCloseAdvanceRequestDetail = viewModel::closeAdvanceRequestDetail,
                onOpenWorkproof = {
                    navigateWithinApp(Route.WORKPROOF, onNavigateToRootTab) { target ->
                        navController.navigate(target)
                    }
                },
                onOpenWorkerRegistrationCode = onOpenWorkerRegistrationCode
            )
        }
        composable(Route.WAGE) {
            WageScreen(
                uiModel = uiState.toWageUiModel(
                    remoteState = wageRemoteState,
                    actionUiState = wageActionUiState,
                    language = appLanguage
                ),
                onApplyActualDeposit = viewModel::submitWageDeposit,
                onRefresh = viewModel::refreshWageRemoteState,
                onNavigateMenu = { openWorkerRegistrationSheet ->
                    onNavigateToRootTab(Route.MENU)
                    if (openWorkerRegistrationSheet) {
                        onOpenWorkerRegistrationCode()
                    }
                },
                onOpenWorkproofPdfCreation = {
                    viewModel.openWorkproofPdfCreation()
                    navigateWithinApp(Route.WORKPROOF, onNavigateToRootTab) { target -> navController.navigate(target) }
                }
            )
        }
        composable(Route.TRANSFER) {
            TransferScreen(
                uiModel = uiState.toTransferUiModel(
                    remoteState = remittanceRemoteState,
                    actionUiState = remittanceActionUiState,
                    isAuthenticated = authUiState.isAuthenticated,
                    language = appLanguage,
                    recipientPhoneSearchUiState = recipientPhoneSearchUiState
                ),
                actionUiState = remittanceActionUiState,
                onSelectAccount = viewModel::selectAccount,
                onSelectDestinationMode = viewModel::selectTransferDestinationMode,
                onSelectRecipient = viewModel::selectRecipient,
                onUpdateRecipientDisplayName = viewModel::updateRecipientDisplayName,
                onUpdateAmount = viewModel::updateTransferAmount,
                onAddRecipient = viewModel::addRecipientFromTransfer,
                onSearchRecipientsByPhone = viewModel::searchRecipientsByPhone,
                onClearPhoneSearch = viewModel::clearRecipientPhoneSearch,
                onRefreshRemittance = viewModel::refreshRemittanceRemoteState,
                onChangeRecipient = viewModel::showRecipientStepFromAmount,
                onChangeAccountFromAmount = viewModel::showAccountStepFromAmount,
                onSubmitTransfer = viewModel::submitTransfer,
                onDismissTransferConfirmation = viewModel::dismissTransferConfirmation,
                onConfirmTransfer = viewModel::confirmTransfer,
                onResetTransfer = {
                    viewModel.resetTransfer()
                    navController.navigate(Route.HOME) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                            saveState = false
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            )
        }
        composable(Route.ACCOUNT) {
            AccountManageScreen(
                uiModel = uiState.toAccountManageUiModel(
                    remittanceRemoteState = remittanceRemoteState,
                    isAuthenticated = authUiState.isAuthenticated,
                    recipientPhoneSearchUiState = recipientPhoneSearchUiState,
                    language = appLanguage
                ),
                actionUiState = remittanceActionUiState,
                onOpenTransactionHistory = { accountId ->
                    navController.navigate(Route.transactionHistory(accountId))
                },
                onAddRecipient = viewModel::addRecipientFromAccountManage,
                onUpdateRecipient = viewModel::updateRecipientFromAccountManage,
                onSearchRecipientsByPhone = viewModel::searchRecipientsByPhone,
                onClearPhoneSearch = viewModel::clearRecipientPhoneSearch
            )
        }
        composable(Route.TRANSACTION_HISTORY) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString(Route.ARG_ACCOUNT_ID) ?: return@composable
            TransactionHistoryMainScreen(
                uiModel = uiState.toTransactionHistoryMainUiModel(
                    accountId = accountId,
                    remittanceRemoteState = remittanceRemoteState,
                    language = appLanguage,
                    isAuthenticated = authUiState.isAuthenticated,
                    overrides = transactionMetadataOverrides
                ),
                onOpenTransaction = { transactionId ->
                    navController.navigate(Route.transactionHistoryDetail(accountId, transactionId))
                }
            )
        }
        composable(Route.TRANSACTION_HISTORY_DETAIL) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString(Route.ARG_ACCOUNT_ID) ?: return@composable
            val transactionId = backStackEntry.arguments?.getString(Route.ARG_TRANSACTION_ID) ?: return@composable
            TransactionHistoryDetailScreen(
                uiModel = uiState.toTransactionHistoryDetailUiModel(
                    accountId = accountId,
                    transactionId = transactionId,
                    remittanceRemoteState = remittanceRemoteState,
                    language = appLanguage,
                    isAuthenticated = authUiState.isAuthenticated,
                    overrides = transactionMetadataOverrides
                ),
                onEdit = {
                    navController.navigate(Route.transactionHistoryEdit(accountId, transactionId))
                }
            )
        }
        composable(Route.TRANSACTION_HISTORY_EDIT) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString(Route.ARG_ACCOUNT_ID) ?: return@composable
            val transactionId = backStackEntry.arguments?.getString(Route.ARG_TRANSACTION_ID) ?: return@composable
            TransactionHistoryEditScreen(
                uiModel = uiState.toTransactionHistoryEditUiModel(
                    accountId = accountId,
                    transactionId = transactionId,
                    remittanceRemoteState = remittanceRemoteState,
                    language = appLanguage,
                    isAuthenticated = authUiState.isAuthenticated,
                    overrides = transactionMetadataOverrides
                ),
                onSave = { category, memo ->
                    viewModel.updateTransactionMetadata(transactionId, category, memo)
                    navController.popBackStack()
                }
            )
        }
        composable(Route.MENU) {
            MenuScreen(
                uiModel = uiState.toMenuUiModel(
                    session = authUiState.session,
                    remittanceRemoteState = remittanceRemoteState,
                    workproofPdfCreateUiState = workproofPdfCreateUiState,
                    language = appLanguage,
                    workproofRemoteState = workproofRemoteState
                ),
                workproofPdfFileUiState = workproofPdfFileUiState,
                launchRequest = menuLaunchRequest,
                profileUpdateUiState = profileUpdateUiState,
                selectedLanguage = appLanguage,
                onOpenWage = { navigateWithinApp(Route.WAGE, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenAccount = { navigateWithinApp(Route.ACCOUNT, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenWorkproofPdfCreation = {
                    viewModel.openWorkproofPdfCreation()
                    navigateWithinApp(Route.WORKPROOF, onNavigateToRootTab) { target -> navController.navigate(target) }
                },
                onOpenWorkproofPdf = viewModel::openWorkproofPdf,
                onShareWorkproofPdf = viewModel::shareWorkproofPdf,
                onClearPdfFileState = viewModel::clearWorkproofPdfFileState,
                onConsumeLaunchRequest = viewModel::consumeMenuLaunchRequest,
                onUpdateProfile = viewModel::updateProfile,
                onClearProfileUpdateMessage = viewModel::clearProfileUpdateMessage,
                onSelectLanguage = { language -> viewModel.updateAppLanguage(language.code) },
                onOpenWorkerRegistrationCode = onOpenWorkerRegistrationCode,
                onLogout = viewModel::logout,
                onShowToast = onShowToast
            )
        }
    }
}
