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

data class AccountManageUiModel(
    val selectedAccountName: String,
    val selectedAccountNumber: String,
    val selectedBalanceText: String,
    val draftAmountText: String,
    val accounts: List<TransferAccountOptionUiModel>
)

fun DemoState.toAccountManageUiModel(): AccountManageUiModel {
    val selectedAccount = remittance.selectedAccount()

    return AccountManageUiModel(
        selectedAccountName = selectedAccount.name,
        selectedAccountNumber = selectedAccount.number,
        selectedBalanceText = formatKrw(selectedAccount.balance),
        draftAmountText = "$${remittance.draftAmountUsd}",
        accounts = remittance.accounts.map { account ->
            TransferAccountOptionUiModel(
                id = account.id,
                name = account.name,
                number = account.number,
                balanceText = formatKrw(account.balance),
                selected = account.id == remittance.selectedAccountId
            )
        }
    )
}
