package com.dondone.mobile.feature.wage.presentation

import com.dondone.mobile.app.session.WageActionUiState
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.text
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.data.wage.WageRemoteMode
import com.dondone.mobile.data.wage.WageRemotePayload
import com.dondone.mobile.data.wage.WageRemoteState
import com.dondone.mobile.data.wage.WageVerificationDetailPayload
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.model.DemoState
import kotlin.math.abs

enum class WageSurfaceState {
    LOADING,
    UNAUTHENTICATED,
    ERROR,
    EMPTY,
    CONTENT
}

enum class WageSurfaceActionType {
    REFRESH,
    OPEN_MENU,
    OPEN_MENU_AND_REGISTRATION_CODE
}

enum class WageMetricIcon {
    BASE,
    OVERTIME,
    NIGHT,
    TOTAL
}

enum class WageDifferenceState {
    PENDING,
    MATCH,
    UNDER,
    OVER
}

data class WageMetricItemUiModel(
    val label: String,
    val value: String,
    val emphasized: Boolean = false,
    val icon: WageMetricIcon? = null
)

data class WageDepositUiModel(
    val isSubmitting: Boolean,
    val statusText: String,
    val statusTone: BadgeTone,
    val headerText: String,
    val descriptionText: String,
    val metaText: String,
    val actualDepositText: String,
    val deductionBadgeText: String,
    val thresholdBadgeText: String
)

data class WageDifferenceUiModel(
    val state: WageDifferenceState,
    val title: String,
    val descriptionText: String,
    val locked: Boolean,
    val statusText: String,
    val statusTone: BadgeTone,
    val summaryItems: List<WageMetricItemUiModel>,
    val evidenceLines: List<String>
)

data class WageUiModel(
    val surfaceState: WageSurfaceState,
    val surfaceMessage: String?,
    val surfaceActionText: String?,
    val surfaceActionType: WageSurfaceActionType?,
    val descriptionText: String,
    val deposit: WageDepositUiModel,
    val overviewItems: List<WageMetricItemUiModel>,
    val estimateItems: List<WageMetricItemUiModel>,
    val difference: WageDifferenceUiModel
)

fun DemoState.toWageUiModel(
    remoteState: WageRemoteState? = null,
    actionUiState: WageActionUiState = WageActionUiState(),
    language: AppLanguage = AppLanguage.KOREAN
): WageUiModel {
    val surfaceState = when (remoteState?.mode) {
        WageRemoteMode.LOADING -> WageSurfaceState.LOADING
        WageRemoteMode.UNAUTHENTICATED -> WageSurfaceState.UNAUTHENTICATED
        WageRemoteMode.ERROR -> WageSurfaceState.ERROR
        WageRemoteMode.EMPTY -> WageSurfaceState.EMPTY
        WageRemoteMode.CONTENT, null -> WageSurfaceState.CONTENT
    }
    val payload = remoteState?.payload
    val estimate = payload?.estimate
    val summary = payload?.summary
    val verification = payload?.latestVerification
    val localEstimate = WageEstimator.calculate(this)

    val estimatedBase = estimate?.baseEstimate?.toInt() ?: localEstimate.base
    val estimatedOvertime = estimate?.overtimePremium?.toInt() ?: localEstimate.overtimePremium
    val estimatedNight = estimate?.nightPremium?.toInt() ?: localEstimate.nightPremium
    val estimatedTotal = estimate?.estimatedTotal?.toInt() ?: localEstimate.total
    val actualDeposit = summary?.actualDepositAmount?.toInt() ?: wage.actualDeposit
    val differenceAmount = verification?.differenceAmount?.toInt()
        ?: summary?.differenceAmount?.toInt()
        ?: estimatedTotal - actualDeposit
    val isRecorded = summary?.actualDepositAmount != null || wage.actualDepositRecordedDay != null
    val workDays = summary?.workDays ?: wage.workDays
    val totalHours = ((summary?.totalWorkedMinutes ?: wage.totalHours.toLong() * 60L) / 60L).toInt()
    val overtimeHours = ((summary?.overtimeMinutes ?: wage.overtimeHours.toLong() * 60L) / 60L).toInt()
    val nightHours = ((summary?.nightMinutes ?: wage.nightHours.toLong() * 60L) / 60L).toInt()
    val modifiedCount = summary?.modifiedRecordCount ?: workproof.audit.size
    val differenceState = when {
        !isRecorded -> WageDifferenceState.PENDING
        differenceAmount == 0 -> WageDifferenceState.MATCH
        differenceAmount > 0 -> WageDifferenceState.UNDER
        else -> WageDifferenceState.OVER
    }
    val differenceStatusText = when (differenceState) {
        WageDifferenceState.PENDING -> language.text("wage_record_needed_status")
        WageDifferenceState.MATCH -> language.text("wage_difference_match_title")
        WageDifferenceState.UNDER -> language.text("wage_difference_under_title")
        WageDifferenceState.OVER -> language.text("wage_difference_over_title")
    }
    val statusTone = when (differenceState) {
        WageDifferenceState.MATCH -> BadgeTone.Success
        WageDifferenceState.OVER -> BadgeTone.Info
        WageDifferenceState.PENDING,
        WageDifferenceState.UNDER -> BadgeTone.Warning
    }
    val evidenceLines = buildEvidenceLines(
        state = this,
        payload = payload,
        verification = verification,
        overtimeHours = overtimeHours,
        nightHours = nightHours,
        modifiedCount = modifiedCount,
        language = language
    )
    val surfaceMessage = when (surfaceState) {
        WageSurfaceState.LOADING -> language.text("wage_loading_message")
        WageSurfaceState.UNAUTHENTICATED -> remoteState?.errorMessage ?: language.text("wage_login_required_message")
        WageSurfaceState.ERROR -> remoteState?.errorMessage ?: language.text("wage_error_message")
        WageSurfaceState.EMPTY -> remoteState?.errorMessage ?: language.text("wage_empty_message")
        WageSurfaceState.CONTENT -> null
    }

    return WageUiModel(
        surfaceState = surfaceState,
        surfaceMessage = surfaceMessage,
        surfaceActionText = when (surfaceState) {
            WageSurfaceState.ERROR,
            WageSurfaceState.EMPTY -> {
                if (surfaceMessage.isActiveContractMissingMessage()) {
                    language.text("open_menu_registration")
                } else {
                    language.text("refresh")
                }
            }

            WageSurfaceState.UNAUTHENTICATED -> language.text("refresh")
            else -> null
        },
        surfaceActionType = when (surfaceState) {
            WageSurfaceState.ERROR,
            WageSurfaceState.EMPTY -> {
                if (surfaceMessage.isActiveContractMissingMessage()) {
                    WageSurfaceActionType.OPEN_MENU_AND_REGISTRATION_CODE
                } else {
                    WageSurfaceActionType.REFRESH
                }
            }

            WageSurfaceState.UNAUTHENTICATED -> WageSurfaceActionType.REFRESH
            else -> null
        },
        descriptionText = language.text("wage_description"),
        deposit = WageDepositUiModel(
            isSubmitting = actionUiState.isSubmittingDeposit,
            statusText = if (isRecorded) "" else language.text("wage_record_needed_status"),
            statusTone = if (isRecorded) BadgeTone.Info else BadgeTone.Warning,
            headerText = if (isRecorded) "" else language.text("wage_enter_actual_deposit"),
            descriptionText = if (isRecorded) "" else language.text("wage_enter_actual_deposit_desc"),
            metaText = if (isRecorded) {
                ""
            } else {
                language.text("wage_payday_basis_format", summary?.paydayDay ?: wage.paydayDay)
            },
            actualDepositText = formatKrw(actualDeposit, language),
            deductionBadgeText = if ((summary?.deductionsKnown ?: wage.deductionsKnown)) {
                language.text("deductions_confirmed")
            } else {
                language.text("deductions_unknown")
            },
            thresholdBadgeText = verification?.threshold?.absoluteWon?.toInt()?.let {
                language.text("threshold_badge_format", formatKrw(it, language))
            } ?: ""
        ),
        overviewItems = listOf(
            WageMetricItemUiModel(language.text("work_days"), language.text("days_format", workDays)),
            WageMetricItemUiModel(language.text("total_hours"), language.text("hours_format", totalHours)),
            WageMetricItemUiModel(language.text("overtime_night"), language.text("overtime_night_hours_format", overtimeHours, nightHours))
        ),
        estimateItems = listOf(
            WageMetricItemUiModel(language.text("estimated_base_pay"), formatKrw(estimatedBase, language), icon = WageMetricIcon.BASE),
            WageMetricItemUiModel(language.text("estimated_overtime_pay"), formatKrw(estimatedOvertime, language), icon = WageMetricIcon.OVERTIME),
            WageMetricItemUiModel(language.text("estimated_night_pay"), formatKrw(estimatedNight, language), icon = WageMetricIcon.NIGHT),
            WageMetricItemUiModel(language.text("estimated_total_pay"), formatKrw(estimatedTotal, language), emphasized = true, icon = WageMetricIcon.TOTAL)
        ),
        difference = WageDifferenceUiModel(
            state = differenceState,
            title = when (differenceState) {
                WageDifferenceState.PENDING -> language.text("wage_difference_pending_title")
                WageDifferenceState.MATCH -> language.text("wage_difference_match_title")
                WageDifferenceState.UNDER -> language.text("wage_difference_under_title")
                WageDifferenceState.OVER -> language.text("wage_difference_over_title")
            },
            descriptionText = when (differenceState) {
                WageDifferenceState.PENDING -> language.text("wage_difference_pending_desc")
                WageDifferenceState.MATCH -> language.text("wage_difference_match_desc")
                WageDifferenceState.UNDER -> language.text("wage_difference_under_desc")
                WageDifferenceState.OVER -> language.text("wage_difference_over_desc")
            },
            locked = !isRecorded,
            statusText = differenceStatusText,
            statusTone = statusTone,
            summaryItems = listOf(
                WageMetricItemUiModel(language.text("wage_estimated_wage"), formatKrw(estimatedTotal, language)),
                WageMetricItemUiModel(language.text("actual_deposit"), formatKrw(actualDeposit, language)),
                WageMetricItemUiModel(
                    language.text("wage_difference_amount"),
                    when {
                        differenceAmount > 0 -> "-${formatKrw(abs(differenceAmount), language)}"
                        differenceAmount < 0 -> "+${formatKrw(abs(differenceAmount), language)}"
                        else -> formatKrw(0, language)
                    },
                    emphasized = true
                )
            ),
            evidenceLines = evidenceLines
        )
    )
}

private fun String?.isActiveContractMissingMessage(): Boolean {
    val message = this ?: return false
    return message.contains("근로계약") ||
        message.contains("active contract", ignoreCase = true) ||
        message.contains("employment contract", ignoreCase = true)
}

private fun buildEvidenceLines(
    state: DemoState,
    payload: WageRemotePayload?,
    verification: WageVerificationDetailPayload?,
    overtimeHours: Int,
    nightHours: Int,
    modifiedCount: Int,
    language: AppLanguage
): List<String> {
    val remoteEvidence = verification?.evidence
    return buildList {
        payload?.summary?.reasons?.firstOrNull()?.let { reason ->
            add(language.text("evidence_reason_format", reason.title))
        }
        add(language.text("evidence_overtime_format", remoteEvidence?.overtimeMinutes?.div(60) ?: overtimeHours))
        add(language.text("evidence_night_format", remoteEvidence?.nightMinutes?.div(60) ?: nightHours))
        add(language.text("evidence_modified_records_format", remoteEvidence?.modifiedRecordCount ?: modifiedCount))
        val relatedCount = verification?.relatedActions?.let {
            listOfNotNull(it.proofPackDocumentId, it.claimKitDocumentId, it.preparationId).size
        } ?: state.documents.size
        add(language.text("evidence_related_docs_format", relatedCount))
    }
}
