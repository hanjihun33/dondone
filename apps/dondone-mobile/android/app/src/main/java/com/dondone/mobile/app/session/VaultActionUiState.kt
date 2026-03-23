package com.dondone.mobile.app.session

enum class VaultSubmittingAction {
    DEPOSIT_CREATE,
    WITHDRAW_CREATE
}

enum class VaultMessagePresentation {
    INLINE,
    TOAST_ONLY
}

data class VaultActionUiState(
    val isSubmitting: Boolean = false,
    val submittingAction: VaultSubmittingAction? = null,
    val message: String? = null,
    val isError: Boolean = false,
    val messagePresentation: VaultMessagePresentation = VaultMessagePresentation.INLINE
)
