package com.dondone.mobile.feature.finance.presentation

import com.dondone.mobile.app.session.RecipientPhoneSearchUiState
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.core.ui.toMaskedPhoneNumber
import com.dondone.mobile.data.remittance.RemittanceRemoteMode
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.remittanceRelationCodeToLabel
import com.dondone.mobile.domain.model.remittanceRelationLabelToCode
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

data class WalletDirectoryContactUiModel(
    val id: String,
    val name: String,
    val maskedPhoneNumber: String,
    val searchablePhoneNumber: String,
    val walletAddress: String?,
    val walletAddressLabel: String,
    val candidateUserId: Long? = null,
    val alreadyRegistered: Boolean = false
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
    val phoneDirectory: List<WalletDirectoryContactUiModel>,
    val supportsRemotePhoneSearch: Boolean,
    val phoneSearchResults: List<WalletDirectoryContactUiModel>,
    val isPhoneSearchLoading: Boolean,
    val phoneSearchErrorMessage: String?
)

private data class WalletDirectoryContactSeed(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val walletAddress: String
)

private val AccountManagePhoneDirectorySeed = listOf(
    WalletDirectoryContactSeed(
        id = "contact-minh",
        name = "Minh Nguyen",
        phoneNumber = "01028411183",
        walletAddress = "0x7F4F0b8E8fA0d3B6bA91F5bEEfa2276c9168a20D"
    ),
    WalletDirectoryContactSeed(
        id = "contact-anh",
        name = "Anh Tran",
        phoneNumber = "01066139214",
        walletAddress = "0x50e8E7E74143F6A4F25e8f6b72A8092d18284D4c"
    ),
    WalletDirectoryContactSeed(
        id = "contact-lina",
        name = "Lina Park",
        phoneNumber = "01041250871",
        walletAddress = "0xF2D0C4b8A7E9E14A4A27055A933fA4DCC5cA8eE1"
    ),
    WalletDirectoryContactSeed(
        id = "contact-jose",
        name = "Jose Rivera",
        phoneNumber = "01090317724",
        walletAddress = "0x6b33B4F2ADeDd1F4e84A4c1c041cF236503A17f8"
    )
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
                WalletDirectoryContactUiModel(
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
        phoneDirectory = buildWalletDirectory(remittance.recipients.map { it.address }),
        supportsRemotePhoneSearch = false,
        phoneSearchResults = emptyList(),
        isPhoneSearchLoading = false,
        phoneSearchErrorMessage = null
    )
}

private fun buildWalletDirectory(registeredAddresses: List<String>): List<WalletDirectoryContactUiModel> {
    val registered = registeredAddresses.toSet()
    return AccountManagePhoneDirectorySeed
        .filterNot { it.walletAddress in registered }
        .map { contact ->
            WalletDirectoryContactUiModel(
                id = contact.id,
                name = contact.name,
                maskedPhoneNumber = contact.phoneNumber.toMaskedPhoneNumber(),
                searchablePhoneNumber = contact.phoneNumber,
                walletAddress = contact.walletAddress,
                walletAddressLabel = contact.walletAddress.toShortWalletAddress(),
                alreadyRegistered = false
            )
        }
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
