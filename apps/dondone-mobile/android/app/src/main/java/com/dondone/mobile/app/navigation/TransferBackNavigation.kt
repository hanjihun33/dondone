package com.dondone.mobile.app.navigation

import com.dondone.mobile.app.session.DemoSessionViewModel
import com.dondone.mobile.app.session.RemittanceSubmittingAction
import com.dondone.mobile.domain.model.RemittanceData
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus

internal sealed interface AppBackAction {
    data object NavigateUp : AppBackAction
    data object DismissTransferConfirmation : AppBackAction
    data object Ignore : AppBackAction
    data class ShowTransferStep(val step: TransferFlowStep) : AppBackAction
}

internal fun resolveAppBackAction(
    currentRoute: String,
    remittance: RemittanceData,
    isRemittanceSubmitting: Boolean,
    remittanceSubmittingAction: RemittanceSubmittingAction? = null
): AppBackAction {
    if (currentRoute != Route.TRANSFER) {
        return AppBackAction.NavigateUp
    }

    if (isRemittanceSubmitting && remittanceSubmittingAction == RemittanceSubmittingAction.TRANSFER_CREATE) {
        return AppBackAction.Ignore
    }

    return when (remittance.status) {
        TransferStatus.REVIEWING -> AppBackAction.DismissTransferConfirmation
        TransferStatus.SUBMITTED,
        TransferStatus.CONFIRMED,
        TransferStatus.FAILED -> AppBackAction.NavigateUp
        TransferStatus.IDLE -> remittance.flowStep.previousStep(remittance.stepReturnTarget)
            ?.let(AppBackAction::ShowTransferStep)
            ?: AppBackAction.NavigateUp
    }
}

internal fun DemoSessionViewModel.showTransferStep(step: TransferFlowStep) {
    when (step) {
        TransferFlowStep.AMOUNT -> showAmountStep()
        TransferFlowStep.RECIPIENT -> showRecipientStep()
        TransferFlowStep.ACCOUNT -> showAccountStep()
    }
}

internal fun TransferFlowStep.previousStep(
    target: TransferFlowStep?
): TransferFlowStep? {
    return when (this) {
        TransferFlowStep.AMOUNT -> TransferFlowStep.RECIPIENT
        TransferFlowStep.RECIPIENT -> when (target) {
            TransferFlowStep.AMOUNT -> TransferFlowStep.AMOUNT
            TransferFlowStep.ACCOUNT -> TransferFlowStep.ACCOUNT
            else -> null
        }

        TransferFlowStep.ACCOUNT -> when (target) {
            TransferFlowStep.RECIPIENT -> TransferFlowStep.RECIPIENT
            TransferFlowStep.AMOUNT -> TransferFlowStep.AMOUNT
            else -> null
        }
    }
}
