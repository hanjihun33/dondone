package com.dondone.mobile.feature.finance.presentation

import com.dondone.mobile.app.session.RecipientPhoneSearchUiState
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.data.remittance.RemittanceRemoteMode
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.remittanceRelationCodeToLabel
import com.dondone.mobile.domain.model.remittanceRelationLabelToCode
import com.dondone.mobile.feature.recipient.presentation.buildDemoRecipientDirectory
import com.dondone.mobile.feature.recipient.presentation.RecipientDirectoryContactUiModel
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
    recipientPhoneSearchUiState: RecipientPhoneSearchUiState = RecipientPhoneSearchUiState()
): AccountManageUiModel {
    if (isAuthenticated) {
        val remotePayload = remittanceRemoteState.payload
        return AccountManageUiModel(
            totalBalanceLabel = "총 지갑 잔액",
            totalBalanceText = when (remittanceRemoteState.mode) {
                RemittanceRemoteMode.CONTENT -> remotePayload?.balance?.formatTokenBalanceForManage() ?: "잔액 확인 중"
                RemittanceRemoteMode.LOADING -> "잔액 확인 중"
                else -> "지갑 정보 확인 중"
            },
            totalBalanceAmountText = when (remittanceRemoteState.mode) {
                RemittanceRemoteMode.CONTENT -> remotePayload?.balance?.formatTokenAmountForManage() ?: "잔액 확인 중"
                RemittanceRemoteMode.LOADING -> "잔액 확인 중"
                else -> "지갑 정보 확인 중"
            },
            totalBalanceUnitText = when (remittanceRemoteState.mode) {
                RemittanceRemoteMode.CONTENT -> remotePayload?.balance?.assetSymbol
                else -> null
            },
            accountSectionTitle = "내 지갑",
            accountActionText = null,
            accounts = listOf(
                TransferAccountOptionUiModel(
                    id = "remote-wallet",
                    name = "DonDone Wallet",
                    number = remotePayload?.wallet?.walletAddress?.toShortWalletAddress() ?: "지갑 주소 확인 중",
                    copyNumber = remotePayload?.wallet?.walletAddress,
                    balanceText = when (remittanceRemoteState.mode) {
                        RemittanceRemoteMode.CONTENT -> remotePayload?.balance?.formatTokenBalanceForManage() ?: "잔액 확인 중"
                        RemittanceRemoteMode.LOADING -> "잔액 확인 중"
                        else -> "지갑 정보 확인 중"
                    },
                    selected = true
                )
            ),
            recipientSectionTitle = "수신 지갑",
            recipientActionText = "지갑 추가",
            recipientWallets = remotePayload?.recipients?.map { recipient ->
                RecipientWalletUiModel(
                    id = recipient.recipientId,
                    name = recipient.alias,
                    address = recipient.walletAddress,
                    relationCode = recipient.relation,
                    relationLabel = remittanceRelationCodeToLabel(recipient.relation),
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
        totalBalanceLabel = "총 계좌 잔액",
        totalBalanceText = formatKrw(remittance.accounts.sumOf { it.balance }),
        totalBalanceAmountText = formatKrw(remittance.accounts.sumOf { it.balance }),
        totalBalanceUnitText = null,
        accountSectionTitle = "내 계좌",
        accountActionText = "계좌 추가",
        accounts = remittance.accounts.map { account ->
            TransferAccountOptionUiModel(
                id = account.id,
                name = account.name,
                number = account.number,
                copyNumber = null,
                balanceText = formatKrw(account.balance),
                selected = account.id == remittance.selectedAccountId
            )
        },
        recipientSectionTitle = "수신 지갑",
        recipientActionText = "지갑 추가",
        recipientWallets = remittance.recipients.map { recipient ->
            RecipientWalletUiModel(
                id = recipient.id,
                name = recipient.name,
                address = recipient.address,
                relationCode = remittanceRelationLabelToCode(recipient.relationship),
                relationLabel = recipient.relationship,
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

private fun com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload.formatTokenBalanceForManage(): String {
    val amount = formatTokenAmountForManage()
    return if (amount == "잔액 확인 중") amount else "$amount $assetSymbol"
}

private fun com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload.formatTokenAmountForManage(): String {
    val normalized = tokenBalanceAtomic.toBigDecimalOrNull()
        ?.movePointLeft(assetDecimals)
        ?.setScale(2, RoundingMode.DOWN)
        ?: return "잔액 확인 중"
    return normalized.stripTrailingZeros().toPlainString()
}

private fun String.toShortWalletAddress(): String {
    return if (length <= 14) {
        this
    } else {
        "${take(8)}...${takeLast(6)}"
    }
}
