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
    transferStatus: TransferStatus,
    isWorkproofDetailVisible: Boolean
): ScreenChrome {
    return if (isRootRoute(route)) {
        ScreenChrome(
            title = when {
                route == Route.HOME -> "DonDone"
                route == Route.WORKPROOF && isWorkproofDetailVisible -> ""
                else -> routeTitle(route)
            },
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
                        ""
                    } else {
                        when (transferStep) {
                            TransferFlowStep.ACCOUNT -> "계좌 선택"
                            TransferFlowStep.RECIPIENT -> ""
                            TransferFlowStep.AMOUNT -> ""
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
