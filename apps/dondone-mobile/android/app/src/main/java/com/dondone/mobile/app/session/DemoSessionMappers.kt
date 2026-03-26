package com.dondone.mobile.app.session

import com.dondone.mobile.data.documents.WorkproofDocumentPreviewPayload
import com.dondone.mobile.data.remittance.RemittanceRemotePayload
import com.dondone.mobile.data.remittance.RemittanceTransferDetailPayload
import com.dondone.mobile.data.remittance.RemittanceTransferPrecheckPayload
import com.dondone.mobile.data.vault.VaultActionType
import com.dondone.mobile.data.vault.VaultSummaryPayload
import com.dondone.mobile.data.vault.VaultTransactionDetailPayload
import com.dondone.mobile.data.wage.WageRemotePayload
import com.dondone.mobile.data.wage.WageVerificationDetailPayload
import com.dondone.mobile.data.workproof.WorkproofCorrectionStatus
import com.dondone.mobile.data.workproof.WorkproofRemotePayload
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.DocumentItem
import com.dondone.mobile.domain.model.Recipient
import com.dondone.mobile.domain.model.TodayWork
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.domain.model.WorkRecord
import com.dondone.mobile.domain.model.remittanceRelationCodeToLabel
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfPreviewUiModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val WAGE_DOCUMENT_STATUS_READY = "READY"
private const val WAGE_DOCUMENT_STATUS_NOT_CREATED = "NOT_CREATED"
private const val WAGE_DOCUMENT_ID_PROOF = "PROOF"
private const val WAGE_DOCUMENT_ID_CLAIM = "CLAIM"
private val WorkproofPdfPreviewDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

internal fun DemoState.syncRemoteWorkproof(payload: WorkproofRemotePayload): DemoState {
    val systemToday = LocalDate.now()
    val activeRecord = payload.records.firstOrNull { record ->
        record.status == "CHECKED_IN" && record.checkOutDeviceAt == null
    }
    val selectedTodayRecord = activeRecord
        ?: payload.records.firstOrNull { it.workDate == systemToday }
        ?: payload.records.firstOrNull()
    val selectedDate = selectedTodayRecord?.workDate ?: systemToday
    val nextRecords = payload.records
        .sortedByDescending { it.workDate }
        .map { record ->
            val actualClockIn = record.checkInDeviceAt.toLocalTime().toString().take(5)
            val actualClockOut = record.checkOutDeviceAt?.toLocalTime()?.toString()?.take(5) ?: "-"
            WorkRecord(
                id = record.recordId.toString(),
                workDate = record.workDate,
                day = record.workDate.dayOfMonth,
                inTime = actualClockIn,
                outTime = actualClockOut,
                modified = record.modified || record.reflectionStatus != "PENDING",
                attachments = 0,
                reflectionStatus = record.reflectionStatus,
                decisionMemo = record.decisionMemo,
                recognizedInTime = record.recognizedClockInAt?.toLocalTime()?.toString()?.take(5)
                    ?: actualClockIn,
                recognizedOutTime = record.recognizedClockOutAt?.toLocalTime()?.toString()?.take(5)
                    ?: actualClockOut.takeUnless { it == "-" }
            )
        }

    return copy(
        demo = demo.copy(
            year = selectedDate.year,
            month = selectedDate.monthValue,
            monthLength = selectedDate.lengthOfMonth(),
            asOfDay = selectedDate.dayOfMonth
        ),
        workproof = workproof.copy(
            workplaceName = payload.workplace.name,
            workplaceAddress = payload.workplace.address,
            workplaceLatitude = payload.workplace.latitude,
            workplaceLongitude = payload.workplace.longitude,
            today = TodayWork(
                clockIn = selectedTodayRecord?.checkInDeviceAt?.toLocalTime()?.toString()?.take(5),
                clockOut = selectedTodayRecord?.checkOutDeviceAt?.toLocalTime()?.toString()?.take(5)
            ),
            records = nextRecords,
            audit = emptyList(),
            workplaceId = payload.workplace.workplaceId,
            allowedRadiusMeters = payload.workplace.allowedRadiusMeters
        )
    )
}

internal fun WorkproofDocumentPreviewPayload.toUiModel(): WorkproofPdfPreviewUiModel {
    return WorkproofPdfPreviewUiModel(
        workplaceName = workplaceName,
        periodText = "${startDate.format(WorkproofPdfPreviewDateFormatter)} - ${endDate.format(WorkproofPdfPreviewDateFormatter)}",
        totalRecordCountText = "${totalRecordCount}건",
        editedCountText = "${editedCount}건",
        attachmentCountText = "${attachmentCount}건",
        totalWorkedHoursText = totalWorkedHoursText,
        sectionSummaryText = "출퇴근 기록, 수정 이력, 기간 요약"
    )
}

internal fun DemoState.syncRemoteWage(payload: WageRemotePayload): DemoState {
    val actualDepositAmount = payload.summary.actualDepositAmount?.toInt()
    return copy(
        wage = wage.copy(
            workDays = payload.summary.workDays,
            totalHours = (payload.summary.totalWorkedMinutes / 60L).toInt(),
            overtimeHours = (payload.summary.overtimeMinutes / 60L).toInt(),
            nightHours = (payload.summary.nightMinutes / 60L).toInt(),
            hourly = payload.monthlySummary.normalizedHourlyWage.toInt(),
            deductionsKnown = payload.summary.deductionsKnown,
            actualDepositRecordedDay = payload.summary.actualDepositRecordedDate?.dayOfMonth
                ?: payload.summary.actualDepositRecordedDay,
            actualDeposit = actualDepositAmount ?: wage.actualDeposit,
            paydayDay = payload.summary.paydayDay
        ),
        workproof = workproof.copy(
            workplaceId = payload.workplaceId,
            workplaceName = payload.workplaceName
        ),
        documents = documents.syncWageDocuments(
            year = demo.year,
            month = demo.month,
            day = demo.asOfDay,
            latestVerification = payload.latestVerification
        )
    )
}

internal fun DemoState.syncRemoteRemittance(payload: RemittanceRemotePayload): DemoState {
    val latestTransfer = payload.activeTransfer
    val inFlightTransfer = latestTransfer?.takeUnless { it.isTerminalStatus() }
    val shouldShowTerminalTracker =
        latestTransfer?.isTerminalStatus() == true &&
            remittance.status in setOf(
                TransferStatus.SUBMITTED,
                TransferStatus.CONFIRMED,
                TransferStatus.FAILED
            )
    val nextRecipients = payload.recipients.map { recipient ->
        Recipient(
            id = recipient.recipientId,
            name = recipient.alias,
            relationship = remittanceRelationCodeToLabel(recipient.relation),
            address = recipient.walletAddress
        )
    }
    val selectedRecipientId = when {
        nextRecipients.isEmpty() -> remittance.selectedRecipientId
        nextRecipients.any { it.id == remittance.selectedRecipientId } -> remittance.selectedRecipientId
        else -> nextRecipients.first().id
    }

    return copy(
        remittance = remittance.copy(
            recipients = nextRecipients,
            selectedRecipientId = selectedRecipientId,
            destinationMode = TransferDestinationMode.WALLET,
            txHash = when {
                inFlightTransfer != null -> inFlightTransfer.txHash ?: remittance.txHash
                shouldShowTerminalTracker -> latestTransfer?.txHash ?: remittance.txHash
                else -> remittance.txHash
            },
            status = when {
                inFlightTransfer != null -> inFlightTransfer.toUiTransferStatus()
                shouldShowTerminalTracker -> latestTransfer?.toUiTransferStatus() ?: remittance.status
                else -> remittance.status
            }
        )
    )
}

internal fun DemoState.syncTransferStatus(detail: RemittanceTransferDetailPayload): DemoState {
    return copy(
        remittance = remittance.copy(
            txHash = detail.txHash ?: remittance.txHash,
            status = detail.toUiTransferStatus()
        )
    )
}

internal fun String.toWorkproofReasonLabel(): String {
    return when (this) {
        "LATE_BUTTON_PRESS" -> "퇴근 버튼을 늦게 눌렀어요."
        "LATE_CLOCK_IN" -> "출근 시간을 다시 인정해 주세요."
        "EARLY_CLOCK_OUT" -> "퇴근 시간을 다시 인정해 주세요."
        else -> "기타 사유로 수정 요청해요."
    }
}

internal fun String.toWorkproofLocalTimeOrNull(): LocalTime? {
    return runCatching { LocalTime.parse(this.trim()) }.getOrNull()
}

internal fun WorkproofCorrectionStatus.toWorkproofSuccessMessage(): String {
    return when (this) {
        WorkproofCorrectionStatus.APPROVED -> "수정 요청이 바로 반영됐어요."
        WorkproofCorrectionStatus.PENDING -> "수정 요청이 검토 대기열에 등록됐어요."
        WorkproofCorrectionStatus.REJECTED -> "수정 요청이 반려됐어요."
    }
}

internal fun List<DocumentItem>.syncWageDocuments(
    year: Int,
    month: Int,
    day: Int,
    latestVerification: WageVerificationDetailPayload?
): List<DocumentItem> {
    if (latestVerification == null) return this

    val relatedActions = latestVerification.relatedActions
    val fallbackUpdatedAt = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')} 18:00"

    return map { document ->
        when {
            document.id.contains(WAGE_DOCUMENT_ID_PROOF) -> document.copy(
                status = if (relatedActions.proofPackDocumentId != null) {
                    WAGE_DOCUMENT_STATUS_READY
                } else {
                    WAGE_DOCUMENT_STATUS_NOT_CREATED
                },
                updatedAt = if (relatedActions.proofPackDocumentId != null) {
                    document.updatedAt ?: fallbackUpdatedAt
                } else {
                    null
                }
            )

            document.id.contains(WAGE_DOCUMENT_ID_CLAIM) -> document.copy(
                status = if (relatedActions.claimKitDocumentId != null) {
                    WAGE_DOCUMENT_STATUS_READY
                } else {
                    WAGE_DOCUMENT_STATUS_NOT_CREATED
                },
                updatedAt = if (relatedActions.claimKitDocumentId != null) {
                    document.updatedAt ?: fallbackUpdatedAt
                } else {
                    null
                }
            )

            else -> document
        }
    }
}

internal fun RemittanceTransferDetailPayload.toUiTransferStatus(): TransferStatus = when (status) {
    "CONFIRMED" -> TransferStatus.CONFIRMED
    "FAILED", "TIMED_OUT" -> TransferStatus.FAILED
    else -> TransferStatus.SUBMITTED
}

internal fun RemittanceTransferDetailPayload.isTerminalStatus(): Boolean =
    status == "CONFIRMED" || status == "FAILED" || status == "TIMED_OUT"

internal fun RemittanceTransferDetailPayload.toCompletionNoticeUiState(): RemittanceCompletionNoticeUiState =
    RemittanceCompletionNoticeUiState(
        transferId = transferId,
        status = toUiTransferStatus(),
        recipientName = recipientAlias,
        amountAtomic = amountAtomic,
        assetSymbol = assetSymbol,
        txHash = txHash
    )

internal fun RemittanceTransferDetailPayload.toCompletionToastMessage(): String =
    when (toUiTransferStatus()) {
        TransferStatus.CONFIRMED -> "송금이 완료됐어요."
        TransferStatus.FAILED -> "송금이 실패했어요."
        else -> "송금 상태를 확인하고 있어요."
    }

internal fun RemittanceTransferPrecheckPayload?.requiresHighAmountConfirmation(): Boolean =
    this?.policyCode == "HIGH_AMOUNT_CONFIRMATION_REQUIRED"

internal fun RemittanceTransferPrecheckPayload.isConfirmable(): Boolean =
    policyCode == "RECENT_RECIPIENT_CONFIRMATION_REQUIRED" || policyCode == "HIGH_AMOUNT_CONFIRMATION_REQUIRED"

internal fun RemittanceTransferPrecheckPayload.resolveBlockedMessage(): String {
    return when (policyCode) {
        "INSUFFICIENT_WALLET_BALANCE" -> "송금 지갑 잔액이 부족해요. 잠시 후 다시 확인해 주세요."
        "RECIPIENT_NOT_ALLOWED" -> "허용된 수신자만 송금할 수 있어요."
        "TRANSFER_ALREADY_IN_PROGRESS" -> "진행 중인 송금이 있어 잠시 후 다시 시도해 주세요."
        "SELF_TRANSFER_NOT_ALLOWED" -> "내 지갑으로는 송금할 수 없어요."
        else -> "현재는 이 송금을 진행할 수 없어요."
    }
}

internal fun VaultActionType.toSubmittingAction(): VaultSubmittingAction =
    when (this) {
        VaultActionType.DEPOSIT -> VaultSubmittingAction.DEPOSIT_CREATE
        VaultActionType.WITHDRAW -> VaultSubmittingAction.WITHDRAW_CREATE
    }

internal fun VaultActionType.toCreateFailureMessage(): String =
    when (this) {
        VaultActionType.DEPOSIT -> "예치 요청을 보내지 못했어요."
        VaultActionType.WITHDRAW -> "출금 요청을 보내지 못했어요."
    }

internal fun VaultActionType.toCreateSuccessMessage(): String =
    when (this) {
        VaultActionType.DEPOSIT -> "예치 요청을 접수했어요."
        VaultActionType.WITHDRAW -> "출금 요청을 접수했어요."
    }

internal fun VaultSummaryPayload.availableAmountFor(actionType: VaultActionType): Int =
    when (actionType) {
        VaultActionType.DEPOSIT -> availableToStoreAmountAtomic.toWholeAssetUnits(assetDecimals)
        VaultActionType.WITHDRAW -> storedAmountAtomic.toWholeAssetUnits(assetDecimals)
    }

internal fun String.toWholeAssetUnits(decimals: Int): Int {
    val sanitized = trim().ifBlank { return 0 }
    if (sanitized == "0") return 0
    if (decimals <= 0) {
        return sanitized.toLongOrNull()
            ?.coerceIn(0L, Int.MAX_VALUE.toLong())
            ?.toInt()
            ?: 0
    }
    val wholePart = if (sanitized.length > decimals) {
        sanitized.dropLast(decimals)
    } else {
        "0"
    }
    return wholePart.toLongOrNull()
        ?.coerceIn(0L, Int.MAX_VALUE.toLong())
        ?.toInt()
        ?: 0
}

internal fun Int.toAtomicAmount(decimals: Int): Long {
    var scale = 1L
    repeat(decimals) {
        scale *= 10L
    }
    return toLong() * scale
}

internal fun pickDefaultVaultAmount(availableAmount: Int): Int {
    val presets = listOf(10, 25, 50, 100)
    return presets.lastOrNull { it in 1..availableAmount }
        ?: availableAmount
}

internal fun VaultTransactionDetailPayload.isTerminalStatus(): Boolean =
    status == "CONFIRMED" || status == "FAILED" || status == "TIMED_OUT"

internal fun VaultTransactionDetailPayload.toCompletionUiState(): VaultActionUiState {
    return when (status) {
        "CONFIRMED" -> VaultActionUiState(
            message = if (txType == "WITHDRAW") "출금이 완료됐어요." else "예치가 완료됐어요.",
            messagePresentation = VaultMessagePresentation.TOAST_ONLY
        )

        else -> VaultActionUiState(
            message = if (txType == "WITHDRAW") {
                "출금이 완료되지 않았어요. 상태를 다시 확인해 주세요."
            } else {
                "예치가 완료되지 않았어요. 상태를 다시 확인해 주세요."
            },
            isError = true,
            messagePresentation = VaultMessagePresentation.TOAST_ONLY
        )
    }
}
