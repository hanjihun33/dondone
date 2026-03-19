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
import com.dondone.mobile.feature.finance.presentation.AccountManageScreen
import com.dondone.mobile.feature.finance.presentation.FinanceHomeScreen
import com.dondone.mobile.feature.finance.presentation.toAccountManageUiModel
import com.dondone.mobile.feature.finance.presentation.toFinanceHomeUiModel
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
    workproofResetVersion: Int,
    onNavigateToRootTab: (String) -> Unit,
    onWorkproofDetailVisibilityChange: (Boolean) -> Unit,
    onShowToast: (String, BadgeTone) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val advanceRemoteState by viewModel.advanceRemoteState.collectAsStateWithLifecycle()
    val wageRemoteState by viewModel.wageRemoteState.collectAsStateWithLifecycle()
    val remittanceRemoteState by viewModel.remittanceRemoteState.collectAsStateWithLifecycle()
    val wageActionUiState by viewModel.wageActionUiState.collectAsStateWithLifecycle()
    val remittanceActionUiState by viewModel.remittanceActionUiState.collectAsStateWithLifecycle()
    val workproofActionUiState by viewModel.workproofActionUiState.collectAsStateWithLifecycle()
    val selectedAdvanceAmount by viewModel.selectedAdvanceAmount.collectAsStateWithLifecycle()
    val advanceRequestUiState by viewModel.advanceRequestUiState.collectAsStateWithLifecycle()
    val advanceRequestDetailUiState by viewModel.advanceRequestDetailUiState.collectAsStateWithLifecycle()
    val profileUpdateUiState by viewModel.profileUpdateUiState.collectAsStateWithLifecycle()
    val recipientPhoneSearchUiState by viewModel.recipientPhoneSearchUiState.collectAsStateWithLifecycle()
    val menuLaunchRequest by viewModel.menuLaunchRequest.collectAsStateWithLifecycle()

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
            message,
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
                    workproofActionUiState = workproofActionUiState,
                    wageRemoteState = wageRemoteState,
                    remittanceRemoteState = remittanceRemoteState,
                    isAuthenticated = authUiState.isAuthenticated
                ),
                onOpenTransfer = {
                    viewModel.openTransferFlow()
                    navigateWithinApp(Route.TRANSFER, onNavigateToRootTab) { target -> navController.navigate(target) }
                },
                onOpenAccount = { navigateWithinApp(Route.ACCOUNT, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenFinance = { navigateWithinApp(Route.FINANCE_HOME, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenWage = { navigateWithinApp(Route.WAGE, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenMenu = { navigateWithinApp(Route.MENU, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenWorkproof = { navigateWithinApp(Route.WORKPROOF, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onClockIn = viewModel::clockIn,
                onClockOut = viewModel::clockOut
            )
        }
        composable(Route.WORKPROOF) {
            WorkproofScreen(
                uiModel = uiState.toWorkproofUiModel(actionUiState = workproofActionUiState),
                onClockIn = viewModel::clockIn,
                onClockOut = viewModel::clockOut,
                onSaveEdit = viewModel::saveWorkproofEdit,
                resetVersion = workproofResetVersion,
                onDetailVisibilityChange = onWorkproofDetailVisibilityChange
            )
        }
        composable(Route.FINANCE_HOME) {
            FinanceHomeScreen(
                uiModel = uiState.toFinanceHomeUiModel(
                    remoteState = advanceRemoteState,
                    wageRemoteState = wageRemoteState,
                    selectedAdvanceAmount = selectedAdvanceAmount,
                    advanceRequestUiState = advanceRequestUiState,
                    advanceRequestDetailUiState = advanceRequestDetailUiState
                ),
                onSelectAdvanceAmount = viewModel::selectAdvanceAmount,
                onRequestAdvance = viewModel::requestAdvance,
                onClearAdvanceMessage = viewModel::clearAdvanceRequestMessage,
                onOpenAdvanceRequestDetail = viewModel::openAdvanceRequestDetail,
                onCloseAdvanceRequestDetail = viewModel::closeAdvanceRequestDetail
            )
        }
        composable(Route.WAGE) {
            WageScreen(
                uiModel = uiState.toWageUiModel(
                    remoteState = wageRemoteState,
                    actionUiState = wageActionUiState
                ),
                onApplyActualDeposit = viewModel::submitWageDeposit,
                onCreateVerification = viewModel::createWageVerification,
                onRefresh = viewModel::refreshWageRemoteState,
                onOpenTransfer = {
                    viewModel.openTransferFlow()
                    navigateWithinApp(Route.TRANSFER, onNavigateToRootTab) { target -> navController.navigate(target) }
                },
                onOpenWorkproof = { navigateWithinApp(Route.WORKPROOF, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenMenu = {
                    viewModel.openMenuForWageDocuments()
                    navController.navigate(Route.MENU) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(Route.TRANSFER) {
            TransferScreen(
                uiModel = uiState.toTransferUiModel(
                    remoteState = remittanceRemoteState,
                    actionUiState = remittanceActionUiState,
                    isAuthenticated = authUiState.isAuthenticated
                ),
                onSelectAccount = viewModel::selectAccount,
                onSelectDestinationMode = viewModel::selectTransferDestinationMode,
                onSelectRecipient = viewModel::selectRecipient,
                onUpdateRecipientDisplayName = viewModel::updateRecipientDisplayName,
                onUpdateAmount = viewModel::updateTransferAmount,
                onCreateRecipient = viewModel::createRemittanceRecipient,
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
                    recipientPhoneSearchUiState = recipientPhoneSearchUiState
                ),
                actionUiState = remittanceActionUiState,
                onSelectAccount = viewModel::selectAccount,
                onAddRecipient = viewModel::addRecipientFromAccountManage,
                onUpdateRecipient = viewModel::updateRecipientFromAccountManage,
                onSearchRecipientsByPhone = viewModel::searchRecipientsByPhone,
                onClearPhoneSearch = viewModel::clearRecipientPhoneSearch
            )
        }
        composable(Route.MENU) {
            MenuScreen(
                uiModel = uiState.toMenuUiModel(
                    session = authUiState.session,
                    remittanceRemoteState = remittanceRemoteState
                ),
                launchRequest = menuLaunchRequest,
                profileUpdateUiState = profileUpdateUiState,
                onOpenWage = { navigateWithinApp(Route.WAGE, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenAccount = { navigateWithinApp(Route.ACCOUNT, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onConsumeLaunchRequest = viewModel::consumeMenuLaunchRequest,
                onUpdateProfile = viewModel::updateProfile,
                onClearProfileUpdateMessage = viewModel::clearProfileUpdateMessage,
                onLogout = viewModel::logout,
                onShowToast = onShowToast
            )
        }
    }
}
