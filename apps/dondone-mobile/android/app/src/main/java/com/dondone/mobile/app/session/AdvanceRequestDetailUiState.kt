package com.dondone.mobile.app.session

import com.dondone.mobile.data.advance.AdvanceRequestDetailPayload

data class AdvanceRequestDetailUiState(
    val isLoading: Boolean = false,
    val detail: AdvanceRequestDetailPayload? = null,
    val errorMessage: String? = null
)
