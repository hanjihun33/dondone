package com.dondone.mobile.app.session

data class WorkproofActionUiState(
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)
