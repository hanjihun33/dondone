package com.dondone.mobile.feature.finance.presentation

import com.dondone.mobile.app.session.RecipientPhoneSearchUiState
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.text
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.data.remittance.RemittanceRemoteMode
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.remittanceRelationLabelToCode
import com.dondone.mobile.feature.recipient.presentation.RecipientDirectoryContactUiModel
import com.dondone.mobile.feature.recipient.presentation.buildDemoRecipientDirectory
import java.math.RoundingMode

data class TransferAccountOptionUiModel(
    val id: String,
    val name: String,
    val number: String,
    val copyNumber: String?,
    val balanceText: String,
    val selected: Boolean
)

data class RecipientWalletUiModel(
    val id: String,
    val name: String,
    val address: String,
    val relationCode: String,
    val relationLabel: String,
    val selected: Boolean
)

data class AccountManageUiModel(
    val totalBalanceLabel: String,
    val totalBalanceText: String,
    val totalBalanceAmountText: String,
    val totalBalanceUnitText: String?,
    val accountSectionTitle: String,
    val accountActionText: String?,
    val accounts: List<TransferAccountOptionUiModel>,
    val recipientSectionTitle: String,
    val recipientActionText: String?,
    val recipientWallets: List<RecipientWalletUiModel>,
    val phoneDirectory: List<RecipientDirectoryContactUiModel>,
    val supportsRemotePhoneSearch: Boolean,
    val phoneSearchResults: List<RecipientDirectoryContactUiModel>,
    val isPhoneSearchLoading: Boolean,
    val phoneSearchErrorMessage: String?
)

fun DemoState.toAccountManageUiModel(
    remittanceRemoteState: RemittanceRemoteState = RemittanceRemoteState.unauthenticated(""),
    isAuthenticated: Boolean = false,
    recipientPhoneSearchUiState: RecipientPhoneSearchUiState = RecipientPhoneSearchUiState(),
    language: AppLanguage = AppLanguage.KOREAN
): AccountManageUiModel {
    if (isAuthenticated) {
        val remotePayload = remittanceRemoteState.payload
        val loadingBalanceText = when (remittanceRemoteState.mode) {
            RemittanceRemoteMode.LOADING -> language.text("home_checking_balance")
            else -> language.text("home_checking_wallet_info")
        }
        return AccountManageUiModel(
            totalBalanceLabel = language.text("total_wallet_balance"),
            totalBalanceText = when (remittanceRemoteState.mode) {
                RemittanceRemoteMode.CONTENT -> remotePayload?.balance?.formatTokenBalanceForManage() ?: loadingBalanceText
                else -> loadingBalanceText
            },
            totalBalanceAmountText = when (remittanceRemoteState.mode) {
                RemittanceRemoteMode.CONTENT -> remotePayload?.balance?.formatTokenAmountForManage() ?: loadingBalanceText
                else -> loadingBalanceText
            },
            totalBalanceUnitText = when (remittanceRemoteState.mode) {
                RemittanceRemoteMode.CONTENT -> remotePayload?.balance?.assetSymbol
                else -> null
            },
            accountSectionTitle = language.text("my_wallet"),
            accountActionText = null,
            accounts = listOf(
                TransferAccountOptionUiModel(
                    id = "remote-wallet",
                    name = "DonDone Wallet",
                    number = remotePayload?.wallet?.walletAddress?.toShortWalletAddress()
                        ?: language.text("wallet_address_checking"),
                    copyNumber = remotePayload?.wallet?.walletAddress,
                    balanceText = when (remittanceRemoteState.mode) {
                        RemittanceRemoteMode.CONTENT -> remotePayload?.balance?.formatTokenBalanceForManage() ?: loadingBalanceText
                        else -> loadingBalanceText
                    },
                    selected = true
                )
            ),
            recipientSectionTitle = language.text("recipient_wallet"),
            recipientActionText = language.text("add_wallet"),
            recipientWallets = remotePayload?.recipients?.map { recipient ->
                RecipientWalletUiModel(
                    id = recipient.recipientId,
                    name = recipient.alias,
                    address = recipient.walletAddress,
                    relationCode = recipient.relation,
                    relationLabel = relationLabel(language, recipient.relation),
                    selected = recipient.recipientId == remittance.selectedRecipientId
                )
            }.orEmpty(),
            phoneDirectory = emptyList(),
            supportsRemotePhoneSearch = true,
            phoneSearchResults = recipientPhoneSearchUiState.results.map { candidate ->
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
            },
            isPhoneSearchLoading = recipientPhoneSearchUiState.isLoading,
            phoneSearchErrorMessage = recipientPhoneSearchUiState.errorMessage
        )
    }

    return AccountManageUiModel(
        totalBalanceLabel = language.text("total_wallet_balance"),
        totalBalanceText = formatKrw(remittance.accounts.sumOf { it.balance }, language),
        totalBalanceAmountText = formatKrw(remittance.accounts.sumOf { it.balance }, language),
        totalBalanceUnitText = null,
        accountSectionTitle = language.text("my_wallet"),
        accountActionText = language.text("add_wallet"),
        accounts = remittance.accounts.map { account ->
            TransferAccountOptionUiModel(
                id = account.id,
                name = account.name,
                number = account.number,
                copyNumber = null,
                balanceText = formatKrw(account.balance, language),
                selected = account.id == remittance.selectedAccountId
            )
        },
        recipientSectionTitle = language.text("recipient_wallet"),
        recipientActionText = language.text("add_wallet"),
        recipientWallets = remittance.recipients.map { recipient ->
            val relationCode = remittanceRelationLabelToCode(recipient.relationship)
            RecipientWalletUiModel(
                id = recipient.id,
                name = recipient.name,
                address = recipient.address,
                relationCode = relationCode,
                relationLabel = relationLabel(language, relationCode),
                selected = recipient.id == remittance.selectedRecipientId
            )
        },
        phoneDirectory = buildDemoRecipientDirectory(remittance.recipients.map { it.address }),
        supportsRemotePhoneSearch = false,
        phoneSearchResults = emptyList(),
        isPhoneSearchLoading = false,
        phoneSearchErrorMessage = null
    )
}

private fun relationLabel(language: AppLanguage, relationCode: String): String =
    when (relationCode.trim().uppercase()) {
        "FAMILY" -> language.text("family")
        "SPOUSE" -> language.text("spouse")
        "PARENT" -> language.text("parent")
        "CHILD" -> language.text("child")
        "SIBLING" -> language.text("sibling")
        "FRIEND" -> language.text("friend")
        "RELATIVE" -> language.text("relative")
        else -> language.text("other")
    }

private fun com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload.formatTokenBalanceForManage(): String {
    val amount = formatTokenAmountForManage()
    return if (amount == "0") {
        "0 $assetSymbol"
    } else {
        "$amount $assetSymbol"
    }
}

private fun com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload.formatTokenAmountForManage(): String {
    return tokenBalanceAtomic.toBigDecimalOrNull()
        ?.movePointLeft(assetDecimals)
        ?.setScale(2, RoundingMode.DOWN)
        ?.stripTrailingZeros()
        ?.toPlainString()
        ?: "0"
}

private fun String.toShortWalletAddress(): String {
    return if (length <= 14) {
        this
    } else {
        "${take(8)}...${takeLast(6)}"
    }
}
