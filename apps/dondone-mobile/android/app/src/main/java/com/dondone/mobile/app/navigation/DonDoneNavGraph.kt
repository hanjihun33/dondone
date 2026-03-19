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
    val remittanceRemoteState by viewModel.remittanceRemoteState.collectAsStateWithLifecycle()
    val remittanceActionUiState by viewModel.remittanceActionUiState.collectAsStateWithLifecycle()
    val workproofActionUiState by viewModel.workproofActionUiState.collectAsStateWithLifecycle()
    val workproofPdfPreviewUiState by viewModel.workproofPdfPreviewUiState.collectAsStateWithLifecycle()
    val workproofPdfCreateUiState by viewModel.workproofPdfCreateUiState.collectAsStateWithLifecycle()
    val workproofPdfFileUiState by viewModel.workproofPdfFileUiState.collectAsStateWithLifecycle()
    val selectedAdvanceAmount by viewModel.selectedAdvanceAmount.collectAsStateWithLifecycle()
    val advanceRequestUiState by viewModel.advanceRequestUiState.collectAsStateWithLifecycle()
    val advanceRequestDetailUiState by viewModel.advanceRequestDetailUiState.collectAsStateWithLifecycle()
    val profileUpdateUiState by viewModel.profileUpdateUiState.collectAsStateWithLifecycle()
    val recipientPhoneSearchUiState by viewModel.recipientPhoneSearchUiState.collectAsStateWithLifecycle()

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
                pdfPreviewUiState = workproofPdfPreviewUiState,
                pdfCreateUiState = workproofPdfCreateUiState,
                pdfFileUiState = workproofPdfFileUiState,
                onClockIn = viewModel::clockIn,
                onClockOut = viewModel::clockOut,
                onSaveEdit = viewModel::saveWorkproofEdit,
                onRefreshPdfPreview = viewModel::previewWorkproofPdf,
                onClearPdfPreview = viewModel::clearWorkproofPdfPreview,
                onCreateWorkproofPdf = viewModel::createWorkproofPdf,
                onClearPdfCreateState = viewModel::clearWorkproofPdfCreateState,
                onOpenWorkproofPdf = viewModel::openWorkproofPdf,
                onShareWorkproofPdf = viewModel::shareWorkproofPdf,
                onClearPdfFileState = viewModel::clearWorkproofPdfFileState,
                resetVersion = workproofResetVersion,
                onDetailVisibilityChange = onWorkproofDetailVisibilityChange
            )
        }
        composable(Route.FINANCE_HOME) {
            FinanceHomeScreen(
                uiModel = uiState.toFinanceHomeUiModel(
                    remoteState = advanceRemoteState,
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
                uiModel = uiState.toWageUiModel(),
                onApplyActualDeposit = viewModel::setActualDeposit,
                onOpenTransfer = {
                    viewModel.openTransferFlow()
                    navigateWithinApp(Route.TRANSFER, onNavigateToRootTab) { target -> navController.navigate(target) }
                },
                onOpenWorkproof = { navigateWithinApp(Route.WORKPROOF, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenMenu = { navigateWithinApp(Route.MENU, onNavigateToRootTab) { target -> navController.navigate(target) } }
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
                    remittanceRemoteState = remittanceRemoteState,
                    workproofPdfCreateUiState = workproofPdfCreateUiState
                ),
                workproofPdfFileUiState = workproofPdfFileUiState,
                profileUpdateUiState = profileUpdateUiState,
                onOpenWage = { navigateWithinApp(Route.WAGE, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenAccount = { navigateWithinApp(Route.ACCOUNT, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenWorkproofPdf = viewModel::openWorkproofPdf,
                onShareWorkproofPdf = viewModel::shareWorkproofPdf,
                onClearPdfFileState = viewModel::clearWorkproofPdfFileState,
                onUpdateProfile = viewModel::updateProfile,
                onClearProfileUpdateMessage = viewModel::clearProfileUpdateMessage,
                onLogout = viewModel::logout,
                onShowToast = onShowToast
            )
        }
    }
}
