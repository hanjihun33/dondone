package com.dondone.mobile.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dondone.mobile.app.session.DemoSessionViewModel
import com.dondone.mobile.feature.finance.presentation.AccountManageScreen
import com.dondone.mobile.feature.finance.presentation.toAccountManageUiModel
import com.dondone.mobile.feature.finance.presentation.FinanceHomeScreen
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
                onOpenWage = { navController.navigate(Route.WAGE) },
                onOpenWorkproof = { navController.navigate(Route.WORKPROOF) },
                onClockIn = viewModel::clockIn,
                onClockOut = viewModel::clockOut
            )
        }
        composable(Route.WORKPROOF) {
            WorkproofScreen(
                uiModel = uiState.toWorkproofUiModel(),
                onClockIn = viewModel::clockIn,
                onClockOut = viewModel::clockOut
            )
        }
        composable(Route.FINANCE_HOME) {
            FinanceHomeScreen(
                uiModel = uiState.toFinanceHomeUiModel(),
                onOpenWage = { navController.navigate(Route.WAGE) },
                onOpenTransfer = {
                    viewModel.openTransferFlow()
                    navController.navigate(Route.TRANSFER)
                },
                onOpenAccount = { navController.navigate(Route.ACCOUNT) }
            )
        }
        composable(Route.WAGE) {
            WageScreen(
                uiModel = uiState.toWageUiModel(),
                onRecordDeposit = { viewModel.recordActualDeposit() },
                onIncreaseDeposit = { viewModel.adjustActualDeposit(50_000) },
                onDecreaseDeposit = { viewModel.adjustActualDeposit(-50_000) },
                onOpenTransfer = {
                    viewModel.openTransferFlow()
                    navController.navigate(Route.TRANSFER)
                }
            )
        }
        composable(Route.TRANSFER) {
            TransferScreen(
                uiModel = uiState.toTransferUiModel(),
                onSelectAccount = viewModel::selectAccount,
                onSelectRecipient = viewModel::selectRecipient,
                onUpdateAmount = viewModel::updateTransferAmount,
                onChangeRecipient = viewModel::showRecipientStep,
                onChangeAccount = viewModel::showAccountStep,
                onSubmitTransfer = viewModel::submitTransfer,
                onConfirmTransfer = viewModel::confirmTransfer,
                onResetTransfer = viewModel::resetTransfer
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
                onResetSeed = viewModel::resetSeed
            )
        }
    }
}
