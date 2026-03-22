package com.dondone.mobile.app.session

import com.dondone.mobile.domain.model.TransactionCategory

data class TransactionMetadataOverride(
    val category: TransactionCategory,
    val memo: String
)
