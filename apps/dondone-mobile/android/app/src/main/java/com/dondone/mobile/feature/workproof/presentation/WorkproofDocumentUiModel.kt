package com.dondone.mobile.feature.workproof.presentation

data class WorkproofPdfPreviewUiState(
    val isLoading: Boolean = false,
    val preview: WorkproofPdfPreviewUiModel? = null,
    val errorMessage: String? = null
)

data class WorkproofPdfCreateUiState(
    val isSubmitting: Boolean = false,
    val isPolling: Boolean = false,
    val requestId: String? = null,
    val documentId: Long? = null,
    val documentUrl: String? = null,
    val status: String? = null,
    val pollUrl: String? = null,
    val errorMessage: String? = null
) {
    val isReady: Boolean
        get() = status == "READY"

    val isFailed: Boolean
        get() = status == "FAILED"
}

data class WorkproofPdfFileUiState(
    val isDownloading: Boolean = false,
    val pendingAction: WorkproofPdfFileAction? = null,
    val fileUri: String? = null,
    val fileName: String? = null,
    val errorMessage: String? = null
)

enum class WorkproofPdfFileAction {
    OPEN,
    SHARE
}

data class WorkproofPdfPreviewUiModel(
    val workplaceName: String,
    val periodText: String,
    val totalRecordCountText: String,
    val editedCountText: String,
    val attachmentCountText: String,
    val totalWorkedHoursText: String,
    val sectionSummaryText: String
)
