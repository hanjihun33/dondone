package com.dondone.mobile.feature.menu.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.data.auth.AuthSession
import com.dondone.mobile.domain.model.DemoState

enum class MenuDocumentAccent {
    Proof,
    Claim,
    Receipt
}

data class MenuDocumentUiModel(
    val id: String,
    val title: String,
    val summaryText: String,
    val updatedAtText: String,
    val statusText: String,
    val statusTone: BadgeTone,
    val accent: MenuDocumentAccent
)

data class MenuUiModel(
    val session: MenuSessionUiModel?,
    val documents: List<MenuDocumentUiModel>
)

data class MenuSessionUiModel(
    val name: String,
    val email: String
)

fun DemoState.toMenuUiModel(session: AuthSession?): MenuUiModel {
    return MenuUiModel(
        session = session?.let {
            MenuSessionUiModel(
                name = it.name,
                email = it.email
            )
        },
        documents = documents.map { document ->
            val accent = when {
                document.id.contains("PROOF") -> MenuDocumentAccent.Proof
                document.id.contains("CLAIM") -> MenuDocumentAccent.Claim
                else -> MenuDocumentAccent.Receipt
            }
            val isReady = document.status == "READY"
            MenuDocumentUiModel(
                id = document.id,
                title = document.title,
                summaryText = when (accent) {
                    MenuDocumentAccent.Proof -> "근무 기록과 차액 검토 근거를 묶어 둔 문서예요."
                    MenuDocumentAccent.Claim -> "신고 준비에 필요한 핵심 자료를 한 번에 정리해요."
                    MenuDocumentAccent.Receipt -> "최근 송금 결과와 해시를 다시 확인할 수 있어요."
                },
                updatedAtText = document.updatedAt?.let { "업데이트 $it" } ?: "아직 생성되지 않았어요",
                statusText = when {
                    isReady -> "준비됨"
                    accent == MenuDocumentAccent.Claim -> "대기"
                    else -> "검토 필요"
                },
                statusTone = if (isReady) BadgeTone.Success else BadgeTone.Warning,
                accent = accent
            )
        }
    )
}
