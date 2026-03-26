package com.dondone.mobile.app.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.AppTextKeys
import com.dondone.mobile.core.i18n.text

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

fun mainTabs(language: AppLanguage): List<MainTab> = listOf(
    MainTab(Route.HOME, language.text(AppTextKeys.HOME)),
    MainTab(Route.FINANCE_HOME, language.text(AppTextKeys.FINANCE)),
    MainTab(Route.WORKPROOF, language.text(AppTextKeys.WORK)),
    MainTab(Route.MENU, language.text(AppTextKeys.MENU))
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

fun routeTitle(
    route: String,
    language: AppLanguage = AppLanguage.KOREAN
): String = when (route) {
    Route.WAGE -> language.text(AppTextKeys.WAGE_REVIEW)
    Route.TRANSFER -> language.text(AppTextKeys.TRANSFER)
    Route.ACCOUNT -> language.text(AppTextKeys.WALLET_ACCOUNTS)
    Route.TRANSACTION_HISTORY -> language.text(AppTextKeys.TRANSACTION_HISTORY)
    Route.TRANSACTION_HISTORY_DETAIL -> language.text(AppTextKeys.TRANSACTION_DETAILS)
    Route.TRANSACTION_HISTORY_EDIT -> language.text(AppTextKeys.EDIT_TRANSACTION)
    Route.WORKPROOF -> language.text(AppTextKeys.WORK)
    Route.FINANCE_HOME -> language.text(AppTextKeys.FINANCE)
    Route.MENU -> language.text(AppTextKeys.MENU)
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
