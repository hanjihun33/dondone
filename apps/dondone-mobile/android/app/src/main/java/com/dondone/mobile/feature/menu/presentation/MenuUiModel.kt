package com.dondone.mobile.feature.menu.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.text
import com.dondone.mobile.core.i18n.translate
import com.dondone.mobile.core.ui.toDisplayPhoneNumber
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.data.auth.AuthSession
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.data.remittance.RemittanceTransferDetailPayload
import com.dondone.mobile.data.workproof.WorkproofRemoteMode
import com.dondone.mobile.data.workproof.WorkproofRemoteState
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
private const val RECEIPT_NETWORK_LABEL = "세폴리아"
private const val RECEIPT_HASH_SECTION_TITLE = "전송 해시"
private const val RECEIPT_HELPER_TEXT = "영수증 링크와 해시는 나중에 다시 확인하거나 공유할 때 그대로 사용할 수 있어요."
private const val RECEIPT_EXPLORER_BUTTON_TEXT = "블록 탐색기에서 보기"
private const val RECEIPT_SHARE_BUTTON_TEXT = "공유"
private const val RECEIPT_SHARE_TITLE = "DonDone 테스트넷 송금 영수증\n"
private const val RECEIPT_SHARE_STATUS_PREFIX = "상태: "
private const val RECEIPT_SHARE_AMOUNT_PREFIX = "금액: "
private const val RECEIPT_SHARE_RECIPIENT_PREFIX = "받는 사람: "
private const val RECEIPT_SHARE_HASH_PREFIX = "전송 해시: "
private const val RECEIPT_SHARE_EXPLORER_PREFIX = "블록 탐색기: "
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
    val receipt: MenuReceiptUiModel?,
    val fallbackNoticeTitle: String? = null,
    val fallbackNoticeMessage: String? = null
)

data class MenuSessionUiModel(
    val name: String,
    val email: String,
    val phoneNumber: String?,
    val companyName: String?,
    val workplaceName: String?
)

fun DemoState.toMenuUiModel(
    session: AuthSession?,
    remittanceRemoteState: RemittanceRemoteState,
    workproofPdfCreateUiState: WorkproofPdfCreateUiState,
    language: AppLanguage = AppLanguage.KOREAN,
    workproofRemoteState: WorkproofRemoteState = WorkproofRemoteState.unauthenticated("")
): MenuUiModel {
    fun tr(text: String): String = language.translate(text)

    val receiptDocument = documents.firstOrNull(DocumentItem::isReceiptDocument)
    val baseDocuments = documents
        .filterNot(DocumentItem::isReceiptDocument)
        .map { it.toMenuDocumentUiModel(language) }
    val liveProofDocument = workproofPdfCreateUiState.toLiveProofDocument(language)
    val mergedDocuments = if (liveProofDocument != null) {
        listOf(liveProofDocument) + baseDocuments.filterNot { it.accent == MenuDocumentAccent.Proof }
    } else {
        baseDocuments
    }
    val remoteTransfer = remittanceRemoteState.payload?.activeTransfer
    val remoteWorkplaceName = workproofRemoteState.payload?.workplace?.name
    val usesFallbackData = workproofRemoteState.mode != WorkproofRemoteMode.CONTENT

    return MenuUiModel(
        session = session?.let {
            MenuSessionUiModel(
                name = it.name,
                email = it.email,
                phoneNumber = it.phoneNumber?.toDisplayPhoneNumber(),
                companyName = it.companyName ?: it.companyCode,
                workplaceName = it.workplaceName ?: remoteWorkplaceName
            )
        },
        documents = mergedDocuments,
        receipt = when {
            remoteTransfer != null -> toRemoteMenuReceiptUiModel(remoteTransfer, receiptDocument, language)
            receiptDocument != null -> toMenuReceiptUiModel(receiptDocument, language)
            else -> null
        },
        fallbackNoticeTitle = if (usesFallbackData) tr("가상 예시 데이터") else null,
        fallbackNoticeMessage = if (usesFallbackData) {
            language.text("menu_demo_fallback_message")
        } else {
            null
        }
    )
}

private fun DocumentItem.toMenuDocumentUiModel(language: AppLanguage): MenuDocumentUiModel {
    val accent = toMenuDocumentAccent()
    val isReady = status == DOCUMENT_TYPE_READY
    fun tr(text: String): String = language.translate(text)

    return MenuDocumentUiModel(
        id = id,
        documentId = null,
        title = title,
        summaryText = tr(accent.summaryText()),
        updatedAtText = updatedAt.toMenuUpdatedAtText(language),
        statusText = tr(accent.statusText(isReady = isReady)),
        statusTone = if (isReady) BadgeTone.Success else BadgeTone.Warning,
        accent = accent
    )
}

private fun WorkproofPdfCreateUiState.toLiveProofDocument(language: AppLanguage): MenuDocumentUiModel? {
    val currentStatus = status ?: return null
    val isActionable = documentId != null && !isFailed
    val statusTone = when (currentStatus) {
        "FAILED" -> BadgeTone.Warning
        else -> if (isActionable) BadgeTone.Success else BadgeTone.Info
    }
    val statusText = when (currentStatus) {
        "QUEUED" -> if (isActionable) language.text("menu_document_open_ready") else language.text("menu_document_pending")
        "RUNNING" -> language.text("menu_document_generating")
        "READY" -> language.text("menu_document_ready")
        "FAILED" -> language.text("menu_document_generation_failed")
        else -> language.text("menu_document_pending")
    }
    val summaryText = when (currentStatus) {
        "FAILED" -> language.text("menu_live_proof_failed_summary")
        else -> if (isActionable) {
            language.text("menu_live_proof_ready_summary")
        } else {
            language.text("menu_live_proof_requested_summary")
        }
    }
    val updatedAtText = when (currentStatus) {
        "FAILED" -> language.text("menu_live_proof_generation_failed")
        else -> if (isActionable) language.text("menu_live_proof_open_ready") else language.text("menu_live_proof_request_saved")
    }

    return MenuDocumentUiModel(
        id = "LIVE-WORKPROOF-PDF",
        documentId = documentId,
        title = language.text("workproof_work_record_document"),
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

private fun String?.toMenuUpdatedAtText(language: AppLanguage): String =
    this?.let { language.translate("$MENU_UPDATED_AT_PREFIX$it") } ?: language.translate(MENU_UPDATED_AT_EMPTY)

private fun DemoState.toMenuReceiptUiModel(
    receiptDocument: DocumentItem,
    language: AppLanguage
): MenuReceiptUiModel {
    val receiptStatus = remittance.toMenuReceiptStatus()

    return MenuReceiptUiModel(
        title = receiptDocument.title,
        status = receiptStatus,
        statusText = receiptStatus.statusText(language),
        statusDetailText = receiptStatus.statusDetailText(language),
        updatedAtText = receiptDocument.updatedAt.toMenuUpdatedAtText(language),
        networkLabel = language.text("menu_network_label"),
        txHashSectionTitle = language.text("menu_tx_hash_section_title"),
        txHashLabel = shortenReceiptHash(remittance.txHash),
        txHashFullText = remittance.txHash,
        helperText = language.text("menu_receipt_helper"),
        pendingNoticeText = receiptStatus.pendingNoticeText(language),
        explorerButtonText = language.text("menu_view_in_block_explorer"),
        explorerUrl = buildReceiptExplorerUrl(remittance.txHash),
        shareButtonText = language.text("share"),
        shareText = buildReceiptShareText(receiptStatus, language)
    )
}

private fun DemoState.toRemoteMenuReceiptUiModel(
    transfer: RemittanceTransferDetailPayload,
    receiptDocument: DocumentItem?,
    language: AppLanguage
): MenuReceiptUiModel {
    val receiptStatus = transfer.toMenuReceiptStatus()
    val txHash = transfer.txHash ?: language.text("menu_receipt_missing_hash")

    return MenuReceiptUiModel(
        title = receiptDocument?.title ?: language.text("menu_receipt_title"),
        status = receiptStatus,
        statusText = receiptStatus.statusText(language),
        statusDetailText = receiptStatus.statusDetailText(language),
        updatedAtText = (receiptDocument?.updatedAt ?: transfer.updatedAt?.toString()).toMenuUpdatedAtText(language),
        networkLabel = language.text("menu_network_label"),
        txHashSectionTitle = language.text("menu_tx_hash_section_title"),
        txHashLabel = shortenReceiptHash(txHash),
        txHashFullText = txHash,
        helperText = language.text("menu_receipt_helper"),
        pendingNoticeText = receiptStatus.pendingNoticeText(language),
        explorerButtonText = language.text("menu_view_in_block_explorer"),
        explorerUrl = if (transfer.txHash.isNullOrBlank()) "" else buildReceiptExplorerUrl(transfer.txHash),
        shareButtonText = language.text("share"),
        shareText = buildRemoteReceiptShareText(transfer, receiptStatus, language)
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

private fun MenuReceiptStatus.statusText(language: AppLanguage): String {
    return when (this) {
        MenuReceiptStatus.Confirmed -> language.text("completed")
        MenuReceiptStatus.Failed -> language.text("menu_receipt_failed")
        MenuReceiptStatus.Pending -> language.text("menu_receipt_checking")
    }
}

private fun MenuReceiptStatus.statusDetailText(language: AppLanguage): String {
    return when (this) {
        MenuReceiptStatus.Confirmed -> language.text("menu_receipt_confirmed_detail")
        MenuReceiptStatus.Failed -> language.text("menu_receipt_failed_detail")
        MenuReceiptStatus.Pending -> language.text("menu_receipt_pending_detail")
    }
}

private fun MenuReceiptStatus.pendingNoticeText(language: AppLanguage): String? {
    return when (this) {
        MenuReceiptStatus.Pending -> language.text("menu_receipt_pending_notice")
        MenuReceiptStatus.Failed -> language.text("menu_receipt_failed_notice")
        MenuReceiptStatus.Confirmed -> null
    }
}

private fun DemoState.buildReceiptShareText(
    receiptStatus: MenuReceiptStatus,
    language: AppLanguage
): String {
    return buildString {
        append(language.text("menu_share_receipt_title"))
        append('\n')
        append(language.text("menu_status_prefix"))
        append(receiptStatus.statusText(language))
        append('\n')
        append(language.text("menu_amount_prefix"))
        append(
            if (remittance.destinationMode == TransferDestinationMode.ACCOUNT) {
                formatKrw(remittance.draftAmountUsd * 1_450, language)
            } else {
                "${'$'}${remittance.draftAmountUsd} USDC"
            }
        )
        append('\n')
        append(language.text("menu_recipient_prefix"))
        append(remittance.displayedRecipientNameOrFallback())
        append('\n')
        append(language.text("menu_hash_prefix"))
        append(remittance.txHash)
        append('\n')
        append(language.text("menu_explorer_prefix"))
        append(buildReceiptExplorerUrl(remittance.txHash))
    }
}

private fun DemoState.buildRemoteReceiptShareText(
    transfer: RemittanceTransferDetailPayload,
    receiptStatus: MenuReceiptStatus,
    language: AppLanguage
): String {
    return buildString {
        append(language.text("menu_share_receipt_title"))
        append('\n')
        append(language.text("menu_status_prefix"))
        append(receiptStatus.statusText(language))
        append('\n')
        append(language.text("menu_amount_prefix"))
        append("${'$'}${transfer.amountAtomic / 1_000_000L} ${transfer.assetSymbol}")
        append('\n')
        append(language.text("menu_recipient_prefix"))
        append(transfer.recipientAlias ?: transfer.recipientAddress)
        append('\n')
        append(language.text("menu_hash_prefix"))
        append(transfer.txHash ?: language.text("none"))
        if (!transfer.txHash.isNullOrBlank()) {
            append('\n')
            append(language.text("menu_explorer_prefix"))
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
