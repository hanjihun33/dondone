package com.dondone.mobile.feature.home.presentation

import android.location.Location
import com.dondone.mobile.app.session.WorkproofActionUiState
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TransferStatus

data class HomeAccountUiModel(
    val balanceText: String
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
    workproofActionUiState: WorkproofActionUiState? = null
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
        account = HomeAccountUiModel(
            balanceText = formatKrw(selectedAccount.balance)
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

private fun com.dondone.mobile.domain.model.WorkproofData.isWithinWorkplaceRadius(): Boolean {
    val result = FloatArray(1)
    Location.distanceBetween(
        currentLatitude,
        currentLongitude,
        workplaceLatitude,
        workplaceLongitude,
        result
    )
    return result.first() <= allowedRadiusMeters
}
