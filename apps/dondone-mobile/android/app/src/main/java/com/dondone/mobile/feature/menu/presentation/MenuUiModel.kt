package com.dondone.mobile.feature.menu.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.domain.model.DemoState

data class MenuDocumentUiModel(
    val title: String,
    val updatedAtText: String,
    val statusText: String,
    val statusTone: BadgeTone
)

data class MenuUiModel(
    val currentDateText: String,
    val documents: List<MenuDocumentUiModel>
)

fun DemoState.toMenuUiModel(): MenuUiModel {
    return MenuUiModel(
        currentDateText = "${demo.year}-${demo.month.toString().padStart(2, '0')}-${demo.asOfDay.toString().padStart(2, '0')}",
        documents = documents.map { document ->
            MenuDocumentUiModel(
                title = document.title,
                updatedAtText = "업데이트: ${document.updatedAt ?: "-"}",
                statusText = document.status,
                statusTone = if (document.status == "READY") BadgeTone.Success else BadgeTone.Warning
            )
        }
    )
}
