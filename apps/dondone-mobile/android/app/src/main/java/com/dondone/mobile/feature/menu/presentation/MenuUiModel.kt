package com.dondone.mobile.feature.menu.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.data.auth.AuthSession
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferStatus

enum class MenuDocumentAccent {
    Proof,
    Claim
}

enum class MenuReceiptStatus {
    Pending,
    Confirmed
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

data class MenuReceiptUiModel(
    val title: String,
    val status: MenuReceiptStatus,
    val statusText: String,
    val statusDetailText: String,
    val updatedAtText: String,
    val networkLabel: String,
    val txHashSectionTitle: String,
    val txHashLabel: String,
    val txHashFullText: String,
    val helperText: String,
    val pendingNoticeText: String?,
    val explorerButtonText: String,
    val explorerUrl: String,
    val shareButtonText: String,
    val shareText: String
)

data class MenuUiModel(
    val session: MenuSessionUiModel?,
    val documents: List<MenuDocumentUiModel>,
    val receipt: MenuReceiptUiModel?
)

data class MenuSessionUiModel(
    val name: String,
    val email: String
)

fun DemoState.toMenuUiModel(session: AuthSession?): MenuUiModel {
    val receiptDocument = documents.firstOrNull { it.id.contains("RECEIPT") }

    return MenuUiModel(
        session = session?.let {
            MenuSessionUiModel(
                name = it.name,
                email = it.email
            )
        },
        documents = documents
            .filterNot { it.id.contains("RECEIPT") }
            .map { document ->
                val accent = if (document.id.contains("PROOF")) {
                    MenuDocumentAccent.Proof
                } else {
                    MenuDocumentAccent.Claim
                }
                val isReady = document.status == "READY"
                MenuDocumentUiModel(
                    id = document.id,
                    title = document.title,
                    summaryText = when (accent) {
                        MenuDocumentAccent.Proof -> "근무 기록과 차액 검토 근거를 묶어 둔 문서예요."
                        MenuDocumentAccent.Claim -> "신고 준비에 필요한 핵심 자료를 한 번에 정리해요."
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
            },
        receipt = receiptDocument?.let { toMenuReceiptUiModel(it.title, it.updatedAt) }
    )
}

private fun DemoState.toMenuReceiptUiModel(
    title: String,
    updatedAt: String?
): MenuReceiptUiModel {
    val receiptStatus = if (remittance.status == TransferStatus.SUBMITTED) {
        MenuReceiptStatus.Pending
    } else {
        MenuReceiptStatus.Confirmed
    }

    return MenuReceiptUiModel(
        title = title,
        status = receiptStatus,
        statusText = if (receiptStatus == MenuReceiptStatus.Confirmed) "완료" else "확인 중",
        statusDetailText = if (receiptStatus == MenuReceiptStatus.Confirmed) {
            "메뉴에서 최근 송금 영수증과 전송 해시를 다시 확인할 수 있어요."
        } else {
            "네트워크 확인이 끝나면 영수증 상태가 최종 확정돼요."
        },
        updatedAtText = updatedAt?.let { "업데이트 $it" } ?: "아직 생성되지 않았어요",
        networkLabel = "Sepolia",
        txHashSectionTitle = "전송 해시",
        txHashLabel = shortenReceiptHash(remittance.txHash),
        txHashFullText = remittance.txHash,
        helperText = "영수증 링크와 해시는 나중에 다시 확인하거나 공유할 때 그대로 사용할 수 있어요.",
        pendingNoticeText = if (receiptStatus == MenuReceiptStatus.Pending) {
            "네트워크 확인이 끝나면 영수증 상태가 자동으로 완료로 바뀝니다."
        } else {
            null
        },
        explorerButtonText = "Explorer에서 보기",
        explorerUrl = buildReceiptExplorerUrl(remittance.txHash),
        shareButtonText = "공유",
        shareText = buildReceiptShareText(receiptStatus)
    )
}

private fun DemoState.buildReceiptShareText(
    receiptStatus: MenuReceiptStatus
): String {
    return buildString {
        append("DonDone 테스트넷 송금 영수증\n")
        append("상태: ")
        append(if (receiptStatus == MenuReceiptStatus.Confirmed) "완료" else "확인 중")
        append('\n')
        append("금액: ")
        append(
            if (remittance.destinationMode == TransferDestinationMode.ACCOUNT) {
                formatKrw(remittance.draftAmountUsd * 1_450)
            } else {
                "${'$'}${remittance.draftAmountUsd} USDC"
            }
        )
        append('\n')
        append("받는 사람: ")
        append(remittance.displayedRecipientName())
        append('\n')
        append("Tx Hash: ")
        append(remittance.txHash)
        append('\n')
        append("Explorer: ")
        append(buildReceiptExplorerUrl(remittance.txHash))
    }
}

private fun shortenReceiptHash(txHash: String): String {
    return if (txHash.length <= 18) {
        txHash
    } else {
        "${txHash.take(10)}...${txHash.takeLast(8)}"
    }
}

private fun buildReceiptExplorerUrl(txHash: String): String =
    "https://sepolia.etherscan.io/tx/$txHash"
