package com.dondone.mobile.feature.finance.presentation

import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.model.DemoState

data class TransferAccountOptionUiModel(
    val id: String,
    val name: String,
    val number: String,
    val balanceText: String,
    val selected: Boolean
)

data class RecipientWalletUiModel(
    val id: String,
    val name: String,
    val address: String,
    val selected: Boolean
)

data class AccountManageUiModel(
    val accounts: List<TransferAccountOptionUiModel>,
    val recipientWallets: List<RecipientWalletUiModel>
)

fun DemoState.toAccountManageUiModel(): AccountManageUiModel {
    return AccountManageUiModel(
        accounts = remittance.accounts.map { account ->
            TransferAccountOptionUiModel(
                id = account.id,
                name = account.name,
                number = account.number,
                balanceText = formatKrw(account.balance),
                selected = account.id == remittance.selectedAccountId
            )
        },
        recipientWallets = remittance.recipients.map { recipient ->
            RecipientWalletUiModel(
                id = recipient.id,
                name = recipient.name,
                address = recipient.address,
                selected = recipient.id == remittance.selectedRecipientId
            )
        }
    )
}
