package com.dondone.mobile.feature.workproof.presentation

data class WorkproofPdfPreviewUiState(
    val isLoading: Boolean = false,
    val preview: WorkproofPdfPreviewUiModel? = null,
    val errorMessage: String? = null
)

data class WorkproofPdfCreateUiState(
    val isSubmitting: Boolean = false,
    val requestId: String? = null,
    val pollUrl: String? = null,
    val errorMessage: String? = null
)

data class WorkproofPdfPreviewUiModel(
    val workplaceName: String,
    val periodText: String,
    val totalRecordCountText: String,
    val editedCountText: String,
    val attachmentCountText: String,
    val totalWorkedHoursText: String,
    val sectionSummaryText: String
)
