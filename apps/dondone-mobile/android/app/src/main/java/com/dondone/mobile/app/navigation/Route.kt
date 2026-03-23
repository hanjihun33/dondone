package com.dondone.mobile.app.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

data class MainTab(
    val rootRoute: String,
    val label: String
)

object Route {
    const val ARG_ACCOUNT_ID = "accountId"
    const val ARG_TRANSACTION_ID = "transactionId"

    const val HOME = "home"
    const val WORKPROOF = "workproof"
    const val FINANCE_HOME = "finance"
    const val WAGE = "finance/wage"
    const val TRANSFER = "finance/transfer"
    const val ACCOUNT = "finance/account"
    const val TRANSACTION_HISTORY = "finance/account/{accountId}/history"
    const val TRANSACTION_HISTORY_DETAIL = "finance/account/{accountId}/history/{transactionId}"
    const val TRANSACTION_HISTORY_EDIT = "finance/account/{accountId}/history/{transactionId}/edit"
    const val MENU = "menu"

    fun transactionHistory(accountId: String): String =
        "finance/account/$accountId/history"

    fun transactionHistoryDetail(accountId: String, transactionId: String): String =
        "finance/account/$accountId/history/$transactionId"

    fun transactionHistoryEdit(accountId: String, transactionId: String): String =
        "finance/account/$accountId/history/$transactionId/edit"
}

val mainTabs = listOf(
    MainTab(Route.HOME, "Home"),
    MainTab(Route.FINANCE_HOME, "Finance"),
    MainTab(Route.WORKPROOF, "Workproof"),
    MainTab(Route.MENU, "Menu")
)

fun isRootRoute(route: String): Boolean = route in setOf(
    Route.HOME,
    Route.FINANCE_HOME,
    Route.WORKPROOF,
    Route.MENU
)

fun isTransactionHistoryRoute(route: String): Boolean = route in setOf(
    Route.TRANSACTION_HISTORY,
    Route.TRANSACTION_HISTORY_DETAIL,
    Route.TRANSACTION_HISTORY_EDIT
)

fun shouldResetWorkproofUiState(
    previousRoute: String?,
    nextRoute: String
): Boolean = previousRoute == Route.WORKPROOF && nextRoute != Route.WORKPROOF

fun routeTitle(route: String): String = when (route) {
    Route.WAGE -> "Wage"
    Route.TRANSFER -> "Transfer"
    Route.ACCOUNT -> "Accounts"
    Route.TRANSACTION_HISTORY -> "Transactions"
    Route.TRANSACTION_HISTORY_DETAIL -> "Transaction Detail"
    Route.TRANSACTION_HISTORY_EDIT -> "Edit Transaction"
    Route.WORKPROOF -> "Workproof"
    Route.FINANCE_HOME -> "Finance"
    Route.MENU -> "Menu"
    else -> "DonDone"
}

fun NavHostController.navigateToRootTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

internal fun navigateWithinApp(
    route: String,
    navigateToRootTab: (String) -> Unit,
    navigateDirect: (String) -> Unit
) {
    if (isRootRoute(route)) {
        navigateToRootTab(route)
    } else {
        navigateDirect(route)
    }
}
