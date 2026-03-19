package com.dondone.mobile.app.session

import com.dondone.mobile.data.remittance.RemittanceTransferPrecheckPayload

enum class RemittanceSubmittingAction {
    RECIPIENT_CREATE,
    RECIPIENT_UPDATE,
    TRANSFER_PRECHECK,
    TRANSFER_CREATE
}

data class RemittanceActionUiState(
    val isSubmitting: Boolean = false,
    val submittingAction: RemittanceSubmittingAction? = null,
    val message: String? = null,
    val isError: Boolean = false,
    val precheck: RemittanceTransferPrecheckPayload? = null
) {
    val blocksTransferBackNavigation: Boolean
        get() = isSubmitting && submittingAction == RemittanceSubmittingAction.TRANSFER_CREATE
}
