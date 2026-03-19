package com.dondone.mobile.app.session

import com.dondone.mobile.data.remittance.RemittanceRecipientSearchPayload

data class RecipientPhoneSearchUiState(
    val isLoading: Boolean = false,
    val results: List<RemittanceRecipientSearchPayload> = emptyList(),
    val errorMessage: String? = null
)
