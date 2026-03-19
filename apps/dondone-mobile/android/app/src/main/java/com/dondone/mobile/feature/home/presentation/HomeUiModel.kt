package com.dondone.mobile.feature.home.presentation

import com.dondone.mobile.app.session.WorkproofActionUiState
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.data.remittance.RemittanceRemoteMode
import com.dondone.mobile.data.remittance.RemittanceRemoteState
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
    val clockOutText: String
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

data class HomeUiModel(
    val account: HomeAccountUiModel,
    val work: HomeWorkUiModel,
    val money: HomeMoneyUiModel
)

fun DemoState.toHomeUiModel(
    workproofActionUiState: WorkproofActionUiState? = null,
    remittanceRemoteState: RemittanceRemoteState = RemittanceRemoteState.unauthenticated(""),
    isAuthenticated: Boolean = false
): HomeUiModel {
    val selectedAccount = remittance.selectedAccount()
    val wageEstimate = WageEstimator.calculate(this)
    val formattedMonth = demo.month.toString().padStart(2, '0')
    fun formatDay(day: Int): String = day.toString().padStart(2, '0')
    val currentDateText = "${demo.year}-$formattedMonth-${formatDay(demo.asOfDay)}"
    val clockIn = workproof.today.clockIn?.let { "$currentDateText · $it" } ?: "-"
    val clockOut = workproof.today.clockOut?.let { "$currentDateText · $it" } ?: "-"

    val workStatusText = when {
        workproof.today.clockOut != null -> "완료"
        workproof.today.clockIn != null -> "출근만 기록"
        else -> "준비됨"
    }
    val workStatusTone = when (workStatusText) {
        "완료" -> BadgeTone.Success
        "출근만 기록" -> BadgeTone.Warning
        else -> BadgeTone.Info
    }

    val isDepositRecorded = wage.actualDepositRecordedDay != null
    val hasDifference = wageEstimate.difference != 0
    val isPaydayUpcoming = demo.asOfDay < wage.paydayDay
    val isTransferConfirmed = remittance.status == TransferStatus.CONFIRMED

    val nextAction = when {
        !isDepositRecorded && isPaydayUpcoming -> HomeNextActionUiModel(
            title = "다음 행동",
            message = "급여일 전에는 이번 달 계획과 미리받기 한도를 먼저 확인해볼까요?",
            buttonText = "금융 보기",
            actionTarget = HomeActionTarget.FINANCE
        )

        !isDepositRecorded -> HomeNextActionUiModel(
            title = "다음 행동",
            message = "실제 입금액을 먼저 입력하면 이번 달 돈 상태를 정확히 확인할 수 있어요.",
            buttonText = "입금 입력하기",
            actionTarget = HomeActionTarget.WAGE
        )

        hasDifference -> HomeNextActionUiModel(
            title = "다음 행동",
            message = "실제 입금액과 예상 급여에 차이가 있어요.",
            buttonText = "보기",
            actionTarget = HomeActionTarget.WAGE
        )

        isTransferConfirmed -> HomeNextActionUiModel(
            title = "다음 행동",
            message = "흐름이 완료되었습니다. 필요하면 영수증/문서를 다시 확인해볼 수 있어요.",
            buttonText = "문서",
            actionTarget = HomeActionTarget.MENU
        )

        else -> HomeNextActionUiModel(
            title = "다음 행동",
            message = "현재는 확인이 필요한 차이가 크지 않아 문서/송금 흐름으로 이동할 수 있어요.",
            buttonText = "금융 보기",
            actionTarget = HomeActionTarget.FINANCE
        )
    }

    return HomeUiModel(
        account = resolveHomeAccountUiModel(
            selectedAccount = selectedAccount,
            remittanceRemoteState = remittanceRemoteState,
            isAuthenticated = isAuthenticated
        ),
        work = HomeWorkUiModel(
            dateText = currentDateText,
            statusText = workStatusText,
            statusTone = workStatusTone,
            canClockIn = workproof.today.clockIn == null && workproofActionUiState?.isSubmitting != true,
            canClockOut = workproof.today.clockIn != null && workproof.today.clockOut == null && workproofActionUiState?.isSubmitting != true,
            isWithinWorkplaceRadius = workproof.isWithinWorkplaceRadius(),
            clockInText = clockIn,
            clockOutText = clockOut
        ),
        money = HomeMoneyUiModel(
            showWorkActionCard = isDepositRecorded && hasDifference,
            nextAction = nextAction
        )
    )
}

private fun resolveHomeAccountUiModel(
    selectedAccount: com.dondone.mobile.domain.model.Account,
    remittanceRemoteState: RemittanceRemoteState,
    isAuthenticated: Boolean
): HomeAccountUiModel {
    if (isAuthenticated) {
        val remotePayload = remittanceRemoteState.payload
        return HomeAccountUiModel(
            titleText = "대표 지갑",
            balanceText = when (remittanceRemoteState.mode) {
                RemittanceRemoteMode.CONTENT -> remotePayload?.balance?.formatTokenBalanceForHome() ?: "잔액 확인 중"
                RemittanceRemoteMode.LOADING -> "잔액 확인 중"
                else -> "지갑 정보 확인 중"
            },
            balanceAmountText = when (remittanceRemoteState.mode) {
                RemittanceRemoteMode.CONTENT -> remotePayload?.balance?.formatTokenAmountForHome() ?: "잔액 확인 중"
                RemittanceRemoteMode.LOADING -> "잔액 확인 중"
                else -> "지갑 정보 확인 중"
            },
            balanceUnitText = when (remittanceRemoteState.mode) {
                RemittanceRemoteMode.CONTENT -> remotePayload?.balance?.assetSymbol
                else -> null
            }
        )
    }

    return HomeAccountUiModel(
        titleText = "대표 계좌",
        balanceText = formatKrw(selectedAccount.balance),
        balanceAmountText = formatKrw(selectedAccount.balance),
        balanceUnitText = null
    )
}

private fun com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload.formatTokenBalanceForHome(): String {
    val amount = formatTokenAmountForHome()
    return if (amount == "잔액 확인 중") amount else "$amount $assetSymbol"
}

private fun com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload.formatTokenAmountForHome(): String {
    val normalized = tokenBalanceAtomic.toBigDecimalOrNull()
        ?.movePointLeft(assetDecimals)
        ?.setScale(2, RoundingMode.DOWN)
        ?: return "잔액 확인 중"
    return normalized.stripTrailingZeros().toPlainString()
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
