package com.dondone.mobile.app.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dondone.mobile.app.session.DemoSessionViewModel
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
    onWorkproofDetailVisibilityChange: (Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                uiModel = uiState.toHomeUiModel(),
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
                uiModel = uiState.toWorkproofUiModel(),
                onClockIn = viewModel::clockIn,
                onClockOut = viewModel::clockOut,
                onSaveEdit = viewModel::saveWorkproofEdit,
                resetVersion = workproofResetVersion,
                onDetailVisibilityChange = onWorkproofDetailVisibilityChange
            )
        }
        composable(Route.FINANCE_HOME) {
            FinanceHomeScreen(
                uiModel = uiState.toFinanceHomeUiModel()
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
                uiModel = uiState.toTransferUiModel(),
                onSelectAccount = viewModel::selectAccount,
                onSelectDestinationMode = viewModel::selectTransferDestinationMode,
                onSelectRecipient = viewModel::selectRecipient,
                onUpdateRecipientDisplayName = viewModel::updateRecipientDisplayName,
                onUpdateAmount = viewModel::updateTransferAmount,
                onChangeRecipient = viewModel::showRecipientStepFromAmount,
                onChangeAccountFromRecipient = viewModel::showAccountStepFromRecipient,
                onChangeAccountFromAmount = viewModel::showAccountStepFromAmount,
                onSubmitTransfer = viewModel::submitTransfer,
                onDismissTransferConfirmation = viewModel::dismissTransferConfirmation,
                onConfirmTransfer = viewModel::confirmTransfer,
                onResetTransfer = { navigateWithinApp(Route.HOME, onNavigateToRootTab) { target -> navController.navigate(target) } }
            )
        }
        composable(Route.ACCOUNT) {
            AccountManageScreen(
                uiModel = uiState.toAccountManageUiModel(),
                onSelectAccount = viewModel::selectAccount
            )
        }
        composable(Route.MENU) {
            MenuScreen(
                uiModel = uiState.toMenuUiModel(),
                onOpenWage = { navigateWithinApp(Route.WAGE, onNavigateToRootTab) { target -> navController.navigate(target) } },
                onOpenAccount = { navigateWithinApp(Route.ACCOUNT, onNavigateToRootTab) { target -> navController.navigate(target) } }
            )
        }
    }
}
