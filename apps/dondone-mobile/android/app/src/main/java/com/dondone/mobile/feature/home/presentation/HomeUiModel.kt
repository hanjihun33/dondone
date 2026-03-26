package com.dondone.mobile.feature.home.presentation

import com.dondone.mobile.app.session.RemittanceCompletionNoticeUiState
import com.dondone.mobile.app.session.WorkproofActionUiState
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.AppTextKeys
import com.dondone.mobile.core.i18n.text
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.data.auth.AuthSession
import com.dondone.mobile.data.remittance.RemittanceRemoteMode
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.data.wage.WageRemoteState
import com.dondone.mobile.data.workproof.WorkproofRemoteMode
import com.dondone.mobile.data.workproof.WorkproofRemoteState
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TransferStatus
import java.math.RoundingMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class HomeAccountUiModel(
    val titleText: String,
    val balanceText: String,
    val balanceAmountText: String,
    val balanceUnitText: String?
)

data class HomeWorkUiModel(
    val dateText: String,
    val statusText: String,
    val statusTone: BadgeTone,
    val canClockIn: Boolean,
    val canClockOut: Boolean,
    val isWithinWorkplaceRadius: Boolean,
    val clockInText: String,
    val clockOutText: String,
    val noticeTitle: String? = null,
    val noticeMessage: String? = null,
    val showRecordSummary: Boolean = true,
    val showActions: Boolean = true
)

enum class HomeActionTarget {
    WAGE,
    FINANCE,
    MENU
}

data class HomeNextActionUiModel(
    val title: String,
    val message: String,
    val buttonText: String,
    val actionTarget: HomeActionTarget
)

data class HomeMoneyUiModel(
    val showWorkActionCard: Boolean,
    val nextAction: HomeNextActionUiModel
)

data class HomeCompletionBannerUiModel(
    val title: String,
    val message: String,
    val tone: BadgeTone
)

data class HomeUiModel(
    val account: HomeAccountUiModel,
    val work: HomeWorkUiModel,
    val money: HomeMoneyUiModel,
    val completionBanner: HomeCompletionBannerUiModel? = null
)

fun DemoState.toHomeUiModel(
    language: AppLanguage = AppLanguage.KOREAN,
    workproofActionUiState: WorkproofActionUiState? = null,
    wageRemoteState: WageRemoteState? = null,
    remittanceRemoteState: RemittanceRemoteState = RemittanceRemoteState.unauthenticated(""),
    remittanceCompletionNoticeUiState: RemittanceCompletionNoticeUiState = RemittanceCompletionNoticeUiState(),
    isAuthenticated: Boolean = false,
    session: AuthSession? = null,
    workproofRemoteState: WorkproofRemoteState = WorkproofRemoteState.unauthenticated("")
): HomeUiModel {
    val selectedAccount = remittance.selectedAccount()
    val wageEstimate = WageEstimator.calculate(this)
    val formattedMonth = demo.month.toString().padStart(2, '0')
    fun formatDay(day: Int): String = day.toString().padStart(2, '0')
    val currentDateText = "${demo.year}-$formattedMonth-${formatDay(demo.asOfDay)}"
    val clockIn = workproof.today.clockIn?.let { "$currentDateText · $it" } ?: "-"
    val clockOut = workproof.today.clockOut?.let { "$currentDateText · $it" } ?: "-"

    val isClockedOut = workproof.today.clockOut != null
    val isClockedInOnly = workproof.today.clockIn != null && workproof.today.clockOut == null
    val workStatusText = when {
        isClockedOut -> language.text("completed")
        isClockedInOnly -> language.text(AppTextKeys.HOME_CLOCK_IN_RECORDED)
        else -> language.text(AppTextKeys.HOME_READY)
    }
    val workStatusTone = when {
        isClockedOut -> BadgeTone.Success
        isClockedInOnly -> BadgeTone.Warning
        else -> BadgeTone.Info
    }

    val remoteSummary = wageRemoteState?.payload?.summary
    val isDepositRecorded = remoteSummary?.actualDepositAmount != null || wage.actualDepositRecordedDay != null
    val hasDifference = remoteSummary?.differenceAmount?.let { it != 0L } ?: (wageEstimate.difference != 0)
    val isPaydayUpcoming = demo.asOfDay < wage.paydayDay

    val nextAction = when {
        !isDepositRecorded && isPaydayUpcoming -> HomeNextActionUiModel(
            title = language.text(AppTextKeys.HOME_NEXT_STEP),
            message = language.text(AppTextKeys.HOME_PREPAYDAY_MESSAGE),
            buttonText = language.text(AppTextKeys.HOME_VIEW_FINANCE),
            actionTarget = HomeActionTarget.FINANCE
        )

        !isDepositRecorded -> HomeNextActionUiModel(
            title = language.text(AppTextKeys.HOME_NEXT_STEP),
            message = language.text(AppTextKeys.HOME_ENTER_DEPOSIT_MESSAGE),
            buttonText = language.text(AppTextKeys.HOME_ENTER_DEPOSIT),
            actionTarget = HomeActionTarget.WAGE
        )

        hasDifference -> HomeNextActionUiModel(
            title = language.text(AppTextKeys.HOME_NEXT_STEP),
            message = language.text(AppTextKeys.HOME_DIFFERENCE_MESSAGE),
            buttonText = language.text(AppTextKeys.HOME_VIEW),
            actionTarget = HomeActionTarget.WAGE
        )

        else -> HomeNextActionUiModel(
            title = language.text(AppTextKeys.HOME_NEXT_STEP),
            message = language.text(AppTextKeys.HOME_SMALL_DIFFERENCE_MESSAGE),
            buttonText = language.text(AppTextKeys.HOME_VIEW_FINANCE),
            actionTarget = HomeActionTarget.FINANCE
        )
    }
    val completionBanner = remittanceCompletionNoticeUiState.toHomeCompletionBannerUiModel(language)
    val requiresCompanyRegistration =
        isAuthenticated &&
            workproofRemoteState.mode == WorkproofRemoteMode.EMPTY &&
            session?.workplaceName.isNullOrBlank()

    return HomeUiModel(
        account = resolveHomeAccountUiModel(
            selectedAccount = selectedAccount,
            remittanceRemoteState = remittanceRemoteState,
            isAuthenticated = isAuthenticated,
            language = language
        ),
        work = HomeWorkUiModel(
            dateText = if (requiresCompanyRegistration) language.text(AppTextKeys.HOME_COMPANY_REGISTRATION_REQUIRED) else currentDateText,
            statusText = if (requiresCompanyRegistration) language.text(AppTextKeys.HOME_NOTICE) else workStatusText,
            statusTone = if (requiresCompanyRegistration) BadgeTone.Info else workStatusTone,
            canClockIn = !requiresCompanyRegistration &&
                workproof.today.clockIn == null &&
                workproofActionUiState?.isSubmitting != true,
            canClockOut = !requiresCompanyRegistration &&
                workproof.today.clockIn != null &&
                workproof.today.clockOut == null &&
                workproofActionUiState?.isSubmitting != true,
            isWithinWorkplaceRadius = workproof.isWithinWorkplaceRadius(),
            clockInText = clockIn,
            clockOutText = clockOut,
            noticeTitle = if (requiresCompanyRegistration) language.text(AppTextKeys.HOME_COMPANY_REGISTRATION_REQUIRED) else null,
            noticeMessage = if (requiresCompanyRegistration) {
                language.text(AppTextKeys.HOME_REGISTRATION_CODE_NOTICE)
            } else {
                null
            },
            showRecordSummary = !requiresCompanyRegistration,
            showActions = !requiresCompanyRegistration
        ),
        money = HomeMoneyUiModel(
            showWorkActionCard = isDepositRecorded && hasDifference,
            nextAction = nextAction
        ),
        completionBanner = completionBanner
    )
}

private fun resolveHomeAccountUiModel(
    selectedAccount: com.dondone.mobile.domain.model.Account,
    remittanceRemoteState: RemittanceRemoteState,
    isAuthenticated: Boolean,
    language: AppLanguage
): HomeAccountUiModel {
    if (isAuthenticated) {
        val remotePayload = remittanceRemoteState.payload
        return HomeAccountUiModel(
            titleText = language.text(AppTextKeys.HOME_PRIMARY_WALLET),
            balanceText = when (remittanceRemoteState.mode) {
                RemittanceRemoteMode.CONTENT -> remotePayload?.balance?.formatTokenBalanceForHome(language) ?: language.text(AppTextKeys.HOME_CHECKING_BALANCE)
                RemittanceRemoteMode.LOADING -> language.text(AppTextKeys.HOME_CHECKING_BALANCE)
                else -> language.text(AppTextKeys.HOME_CHECKING_WALLET_INFO)
            },
            balanceAmountText = when (remittanceRemoteState.mode) {
                RemittanceRemoteMode.CONTENT -> remotePayload?.balance?.formatTokenAmountForHome(language) ?: language.text(AppTextKeys.HOME_CHECKING_BALANCE)
                RemittanceRemoteMode.LOADING -> language.text(AppTextKeys.HOME_CHECKING_BALANCE)
                else -> language.text(AppTextKeys.HOME_CHECKING_WALLET_INFO)
            },
            balanceUnitText = when (remittanceRemoteState.mode) {
                RemittanceRemoteMode.CONTENT -> remotePayload?.balance?.assetSymbol
                else -> null
            }
        )
    }

    return HomeAccountUiModel(
        titleText = language.text(AppTextKeys.HOME_PRIMARY_ACCOUNT),
        balanceText = formatKrw(selectedAccount.balance),
        balanceAmountText = formatKrw(selectedAccount.balance),
        balanceUnitText = null
    )
}

private fun com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload.formatTokenBalanceForHome(
    language: AppLanguage
): String {
    val amount = formatTokenAmountForHome(language)
    return if (amount == language.text(AppTextKeys.HOME_CHECKING_BALANCE)) amount else "$amount $assetSymbol"
}

private fun com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload.formatTokenAmountForHome(
    language: AppLanguage
): String {
    val normalized = tokenBalanceAtomic.toBigDecimalOrNull()
        ?.movePointLeft(assetDecimals)
        ?.setScale(2, RoundingMode.DOWN)
        ?: return language.text(AppTextKeys.HOME_CHECKING_BALANCE)
    return normalized.stripTrailingZeros().toPlainString()
}

private fun RemittanceCompletionNoticeUiState.toHomeCompletionBannerUiModel(
    language: AppLanguage
): HomeCompletionBannerUiModel? {
    if (!isVisible || status == null) {
        return null
    }

    val recipientLabel = recipientName ?: language.text(AppTextKeys.HOME_SAVED_RECIPIENT)
    return when (status) {
        TransferStatus.CONFIRMED -> HomeCompletionBannerUiModel(
            title = language.text(AppTextKeys.HOME_TRANSFER_COMPLETE),
            message = language.text(AppTextKeys.HOME_TRANSFER_COMPLETE_MESSAGE, recipientLabel),
            tone = BadgeTone.Success
        )

        TransferStatus.FAILED -> HomeCompletionBannerUiModel(
            title = language.text(AppTextKeys.HOME_TRANSFER_FAILED),
            message = language.text(AppTextKeys.HOME_TRANSFER_FAILED_MESSAGE, recipientLabel),
            tone = BadgeTone.Warning
        )

        else -> null
    }
}

private fun com.dondone.mobile.domain.model.WorkproofData.isWithinWorkplaceRadius(): Boolean {
    return haversineDistanceMeters(
        startLatitude = currentLatitude,
        startLongitude = currentLongitude,
        endLatitude = workplaceLatitude,
        endLongitude = workplaceLongitude
    ) <= allowedRadiusMeters
}

private fun haversineDistanceMeters(
    startLatitude: Double,
    startLongitude: Double,
    endLatitude: Double,
    endLongitude: Double
): Double {
    val earthRadiusMeters = 6_371_000.0
    val latitudeDelta = Math.toRadians(endLatitude - startLatitude)
    val longitudeDelta = Math.toRadians(endLongitude - startLongitude)
    val startLatitudeRadians = Math.toRadians(startLatitude)
    val endLatitudeRadians = Math.toRadians(endLatitude)

    val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(startLatitudeRadians) * cos(endLatitudeRadians) *
        sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusMeters * c
}
