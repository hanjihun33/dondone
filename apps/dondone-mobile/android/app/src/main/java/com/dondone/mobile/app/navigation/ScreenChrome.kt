package com.dondone.mobile.app.navigation

import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.AppTextKeys
import com.dondone.mobile.core.i18n.text
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
    isWorkproofDetailVisible: Boolean,
    language: AppLanguage = AppLanguage.fromDefault()
): ScreenChrome {
    return if (isRootRoute(route)) {
        ScreenChrome(
            headerTitle = when {
                route == Route.HOME -> null
                route == Route.WORKPROOF && isWorkproofDetailVisible -> null
                else -> routeTitle(route, language)
            },
            showRootTabs = true,
            showSettingsAction = false,
            showDate = route != Route.HOME && route != Route.MENU &&
                route != Route.FINANCE_HOME &&
                !(route == Route.WORKPROOF && isWorkproofDetailVisible)
        )
    } else {
        ScreenChrome(
            headerTitle = resolveChildHeaderTitle(route, transferStep, transferStatus, language),
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
    transferStatus: TransferStatus,
    language: AppLanguage
): String? {
    return when (route) {
        Route.WAGE -> null
        Route.TRANSFER -> resolveTransferHeaderTitle(transferStep, transferStatus, language)
        else -> routeTitle(route, language)
    }
}

private fun resolveTransferHeaderTitle(
    transferStep: TransferFlowStep,
    transferStatus: TransferStatus,
    language: AppLanguage
): String? {
    if (
        transferStatus == TransferStatus.SUBMITTED ||
        transferStatus == TransferStatus.CONFIRMED ||
        transferStatus == TransferStatus.FAILED
    ) {
        return null
    }

    return when (transferStep) {
        TransferFlowStep.ACCOUNT -> language.text(AppTextKeys.SELECT_ACCOUNT)
        TransferFlowStep.RECIPIENT -> null
        TransferFlowStep.AMOUNT -> null
    }
}
