package com.dondone.mobile.app.session

data class WageActionUiState(
    val isSubmittingDeposit: Boolean = false,
    val isSubmittingVerification: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
) {
    val isSubmitting: Boolean
        get() = isSubmittingDeposit || isSubmittingVerification
}
