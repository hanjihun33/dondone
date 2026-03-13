package com.dondone.mobile.feature.home.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.calculator.AdvanceCalculator
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TransferStatus
import kotlin.math.abs

data class HomeAccountUiModel(
    val balanceText: String
)

data class HomeWorkUiModel(
    val dateText: String,
    val statusText: String,
    val statusTone: BadgeTone,
    val canClockIn: Boolean,
    val canClockOut: Boolean,
    val clockInText: String,
    val clockOutText: String,
    val impactText: String,
    val advanceAvailableText: String,
    val advanceProgressText: String,
    val advanceProgress: Float,
    val advanceHintText: String
)

enum class HomeActionTarget {
    WAGE,
    FINANCE,
    MENU
}

data class HomePaydayUiModel(
    val kicker: String,
    val title: String,
    val description: String,
    val metaText: String,
    val buttonText: String,
    val actionTarget: HomeActionTarget
)

data class HomeNextActionUiModel(
    val title: String,
    val message: String,
    val buttonText: String,
    val actionTarget: HomeActionTarget
)

data class HomeMoneyUiModel(
    val statusText: String,
    val statusTone: BadgeTone,
    val briefText: String,
    val estimatedText: String,
    val actualText: String,
    val differenceText: String,
    val showPaydayCard: Boolean,
    val payday: HomePaydayUiModel,
    val nextAction: HomeNextActionUiModel
)

data class HomeUiModel(
    val account: HomeAccountUiModel,
    val work: HomeWorkUiModel,
    val money: HomeMoneyUiModel
)

fun DemoState.toHomeUiModel(): HomeUiModel {
    val selectedAccount = remittance.selectedAccount()
    val wageEstimate = WageEstimator.calculate(this)
    val advance = AdvanceCalculator.calculate(this)
    val progress = if (advance.progressTargetDays == 0) 1f else advance.verifiedDays / advance.progressTargetDays.toFloat()
    val clockIn = workproof.today.clockIn ?: "-"
    val clockOut = workproof.today.clockOut ?: "-"
    val formattedMonth = demo.month.toString().padStart(2, '0')
    fun formatDay(day: Int): String = day.toString().padStart(2, '0')

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

    val moneyStatusText = when {
        !isDepositRecorded -> "입금 대기"
        hasDifference -> "확인 필요한 차이"
        else -> "이상 없음"
    }
    val moneyStatusTone = when {
        !isDepositRecorded -> BadgeTone.Info
        hasDifference -> BadgeTone.Warning
        else -> BadgeTone.Success
    }

    val payday = when {
        !isDepositRecorded -> HomePaydayUiModel(
            kicker = "급여일 체크",
            title = "실제 입금액을 먼저 확인해 주세요",
            description = "실입금을 입력하면 차이 확인과 다음 행동이 바로 열려요.",
            metaText = "예상 급여일 · ${demo.year}-$formattedMonth-${formatDay(wage.paydayDay)}",
            buttonText = "급여 확인",
            actionTarget = HomeActionTarget.WAGE
        )

        hasDifference -> HomePaydayUiModel(
            kicker = "급여 확인 완료",
            title = "차이 확인이 열렸어요",
            description = "예상과 실제 입금 차이를 근거와 함께 바로 확인할 수 있어요.",
            metaText = "입금 기록일 · ${demo.year}-$formattedMonth-${formatDay(requireNotNull(wage.actualDepositRecordedDay))}",
            buttonText = "차이 확인",
            actionTarget = HomeActionTarget.WAGE
        )

        else -> HomePaydayUiModel(
            kicker = "급여 확인 완료",
            title = "이번 달 실입금이 반영됐어요",
            description = "예상과 큰 차이 없이 확인됐어요. 다음 흐름으로 이어갈 수 있어요.",
            metaText = "입금 기록일 · ${demo.year}-$formattedMonth-${formatDay(requireNotNull(wage.actualDepositRecordedDay))}",
            buttonText = "정산 보기",
            actionTarget = HomeActionTarget.WAGE
        )
    }

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
            message = "확인 필요한 차이가 보여요.\n급여 점검에서 근거부터 확인해볼까요?",
            buttonText = "급여 점검",
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
            dateText = "${demo.year}-$formattedMonth-${formatDay(demo.asOfDay)} · ${workproof.workplaceName}",
            statusText = workStatusText,
            statusTone = workStatusTone,
            canClockIn = workproof.today.clockIn == null,
            canClockOut = workproof.today.clockIn != null && workproof.today.clockOut == null,
            clockInText = clockIn,
            clockOutText = clockOut,
            impactText = "오늘 기록이 저장되어 다음 반영 후보에 포함돼요.",
            advanceAvailableText = formatKrw(advance.available),
            advanceProgressText = "${advance.verifiedDays}일 / ${advance.progressTargetDays}일",
            advanceProgress = progress,
            advanceHintText = if (advance.nextTierInDays > 0) {
                "다음 구간까지 ${advance.nextTierInDays}일 · 예상 증가 ${formatKrw(advance.nextTierGain)}"
            } else {
                "이번 달 최고 구간에 도달했어요."
            }
        ),
        money = HomeMoneyUiModel(
            statusText = moneyStatusText,
            statusTone = moneyStatusTone,
            briefText = when {
                !isDepositRecorded -> "실입금 전에는 차이 비교가 잠겨 있어요."
                hasDifference -> "실입금 기준으로 차이 분석이 열렸어요."
                else -> "이번 달 정산이 안정적으로 반영됐어요."
            },
            estimatedText = formatKrw(wageEstimate.total),
            actualText = formatKrw(wage.actualDeposit),
            differenceText = formatKrw(abs(wageEstimate.difference)),
            showPaydayCard = isDepositRecorded && !hasDifference,
            payday = payday,
            nextAction = nextAction
        )
    )
}
