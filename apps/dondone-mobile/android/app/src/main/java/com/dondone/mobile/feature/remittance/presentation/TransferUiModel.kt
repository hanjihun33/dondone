package com.dondone.mobile.feature.remittance.presentation

import com.dondone.mobile.app.session.RemittanceActionUiState
import com.dondone.mobile.app.session.RecipientPhoneSearchUiState
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.text
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.data.remittance.RemittanceRemoteMode
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.feature.recipient.presentation.buildDemoRecipientDirectory
import com.dondone.mobile.feature.recipient.presentation.RecipientDirectoryContactUiModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

private const val REMITTANCE_WALLET_ID = "remote-wallet"

enum class TransferRecipientTone {
    Amber,
    Blue,
    Indigo,
    Teal
}

data class TransferAccountUiModel(
    val id: String,
    val name: String,
    val number: String,
    val balanceText: String,
    val selected: Boolean
)

data class TransferRecipientUiModel(
    val id: String,
    val name: String,
    val address: String,
    val relationship: String,
    val accountLabel: String,
    val contactLabel: String,
    val tone: TransferRecipientTone,
    val selected: Boolean
)

data class TransferRecipientSectionUiModel(
    val title: String,
    val items: List<TransferRecipientUiModel>
)

data class TransferRemoteGateUiModel(
    val title: String,
    val description: String,
    val actionText: String? = null,
    val isLoading: Boolean = false
)

data class TransferReviewNoticeUiModel(
    val title: String,
    val description: String
)

data class TransferUiModel(
    val flowStep: TransferFlowStep,
    val transferStatus: TransferStatus,
    val isActionSubmitting: Boolean,
    val destinationMode: TransferDestinationMode,
    val isRemoteMode: Boolean,
    val selectedAccountName: String,
    val selectedAccountNumber: String,
    val selectedAccountBalanceText: String,
    val selectedRecipientName: String,
    val selectedRecipientAccountLabel: String,
    val selectedRecipientWalletLabel: String,
    val selectedRecipientWalletFullLabel: String,
    val amountUsd: String,
    val confirmationAmountText: String,
    val accountStepHintText: String,
    val canSubmit: Boolean,
    val showReviewScreen: Boolean,
    val showTrackerScreen: Boolean,
    val remoteGate: TransferRemoteGateUiModel?,
    val reviewNotice: TransferReviewNoticeUiModel?,
    val trackerDetailText: String,
    val trackerTxHashText: String?,
    val recipientScreenTitle: String,
    val recipientSearchPlaceholderText: String,
    val showAddRecipientAction: Boolean,
    val addRecipientPhoneDirectory: List<RecipientDirectoryContactUiModel>,
    val addRecipientSupportsRemotePhoneSearch: Boolean,
    val addRecipientPhoneSearchResults: List<RecipientDirectoryContactUiModel>,
    val addRecipientPhoneSearchLoading: Boolean,
    val addRecipientPhoneSearchErrorMessage: String?,
    val accounts: List<TransferAccountUiModel>,
    val recipientSections: List<TransferRecipientSectionUiModel>
)

fun DemoState.toTransferUiModel(
    remoteState: RemittanceRemoteState,
    actionUiState: RemittanceActionUiState,
    isAuthenticated: Boolean,
    language: AppLanguage = AppLanguage.fromDefault(),
    recipientPhoneSearchUiState: RecipientPhoneSearchUiState = RecipientPhoneSearchUiState()
): TransferUiModel {
    val isRemoteMode = isAuthenticated
    val remotePayload = remoteState.payload
    val remoteAccount = if (isRemoteMode && remotePayload != null) {
        TransferAccountUiModel(
            id = REMITTANCE_WALLET_ID,
            name = language.text("transfer_dondone_wallet"),
            number = shortenWalletAddress(remotePayload.wallet.walletAddress),
            balanceText = remotePayload.balance?.formatTokenBalance(language) ?: language.text("transfer_checking_balance"),
            selected = true
        )
    } else {
        null
    }

    val selectedAccount = remittance.selectedAccount()
    val selectedRecipient = remittance.selectedRecipientOrNull()
    val selectedRecipientName = selectedRecipient?.let { remittance.displayedRecipientName() } ?: language.text("transfer_select_recipient")
    val amountUsd = remittance.draftAmountUsd
    val amountKrw = amountUsd * 1_450
    val remoteBalanceAtomic = remotePayload?.balance?.tokenBalanceAtomic?.toLongOrNull()
    val amountAtomic = amountUsd.toLong() * 1_000_000L
    val canSubmit = if (isRemoteMode) {
        amountUsd > 0 &&
            remotePayload?.wallet?.fundingStatus == "FUNDED" &&
            remoteBalanceAtomic != null &&
            amountAtomic <= remoteBalanceAtomic
    } else {
        amountUsd > 0 && amountKrw <= selectedAccount.balance
    }

    val recipientItems = remittance.recipients.mapIndexed { index, recipient ->
        TransferRecipientUiModel(
            id = recipient.id,
            name = recipient.name,
            address = recipient.address,
            relationship = recipient.relationship,
            accountLabel = buildRecipientAccountLabel(recipient.address),
            contactLabel = "${recipient.relationship} · ${shortenWalletAddress(recipient.address)}",
            tone = recipientTone(index),
            selected = recipient.id == remittance.selectedRecipientId
        )
    }

    val remoteGate = if (isRemoteMode) resolveRemoteGate(remoteState, language) else null
    val reviewNotice = actionUiState.precheck?.toReviewNotice(language)
    val activeTransfer = remotePayload?.activeTransfer
    val trackerDetailText = when {
        remittance.status == TransferStatus.REVIEWING && actionUiState.isSubmitting ->
            language.text("transfer_sending_request")

        remittance.status == TransferStatus.CONFIRMED ->
            language.text("transfer_completed_detail")

        remittance.status == TransferStatus.FAILED -> {
            activeTransfer?.failureCode?.let { language.text("transfer_failed_with_reason", it) }
                ?: language.text("transfer_failed_detail")
        }

        else ->
            activeTransfer?.status?.toTrackerDetailText(language) ?: language.text("transfer_checking_progress")
    }

    return TransferUiModel(
        flowStep = remittance.flowStep,
        transferStatus = remittance.status,
        isActionSubmitting = actionUiState.isSubmitting,
        destinationMode = if (isRemoteMode) TransferDestinationMode.WALLET else remittance.destinationMode,
        isRemoteMode = isRemoteMode,
        selectedAccountName = remoteAccount?.name ?: selectedAccount.name,
        selectedAccountNumber = remoteAccount?.number ?: selectedAccount.number,
        selectedAccountBalanceText = remoteAccount?.balanceText ?: formatKrw(selectedAccount.balance),
        selectedRecipientName = selectedRecipientName,
        selectedRecipientAccountLabel = selectedRecipient?.let { buildRecipientAccountLabel(it.address) }
            ?: language.text("transfer_select_account"),
        selectedRecipientWalletLabel = selectedRecipient?.let { shortenWalletAddress(it.address) }
            ?: language.text("transfer_select_wallet"),
        selectedRecipientWalletFullLabel = selectedRecipient?.address ?: language.text("transfer_select_wallet"),
        amountUsd = amountUsd.toString(),
        confirmationAmountText = if (!isRemoteMode && remittance.destinationMode == TransferDestinationMode.ACCOUNT) {
            formatKrw(amountKrw)
        } else {
            "$$amountUsd USDC"
        },
        accountStepHintText = if (isRemoteMode) {
            language.text("transfer_send_from_wallet_hint")
        } else {
            language.text("transfer_choose_account_to_send")
        },
        canSubmit = canSubmit,
        showReviewScreen = remittance.status == TransferStatus.REVIEWING && !actionUiState.isSubmitting,
        showTrackerScreen = (remittance.status == TransferStatus.REVIEWING && actionUiState.isSubmitting) ||
            remittance.status == TransferStatus.SUBMITTED ||
            remittance.status == TransferStatus.CONFIRMED ||
            remittance.status == TransferStatus.FAILED,
        remoteGate = remoteGate,
        reviewNotice = reviewNotice,
        trackerDetailText = trackerDetailText,
        trackerTxHashText = activeTransfer?.txHash,
        recipientScreenTitle = if (isRemoteMode) language.text("transfer_choose_wallet_to_receive") else language.text("transfer_choose_account_to_receive"),
        recipientSearchPlaceholderText = if (isRemoteMode) language.text("transfer_search_wallet_address") else language.text("transfer_enter_account_name"),
        showAddRecipientAction = isRemoteMode,
        addRecipientPhoneDirectory = if (isRemoteMode) {
            emptyList()
        } else {
            buildDemoRecipientDirectory(remittance.recipients.map { it.address })
        },
        addRecipientSupportsRemotePhoneSearch = isRemoteMode,
        addRecipientPhoneSearchResults = if (isRemoteMode) {
            recipientPhoneSearchUiState.results.map { candidate ->
                RecipientDirectoryContactUiModel(
                    id = "search-${candidate.candidateUserId}",
                    name = candidate.displayName,
                    maskedPhoneNumber = candidate.maskedPhoneNumber,
                    searchablePhoneNumber = "",
                    walletAddress = null,
                    walletAddressLabel = candidate.walletAddressMasked,
                    candidateUserId = candidate.candidateUserId,
                    alreadyRegistered = candidate.alreadyRegistered
                )
            }
        } else {
            emptyList()
        },
        addRecipientPhoneSearchLoading = isRemoteMode && recipientPhoneSearchUiState.isLoading,
        addRecipientPhoneSearchErrorMessage = if (isRemoteMode) {
            recipientPhoneSearchUiState.errorMessage
        } else {
            null
        },
        accounts = remoteAccount?.let(::listOf) ?: remittance.accounts.map { account ->
            TransferAccountUiModel(
                id = account.id,
                name = account.name,
                number = account.number,
                balanceText = formatKrw(account.balance),
                selected = account.id == remittance.selectedAccountId
            )
        },
        recipientSections = buildRecipientSections(recipientItems, language)
    )
}

private fun buildRecipientSections(
    recipients: List<TransferRecipientUiModel>,
    language: AppLanguage = AppLanguage.fromDefault()
): List<TransferRecipientSectionUiModel> {
    if (recipients.isEmpty()) return emptyList()
    if (recipients.size == 1) {
        return listOf(
            TransferRecipientSectionUiModel(
                title = language.text("transfer_recent_wallet"),
                items = recipients
            )
        )
    }

    val frequent = recipients.take(1)
    val recent = recipients.drop(1)

    return buildList {
        if (frequent.isNotEmpty()) {
            add(TransferRecipientSectionUiModel(title = language.text("transfer_frequent_wallet"), items = frequent))
        }
        if (recent.isNotEmpty()) {
            add(TransferRecipientSectionUiModel(title = language.text("transfer_recent_wallet"), items = recent))
        }
    }
}

private fun buildRecipientAccountLabel(address: String): String = shortenWalletAddress(address)

private fun recipientTone(index: Int): TransferRecipientTone =
    when (index % 4) {
        0 -> TransferRecipientTone.Amber
        1 -> TransferRecipientTone.Blue
        2 -> TransferRecipientTone.Indigo
        else -> TransferRecipientTone.Teal
    }

private fun resolveRemoteGate(
    remoteState: RemittanceRemoteState,
    language: AppLanguage = AppLanguage.fromDefault()
): TransferRemoteGateUiModel? {
    return when (remoteState.mode) {
        RemittanceRemoteMode.LOADING -> TransferRemoteGateUiModel(
            title = language.text("transfer_loading_wallet_info"),
            description = language.text("transfer_loading_wallet_info_desc"),
            isLoading = true
        )

        RemittanceRemoteMode.ERROR -> TransferRemoteGateUiModel(
            title = language.text("transfer_failed_to_load_info"),
            description = remoteState.errorMessage ?: language.text("workproof_map_error_message_default"),
            actionText = language.text("transfer_try_again")
        )

        RemittanceRemoteMode.CONTENT -> {
            val wallet = remoteState.payload?.wallet ?: return null
            when (wallet.fundingStatus) {
                "PENDING" -> TransferRemoteGateUiModel(
                    title = language.text("transfer_wallet_preparing"),
                    description = language.text("transfer_wallet_ready_after_funding"),
                    actionText = language.text("transfer_check_status")
                )

                "FAILED" -> TransferRemoteGateUiModel(
                    title = language.text("transfer_wallet_preparation_failed"),
                    description = wallet.fundingFailureReason ?: language.text("workproof_map_error_message_default"),
                    actionText = language.text("transfer_try_again")
                )

                else -> null
            }
        }

        RemittanceRemoteMode.UNAUTHENTICATED -> null
    }
}

private fun com.dondone.mobile.data.remittance.RemittanceTransferPrecheckPayload.toReviewNotice(
    language: AppLanguage = AppLanguage.fromDefault()
): TransferReviewNoticeUiModel? {
    return when (policyCode) {
        "RECENT_RECIPIENT_CONFIRMATION_REQUIRED" -> TransferReviewNoticeUiModel(
            title = language.text("transfer_recently_updated_recipient"),
            description = language.text("transfer_check_wallet_address_again")
        )

        "HIGH_AMOUNT_CONFIRMATION_REQUIRED" -> TransferReviewNoticeUiModel(
            title = language.text("transfer_high_amount_confirmation"),
            description = language.text("transfer_check_amount_recipient_again")
        )

        else -> null
    }
}

private fun com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload.formatTokenBalance(
    language: AppLanguage = AppLanguage.fromDefault()
): String {
    val normalized = tokenBalanceAtomic.toBigDecimalOrNull()
        ?.movePointLeft(assetDecimals)
        ?.setScale(2, RoundingMode.DOWN)
        ?: return language.text("transfer_checking_balance")
    return "${normalized.stripTrailingZeros().toPlainString()} $assetSymbol"
}

private fun String.toTrackerDetailText(language: AppLanguage = AppLanguage.fromDefault()): String = when (uppercase(Locale.ROOT)) {
    "REQUESTED" -> language.text("transfer_request_created")
    "SIGNED" -> language.text("transfer_preparing_signature")
    "BROADCASTED" -> language.text("transfer_waiting_blockchain_result")
    "CONFIRMED" -> language.text("transfer_confirmed_status_detail")
    "FAILED" -> language.text("transfer_failed_status_detail")
    "TIMED_OUT" -> language.text("transfer_timeout_status_detail")
    else -> language.text("transfer_checking_progress")
}

private fun shortenWalletAddress(address: String): String {
    return if (address.length <= 14) {
        address
    } else {
        "${address.take(8)}...${address.takeLast(6)}"
    }
}
