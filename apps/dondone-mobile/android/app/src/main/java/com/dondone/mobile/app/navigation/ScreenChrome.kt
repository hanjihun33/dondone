package com.dondone.mobile.app.navigation

import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus

data class ScreenChrome(
    val headerTitle: String?,
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
            headerTitle = when {
                route == Route.HOME -> null
                route == Route.WORKPROOF && isWorkproofDetailVisible -> null
                else -> routeTitle(route)
            },
            showRootTabs = true,
            showSettingsAction = route == Route.HOME,
            showDate = route != Route.HOME && route != Route.MENU &&
                !(route == Route.WORKPROOF && isWorkproofDetailVisible)
        )
    } else {
        ScreenChrome(
            headerTitle = resolveChildHeaderTitle(route, transferStep, transferStatus),
            showRootTabs = false,
            showSettingsAction = false,
            showDate = route != Route.TRANSFER &&
                route != Route.ACCOUNT &&
                !isTransactionHistoryRoute(route)
        )
    }
}

private fun resolveChildHeaderTitle(
    route: String,
    transferStep: TransferFlowStep,
    transferStatus: TransferStatus
): String? {
    return when (route) {
        Route.WAGE -> null
        Route.TRANSFER -> resolveTransferHeaderTitle(transferStep, transferStatus)
        else -> routeTitle(route)
    }
}

private fun resolveTransferHeaderTitle(
    transferStep: TransferFlowStep,
    transferStatus: TransferStatus
): String? {
    if (
        transferStatus == TransferStatus.SUBMITTED ||
        transferStatus == TransferStatus.CONFIRMED ||
        transferStatus == TransferStatus.FAILED
    ) {
        return null
    }

    return when (transferStep) {
        TransferFlowStep.ACCOUNT -> "계좌 선택"
        TransferFlowStep.RECIPIENT -> null
        TransferFlowStep.AMOUNT -> null
    }
}
