package com.dondone.mobile.app.navigation

data class MainTab(
    val rootRoute: String,
    val label: String
)

object Route {
    const val HOME = "home"
    const val WORKPROOF = "workproof"
    const val FINANCE_HOME = "finance"
    const val WAGE = "finance/wage"
    const val TRANSFER = "finance/transfer"
    const val ACCOUNT = "finance/account"
    const val MENU = "menu"
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

fun routeTitle(route: String): String = when (route) {
    Route.WAGE -> "급여 점검"
    Route.TRANSFER -> "송금"
    Route.ACCOUNT -> "계좌·지갑 관리"
    Route.WORKPROOF -> "근무 일지"
    Route.FINANCE_HOME -> "금융"
    Route.MENU -> "메뉴"
    else -> "DonDone"
}
