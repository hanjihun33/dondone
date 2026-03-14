package com.dondone.mobile.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
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
    viewModel: DemoSessionViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Route.HOME
    ) {
        composable(Route.HOME) {
            HomeScreen(
                uiModel = uiState.toHomeUiModel(),
                onOpenTransfer = {
                    viewModel.openTransferFlow()
                    navController.navigate(Route.TRANSFER)
                },
                onOpenAccount = { navController.navigate(Route.ACCOUNT) },
                onOpenFinance = { navController.navigate(Route.FINANCE_HOME) },
                onOpenWage = { navController.navigate(Route.WAGE) },
                onOpenMenu = { navController.navigate(Route.MENU) },
                onOpenWorkproof = { navController.navigate(Route.WORKPROOF) },
                onClockIn = viewModel::clockIn,
                onClockOut = viewModel::clockOut
            )
        }
        composable(Route.WORKPROOF) {
            WorkproofScreen(
                uiModel = uiState.toWorkproofUiModel(),
                onClockIn = viewModel::clockIn,
                onClockOut = viewModel::clockOut,
                onSaveEdit = viewModel::saveWorkproofEdit
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
                    navController.navigate(Route.TRANSFER)
                },
                onOpenWorkproof = { navController.navigate(Route.WORKPROOF) },
                onOpenMenu = { navController.navigate(Route.MENU) }
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
                onResetTransfer = {
                    val returnedHome = navController.popBackStack(Route.HOME, false)
                    if (!returnedHome) {
                        navController.navigate(Route.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
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
                onShiftAsOf = viewModel::shiftAsOfDay,
                onResetSeed = viewModel::resetSeed,
                onOpenWage = { navController.navigate(Route.WAGE) },
                onOpenAccount = { navController.navigate(Route.ACCOUNT) }
            )
        }
    }
}
