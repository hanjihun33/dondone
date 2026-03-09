package com.dondone.mobile.app.navigation

import com.dondone.mobile.domain.model.TransferFlowStep

data class ScreenChrome(
    val title: String,
    val showRootTabs: Boolean,
    val showSettingsAction: Boolean
)

fun resolveScreenChrome(
    route: String,
    transferStep: TransferFlowStep
): ScreenChrome {
    return if (isRootRoute(route)) {
        ScreenChrome(
            title = if (route == Route.HOME) "DonDone" else routeTitle(route),
            showRootTabs = true,
            showSettingsAction = route == Route.HOME
        )
    } else {
        ScreenChrome(
            title = when (route) {
                Route.TRANSFER -> if (transferStep == TransferFlowStep.AMOUNT) "금액 입력" else "받는 사람 선택"
                else -> routeTitle(route)
            },
            showRootTabs = false,
            showSettingsAction = false
        )
    }
}
