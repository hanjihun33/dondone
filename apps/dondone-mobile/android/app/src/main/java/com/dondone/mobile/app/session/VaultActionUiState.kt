package com.dondone.mobile.app.session

enum class VaultSubmittingAction {
    DEPOSIT_CREATE,
    WITHDRAW_CREATE
}

data class VaultActionUiState(
    val isSubmitting: Boolean = false,
    val submittingAction: VaultSubmittingAction? = null,
    val message: String? = null,
    val isError: Boolean = false
)
