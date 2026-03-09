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
                Route.TRANSFER -> when (transferStep) {
                    TransferFlowStep.ACCOUNT -> "계좌 선택"
                    TransferFlowStep.RECIPIENT -> "받는 사람 선택"
                    TransferFlowStep.AMOUNT -> "금액 입력"
                }
                else -> routeTitle(route)
            },
            showRootTabs = false,
            showSettingsAction = false
        )
    }
}
