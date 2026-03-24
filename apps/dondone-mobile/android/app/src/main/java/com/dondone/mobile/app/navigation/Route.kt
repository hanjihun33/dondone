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
    MainTab(Route.HOME, "홈"),
    MainTab(Route.FINANCE_HOME, "금융"),
    MainTab(Route.WORKPROOF, "근무"),
    MainTab(Route.MENU, "메뉴")
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
    Route.WAGE -> "급여 점검"
    Route.TRANSFER -> "송금"
    Route.ACCOUNT -> "계좌 지갑"
    Route.TRANSACTION_HISTORY -> "거래 내역"
    Route.TRANSACTION_HISTORY_DETAIL -> "거래 상세"
    Route.TRANSACTION_HISTORY_EDIT -> "거래 수정"
    Route.WORKPROOF -> "근무"
    Route.FINANCE_HOME -> "금융"
    Route.MENU -> "메뉴"
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
