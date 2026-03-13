package com.dondone.mobile.app.navigation

import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus

data class ScreenChrome(
    val title: String,
    val showRootTabs: Boolean,
    val showSettingsAction: Boolean,
    val showDate: Boolean
)

fun resolveScreenChrome(
    route: String,
    transferStep: TransferFlowStep,
    transferStatus: TransferStatus
): ScreenChrome {
    return if (isRootRoute(route)) {
        ScreenChrome(
            title = if (route == Route.HOME) "DonDone" else routeTitle(route),
            showRootTabs = true,
            showSettingsAction = route == Route.HOME,
            showDate = route != Route.HOME && route != Route.FINANCE_HOME && route != Route.WORKPROOF && route != Route.MENU
        )
    } else {
        ScreenChrome(
            title = when (route) {
                Route.WAGE -> ""
                Route.TRANSFER -> {
                    if (transferStatus == TransferStatus.SUBMITTED || transferStatus == TransferStatus.CONFIRMED) {
                        "송금 상태"
                    } else {
                        when (transferStep) {
                            TransferFlowStep.ACCOUNT -> "계좌 선택"
                            TransferFlowStep.RECIPIENT -> "받는 사람 선택"
                            TransferFlowStep.AMOUNT -> "금액 입력"
                        }
                    }
                }

                else -> routeTitle(route)
            },
            showRootTabs = false,
            showSettingsAction = false,
            showDate = route != Route.TRANSFER && route != Route.WAGE
        )
    }
}
