package com.dondone.mobile.feature.menu.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.toDisplayPhoneNumber
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.data.auth.AuthSession
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.data.remittance.RemittanceTransferDetailPayload
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.DocumentItem
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfCreateUiState

enum class MenuDocumentAccent {
    Proof,
    Claim,
    Receipt
}

enum class MenuReceiptStatus {
    Pending,
    Confirmed,
    Failed
}

private const val DOCUMENT_STATUS_READY = "준비됨"
private const val DOCUMENT_STATUS_PENDING = "대기"
private const val DOCUMENT_STATUS_GENERATING = "준비 중"
private const val RECEIPT_STATUS_PENDING = "확인 중"
private const val RECEIPT_STATUS_CONFIRMED = "완료"
private const val MENU_UPDATED_AT_PREFIX = "업데이트 "
private const val MENU_UPDATED_AT_EMPTY = "아직 생성되지 않았어요"
private const val RECEIPT_NETWORK_LABEL = "Sepolia"
private const val RECEIPT_HASH_SECTION_TITLE = "전송 해시"
private const val RECEIPT_HELPER_TEXT = "영수증 링크와 해시는 나중에 다시 확인하거나 공유할 때 그대로 사용할 수 있어요."
private const val RECEIPT_EXPLORER_BUTTON_TEXT = "Explorer에서 보기"
private const val RECEIPT_SHARE_BUTTON_TEXT = "공유"
private const val RECEIPT_SHARE_TITLE = "DonDone 테스트넷 송금 영수증\n"
private const val RECEIPT_SHARE_STATUS_PREFIX = "상태: "
private const val RECEIPT_SHARE_AMOUNT_PREFIX = "금액: "
private const val RECEIPT_SHARE_RECIPIENT_PREFIX = "받는 사람: "
private const val RECEIPT_SHARE_HASH_PREFIX = "Tx Hash: "
private const val RECEIPT_SHARE_EXPLORER_PREFIX = "Explorer: "
private const val DOCUMENT_TYPE_READY = "READY"
private const val DOCUMENT_ID_PROOF = "PROOF"
private const val DOCUMENT_ID_RECEIPT = "RECEIPT"

data class MenuDocumentUiModel(
    val id: String,
    val documentId: Long?,
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
    val email: String,
    val phoneNumber: String?
)

fun DemoState.toMenuUiModel(
    session: AuthSession?,
    remittanceRemoteState: RemittanceRemoteState,
    workproofPdfCreateUiState: WorkproofPdfCreateUiState
): MenuUiModel {
    val receiptDocument = documents.firstOrNull(DocumentItem::isReceiptDocument)
    val baseDocuments = documents.map(DocumentItem::toMenuDocumentUiModel)
    val liveProofDocument = workproofPdfCreateUiState.toLiveProofDocument()
    val mergedDocuments = if (liveProofDocument != null) {
        listOf(liveProofDocument) + baseDocuments.filterNot { it.accent == MenuDocumentAccent.Proof }
    } else {
        baseDocuments
    }
    val remoteTransfer = remittanceRemoteState.payload?.activeTransfer

    return MenuUiModel(
        session = session?.let {
            MenuSessionUiModel(
                name = it.name,
                email = it.email,
                phoneNumber = it.phoneNumber?.toDisplayPhoneNumber()
            )
        },
        documents = mergedDocuments,
        receipt = when {
            remoteTransfer != null -> toRemoteMenuReceiptUiModel(remoteTransfer, receiptDocument)
            receiptDocument != null -> toMenuReceiptUiModel(receiptDocument)
            else -> null
        }
    )
}

private fun DocumentItem.toMenuDocumentUiModel(): MenuDocumentUiModel {
    val accent = toMenuDocumentAccent()
    val isReady = status == DOCUMENT_TYPE_READY

    return MenuDocumentUiModel(
        id = id,
        documentId = null,
        title = title,
        summaryText = accent.summaryText(),
        updatedAtText = updatedAt.toMenuUpdatedAtText(),
        statusText = accent.statusText(isReady = isReady),
        statusTone = if (isReady) BadgeTone.Success else BadgeTone.Warning,
        accent = accent
    )
}

private fun WorkproofPdfCreateUiState.toLiveProofDocument(): MenuDocumentUiModel? {
    val currentStatus = status ?: return null
    val isActionable = documentId != null && !isFailed
    val statusTone = when (currentStatus) {
        "FAILED" -> BadgeTone.Warning
        else -> if (isActionable) BadgeTone.Success else BadgeTone.Info
    }
    val statusText = when (currentStatus) {
        "QUEUED" -> if (isActionable) "열기 가능" else DOCUMENT_STATUS_PENDING
        "RUNNING" -> DOCUMENT_STATUS_GENERATING
        "READY" -> DOCUMENT_STATUS_READY
        "FAILED" -> "생성 실패"
        else -> DOCUMENT_STATUS_PENDING
    }
    val summaryText = when (currentStatus) {
        "FAILED" -> "근무 기록 문서 생성이 실패했어요. 기간을 다시 선택해 재시도해 주세요."
        else -> if (isActionable) {
            "메뉴에서 문서를 열거나 공유할 때 선택한 기간의 PDF를 바로 생성할 수 있어요."
        } else {
            "선택한 기간의 출퇴근 기록과 변경 이력을 정리한 PDF 문서 요청이 저장됐어요."
        }
    }
    val updatedAtText = when (currentStatus) {
        "FAILED" -> "업데이트 생성 실패"
        else -> if (isActionable) "업데이트 바로 생성 가능" else "업데이트 생성 요청 접수됨"
    }

    return MenuDocumentUiModel(
        id = "LIVE-WORKPROOF-PDF",
        documentId = documentId,
        title = "근무 기록 문서",
        summaryText = summaryText,
        updatedAtText = updatedAtText,
        statusText = statusText,
        statusTone = statusTone,
        accent = MenuDocumentAccent.Proof
    )
}

private fun DocumentItem.toMenuDocumentAccent(): MenuDocumentAccent {
    return when {
        id.contains(DOCUMENT_ID_PROOF) -> MenuDocumentAccent.Proof
        isReceiptDocument() -> MenuDocumentAccent.Receipt
        else -> MenuDocumentAccent.Claim
    }
}

private fun DocumentItem.isReceiptDocument(): Boolean = id.contains(DOCUMENT_ID_RECEIPT)

private fun MenuDocumentAccent.summaryText(): String {
    return when (this) {
        MenuDocumentAccent.Proof -> "선택한 기간의 출퇴근 기록과 변경 이력을 정리한 PDF 문서예요."
        MenuDocumentAccent.Claim -> "신고 준비에 필요한 핵심 자료를 한 번에 정리해요."
        MenuDocumentAccent.Receipt -> "최근 송금 영수증과 전송 해시를 다시 확인할 수 있어요."
    }
}

private fun MenuDocumentAccent.statusText(isReady: Boolean): String {
    return when {
        isReady -> DOCUMENT_STATUS_READY
        this == MenuDocumentAccent.Receipt -> RECEIPT_STATUS_PENDING
        this == MenuDocumentAccent.Claim -> DOCUMENT_STATUS_PENDING
        else -> DOCUMENT_STATUS_GENERATING
    }
}

private fun String?.toMenuUpdatedAtText(): String =
    this?.let { "$MENU_UPDATED_AT_PREFIX$it" } ?: MENU_UPDATED_AT_EMPTY

private fun DemoState.toMenuReceiptUiModel(receiptDocument: DocumentItem): MenuReceiptUiModel {
    val receiptStatus = remittance.toMenuReceiptStatus()

    return MenuReceiptUiModel(
        title = receiptDocument.title,
        status = receiptStatus,
        statusText = receiptStatus.statusText(),
        statusDetailText = receiptStatus.statusDetailText(),
        updatedAtText = receiptDocument.updatedAt.toMenuUpdatedAtText(),
        networkLabel = RECEIPT_NETWORK_LABEL,
        txHashSectionTitle = RECEIPT_HASH_SECTION_TITLE,
        txHashLabel = shortenReceiptHash(remittance.txHash),
        txHashFullText = remittance.txHash,
        helperText = RECEIPT_HELPER_TEXT,
        pendingNoticeText = receiptStatus.pendingNoticeText(),
        explorerButtonText = RECEIPT_EXPLORER_BUTTON_TEXT,
        explorerUrl = buildReceiptExplorerUrl(remittance.txHash),
        shareButtonText = RECEIPT_SHARE_BUTTON_TEXT,
        shareText = buildReceiptShareText(receiptStatus)
    )
}

private fun DemoState.toRemoteMenuReceiptUiModel(
    transfer: RemittanceTransferDetailPayload,
    receiptDocument: DocumentItem?
): MenuReceiptUiModel {
    val receiptStatus = transfer.toMenuReceiptStatus()
    val txHash = transfer.txHash ?: "아직 전송 해시가 없어요"

    return MenuReceiptUiModel(
        title = receiptDocument?.title ?: "송금 영수증",
        status = receiptStatus,
        statusText = receiptStatus.statusText(),
        statusDetailText = receiptStatus.statusDetailText(),
        updatedAtText = (receiptDocument?.updatedAt ?: transfer.updatedAt?.toString()).toMenuUpdatedAtText(),
        networkLabel = RECEIPT_NETWORK_LABEL,
        txHashSectionTitle = RECEIPT_HASH_SECTION_TITLE,
        txHashLabel = shortenReceiptHash(txHash),
        txHashFullText = txHash,
        helperText = RECEIPT_HELPER_TEXT,
        pendingNoticeText = receiptStatus.pendingNoticeText(),
        explorerButtonText = RECEIPT_EXPLORER_BUTTON_TEXT,
        explorerUrl = if (transfer.txHash.isNullOrBlank()) "" else buildReceiptExplorerUrl(transfer.txHash),
        shareButtonText = RECEIPT_SHARE_BUTTON_TEXT,
        shareText = buildRemoteReceiptShareText(transfer, receiptStatus)
    )
}

private fun com.dondone.mobile.domain.model.RemittanceData.toMenuReceiptStatus(): MenuReceiptStatus {
    return if (status == TransferStatus.SUBMITTED) {
        MenuReceiptStatus.Pending
    } else if (status == TransferStatus.FAILED) {
        MenuReceiptStatus.Failed
    } else {
        MenuReceiptStatus.Confirmed
    }
}

private fun RemittanceTransferDetailPayload.toMenuReceiptStatus(): MenuReceiptStatus {
    return when (status) {
        "CONFIRMED" -> MenuReceiptStatus.Confirmed
        "FAILED", "TIMED_OUT" -> MenuReceiptStatus.Failed
        else -> MenuReceiptStatus.Pending
    }
}

private fun MenuReceiptStatus.statusText(): String {
    return when (this) {
        MenuReceiptStatus.Confirmed -> RECEIPT_STATUS_CONFIRMED
        MenuReceiptStatus.Failed -> "실패"
        MenuReceiptStatus.Pending -> RECEIPT_STATUS_PENDING
    }
}

private fun MenuReceiptStatus.statusDetailText(): String {
    return when (this) {
        MenuReceiptStatus.Confirmed -> "메뉴에서 최근 송금 영수증과 전송 해시를 다시 확인할 수 있어요."
        MenuReceiptStatus.Failed -> "전송이 완료되지 않아 실패 원인을 먼저 확인해야 해요."
        MenuReceiptStatus.Pending -> "네트워크 확인이 끝나면 영수증 상태가 최종 확정돼요."
    }
}

private fun MenuReceiptStatus.pendingNoticeText(): String? {
    return when (this) {
        MenuReceiptStatus.Pending -> "네트워크 확인이 끝나면 영수증 상태가 자동으로 완료로 바뀝니다."
        MenuReceiptStatus.Failed -> "실패한 전송은 영수증 대신 상태와 해시만 다시 확인할 수 있어요."
        MenuReceiptStatus.Confirmed -> null
    }
}

private fun DemoState.buildReceiptShareText(
    receiptStatus: MenuReceiptStatus
): String {
    return buildString {
        append(RECEIPT_SHARE_TITLE)
        append(RECEIPT_SHARE_STATUS_PREFIX)
        append(receiptStatus.statusText())
        append('\n')
        append(RECEIPT_SHARE_AMOUNT_PREFIX)
        append(
            if (remittance.destinationMode == TransferDestinationMode.ACCOUNT) {
                formatKrw(remittance.draftAmountUsd * 1_450)
            } else {
                "${'$'}${remittance.draftAmountUsd} USDC"
            }
        )
        append('\n')
        append(RECEIPT_SHARE_RECIPIENT_PREFIX)
        append(remittance.displayedRecipientNameOrFallback())
        append('\n')
        append(RECEIPT_SHARE_HASH_PREFIX)
        append(remittance.txHash)
        append('\n')
        append(RECEIPT_SHARE_EXPLORER_PREFIX)
        append(buildReceiptExplorerUrl(remittance.txHash))
    }
}

private fun DemoState.buildRemoteReceiptShareText(
    transfer: RemittanceTransferDetailPayload,
    receiptStatus: MenuReceiptStatus
): String {
    return buildString {
        append(RECEIPT_SHARE_TITLE)
        append(RECEIPT_SHARE_STATUS_PREFIX)
        append(receiptStatus.statusText())
        append('\n')
        append(RECEIPT_SHARE_AMOUNT_PREFIX)
        append("${'$'}${transfer.amountAtomic / 1_000_000L} ${transfer.assetSymbol}")
        append('\n')
        append(RECEIPT_SHARE_RECIPIENT_PREFIX)
        append(transfer.recipientAlias ?: transfer.recipientAddress)
        append('\n')
        append(RECEIPT_SHARE_HASH_PREFIX)
        append(transfer.txHash ?: "아직 없음")
        if (!transfer.txHash.isNullOrBlank()) {
            append('\n')
            append(RECEIPT_SHARE_EXPLORER_PREFIX)
            append(buildReceiptExplorerUrl(transfer.txHash))
        }
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

private fun com.dondone.mobile.domain.model.RemittanceData.displayedRecipientNameOrFallback(): String {
    return recipientDisplayNameOverride?.takeIf { it.isNotBlank() }
        ?: selectedRecipientOrNull()?.name
        ?: "-"
}
