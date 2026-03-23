package com.dondone.mobile.feature.recipient.presentation

import com.dondone.mobile.app.session.RemittanceActionUiState

internal fun resolveRecipientSheetErrorMessage(
    isAwaitingResult: Boolean,
    actionUiState: RemittanceActionUiState
): String? {
    return if (
        isAwaitingResult &&
        !actionUiState.isSubmitting &&
        actionUiState.isError
    ) {
        actionUiState.message
    } else {
        null
    }
}

internal fun shouldCloseRecipientSheetAfterResult(
    isAwaitingResult: Boolean,
    actionUiState: RemittanceActionUiState
): Boolean {
    return isAwaitingResult &&
        !actionUiState.isSubmitting &&
        !actionUiState.isError &&
        actionUiState.message != null
}
