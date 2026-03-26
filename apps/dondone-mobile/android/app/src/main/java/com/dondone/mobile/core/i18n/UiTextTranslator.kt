package com.dondone.mobile.core.i18n

typealias AppTextKey = String

private const val UPDATED_PREFIX = "\uc5c5\ub370\uc774\ud2b8 "

private data class AppTextEntry(
    val values: Map<AppLanguage, String>
) {
    fun resolve(language: AppLanguage): String =
        values[language] ?: values[AppLanguage.KOREAN].orEmpty()
}

private fun appTextEntry(korean: String, english: String): AppTextEntry =
    AppTextEntry(
        values = mapOf(
            AppLanguage.KOREAN to korean,
            AppLanguage.ENGLISH to english
        )
    )

object AppTextKeys {
    const val BACK = "back"
    const val SETTINGS = "settings"
    const val CLOSE = "close"
    const val WAGE_REVIEW = "wage_review"
    const val ENTER_WORKER_REGISTRATION_CODE = "enter_worker_registration_code"
    const val HOME = "home_tab"
    const val FINANCE = "finance_tab"
    const val WORK = "work_tab"
    const val MENU = "menu_tab"
    const val TRANSFER = "transfer"
    const val WALLET_ACCOUNTS = "wallet_accounts"
    const val TRANSACTION_HISTORY = "transaction_history"
    const val TRANSACTION_DETAILS = "transaction_details"
    const val EDIT_TRANSACTION = "edit_transaction"
    const val SELECT_ACCOUNT = "select_account"
    const val REGISTRATION_CODE = "registration_code"
    const val EXAMPLE_WORKER_CODE = "example_worker_code"
    const val REGISTRATION_ALLOWED_CHARACTERS = "registration_allowed_characters"
    const val REGISTER = "register"
    const val REGISTERING = "registering"
    const val REGISTRATION_CODE_MIN = "registration_code_min"
    const val REGISTRATION_CODE_MAX = "registration_code_max"
    const val HOME_AVAILABLE_NOW = "home_available_now"
    const val HOME_SEND_MONEY = "home_send_money"
    const val HOME_TODAYS_WORK = "home_todays_work"
    const val HOME_VIEW_WORK_RECORDS = "home_view_work_records"
    const val HOME_CLOCK_IN = "home_clock_in"
    const val HOME_CLOCK_OUT = "home_clock_out"
    const val HOME_CLOCK_IN_RECORDED = "home_clock_in_recorded"
    const val HOME_READY = "home_ready"
    const val HOME_NEXT_STEP = "home_next_step"
    const val HOME_VIEW = "home_view"
    const val HOME_VIEW_FINANCE = "home_view_finance"
    const val HOME_ENTER_DEPOSIT = "home_enter_deposit"
    const val HOME_COMPANY_REGISTRATION_REQUIRED = "home_company_registration_required"
    const val HOME_NOTICE = "home_notice"
    const val HOME_REGISTRATION_CODE_NOTICE = "home_registration_code_notice"
    const val HOME_PRIMARY_WALLET = "home_primary_wallet"
    const val HOME_CHECKING_BALANCE = "home_checking_balance"
    const val HOME_CHECKING_WALLET_INFO = "home_checking_wallet_info"
    const val HOME_PRIMARY_ACCOUNT = "home_primary_account"
    const val HOME_SAVED_RECIPIENT = "home_saved_recipient"
    const val HOME_TRANSFER_COMPLETE = "home_transfer_complete"
    const val HOME_TRANSFER_COMPLETE_MESSAGE = "home_transfer_complete_message"
    const val HOME_TRANSFER_FAILED = "home_transfer_failed"
    const val HOME_TRANSFER_FAILED_MESSAGE = "home_transfer_failed_message"
    const val HOME_PREPAYDAY_MESSAGE = "home_prepayday_message"
    const val HOME_ENTER_DEPOSIT_MESSAGE = "home_enter_deposit_message"
    const val HOME_DIFFERENCE_MESSAGE = "home_difference_message"
    const val HOME_SMALL_DIFFERENCE_MESSAGE = "home_small_difference_message"
}

private val appTextCatalog: Map<AppTextKey, AppTextEntry> = mapOf(
    AppTextKeys.BACK to appTextEntry(korean = "\ub4a4\ub85c", english = "Back"),
    AppTextKeys.HOME to appTextEntry(korean = "\ud648", english = "Home"),
    AppTextKeys.FINANCE to appTextEntry(korean = "\uae08\uc735", english = "Finance"),
    AppTextKeys.WORK to appTextEntry(korean = "\uadfc\ubb34", english = "Work"),
    AppTextKeys.MENU to appTextEntry(korean = "\uba54\ub274", english = "Menu"),
    AppTextKeys.TRANSFER to appTextEntry(korean = "\uc1a1\uae08", english = "Transfer"),
    AppTextKeys.WALLET_ACCOUNTS to appTextEntry(korean = "\uacc4\uc88c \uc9c0\uac11", english = "Wallet Accounts"),
    AppTextKeys.TRANSACTION_HISTORY to appTextEntry(korean = "\uac70\ub798 \ub0b4\uc5ed", english = "Transaction History"),
    AppTextKeys.TRANSACTION_DETAILS to appTextEntry(korean = "\uac70\ub798 \uc0c1\uc138", english = "Transaction Details"),
    AppTextKeys.EDIT_TRANSACTION to appTextEntry(korean = "\uac70\ub798 \uc218\uc815", english = "Edit Transaction"),
    AppTextKeys.SELECT_ACCOUNT to appTextEntry(korean = "\uacc4\uc88c \uc120\ud0dd", english = "Select account"),
    AppTextKeys.REGISTRATION_CODE to appTextEntry(korean = "\ub4f1\ub85d \ucf54\ub4dc", english = "Registration code"),
    AppTextKeys.EXAMPLE_WORKER_CODE to appTextEntry(korean = "\uc608: WORKER-AB12-CD34", english = "Example: WORKER-AB12-CD34"),
    AppTextKeys.REGISTRATION_ALLOWED_CHARACTERS to appTextEntry(
        korean = "\uc601\ubb38 \ub300\ubb38\uc790, \uc22b\uc790, \ud558\uc774\ud508(-)\ub9cc \uc785\ub825\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "Only uppercase letters, numbers, and hyphens (-) are allowed."
    ),
    AppTextKeys.REGISTER to appTextEntry(korean = "\ub4f1\ub85d", english = "Register"),
    AppTextKeys.REGISTERING to appTextEntry(korean = "\ub4f1\ub85d \uc911...", english = "Registering..."),
    AppTextKeys.REGISTRATION_CODE_MIN to appTextEntry(
        korean = "\ub4f1\ub85d \ucf54\ub4dc\ub294 8\uc790 \uc774\uc0c1\uc774\uc5b4\uc57c \ud574\uc694.",
        english = "The registration code must be at least 8 characters."
    ),
    AppTextKeys.REGISTRATION_CODE_MAX to appTextEntry(
        korean = "\ub4f1\ub85d \ucf54\ub4dc\ub294 32\uc790 \uc774\ud558\uc5ec\uc57c \ud574\uc694.",
        english = "The registration code must be 32 characters or fewer."
    ),
    AppTextKeys.HOME_AVAILABLE_NOW to appTextEntry(korean = "\uc9c0\uae08 \uc4f8 \uc218 \uc788\ub294 \ub3c8", english = "Available Now"),
    AppTextKeys.HOME_SEND_MONEY to appTextEntry(korean = "\uc1a1\uae08\ud558\uae30", english = "Send Money"),
    AppTextKeys.HOME_TODAYS_WORK to appTextEntry(korean = "\uc624\ub298 \uadfc\ubb34", english = "Today's Work"),
    AppTextKeys.HOME_VIEW_WORK_RECORDS to appTextEntry(korean = "\uadfc\ubb34 \uae30\ub85d \ubcf4\uae30", english = "View Work Records"),
    AppTextKeys.HOME_CLOCK_IN to appTextEntry(korean = "\ucd9c\uadfc", english = "Clock In"),
    AppTextKeys.HOME_CLOCK_OUT to appTextEntry(korean = "\ud1f4\uadfc", english = "Clock Out"),
    AppTextKeys.HOME_CLOCK_IN_RECORDED to appTextEntry(korean = "\ucd9c\uadfc\ub9cc \uae30\ub85d", english = "Clock-in Only"),
    AppTextKeys.HOME_READY to appTextEntry(korean = "\uc900\ube44\ub428", english = "Ready"),
    AppTextKeys.HOME_NEXT_STEP to appTextEntry(korean = "\ub2e4\uc74c \ud589\ub3d9", english = "Next Step"),
    AppTextKeys.HOME_VIEW to appTextEntry(korean = "\ubcf4\uae30", english = "View"),
    AppTextKeys.HOME_VIEW_FINANCE to appTextEntry(korean = "\uae08\uc735 \ubcf4\uae30", english = "View Finance"),
    AppTextKeys.HOME_ENTER_DEPOSIT to appTextEntry(korean = "\uc785\uae08 \uc785\ub825\ud558\uae30", english = "Enter Deposit"),
    AppTextKeys.HOME_COMPANY_REGISTRATION_REQUIRED to appTextEntry(
        korean = "\ud68c\uc0ac \ub4f1\ub85d \ud544\uc694",
        english = "Company Registration Required"
    ),
    AppTextKeys.HOME_NOTICE to appTextEntry(korean = "\uc548\ub0b4", english = "Notice"),
    AppTextKeys.HOME_REGISTRATION_CODE_NOTICE to appTextEntry(
        korean = "\ub4f1\ub85d \ucf54\ub4dc\ub97c \uc785\ub825\ud558\uba74 \uc2e4\uc81c \uadfc\ubb34 \ub370\uc774\ud130\uac00 \ud45c\uc2dc\ub429\ub2c8\ub2e4.",
        english = "Enter the registration code to view your real work data."
    ),
    AppTextKeys.HOME_PRIMARY_WALLET to appTextEntry(korean = "\ub300\ud45c \uc9c0\uac11", english = "Primary Wallet"),
    AppTextKeys.HOME_CHECKING_BALANCE to appTextEntry(korean = "\uc794\uc561 \ud655\uc778 \uc911", english = "Checking Balance"),
    AppTextKeys.HOME_CHECKING_WALLET_INFO to appTextEntry(
        korean = "\uc9c0\uac11 \uc815\ubcf4 \ud655\uc778 \uc911",
        english = "Checking Wallet Info"
    ),
    AppTextKeys.HOME_PRIMARY_ACCOUNT to appTextEntry(korean = "\ub300\ud45c \uacc4\uc88c", english = "Primary Account"),
    AppTextKeys.HOME_SAVED_RECIPIENT to appTextEntry(korean = "\ub4f1\ub85d\ud55c \uc218\uc2e0\uc790", english = "Saved Recipient"),
    AppTextKeys.HOME_TRANSFER_COMPLETE to appTextEntry(korean = "\uc1a1\uae08 \uc644\ub8cc", english = "Transfer Complete"),
    AppTextKeys.HOME_TRANSFER_COMPLETE_MESSAGE to appTextEntry(
        korean = "%s\uc5d0\uac8c \uc1a1\uae08\uc774 \uc644\ub8cc\ub410\uc5b4\uc694.",
        english = "Transfer to %s is complete."
    ),
    AppTextKeys.HOME_TRANSFER_FAILED to appTextEntry(korean = "\uc1a1\uae08 \uc2e4\ud328", english = "Transfer Failed"),
    AppTextKeys.HOME_TRANSFER_FAILED_MESSAGE to appTextEntry(
        korean = "%s\uc5d0\uac8c \uc1a1\uae08\uc744 \ub9c8\uce58\uc9c0 \ubabb\ud588\uc5b4\uc694. \uc7a0\uc2dc \ud6c4 \ub2e4\uc2dc \uc2dc\ub3c4\ud574 \uc8fc\uc138\uc694.",
        english = "Couldn't complete the transfer to %s. Please try again shortly."
    ),
    AppTextKeys.HOME_PREPAYDAY_MESSAGE to appTextEntry(
        korean = "\uae09\uc5ec\uc77c \uc804\uc5d0\ub294 \uc774\ubc88 \ub2ec \uacc4\ud68d\uacfc \ubbf8\ub9ac\ubc1b\uae30 \ud55c\ub3c4\ub97c \uba3c\uc800 \ud655\uc778\ud574\ubcfc\uae4c\uc694?",
        english = "Before payday, check this month's plan and your advance limit first."
    ),
    AppTextKeys.HOME_ENTER_DEPOSIT_MESSAGE to appTextEntry(
        korean = "\uc2e4\uc81c \uc785\uae08\uc561\uc744 \uba3c\uc800 \uc785\ub825\ud558\uba74 \uc774\ubc88 \ub2ec \ub3c8 \uc0c1\ud0dc\ub97c \uc815\ud655\ud788 \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "Enter the actual deposit amount first to check this month's finances accurately."
    ),
    AppTextKeys.HOME_DIFFERENCE_MESSAGE to appTextEntry(
        korean = "\uc2e4\uc81c \uc785\uae08\uc561\uacfc \uc608\uc0c1 \uae09\uc5ec\uc5d0 \ucc28\uc774\uac00 \uc788\uc5b4\uc694.",
        english = "There's a difference between the actual deposit and the expected wage."
    ),
    AppTextKeys.HOME_SMALL_DIFFERENCE_MESSAGE to appTextEntry(
        korean = "\ud604\uc7ac\ub294 \ud655\uc778\uc774 \ud544\uc694\ud55c \ucc28\uc774\uac00 \ud06c\uc9c0 \uc54a\uc544 \ubb38\uc11c/\uc1a1\uae08 \ud750\ub984\uc73c\ub85c \uc774\ub3d9\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "The difference is small enough that you can move on to documents or transfers."
    ),
    "finance_advance_title" to appTextEntry(korean = "\ubbf8\ub9ac\ubc1b\uae30", english = "Advance"),
    "finance_vault_yield" to appTextEntry(korean = "\uc608\uce58 \uc774\uc790", english = "Vault Yield"),
    "finance_vault_balance" to appTextEntry(korean = "\uc608\uce58 \uc794\uc561", english = "Vault Balance"),
    "finance_accrued_yield_est" to appTextEntry(korean = "\ub204\uc801 \uc774\uc790(\ucd94\uc815)", english = "Accrued Yield (Est.)"),
    "finance_estimated_apy" to appTextEntry(korean = "\uc608\uc0c1 \uc5f0\uc774\uc728", english = "Estimated APY"),
    "finance_planned_payout" to appTextEntry(korean = "\uc774\ubc88 \uc218\ub839 \uc608\uc815", english = "Planned Payout"),
    "finance_receivable_amount" to appTextEntry(korean = "\ubc1b\uc744 \uae08\uc561", english = "Receivable Amount"),
    "finance_request_limits" to appTextEntry(korean = "\uc2e0\uccad \uc81c\ud55c", english = "Request Limits"),
    "finance_additional_request" to appTextEntry(korean = "\ucd94\uac00 \uc2e0\uccad", english = "Additional Request"),
    "finance_recent_requested_amount" to appTextEntry(korean = "\ucd5c\uadfc \uc2e0\uccad \uae08\uc561", english = "Most Recent Requested Amount"),
    "finance_view_history" to appTextEntry(korean = "\ub0b4\uc5ed \ubcf4\uae30", english = "View History"),
    "finance_submitting" to appTextEntry(korean = "\uc2e0\uccad \uc911...", english = "Submitting..."),
    "finance_before_deposit" to appTextEntry(korean = "\uc785\uae08 \uc804", english = "Before Deposit"),
    "finance_no_difference" to appTextEntry(korean = "\ucc28\uc774 \uc5c6\uc74c", english = "No Difference"),
    "finance_difference_needs_review" to appTextEntry(korean = "\ud655\uc778 \ud544\uc694\ud55c \ucc28\uc774", english = "Difference Requires Review"),
    "finance_before_review" to appTextEntry(korean = "\ud655\uc778 \uc804", english = "Before Review"),
    "finance_not_entered" to appTextEntry(korean = "\ubbf8\uc785\ub825", english = "Not Entered"),
    "finance_actual_deposit_applied" to appTextEntry(korean = "\uc2e4\uc785\uae08 \ubc18\uc601", english = "Actual Deposit Applied"),
    "finance_manage" to appTextEntry(korean = "\uad00\ub9ac", english = "Manage"),
    "finance_checking_live_data" to appTextEntry(korean = "\uc2e4\uc5f0\ub3d9 \ud655\uc778 \uc911", english = "Checking Live Data"),
    "finance_retry_needed" to appTextEntry(korean = "\ub2e4\uc2dc \ud655\uc778 \ud544\uc694", english = "Retry Needed"),
    "finance_login_required" to appTextEntry(korean = "\ub85c\uadf8\uc778 \ud544\uc694", english = "Login Required"),
    "finance_advance_balance" to appTextEntry(korean = "\ubbf8\ub9ac\ubc1b\uae30 \uc794\uc561", english = "Advance Balance"),
    "finance_next_limit_tier" to appTextEntry(korean = "\ub2e4\uc74c \ud55c\ub3c4 \uad6c\uac04", english = "Next Limit Tier"),
    "finance_can_request_additional_now" to appTextEntry(korean = "\uc9c0\uae08 \ucd94\uac00 \uc2e0\uccad\ud560 \uc218 \uc788\uc5b4\uc694", english = "You can request an additional amount now"),
    "finance_payout_in_progress_now" to appTextEntry(korean = "\uc9c0\uae09\uc774 \uc9c4\ud589 \uc911\uc774\uc5d0\uc694", english = "The payout is in progress"),
    "finance_check_payout_status_again" to appTextEntry(korean = "\uc9c0\uae09 \uc0c1\ud0dc\ub97c \ub2e4\uc2dc \ud655\uc778\ud574 \uc8fc\uc138\uc694", english = "Please check the payout status again"),
    "finance_recent_request_rejected" to appTextEntry(korean = "\ucd5c\uadfc \uc694\uccad\uc774 \ubc18\ub824\ub410\uc5b4\uc694", english = "The most recent request was rejected"),
    "finance_recent_request_status_available" to appTextEntry(korean = "\ucd5c\uadfc \uc2e0\uccad \uc0c1\ud0dc\ub97c \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694", english = "You can check the most recent request status"),
    "finance_remaining_limit_message" to appTextEntry(korean = "\ub0a8\uc740 \ud55c\ub3c4 \uc548\uc5d0\uc11c \ucd94\uac00\ub85c \uc2e0\uccad\ud560 \uc218 \uc788\uc5b4\uc694.", english = "You can request more within the remaining limit."),
    "finance_check_payout_status_and_due_date" to appTextEntry(korean = "\uc9c0\uae09 \uc0c1\ud0dc\uc640 \uc815\uc0b0 \uc608\uc815\uc77c\uc744 \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694.", english = "You can check the payout status and scheduled settlement date."),
    "finance_check_request_result_again" to appTextEntry(korean = "\uc694\uccad \uacb0\uacfc\ub97c \ub2e4\uc2dc \ud655\uc778\ud574 \uc8fc\uc138\uc694.", english = "Please check the request result again."),
    "finance_check_current_round_status" to appTextEntry(korean = "\uc774\ubc88 \ud68c\ucc28 \uc0c1\ud0dc\ub97c \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694.", english = "You can check the current round status."),
    "finance_available_now_amount" to appTextEntry(korean = "\uc9c0\uae08 \uc2e0\uccad \uac00\ub2a5 \uae08\uc561", english = "Amount Available Now"),
    "finance_progress_days_format" to appTextEntry(korean = "%d/%d\uc77c", english = "%d/%dd"),
    "finance_detail_subtitle_current_month" to appTextEntry(korean = "\uc774\ubc88 \ub2ec \uc9c0\uae09 \ub0b4\uc5ed\uacfc \ub0a8\uc740 \ud55c\ub3c4\ub97c \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694.", english = "You can check this month's payout history and the remaining limit."),
    "finance_request_advance" to appTextEntry(korean = "\ubbf8\ub9ac\ubc1b\uae30 \uc2e0\uccad", english = "Request Advance"),
    "finance_check_advance_basis_after_workproof" to appTextEntry(korean = "\uadfc\ubb34 \uc815\ubcf4\uac00 \ud655\uc778\ub418\uba74 \ubbf8\ub9ac\ubc1b\uae30 \uadfc\uac70\ub97c \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694.", english = "Once the work information is verified, you can check the basis for the advance."),
    "finance_approx_amount_format" to appTextEntry(korean = "\uc57d %s", english = "Approx. %s"),
    "finance_paid" to appTextEntry(korean = "\uc9c0\uae09\uc644\ub8cc", english = "Paid"),
    "finance_paying" to appTextEntry(korean = "\uc9c0\uae09\uc911", english = "Paying"),
    "finance_submitted" to appTextEntry(korean = "\uc2e0\uccad\uc644\ub8cc", english = "Submitted"),
    "finance_payout_failed" to appTextEntry(korean = "\uc9c0\uae09\uc2e4\ud328", english = "Payout Failed"),
    "finance_rejected" to appTextEntry(korean = "\ubc18\ub824\ub428", english = "Rejected"),
    "finance_pending_approval" to appTextEntry(korean = "\uc2b9\uc778 \ub300\uae30", english = "Pending Approval"),
    "finance_settlement_complete" to appTextEntry(korean = "\uc815\uc0b0 \uc644\ub8cc", english = "Settlement Complete"),
    "finance_settlement_failed" to appTextEntry(korean = "\uc815\uc0b0 \uc2e4\ud328", english = "Settlement Failed"),
    "finance_auto_settlement_on_payday" to appTextEntry(korean = "\uae09\uc5ec\uc77c \uc790\ub3d9 \uc815\uc0b0 \uc608\uc815", english = "Auto-settlement on Payday"),
    "finance_no_history_this_month" to appTextEntry(korean = "\uc774\ubc88 \ub2ec \uc774\ub825\uc774 \uc5c6\uc5b4\uc694", english = "There Is No History This Month"),
    "finance_close" to appTextEntry(korean = "\ub2eb\uae30", english = "Close"),
    "finance_not_requested" to appTextEntry(korean = "\ubbf8\uc2e0\uccad", english = "Not Requested"),
    "finance_not_deposited" to appTextEntry(korean = "\ubbf8\uc608\uce58", english = "Not Deposited"),
    "finance_failure" to appTextEntry(korean = "\uc2e4\ud328", english = "Failed"),
    "finance_pending_confirmation" to appTextEntry(korean = "\ud655\uc778 \ub300\uae30", english = "Pending Confirmation"),
    "finance_processing" to appTextEntry(korean = "\ucc98\ub9ac \uc911", english = "Processing"),
    "finance_vault_returned_to_wallet" to appTextEntry(
        korean = "\uc9c0\uac11 \uc794\uc561\uc73c\ub85c \ub3cc\uc544\uc654\uc5b4\uc694.",
        english = "It has returned to your wallet balance."
    ),
    "finance_wallet_reflected_to_vault" to appTextEntry(
        korean = "\uc9c0\uac11 \uc794\uc561\uc774 \uc608\uce58 \uc794\uc561\uc73c\ub85c \ubc18\uc601\ub410\uc5b4\uc694.",
        english = "Your wallet balance has been reflected in the vault balance."
    ),
    "finance_withdraw_not_completed_try_again" to appTextEntry(
        korean = "\ucd9c\uae08\uc774 \uc644\ub8cc\ub418\uc9c0 \uc54a\uc558\uc5b4\uc694. \uc7a0\uc2dc \ud6c4 \ub2e4\uc2dc \uc2dc\ub3c4\ud574 \uc8fc\uc138\uc694.",
        english = "The withdrawal was not completed. Please try again shortly."
    ),
    "finance_deposit_not_completed_try_again" to appTextEntry(
        korean = "\uc608\uce58\uac00 \uc644\ub8cc\ub418\uc9c0 \uc54a\uc558\uc5b4\uc694. \uc7a0\uc2dc \ud6c4 \ub2e4\uc2dc \uc2dc\ub3c4\ud574 \uc8fc\uc138\uc694.",
        english = "The deposit was not completed. Please try again shortly."
    ),
    "finance_chain_transfer_waiting_confirmation" to appTextEntry(
        korean = "\uccb4\uc778 \uc804\uc1a1\uc774 \uc644\ub8cc\ub3fc \ud655\uc815\uc744 \uae30\ub2e4\ub9ac\ub294 \uc911\uc785\ub2c8\ub2e4.",
        english = "The chain transfer is complete and waiting for confirmation."
    ),
    "finance_request_preparing_sign_and_transfer" to appTextEntry(
        korean = "\uc694\uccad\uc774 \uc811\uc218\ub3fc \uc11c\uba85\uacfc \uc804\uc1a1\uc744 \uc900\ube44\ud558\ub294 \uc911\uc785\ub2c8\ub2e4.",
        english = "The request was received and is preparing signing and transfer."
    ),
    "finance_loading_wallet_and_vault_status" to appTextEntry(
        korean = "\uc9c0\uac11\uacfc \uc608\uce58 \uc0c1\ud0dc\ub97c \ubd88\ub7ec\uc624\ub294 \uc911\uc774\uc5d0\uc694.",
        english = "Loading wallet and vault status."
    ),
    "finance_reload_vault_status" to appTextEntry(
        korean = "\uc608\uce58 \uc0c1\ud0dc\ub97c \ub2e4\uc2dc \ubd88\ub7ec\uc640 \uc8fc\uc138\uc694.",
        english = "Please reload the vault status."
    ),
    "finance_load_vault_after_login" to appTextEntry(
        korean = "\ub85c\uadf8\uc778 \ud6c4 \uc608\uce58 \uc2e4\uc5f0\ub3d9 \ub370\uc774\ud130\ub97c \ubd88\ub7ec\uc635\ub2c8\ub2e4.",
        english = "The live vault data will load after you sign in."
    ),
    "finance_helper_active_remote" to appTextEntry(
        korean = "\uc608\uce58 \uc794\uc561\uacfc \uc608\uc0c1 \uc774\uc790\ub97c \ubc14\ub85c \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "You can immediately check the vault balance and estimated yield."
    ),
    "finance_helper_available_remote" to appTextEntry(
        korean = "\uc608\uce58 \uac00\ub2a5 \uae08\uc561\uacfc \uc608\uc0c1 \uc774\uc790\ub97c \uba3c\uc800 \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "You can first check the amount available to deposit and the estimated yield."
    ),
    "finance_helper_active_local" to appTextEntry(
        korean = "\uc608\uce58 \uc911\uc778 \uae08\uc561\uacfc \ub204\uc801 \uc774\uc790\ub97c \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "You can check the deposited amount and accrued yield."
    ),
    "finance_helper_before_start" to appTextEntry(
        korean = "\uc608\uce58 \uc2dc\uc791 \uc804 \uc608\uc0c1 \uc774\uc790\uc640 \uc218\uc775 \uad6c\uc131\uc744 \uba3c\uc800 \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "Before you start, you can check the estimated yield and yield mix."
    ),
    "workproof_attendance" to appTextEntry(korean = "\ucd9c\ud1f4\uadfc", english = "Attendance"),
    "workproof_view_detailed_records" to appTextEntry(korean = "\uc0c1\uc138 \uae30\ub85d \ubcf4\uae30", english = "View Detailed Records"),
    "workproof_workplace_location_check_required" to appTextEntry(korean = "\uadfc\ubb34\uc9c0 \uc704\uce58 \ud655\uc778 \ud544\uc694", english = "Workplace Location Check Required"),
    "workproof_demo_sample_data" to appTextEntry(korean = "\uac00\uc0c1 \uc608\uc2dc \ub370\uc774\ud130", english = "Demo Sample Data"),
    "workproof_work_calendar" to appTextEntry(korean = "\uadfc\ubb34 \ub2ec\ub825", english = "Work Calendar"),
    "workproof_previous_month" to appTextEntry(korean = "\uc774\uc804 \ub2ec", english = "Previous Month"),
    "workproof_next_month" to appTextEntry(korean = "\ub2e4\uc74c \ub2ec", english = "Next Month"),
    "workproof_recent_records" to appTextEntry(korean = "\ucd5c\uadfc \uae30\ub85d", english = "Recent Records"),
    "workproof_change_history" to appTextEntry(korean = "\ubcc0\uacbd \uae30\ub85d", english = "Change History"),
    "workproof_edit_work_hours" to appTextEntry(korean = "\uadfc\ubb34 \uc2dc\uac04 \uc218\uc815", english = "Edit Work Hours"),
    "workproof_requested_time" to appTextEntry(korean = "\uc694\uccad \uc2dc\uac04", english = "Requested Time"),
    "workproof_edit_reason" to appTextEntry(korean = "\uc218\uc815 \uc0ac\uc720", english = "Edit Reason"),
    "workproof_select_one" to appTextEntry(korean = "\uc120\ud0dd\ud558\uc138\uc694", english = "Select One"),
    "workproof_memo" to appTextEntry(korean = "\uba54\ubaa8", english = "Memo"),
    "workproof_additional_memo_optional" to appTextEntry(korean = "\ucd94\uac00 \uba54\ubaa8(\uc120\ud0dd)", english = "Additional Memo (Optional)"),
    "workproof_attachment" to appTextEntry(korean = "\ucca8\ubd80", english = "Attachment"),
    "workproof_choose_file" to appTextEntry(korean = "\ud30c\uc77c \uc120\ud0dd", english = "Choose File"),
    "workproof_choose_again" to appTextEntry(korean = "\ub2e4\uc2dc \uc120\ud0dd", english = "Choose Again"),
    "workproof_remove" to appTextEntry(korean = "\uc81c\uac70", english = "Remove"),
    "workproof_no_file_selected" to appTextEntry(korean = "\uc120\ud0dd\ub41c \ud30c\uc77c \uc5c6\uc74c", english = "No File Selected"),
    "workproof_submit_request" to appTextEntry(korean = "\uc694\uccad \ubcf4\ub0b4\uae30", english = "Submit Request"),
    "workproof_generate_pdf" to appTextEntry(korean = "\uadfc\ubb34 \uae30\ub85d PDF \uc0dd\uc131", english = "Generate Work Record PDF"),
    "workproof_document_preview" to appTextEntry(korean = "\ubb38\uc11c \ubbf8\ub9ac\ubcf4\uae30", english = "Document Preview"),
    "workproof_open_pdf" to appTextEntry(korean = "\uc5f4\uae30", english = "Open"),
    "workproof_share_pdf" to appTextEntry(korean = "\uacf5\uc720", english = "Share"),
    "workproof_day_count" to appTextEntry(korean = "%1\$d\uc77c", english = "%1\$d Days"),
    "workproof_attachment_count_value" to appTextEntry(korean = "\ucca8\ubd80 %1\$d\uac1c", english = "%1\$d Attachments"),
    "workproof_attachment_none" to appTextEntry(korean = "\ucca8\ubd80 \uc5c6\uc74c", english = "No Attachments"),
    "workproof_modified_reason_value" to appTextEntry(korean = "\uc218\uc815 \uc0ac\uc720: %1\$s", english = "Edit Reason: %1\$s"),
    "workproof_record_time_line" to appTextEntry(
        korean = "\ucd9c\uadfc %1\$s / \ud1f4\uadfc %2\$s",
        english = "Clock In %1\$s / Clock Out %2\$s"
    ),
    "workproof_calendar_recorded_count" to appTextEntry(korean = "\uae30\ub85d %1\$d\uc77c", english = "%1\$d Days Recorded"),
    "workproof_work_record_document" to appTextEntry(korean = "\uadfc\ubb34 \uae30\ub85d \ubb38\uc11c", english = "Work Record Document"),
    "workproof_share_work_record_document" to appTextEntry(korean = "\uadfc\ubb34 \uae30\ub85d \ubb38\uc11c \uacf5\uc720", english = "Share Work Record Document"),
    "workproof_reviewing" to appTextEntry(korean = "\uac80\ud1a0 \uc911", english = "Under Review"),
    "workproof_reflected" to appTextEntry(korean = "\ubc18\uc601\ub428", english = "Reflected"),
    "workproof_excluded" to appTextEntry(korean = "\uc81c\uc678\ub428", english = "Excluded"),
    "workproof_recorded" to appTextEntry(korean = "\uae30\ub85d", english = "Recorded"),
    "workproof_review_status" to appTextEntry(korean = "\uac80\ud1a0 \uc0c1\ud0dc", english = "Review Status"),
    "workproof_recognized_time" to appTextEntry(korean = "\uc778\uc815 \uc2dc\uac04", english = "Recognized Time"),
    "workproof_edit_reason_prefix" to appTextEntry(korean = "\uc218\uc815 \uc0ac\uc720", english = "Edit Reason"),
    "workproof_waiting_for_workplace_review" to appTextEntry(korean = "\uc0ac\uc5c5\uc7a5 \uac80\ud1a0 \ub300\uae30 \uc911", english = "Waiting for Workplace Review"),
    "workproof_reflected_in_recognized_time" to appTextEntry(korean = "\uc778\uc815 \uc2dc\uac04\uc5d0 \ubc18\uc601\ub428", english = "Reflected in Recognized Time"),
    "workproof_excluded_from_settlement" to appTextEntry(korean = "\uc815\uc0b0 \ub300\uc0c1\uc5d0\uc11c \uc81c\uc678\ub428", english = "Excluded from Settlement"),
    "workproof_edit_request_submitted" to appTextEntry(korean = "\uc218\uc815 \uc694\uccad\uc774 \uc811\uc218\ub428", english = "Edit Request Submitted"),
    "workproof_reason_late_button_press" to appTextEntry(
        korean = "\ucd9c\uadfc/\ud1f4\uadfc \ud0ed\uc744 \ub2a6\uac8c \ub20c\ub800\uc5b4\uc694",
        english = "I tapped clock in/out too late."
    ),
    "workproof_reason_late_clock_in" to appTextEntry(
        korean = "\ucd9c\uadfc \uc2dc\uac04\uc744 \ub2e4\uc2dc \uc778\uc815\ubc1b\uace0 \uc2f6\uc5b4\uc694",
        english = "I want the clock-in time to be recognized again."
    ),
    "workproof_reason_early_clock_out" to appTextEntry(
        korean = "\ud1f4\uadfc \uc2dc\uac04\uc744 \ub2e4\uc2dc \uc778\uc815\ubc1b\uace0 \uc2f6\uc5b4\uc694",
        english = "I want the clock-out time to be recognized again."
    ),
    "workproof_reason_other" to appTextEntry(korean = "\uae30\ud0c0 \uc0ac\uc720", english = "Other Reason"),
    "workproof_selected_attachment" to appTextEntry(korean = "\uc120\ud0dd\ud55c \ucca8\ubd80", english = "Selected Attachment"),
    "workproof_live_location_data_missing" to appTextEntry(
        korean = "\uadfc\ubb34\uc9c0 \uc2e4\uc5f0\ub3d9 \ub370\uc774\ud130\uac00 \uc5c6\uc5b4 \uc9c0\ub3c4 \uc88c\ud45c\uac00 \uc2e4\uc81c\uc640 \ub2e4\ub97c \uc218 \uc788\uc5b4\uc694.",
        english = "There is no live workplace location data, so the map coordinates may differ from the real location."
    ),
    "workproof_outside_workplace_radius" to appTextEntry(
        korean = "\uadfc\ubb34\uc9c0 \ubc18\uacbd \ubc16\uc5d0\uc11c\ub294 \ucd9c\ud1f4\uadfc\ud560 \uc218 \uc5c6\uc5b4\uc694.",
        english = "You can't clock in or out outside the workplace radius."
    ),
    "workproof_location" to appTextEntry(korean = "\uc704\uce58", english = "Location"),
    "workproof_current_location" to appTextEntry(korean = "\ud604\uc7ac \uc704\uce58", english = "Current Location"),
    "workproof_my_current_location" to appTextEntry(korean = "\ud604\uc7ac \ub0b4 \uc704\uce58", english = "My Current Location"),
    "workproof_workplace_location" to appTextEntry(korean = "\uadfc\ubb34\uc9c0 \uc704\uce58", english = "Workplace Location"),
    "workproof_move_pin_to_current_location" to appTextEntry(
        korean = "\ud604\uc7ac \ub0b4 \uc704\uce58\ub85c \ud540 \uc774\ub3d9",
        english = "Move the Pin to My Current Location"
    ),
    "workproof_location_status_loading_full" to appTextEntry(
        korean = "\ud604\uc7ac \uc704\uce58\ub97c \ud655\uc778 \uc911\uc774\uc5d0\uc694.",
        english = "Checking your current location."
    ),
    "workproof_location_status_permission_required" to appTextEntry(
        korean = "\uc704\uce58 \uad8c\ud55c\uc744 \ud5c8\uc6a9\ud558\uba74 \ud604\uc7ac \uc704\uce58\ub97c \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "Allow location permission to check your current location."
    ),
    "workproof_location_status_service_unavailable" to appTextEntry(
        korean = "\uc704\uce58 \uc11c\ube44\uc2a4\ub97c \uc0ac\uc6a9\ud560 \uc218 \uc5c6\uc5b4\uc694.",
        english = "Location services are unavailable."
    ),
    "workproof_location_status_disabled" to appTextEntry(
        korean = "\uc704\uce58 \uc11c\ube44\uc2a4\uac00 \uaebc\uc838 \uc788\uc5b4 \ud604\uc7ac \uc704\uce58\ub97c \ud655\uc778\ud560 \uc218 \uc5c6\uc5b4\uc694.",
        english = "Location services are turned off, so your current location can't be checked."
    ),
    "workproof_location_status_error" to appTextEntry(
        korean = "\ud604\uc7ac \uc704\uce58\ub97c \ud655\uc778\ud558\uc9c0 \ubabb\ud588\uc5b4\uc694. \uc704\uce58 \uc11c\ube44\uc2a4\uc640 GPS\ub97c \ud655\uc778\ud574 \uc8fc\uc138\uc694.",
        english = "Couldn't check your current location. Please check location services and GPS."
    ),
    "workproof_location_status_try_again" to appTextEntry(
        korean = "\ud604\uc7ac \uc704\uce58\ub97c \ud655\uc778\ud55c \ub4a4 \ub2e4\uc2dc \uc2dc\ub3c4\ud574 \uc8fc\uc138\uc694.",
        english = "Check your current location and try again."
    ),
    "workproof_empty_audit_title" to appTextEntry(
        korean = "\uc544\uc9c1 \uc313\uc778 \ubcc0\uacbd \uae30\ub85d\uc774 \uc5c6\uc5b4\uc694.",
        english = "There Is No Saved Change History Yet."
    ),
    "workproof_empty_audit_description" to appTextEntry(
        korean = "\uc218\uc815\uc774 \uc0dd\uae30\uba74 \uc0ac\uc720\uc640 \ucca8\ubd80 \uc5ec\ubd80\uac00 \uc5ec\uae30\uc5d0 \uba3c\uc800 \ud45c\uc2dc\ub3fc\uc694.",
        english = "When an edit happens, the reason and attachment status appear here first."
    ),
    "workproof_map_missing_key" to appTextEntry(
        korean = "KAKAO_NATIVE_APP_KEY\ub97c \uc124\uc815\ud558\uba74 \uc9c0\ub3c4\uac00 \ud45c\uc2dc\ub3fc\uc694.",
        english = "Set KAKAO_NATIVE_APP_KEY to show the map."
    ),
    "workproof_map_runtime_unsupported" to appTextEntry(
        korean = "\ud604\uc7ac \uc2e4\ud589 \ud658\uacbd\uc5d0\uc11c\ub294 \uce74\uce74\uc624 \uc9c0\ub3c4\ub97c \uc9c0\uc6d0\ud558\uc9c0 \uc54a\uc544 \uc9c0\ub3c4 \uc5c6\uc774 \ud45c\uc2dc\ub3fc\uc694.",
        english = "The current runtime doesn't support Kakao Maps, so this is shown without the map."
    ),
    "workproof_map_fallback_default" to appTextEntry(korean = "\uc9c0\ub3c4\ub97c \ubd88\ub7ec\uc62c \uc218 \uc5c6\uc5b4\uc694.", english = "Unable to load the map."),
    "workproof_map_error_title" to appTextEntry(korean = "\uc9c0\ub3c4\ub97c \ubd88\ub7ec\uc624\uc9c0 \ubabb\ud588\uc5b4\uc694", english = "Failed to Load the Map"),
    "workproof_map_error_message_default" to appTextEntry(korean = "\uc7a0\uc2dc \ud6c4 \ub2e4\uc2dc \uc2dc\ub3c4\ud574 \uc8fc\uc138\uc694.", english = "Please try again shortly."),
    "workproof_map_retry" to appTextEntry(korean = "\ub2e4\uc2dc \uc2dc\ub3c4", english = "Try Again"),
    "workproof_this_month" to appTextEntry(korean = "\uc774\ubc88 \ub2ec", english = "This Month"),
    "workproof_last_month" to appTextEntry(korean = "\uc9c0\ub09c \ub2ec", english = "Last Month"),
    "workproof_custom_range" to appTextEntry(korean = "\uc9c1\uc811 \uc120\ud0dd", english = "Custom"),
    "workproof_start_date" to appTextEntry(korean = "\uc2dc\uc791\uc77c", english = "Start Date"),
    "workproof_end_date" to appTextEntry(korean = "\uc885\ub8cc\uc77c", english = "End Date"),
    "workproof_end_date_invalid" to appTextEntry(
        korean = "\uc885\ub8cc\uc77c\uc740 \uc2dc\uc791\uc77c\ubcf4\ub2e4 \ube60\ub97c \uc218 \uc5c6\uc5b4\uc694.",
        english = "The end date cannot be earlier than the start date."
    ),
    "workproof_loading_preview" to appTextEntry(korean = "\ubbf8\ub9ac\ubcf4\uae30 \ubd88\ub7ec\uc624\ub294 \uc911", english = "Loading Preview"),
    "workproof_loading_preview_message" to appTextEntry(
        korean = "\uc120\ud0dd\ud55c \uae30\uac04\uc758 \uadfc\ubb34 \uae30\ub85d \uc694\uc57d\uc744 \ud655\uc778\ud558\uace0 \uc788\uc5b4\uc694.",
        english = "Checking the work record summary for the selected period."
    ),
    "workproof_preview_failed" to appTextEntry(korean = "\ubbf8\ub9ac\ubcf4\uae30\ub97c \ubd88\ub7ec\uc624\uc9c0 \ubabb\ud588\uc5b4\uc694", english = "Failed to Load Preview"),
    "workproof_select_period_again" to appTextEntry(korean = "\uae30\uac04 \ub2e4\uc2dc \uc120\ud0dd", english = "Select Period Again"),
    "workproof_failed_to_submit_pdf_request" to appTextEntry(
        korean = "\ubb38\uc11c \uc0dd\uc131 \uc694\uccad\uc744 \uc811\uc218\ud558\uc9c0 \ubabb\ud588\uc5b4\uc694",
        english = "Failed to Submit the Document Request"
    ),
    "workproof_try_again" to appTextEntry(korean = "\ub2e4\uc2dc \uc2dc\ub3c4", english = "Try Again"),
    "workproof_submitting_pdf_request" to appTextEntry(korean = "PDF \uc0dd\uc131 \uc694\uccad \uc811\uc218 \uc911", english = "Submitting PDF Request"),
    "workproof_submitting_pdf_request_message" to appTextEntry(
        korean = "\uc120\ud0dd\ud55c \uae30\uac04\uc758 \uadfc\ubb34 \uae30\ub85d \ubb38\uc11c \uc0dd\uc131\uc744 \uc694\uccad\ud558\uace0 \uc788\uc5b4\uc694.",
        english = "Submitting a work record document request for the selected period."
    ),
    "workproof_submitting_pdf_request_in_progress" to appTextEntry(korean = "PDF \uc0dd\uc131 \uc694\uccad \uc911...", english = "Submitting PDF Request..."),
    "workproof_workplace" to appTextEntry(korean = "\uadfc\ubb34\uc9c0", english = "Workplace"),
    "workproof_selected_period" to appTextEntry(korean = "\uc120\ud0dd \uae30\uac04", english = "Selected Period"),
    "workproof_record_count" to appTextEntry(korean = "\uae30\ub85d \uc218", english = "Record Count"),
    "workproof_edited_records" to appTextEntry(korean = "\uc218\uc815 \uae30\ub85d", english = "Edited Records"),
    "workproof_attachment_count" to appTextEntry(korean = "\ucca8\ubd80 \uc218", english = "Attachment Count"),
    "workproof_total_work_hours" to appTextEntry(korean = "\ucd1d \uadfc\ubb34\uc2dc\uac04", english = "Total Work Hours"),
    "workproof_document_includes" to appTextEntry(korean = "\ubb38\uc11c \ud3ec\ud568 \ud56d\ubaa9", english = "Included in Document"),
    "workproof_pdf_generation_failed" to appTextEntry(korean = "PDF \uc0dd\uc131 \uc2e4\ud328", english = "PDF Generation Failed"),
    "workproof_pdf_ready_to_open" to appTextEntry(korean = "PDF \uc5f4\uae30 \uc900\ube44 \uc644\ub8cc", english = "PDF Ready to Open"),
    "workproof_pdf_request_submitted" to appTextEntry(korean = "PDF \uc0dd\uc131 \uc694\uccad \uc644\ub8cc", english = "PDF Request Submitted"),
    "workproof_file_name" to appTextEntry(korean = "\ud30c\uc77c\uba85", english = "File Name"),
    "workproof_document_generation_failed_message" to appTextEntry(
        korean = "\ubb38\uc11c \uc0dd\uc131\uc5d0 \uc2e4\ud328\ud588\uc5b4\uc694.",
        english = "Document generation failed."
    ),
    "workproof_open_or_share_message" to appTextEntry(
        korean = "\uc5f4\uae30 \ub610\ub294 \uacf5\uc720\ub97c \ub20c\ub7ec \ubb38\uc11c\ub97c \ud655\uc778\ud574 \uc8fc\uc138\uc694.",
        english = "Tap Open or Share to check the document."
    ),
    "workproof_preparing_document_message" to appTextEntry(
        korean = "\uc120\ud0dd\ud55c \uae30\uac04\uc758 \uadfc\ubb34\uae30\ub85d \ubb38\uc11c\ub97c \uc900\ube44\ud558\uace0 \uc788\uc5b4\uc694.",
        english = "Preparing the work record document for the selected period."
    ),
    "workproof_can_generate_after_review_message" to appTextEntry(
        korean = "\uc120\ud0dd\ud55c \uae30\uac04 \uae30\uc900 \uc2e4\uc81c \uadfc\ubb34 \uae30\ub85d \uc694\uc57d\uc744 \ud655\uc778\ud55c \ub4a4 \ubb38\uc11c\ub97c \uc0dd\uc131\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "You can generate the document after checking the work record summary for the selected period."
    ),
    "workproof_unable_to_open_pdf" to appTextEntry(korean = "PDF\ub97c \uc5f4 \uc218 \uc5c6\uc5b4\uc694.", english = "Unable to Open PDF"),
    "workproof_unable_to_share_pdf" to appTextEntry(korean = "PDF\ub97c \uacf5\uc720\ud560 \uc218 \uc5c6\uc5b4\uc694.", english = "Unable to Share PDF"),
    "none" to appTextEntry(korean = "\uc5c6\uc74c", english = "None"),
    "account" to appTextEntry(korean = "\uacc4\uc815", english = "Account"),
    "services" to appTextEntry(korean = "\uc11c\ube44\uc2a4", english = "Services"),
    AppTextKeys.SETTINGS to appTextEntry(korean = "\uc124\uc815", english = "Settings"),
    "language" to appTextEntry(korean = "\uc5b8\uc5b4", english = "Language"),
    "log_out" to appTextEntry(korean = "\ub85c\uadf8\uc544\uc6c3", english = "Log out"),
    "cancel" to appTextEntry(korean = "\ucde8\uc18c", english = "Cancel"),
    "select" to appTextEntry(korean = "\uc120\ud0dd", english = "Select"),
    AppTextKeys.CLOSE to appTextEntry(korean = "\ub2eb\uae30", english = "Close"),
    "share" to appTextEntry(korean = "\uacf5\uc720", english = "Share"),
    "open" to appTextEntry(korean = "\uc5f4\uae30", english = "Open"),
    "view" to appTextEntry(korean = "\ud655\uc778", english = "View"),
    "edit_profile" to appTextEntry(korean = "\ud504\ub85c\ud544 \uc218\uc815", english = "Edit profile"),
    "account_link" to appTextEntry(korean = "\uacc4\uc815 \uc5f0\ub3d9", english = "Account Link"),
    AppTextKeys.ENTER_WORKER_REGISTRATION_CODE to appTextEntry(
        korean = "\uadfc\ub85c\uc790 \ub4f1\ub85d \ucf54\ub4dc \uc785\ub825",
        english = "Enter worker registration code"
    ),
    "you_can_register_your_workplace_link_code" to appTextEntry(
        korean = "\uc18c\uc18d \uc5f0\ub3d9 \ucf54\ub4dc\ub97c \ub4f1\ub85d\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "You can register your workplace link code."
    ),
    "email" to appTextEntry(korean = "\uc774\uba54\uc77c \uc8fc\uc18c", english = "Email"),
    "company" to appTextEntry(korean = "\ud68c\uc0ac", english = "Company"),
    "workplace" to appTextEntry(korean = "\uadfc\ubb34\uc9c0", english = "Workplace"),
    "editable_information" to appTextEntry(korean = "\uc218\uc815 \uac00\ub2a5 \uc815\ubcf4", english = "Editable information"),
    "read_only_information" to appTextEntry(korean = "\uc77d\uae30 \uc804\uc6a9 \uc815\ubcf4", english = "Read-only information"),
    "name" to appTextEntry(korean = "\uc774\ub984", english = "Name"),
    "phone_number" to appTextEntry(korean = "\ud734\ub300\ud3f0 \ubc88\ud638", english = "Phone number"),
    "save" to appTextEntry(korean = "\uc800\uc7a5\ud558\uae30", english = "Save"),
    "saving" to appTextEntry(korean = "\uc800\uc7a5 \uc911...", english = "Saving..."),
    "generate_documents" to appTextEntry(korean = "\ubb38\uc11c \uc0dd\uc131", english = "Generate Documents"),
    AppTextKeys.WAGE_REVIEW to appTextEntry(korean = "\uae09\uc5ec \uc810\uac80", english = "Wage Review"),
    "wallet_management" to appTextEntry(korean = "\uacc4\uc88c \uc9c0\uac11 \uad00\ub9ac", english = "Wallet Management"),
    "total_wallet_balance" to appTextEntry(korean = "총 지갑 잔액", english = "Total Wallet Balance"),
    "my_wallet" to appTextEntry(korean = "내 지갑", english = "My Wallet"),
    "recipient_wallet" to appTextEntry(korean = "수신 지갑", english = "Recipient Wallet"),
    "add_wallet" to appTextEntry(korean = "지갑 추가", english = "Add Wallet"),
    "primary" to appTextEntry(korean = "대표", english = "Primary"),
    "edit" to appTextEntry(korean = "수정", english = "Edit"),
    "wallet_address_checking" to appTextEntry(korean = "지갑 주소 확인 중", english = "Checking Wallet Address"),
    "no_recipient_wallet_registered" to appTextEntry(korean = "등록된 수신 지갑이 없어요", english = "No Recipient Wallets Registered"),
    "add_recipient_wallet_helper" to appTextEntry(korean = "대상을 휴대폰으로 찾고, 없으면 지갑 주소를 직접 입력해서 등록할 수 있어요.", english = "Search by phone first, or enter a wallet address directly to register one."),
    "wallet_address_copied" to appTextEntry(korean = "지갑 주소를 복사했어요", english = "Wallet address copied."),
    "edit_recipient_wallet" to appTextEntry(korean = "수신 지갑 수정", english = "Edit Recipient Wallet"),
    "edit_recipient_wallet_helper" to appTextEntry(korean = "이름, 관계, 지갑 주소를 바꾸면 다음 송금부터 반영돼요.", english = "Changes to the name, relationship, and wallet address apply from the next transfer."),
    "notice" to appTextEntry(korean = "안내", english = "Notice"),
    "edit_recipient_wallet_notice" to appTextEntry(korean = "지갑 주소는 송금 중 실수로 잘못 붙여넣지 않도록 다시 확인해 주세요.", english = "Please double-check the wallet address so it is not pasted incorrectly during transfers."),
    "save_changes" to appTextEntry(korean = "변경 저장", english = "Save Changes"),
    "change_visible_next_transfer" to appTextEntry(korean = "변경한 내용은 다음 송금부터 적용될 수 있어요.", english = "Your changes may apply from the next transfer."),
    "transfer_demo_notice" to appTextEntry(korean = "현재는 테스트넷 데모입니다. 실제 자금 이동이 발생하지 않습니다.", english = "This is a testnet demo. No real funds are moved."),
    "alias" to appTextEntry(korean = "이름", english = "Name"),
    "wallet_address" to appTextEntry(korean = "지갑 주소", english = "Wallet Address"),
    "enter_testnet_evm_wallet_address" to appTextEntry(korean = "테스트넷 EVM 지갑 주소를 입력해 주세요.", english = "Enter a testnet EVM wallet address."),
    "wallet_address_length_error" to appTextEntry(korean = "0x로 시작하는 42자 지갑 주소를 입력해 주세요.", english = "Enter a 42-character wallet address starting with 0x."),
    "relationship" to appTextEntry(korean = "관계", english = "Relationship"),
    "family" to appTextEntry(korean = "가족", english = "Family"),
    "spouse" to appTextEntry(korean = "배우자", english = "Spouse"),
    "parent" to appTextEntry(korean = "부모", english = "Parent"),
    "child" to appTextEntry(korean = "자녀", english = "Child"),
    "sibling" to appTextEntry(korean = "형제자매", english = "Sibling"),
    "relative" to appTextEntry(korean = "친척", english = "Relative"),
    "friend" to appTextEntry(korean = "친구", english = "Friend"),
    "other" to appTextEntry(korean = "기타", english = "Other"),
    "all" to appTextEntry(korean = "전체", english = "All"),
    "income" to appTextEntry(korean = "입금", english = "Deposit"),
    "expense" to appTextEntry(korean = "출금", english = "Withdrawal"),
    "search" to appTextEntry(korean = "검색", english = "Search"),
    "no_search_results" to appTextEntry(korean = "검색 결과가 없어요.", english = "No search results."),
    "transaction_not_found" to appTextEntry(korean = "거래 정보를 찾지 못했어요.", english = "Couldn't find the transaction details."),
    "category" to appTextEntry(korean = "거래 분류", english = "Category"),
    "memo" to appTextEntry(korean = "메모", english = "Memo"),
    "no_memo" to appTextEntry(korean = "메모 없음", english = "No memo"),
    "direction" to appTextEntry(korean = "거래 방향", english = "Direction"),
    "method" to appTextEntry(korean = "거래 수단", english = "Method"),
    "occurred_at" to appTextEntry(korean = "거래 시각", english = "Time"),
    "save" to appTextEntry(korean = "저장", english = "Save"),
    "enter_memo" to appTextEntry(korean = "메모를 입력해 주세요.", english = "Enter a memo."),
    "previous_month" to appTextEntry(korean = "이전 달", english = "Previous Month"),
    "next_month" to appTextEntry(korean = "다음 달", english = "Next Month"),
    "wage_check_title" to appTextEntry(korean = "급여 점검", english = "Wage Review"),
    "wage_loading_message" to appTextEntry(korean = "급여 정보를 불러오는 중입니다.", english = "Loading wage information."),
    "wage_login_required_message" to appTextEntry(korean = "로그인 후 급여 정보를 확인할 수 있어요.", english = "Sign in to check wage information."),
    "wage_error_message" to appTextEntry(korean = "급여 정보를 다시 확인해 주세요.", english = "Please check the wage information again."),
    "wage_empty_message" to appTextEntry(korean = "아직 급여 정보가 없어요.", english = "There is no wage information yet."),
    "refresh" to appTextEntry(korean = "다시 확인", english = "Refresh"),
    "open_menu_registration" to appTextEntry(korean = "근로자 등록 코드 입력", english = "Open Worker Registration"),
    "wage_description" to appTextEntry(korean = "실제 입금액을 입력하면 예상 급여와 차이를 비교할 수 있어요.", english = "Enter the actual deposit to compare it with the estimated wage."),
    "wage_record_needed_status" to appTextEntry(korean = "입금 기록 필요", english = "Deposit Record Needed"),
    "wage_enter_actual_deposit" to appTextEntry(korean = "실제 입금액 입력", english = "Enter Actual Deposit"),
    "wage_enter_actual_deposit_desc" to appTextEntry(korean = "이번 달 실제 입금된 금액을 입력하면 급여 차이를 계산할 수 있어요.", english = "Enter the actual deposited amount for this month to calculate the wage difference."),
    "wage_payday_basis_format" to appTextEntry(korean = "급여일 %d일 기준", english = "Based on payday %d"),
    "deductions_confirmed" to appTextEntry(korean = "공제 확인", english = "Deductions Confirmed"),
    "deductions_unknown" to appTextEntry(korean = "공제 미확인", english = "Deductions Unknown"),
    "threshold_badge_format" to appTextEntry(korean = "임계값 %s", english = "Threshold %s"),
    "work_days" to appTextEntry(korean = "근무일", english = "Work Days"),
    "total_hours" to appTextEntry(korean = "총 근무시간", english = "Total Hours"),
    "overtime_night" to appTextEntry(korean = "연장/야간", english = "Overtime/Night"),
    "days_format" to appTextEntry(korean = "%d일", english = "%dd"),
    "hours_format" to appTextEntry(korean = "%d시간", english = "%dh"),
    "overtime_night_hours_format" to appTextEntry(korean = "%d/%d시간", english = "%d/%dh"),
    "estimated_base_pay" to appTextEntry(korean = "기본급", english = "Base Pay"),
    "estimated_overtime_pay" to appTextEntry(korean = "연장 수당", english = "Overtime Pay"),
    "estimated_night_pay" to appTextEntry(korean = "야간 수당", english = "Night Pay"),
    "estimated_total_pay" to appTextEntry(korean = "예상 급여", english = "Estimated Total Pay"),
    "wage_difference_pending_title" to appTextEntry(korean = "입금 기록을 먼저 입력해 주세요", english = "Enter the deposit record first"),
    "wage_difference_match_title" to appTextEntry(korean = "예상 급여와 실제 입금액이 일치해요", english = "The expected wage matches the actual deposit"),
    "wage_difference_under_title" to appTextEntry(korean = "실제 입금액이 예상 급여보다 적어요", english = "The actual deposit is lower than the expected wage"),
    "wage_difference_over_title" to appTextEntry(korean = "실제 입금액이 예상 급여보다 많아요", english = "The actual deposit is higher than the expected wage"),
    "wage_difference_pending_desc" to appTextEntry(korean = "실제 입금액을 입력해야 급여 차이를 확인할 수 있어요.", english = "You need to enter the actual deposit to check the wage difference."),
    "wage_difference_match_desc" to appTextEntry(korean = "이번 달 예상 급여와 실제 입금액이 잘 맞아요.", english = "This month's expected wage and actual deposit are aligned."),
    "wage_difference_under_desc" to appTextEntry(korean = "누락된 수당이나 공제 내역이 있는지 확인해 보세요.", english = "Check whether any allowance or deduction details are missing."),
    "wage_difference_over_desc" to appTextEntry(korean = "추가 입금이나 정산 반영 여부를 확인해 보세요.", english = "Check whether an extra deposit or settlement adjustment was applied."),
    "wage_difference_amount" to appTextEntry(korean = "차이액", english = "Difference"),
    "wage_estimated_wage" to appTextEntry(korean = "예상 급여", english = "Estimated Wage"),
    "actual_deposit" to appTextEntry(korean = "실제 입금", english = "Actual Deposit"),
    "evidence_reason_format" to appTextEntry(korean = "가능 원인: %s", english = "Possible cause: %s"),
    "evidence_overtime_format" to appTextEntry(korean = "연장 %d시간 반영", english = "Overtime %dh applied"),
    "evidence_night_format" to appTextEntry(korean = "야간 %d시간 반영", english = "Night %dh applied"),
    "evidence_modified_records_format" to appTextEntry(korean = "수정 기록 %d건 확인", english = "Checked %d edited records"),
    "evidence_related_docs_format" to appTextEntry(korean = "관련 문서 %d건 준비", english = "Prepared %d related documents"),
    "create_evidence_document" to appTextEntry(korean = "증빙 문서 생성하기", english = "Create Evidence Document"),
    "completed" to appTextEntry(korean = "완료", english = "Completed"),
    "back_to_deposit_input" to appTextEntry(korean = "입금으로 돌아가기", english = "Back to Deposit Input"),
    "details_summary" to appTextEntry(korean = "상세 점검", english = "Detailed Review"),
    "hide_details" to appTextEntry(korean = "상세 닫기", english = "Hide Details"),
    "view_details" to appTextEntry(korean = "상세 보기", english = "View Details"),
    "input_actual_deposit_placeholder" to appTextEntry(korean = "실제 입금액 입력", english = "Enter Actual Deposit"),
    "apply" to appTextEntry(korean = "기록", english = "Apply"),
    "minus_50k" to appTextEntry(korean = "-5만원", english = "-₩50k"),
    "plus_50k" to appTextEntry(korean = "+5만원", english = "+₩50k"),
    "estimate_breakdown" to appTextEntry(korean = "예상 급여 계산", english = "Estimated Wage Breakdown"),
    "based_on_work_records" to appTextEntry(korean = "근무기록 기준 계산", english = "Calculated from work records"),
    "evidence_summary" to appTextEntry(korean = "증빙 요약", english = "Evidence Summary"),
    "start_wage_check" to appTextEntry(korean = "급여 점검 시작", english = "Start Wage Review"),
    "no_previous_step" to appTextEntry(korean = "이전 단계 없음", english = "No Previous Step"),
    "final_step" to appTextEntry(korean = "마지막 단계", english = "Final Step"),
    "go_to_summary" to appTextEntry(korean = "입력 화면으로", english = "Go to Summary"),
    "go_to_details" to appTextEntry(korean = "상세 점검으로", english = "Go to Details"),
    "go_to_actions" to appTextEntry(korean = "다음 단계로", english = "Go to Actions"),
    "wage_section_state" to appTextEntry(korean = "급여 상태", english = "Wage Status"),
    "demo_sample_data" to appTextEntry(korean = "\uac00\uc0c1 \uc608\uc2dc \ub370\uc774\ud130", english = "Demo sample data"),
    "no_document_is_ready_yet" to appTextEntry(korean = "\uc544\uc9c1 \uc900\ube44\ub41c \ubb38\uc11c\uac00 \uc5c6\uc5b4\uc694.", english = "No document is ready yet."),
    "there_is_no_document_to_share_yet" to appTextEntry(korean = "\uc544\uc9c1 \uacf5\uc720\ud560 \ubb38\uc11c\uac00 \uc5c6\uc5b4\uc694.", english = "There is no document to share yet."),
    "current_status" to appTextEntry(korean = "\ud604\uc7ac \uc0c1\ud0dc", english = "Current status"),
    "claim_prep" to appTextEntry(korean = "\uc2e0\uace0 \uc900\ube44", english = "Claim prep"),
    "generate_copy" to appTextEntry(korean = "\ubb38\uc7a5 \ub9cc\ub4e4\uae30", english = "Generate copy"),
    "default" to appTextEntry(korean = "\uae30\ubcf8", english = "Default"),
    "formal" to appTextEntry(korean = "\uc815\uc911\ud558\uac8c", english = "Formal"),
    "short" to appTextEntry(korean = "\uc9e7\uac8c", english = "Short"),
    "copy" to appTextEntry(korean = "\ubcf5\uc0ac", english = "Copy"),
    "files" to appTextEntry(korean = "\ud30c\uc77c", english = "Files"),
    "open_document" to appTextEntry(korean = "\ubb38\uc11c\ub85c \uc774\ub3d9", english = "Open document"),
    "work_record_document" to appTextEntry(korean = "\uadfc\ubb34 \uae30\ub85d \ubb38\uc11c", english = "Work record document"),
    "submission_paths" to appTextEntry(korean = "\uc811\uc218 \uacbd\ub85c \uc548\ub0b4", english = "Submission paths"),
    "online" to appTextEntry(korean = "\uc628\ub77c\uc778", english = "Online"),
    "phone" to appTextEntry(korean = "\uc804\ud654", english = "Phone"),
    "visit" to appTextEntry(korean = "\ubc29\ubb38", english = "Visit"),
    "checklist" to appTextEntry(korean = "\uccb4\ud06c\ub9ac\uc2a4\ud2b8", english = "Checklist"),
    "prepare_work_record_document" to appTextEntry(korean = "\uadfc\ubb34 \uae30\ub85d \ubb38\uc11c \uc900\ube44", english = "Prepare work record document"),
    "prepare_evidence_bundle" to appTextEntry(korean = "\uadfc\uac70 \uc790\ub8cc \ubb36\uc74c \uc900\ube44", english = "Prepare evidence bundle"),
    "check_submission_path_online_phone_visit" to appTextEntry(
        korean = "\uc811\uc218 \uacbd\ub85c \ud655\uc778(\uc628\ub77c\uc778/\uc804\ud654/\ubc29\ubb38)",
        english = "Check submission path (online/phone/visit)"
    ),
    "open_evidence_bundle" to appTextEntry(korean = "\uadfc\uac70 \uc790\ub8cc \ubb36\uc74c \uc5f4\uae30", english = "Open evidence bundle"),
    "difference_screen" to appTextEntry(korean = "\ucc28\uc561 \ud654\uba74", english = "Difference screen"),
    "menu_document_ready" to appTextEntry(korean = "\uc900\ube44\ub428", english = "Ready"),
    "menu_document_pending" to appTextEntry(korean = "\ub300\uae30", english = "Pending"),
    "menu_document_generating" to appTextEntry(korean = "\uc900\ube44 \uc911", english = "Preparing"),
    "menu_document_open_ready" to appTextEntry(korean = "\uc5f4\uae30 \uac00\ub2a5", english = "Ready to Open"),
    "menu_document_generation_failed" to appTextEntry(korean = "\uc0dd\uc131 \uc2e4\ud328", english = "Generation Failed"),
    "menu_document_proof_summary" to appTextEntry(
        korean = "\uc120\ud0dd\ud55c \uae30\uac04\uc758 \ucd9c\ud1f4\uadfc \uae30\ub85d\uacfc \ubcc0\uacbd \uc774\ub825\uc744 \uc815\ub9ac\ud55c PDF \ubb38\uc11c\uc608\uc694.",
        english = "This PDF document summarizes the selected period's attendance records and edit history."
    ),
    "menu_document_claim_summary" to appTextEntry(
        korean = "\uc2e0\uace0 \uc900\ube44\uc5d0 \ud544\uc694\ud55c \ud575\uc2ec \uc790\ub8cc\ub97c \ud55c \ubc88\uc5d0 \uc815\ub9ac\ud574\uc694.",
        english = "This organizes the core materials needed for claim preparation in one place."
    ),
    "menu_document_receipt_summary" to appTextEntry(
        korean = "\ucd5c\uadfc \uc1a1\uae08 \uc601\uc218\uc99d\uacfc \uc804\uc1a1 \ud574\uc2dc\ub97c \ub2e4\uc2dc \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "You can review the recent transfer receipt and transfer hash again."
    ),
    "menu_receipt_checking" to appTextEntry(korean = "\ud655\uc778 \uc911", english = "Checking"),
    "menu_receipt_title" to appTextEntry(korean = "\uc1a1\uae08 \uc601\uc218\uc99d", english = "Transfer Receipt"),
    "menu_receipt_missing_hash" to appTextEntry(
        korean = "\uc544\uc9c1 \uc804\uc1a1 \ud574\uc2dc\uac00 \uc5c6\uc5b4\uc694",
        english = "There is no transfer hash yet"
    ),
    "menu_network_label" to appTextEntry(korean = "\uc138\ud3f4\ub9ac\uc544", english = "Sepolia"),
    "menu_tx_hash_section_title" to appTextEntry(korean = "\uc804\uc1a1 \ud574\uc2dc", english = "Transfer Hash"),
    "menu_not_created_yet" to appTextEntry(korean = "\uc544\uc9c1 \uc0dd\uc131\ub418\uc9c0 \uc54a\uc558\uc5b4\uc694", english = "Not created yet"),
    "menu_receipt_helper" to appTextEntry(
        korean = "\uc601\uc218\uc99d \ub9c1\ud06c\uc640 \ud574\uc2dc\ub294 \ub098\uc911\uc5d0 \ub2e4\uc2dc \ud655\uc778\ud558\uac70\ub098 \uacf5\uc720\ud560 \ub54c \uadf8\ub300\ub85c \uc0ac\uc6a9\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "You can use the receipt link and hash later to check or share it again."
    ),
    "menu_view_in_block_explorer" to appTextEntry(korean = "\ube14\ub85d \ud0d0\uc0c9\uae30\uc5d0\uc11c \ubcf4\uae30", english = "View in Block Explorer"),
    "menu_share_receipt_title" to appTextEntry(korean = "DonDone \ud14c\uc2a4\ud2b8\ub137 \uc1a1\uae08 \uc601\uc218\uc99d", english = "DonDone Testnet Transfer Receipt"),
    "menu_status_prefix" to appTextEntry(korean = "\uc0c1\ud0dc: ", english = "Status: "),
    "menu_amount_prefix" to appTextEntry(korean = "\uae08\uc561: ", english = "Amount: "),
    "menu_recipient_prefix" to appTextEntry(korean = "\ubc1b\ub294 \uc0ac\ub78c: ", english = "Recipient: "),
    "menu_hash_prefix" to appTextEntry(korean = "\uc804\uc1a1 \ud574\uc2dc: ", english = "Transfer Hash: "),
    "menu_explorer_prefix" to appTextEntry(korean = "\ube14\ub85d \ud0d0\uc0c9\uae30: ", english = "Block Explorer: "),
    "menu_receipt_failed" to appTextEntry(korean = "\uc2e4\ud328", english = "Failed"),
    "menu_receipt_confirmed_detail" to appTextEntry(
        korean = "\uba54\ub274\uc5d0\uc11c \ucd5c\uadfc \uc1a1\uae08 \uc601\uc218\uc99d\uacfc \uc804\uc1a1 \ud574\uc2dc\ub97c \ub2e4\uc2dc \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "You can review the recent transfer receipt and transfer hash again from the menu."
    ),
    "menu_receipt_failed_detail" to appTextEntry(
        korean = "\uc804\uc1a1\uc774 \uc644\ub8cc\ub418\uc9c0 \uc54a\uc544 \uc2e4\ud328 \uc6d0\uc778\uc744 \uba3c\uc800 \ud655\uc778\ud574\uc57c \ud574\uc694.",
        english = "The transfer did not complete, so you should check the failure reason first."
    ),
    "menu_receipt_pending_detail" to appTextEntry(
        korean = "\ub124\ud2b8\uc6cc\ud06c \ud655\uc778\uc774 \ub05d\ub098\uba74 \uc601\uc218\uc99d \uc0c1\ud0dc\uac00 \ucd5c\uc885 \ud655\uc815\ub3fc\uc694.",
        english = "Once the network check finishes, the receipt status will be finalized."
    ),
    "menu_receipt_pending_notice" to appTextEntry(
        korean = "\ub124\ud2b8\uc6cc\ud06c \ud655\uc778\uc774 \ub05d\ub098\uba74 \uc601\uc218\uc99d \uc0c1\ud0dc\uac00 \uc790\ub3d9\uc73c\ub85c \uc644\ub8cc\ub85c \ubc14\ub031\ub2c8\ub2e4.",
        english = "Once the network check finishes, the receipt status changes to complete automatically."
    ),
    "menu_receipt_failed_notice" to appTextEntry(
        korean = "\uc2e4\ud328\ud55c \uc804\uc1a1\uc740 \uc601\uc218\uc99d \ub300\uc2e0 \uc0c1\ud0dc\uc640 \ud574\uc2dc\ub9cc \ub2e4\uc2dc \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "For a failed transfer, you can review only the status and hash instead of a receipt."
    ),
    "menu_demo_fallback_message" to appTextEntry(
        korean = "\ubb38\uc11c\uc640 \uadfc\ubb34 \uc815\ubcf4 \uc77c\ubd80\ub294 \uc608\uc2dc \ub370\uc774\ud130\uc785\ub2c8\ub2e4. \ud68c\uc0ac \ub4f1\ub85d \ud6c4 \uc2e4\uc81c \ub370\uc774\ud130\ub85c \uc804\ud658\ub429\ub2c8\ub2e4.",
        english = "Some documents and work information are demo data. They switch to real data after company registration."
    ),
    "menu_open_ready_document_missing" to appTextEntry(korean = "\uc544\uc9c1 \uc900\ube44\ub41c \ubb38\uc11c\uac00 \uc5c6\uc5b4\uc694.", english = "No document is ready yet."),
    "menu_share_ready_document_missing" to appTextEntry(korean = "\uc544\uc9c1 \uacf5\uc720\ud560 \ubb38\uc11c\uac00 \uc5c6\uc5b4\uc694.", english = "There is no document to share yet."),
    "menu_logout_confirm_message" to appTextEntry(
        korean = "\ud604\uc7ac \uacc4\uc815\uc5d0\uc11c \ub85c\uadf8\uc544\uc6c3\ud560\uae4c\uc694?",
        english = "Do you want to log out of the current account?"
    ),
    "menu_profile_subtitle" to appTextEntry(
        korean = "\uc774\ub984\uacfc \ud734\ub300\ud3f0 \ubc88\ud638\ub97c \uc218\uc815\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "You can update your name and phone number."
    ),
    "menu_readonly_notice" to appTextEntry(
        korean = "\uc774\uba54\uc77c/\ud68c\uc0ac/\uadfc\ubb34\uc9c0 \uc815\ubcf4\ub294 \uacc4\uc815 \uc5f0\ub3d9 \uc815\ubcf4\ub77c \uc571\uc5d0\uc11c \uc9c1\uc811 \ubcc0\uacbd\ud560 \uc218 \uc5c6\uc5b4\uc694.",
        english = "Email, company, and workplace information come from account linking, so they can't be changed directly in the app."
    ),
    "menu_no_affiliation_info" to appTextEntry(korean = "\uc18c\uc18d \uc815\ubcf4 \uc5c6\uc74c", english = "No Affiliation Info"),
    "menu_document_proof_detail" to appTextEntry(
        korean = "\uadfc\ubb34 \ud0ed\uc5d0\uc11c \uc120\ud0dd\ud55c \uae30\uac04\uc758 \ucd9c\ud1f4\uadfc \uae30\ub85d\uacfc \ubcc0\uacbd \uc774\ub825\uc744 \ub2e4\uc2dc \uc5f4\uace0 \uacf5\uc720\ud560 \uc218 \uc788\ub294 \ubb38\uc11c\uc608\uc694.",
        english = "This document lets you reopen and share the attendance records and edit history for the selected period from the Work tab."
    ),
    "menu_document_claim_detail" to appTextEntry(
        korean = "\ucc28\uc561 \uac80\ud1a0 \uacb0\uacfc\ub97c \ud1a0\ub300\ub85c \uc2e0\uace0 \uc900\ube44 \ud750\ub984\uc73c\ub85c \uc774\uc5b4\uc9c0\ub294 \ubb36\uc74c \ubb38\uc11c\uc608\uc694.",
        english = "This bundled document continues into the claim preparation flow based on the difference review result."
    ),
    "menu_document_receipt_detail" to appTextEntry(
        korean = "\ucd5c\uadfc \ud14c\uc2a4\ud2b8\ub137 \uc1a1\uae08 \ub0b4\uc5ed\uacfc \uc804\uc1a1 \ud574\uc2dc\ub97c \ud655\uc778\ud560 \uc218 \uc788\ub294 \uc601\uc218\uc99d \ubb38\uc11c\uc608\uc694.",
        english = "This receipt document lets you review the recent testnet transfer history and transfer hash."
    ),
    "menu_claim_summary_firm" to appTextEntry(
        korean = "\uadfc\ubb34 \uae30\ub85d\uacfc \ucc28\uc561 \uadfc\uac70\ub97c \ubc14\ud0d5\uc73c\ub85c \uc811\uc218 \uc0ac\uc2e4\uc744 \uc815\uc911\ud558\uac8c \uc815\ub9ac\ud55c \ucd08\uc548\uc785\ub2c8\ub2e4. \uc81c\ucd9c \uc804\uc5d0 \ubb38\uc7a5\uc744 \ud55c \ubc88 \ub354 \ub2e4\ub4ec\uc5b4 \uc8fc\uc138\uc694.",
        english = "This is a formal draft that organizes the filing facts based on work records and supporting evidence for the difference. Please review the wording once more before submission."
    ),
    "menu_claim_summary_short" to appTextEntry(
        korean = "\ucc28\uc561\uacfc \uadfc\uac70\ub97c \uc9e7\uac8c \uc815\ub9ac\ud574 \uc2e0\uace0 \uc900\ube44\ub97c \ube60\ub974\uac8c \uc2dc\uc791\ud560 \uc218 \uc788\ub3c4\ub85d \uc694\uc57d\ud55c \ubb38\uc7a5\uc785\ub2c8\ub2e4.",
        english = "This is a shortened summary of the difference and evidence so you can start claim preparation quickly."
    ),
    "menu_claim_summary_default" to appTextEntry(
        korean = "\uc790\ub3d9 \uc81c\ucd9c\uc774 \uc544\ub2c8\ub77c \uc81c\ucd9c \uc804\uc5d0 \ubb38\uc7a5\uacfc \uc790\ub8cc\ub97c \ube60\ub974\uac8c \uc815\ub9ac\ud574 \ubcf4\ub294 \ub370\ubaa8 \ub2e8\uacc4\uc608\uc694.",
        english = "This is a demo step for quickly organizing your text and materials before submission, not an automatic filing."
    ),
    "menu_claim_sheet_subtitle" to appTextEntry(
        korean = "\ud544\uc694\ud55c \ubb38\uc11c\uc640 \uc811\uc218 \uacbd\ub85c\ub97c \ud55c \ud654\uba74\uc5d0\uc11c \uc815\ub9ac\ud574 \ub458 \uc218 \uc788\uc5b4\uc694.",
        english = "You can organize the required documents and filing paths on one screen."
    ),
    "menu_claim_summary_section" to appTextEntry(korean = "\uc81c\ucd9c \ubb38\uc7a5 \uc694\uc57d", english = "Submission Copy Summary"),
    "menu_claim_demo_notice" to appTextEntry(
        korean = "\uc774 \ubb38\uc7a5\uc740 \ub370\ubaa8\uc6a9 \ucc38\uace0 \ubb38\uc7a5\uc785\ub2c8\ub2e4. \uc2e4\uc81c \uc81c\ucd9c \uc804\uc5d0\ub294 \ubb38\uc7a5\uacfc \uadfc\uac70\ub97c \ub2e4\uc2dc \ud655\uc778\ud574 \uc8fc\uc138\uc694.",
        english = "This copy is demo reference text. Please review the wording and evidence again before actual submission."
    ),
    "menu_online_submission_desc" to appTextEntry(
        korean = "\uc628\ub77c\uc778 \uc811\uc218 \uc804\uc5d0\ub294 \ud544\uc694\ud55c \ud56d\ubaa9\uacfc \uc81c\ucd9c \uc21c\uc11c\ub97c \uba3c\uc800 \ud655\uc778\ud574 \ub450\ub294 \ud3b8\uc774 \uc548\uc804\ud574\uc694.",
        english = "Before filing online, it's safer to check the required items and submission order first."
    ),
    "menu_phone_submission_desc" to appTextEntry(
        korean = "\uc0c1\ub2f4\uc774\ub098 \uc811\uc218 \uc804\uc5d0 \uc900\ube44\ud560 \ubb38\uc7a5\uacfc \uc790\ub8cc\ub97c \ube60\ub974\uac8c \ud655\uc778\ud560 \uc218 \uc788\ub3c4\ub85d \uc815\ub9ac\ud588\uc5b4\uc694.",
        english = "This is organized so you can quickly review the text and materials to prepare before a call or filing."
    ),
    "menu_visit_submission_desc" to appTextEntry(
        korean = "\ubc29\ubb38 \uc811\uc218 \uc804\uc5d0\ub294 \ucc59\uae38 \ud30c\uc77c\uacfc \uc804\ub2ec \uc21c\uc11c\ub97c \uccb4\ud06c\ud574 \ub450\uc138\uc694.",
        english = "Before an in-person filing, check the files to bring and the delivery order."
    ),
    "menu_claim_legal_notice" to appTextEntry(
        korean = "\uc774 \uc548\ub0b4\ub294 \ucc38\uace0\uc6a9\uc774\uba70 \ubc95\ub960 \uc790\ubb38\uc774 \uc544\ub2d9\ub2c8\ub2e4. \ud544\uc694\ud558\uba74 \uc804\ubb38\uae30\uad00 \uc0c1\ub2f4\ub3c4 \ud568\uaed8 \ud655\uc778\ud574 \uc8fc\uc138\uc694.",
        english = "This guidance is for reference and is not legal advice. If needed, also consult a professional organization."
    ),
    "menu_live_proof_failed_summary" to appTextEntry(
        korean = "\uadfc\ubb34 \uae30\ub85d \ubb38\uc11c \uc0dd\uc131\uc774 \uc2e4\ud328\ud588\uc5b4\uc694. \uae30\uac04\uc744 \ub2e4\uc2dc \uc120\ud0dd\ud574 \uc7ac\uc2dc\ub3c4\ud574 \uc8fc\uc138\uc694.",
        english = "Work record document generation failed. Please select the period again and retry."
    ),
    "menu_live_proof_ready_summary" to appTextEntry(
        korean = "\uba54\ub274\uc5d0\uc11c \ubb38\uc11c\ub97c \uc5f4\uac70\ub098 \uacf5\uc720\ud560 \ub54c \uc120\ud0dd\ud55c \uae30\uac04\uc758 PDF\ub97c \ubc14\ub85c \uc0dd\uc131\ud560 \uc218 \uc788\uc5b4\uc694.",
        english = "When you open or share the document from the menu, you can generate the PDF for the selected period immediately."
    ),
    "menu_live_proof_requested_summary" to appTextEntry(
        korean = "\uc120\ud0dd\ud55c \uae30\uac04\uc758 \ucd9c\ud1f4\uadfc \uae30\ub85d\uacfc \ubcc0\uacbd \uc774\ub825\uc744 \uc815\ub9ac\ud55c PDF \ubb38\uc11c \uc694\uccad\uc774 \uc800\uc7a5\ub410\uc5b4\uc694.",
        english = "A PDF document request containing the selected period's attendance records and edit history has been saved."
    ),
    "menu_live_proof_generation_failed" to appTextEntry(korean = "\uc5c5\ub370\uc774\ud2b8 \uc0dd\uc131 \uc2e4\ud328", english = "Update: generation failed"),
    "menu_live_proof_open_ready" to appTextEntry(korean = "\uc5c5\ub370\uc774\ud2b8 \ubc14\ub85c \uc0dd\uc131 \uac00\ub2a5", english = "Update: ready to open"),
    "menu_live_proof_request_saved" to appTextEntry(korean = "\uc5c5\ub370\uc774\ud2b8 \uc0dd\uc131 \uc694\uccad \uc811\uc218\ub428", english = "Update: request saved"),
    "transfer_dondone_wallet" to appTextEntry(korean = "DonDone \uc9c0\uac11", english = "DonDone Wallet"),
    "transfer_checking_balance" to appTextEntry(korean = "\uc794\uc561 \ud655\uc778 \uc911", english = "Checking Balance"),
    "transfer_select_recipient" to appTextEntry(korean = "\uc218\uc2e0\uc790\ub97c \uc120\ud0dd\ud574 \uc8fc\uc138\uc694", english = "Please select a recipient"),
    "transfer_sending_request" to appTextEntry(korean = "\uc1a1\uae08 \uc694\uccad\uc744 \ubcf4\ub0b4\ub294 \uc911\uc774\uc5d0\uc694.", english = "Sending the transfer request."),
    "transfer_completed_detail" to appTextEntry(korean = "\uc1a1\uae08\uc774 \uc815\uc0c1\uc801\uc73c\ub85c \uc644\ub8cc\ub418\uc5c8\uc5b4\uc694.", english = "The transfer completed successfully."),
    "transfer_failed_detail" to appTextEntry(korean = "\uc1a1\uae08\uc744 \uc644\ub8cc\ud558\uc9c0 \ubabb\ud588\uc5b4\uc694.", english = "Couldn't complete the transfer."),
    "transfer_failed_with_reason" to appTextEntry(korean = "\uc804\uc1a1\uc774 \uc2e4\ud328\ud588\uc5b4\uc694. \uc0ac\uc720 : %s", english = "The transfer failed. Reason: %s"),
    "transfer_checking_progress" to appTextEntry(korean = "\uc1a1\uae08 \uc9c4\ud589 \uc0c1\ud0dc\ub97c \ud655\uc778\ud558\uace0 \uc788\uc5b4\uc694.", english = "Checking the transfer progress."),
    "transfer_select_account" to appTextEntry(korean = "\uacc4\uc88c\ub97c \uc120\ud0dd\ud574 \uc8fc\uc138\uc694", english = "Please select an account"),
    "transfer_select_wallet" to appTextEntry(korean = "\ubc1b\ub294 \uc9c0\uac11\uc744 \uc120\ud0dd\ud574 \uc8fc\uc138\uc694", english = "Please select the destination wallet"),
    "transfer_send_from_wallet_hint" to appTextEntry(korean = "DonDone \uc9c0\uac11 \uc794\uc561\uc73c\ub85c dUSDC\ub97c \uc1a1\uae08\ud574\uc694.", english = "Send dUSDC from your DonDone wallet balance."),
    "transfer_choose_account_to_send" to appTextEntry(korean = "\uc1a1\uae08\ud560 \uacc4\uc88c\ub97c \uc120\ud0dd\ud574 \uc8fc\uc138\uc694.", english = "Choose the account to send from."),
    "transfer_choose_wallet_to_receive" to appTextEntry(korean = "\uc5b4\ub514\ub85c \ub3c8\uc744 \ubcf4\ub0bc\uae4c\uc694?", english = "Where should the money go?"),
    "transfer_choose_account_to_receive" to appTextEntry(korean = "\uc5b4\ub514\ub85c \ub3c8\uc744 \ubcf4\ub0bc\uae4c\uc694?", english = "Where should the money go?"),
    "transfer_search_wallet_address" to appTextEntry(korean = "\uc9c0\uac11 \uc8fc\uc18c \uac80\uc0c9", english = "Search wallet address"),
    "transfer_enter_account_name" to appTextEntry(korean = "\uacc4\uc88c\ubc88\ud638 \uc785\ub825", english = "Enter account number"),
    "transfer_frequent_wallet" to appTextEntry(korean = "\uc790\uc8fc \ubcf4\ub0b4\ub294 \uc9c0\uac11", english = "Frequent Wallets"),
    "transfer_recent_wallet" to appTextEntry(korean = "\ucd5c\uadfc \ubcf4\ub0b8 \uc9c0\uac11", english = "Recent Wallets"),
    "transfer_loading_wallet_info" to appTextEntry(korean = "\uc1a1\uae08 \uc9c0\uac11 \uc815\ubcf4\ub97c \ubd88\ub7ec\uc624\ub294 \uc911", english = "Loading Transfer Wallet Info"),
    "transfer_loading_wallet_info_desc" to appTextEntry(korean = "\uc5f0\uacb0\ub41c \uc9c0\uac11\uacfc \uc794\uc561 \uc815\ubcf4\ub97c \ud655\uc778\ud558\uace0 \uc788\uc5b4\uc694.", english = "Checking the connected wallet and balance."),
    "transfer_failed_to_load_info" to appTextEntry(korean = "\uc1a1\uae08 \uc815\ubcf4\ub97c \ubd88\ub7ec\uc624\uc9c0 \ubabb\ud588\uc5b4\uc694", english = "Failed to Load Transfer Info"),
    "transfer_try_again" to appTextEntry(korean = "\ub2e4\uc2dc \uc2dc\ub3c4", english = "Try Again"),
    "transfer_wallet_preparing" to appTextEntry(korean = "\uc9c0\uac11 \uc900\ube44\uac00 \uc9c4\ud589 \uc911\uc774\uc5d0\uc694", english = "Wallet Setup in Progress"),
    "transfer_wallet_ready_after_funding" to appTextEntry(korean = "\ucd08\uae30 \uc790\uae08 \ucda9\uc804\uc774 \ub05d\ub098\uba74 \ubc14\ub85c \uc1a1\uae08\ud560 \uc218 \uc788\uc5b4\uc694.", english = "You can transfer as soon as the initial funding is finished."),
    "transfer_check_status" to appTextEntry(korean = "\uc0c1\ud0dc \ud655\uc778", english = "Check Status"),
    "transfer_wallet_preparation_failed" to appTextEntry(korean = "\uc9c0\uac11 \uc900\ube44\uc5d0 \uc2e4\ud328\ud588\uc5b4\uc694", english = "Wallet Setup Failed"),
    "transfer_recently_updated_recipient" to appTextEntry(korean = "\ucd5c\uadfc \uc218\uc815\ub41c \uc218\uc2e0\uc790\uc608\uc694", english = "This Recipient Was Updated Recently"),
    "transfer_check_wallet_address_again" to appTextEntry(korean = "\uc9c0\uac11 \uc8fc\uc18c\ub97c \ud55c \ubc88 \ub354 \ud655\uc778\ud55c \ub4a4 \uc1a1\uae08\ud574 \uc8fc\uc138\uc694.", english = "Please check the wallet address one more time before sending."),
    "transfer_high_amount_confirmation" to appTextEntry(korean = "\uace0\uc561 \uc1a1\uae08 \ud655\uc778\uc774 \ud544\uc694\ud574\uc694", english = "High-Amount Transfer Confirmation Required"),
    "transfer_check_amount_recipient_again" to appTextEntry(korean = "\uc1a1\uae08 \uae08\uc561\uacfc \uc218\uc2e0\uc790 \uc815\ubcf4\ub97c \ub2e4\uc2dc \ud655\uc778\ud574 \uc8fc\uc138\uc694.", english = "Please check the transfer amount and recipient information again."),
    "transfer_request_created" to appTextEntry(korean = "\uc1a1\uae08 \uc694\uccad\uc774 \uc0dd\uc131\ub418\uc5c8\uc5b4\uc694.", english = "The transfer request was created."),
    "transfer_preparing_signature" to appTextEntry(korean = "\uc1a1\uae08 \uc11c\uba85\uc744 \uc900\ube44\ud558\uace0 \uc788\uc5b4\uc694.", english = "Preparing the transfer signature."),
    "transfer_waiting_blockchain_result" to appTextEntry(korean = "\ube14\ub85d\uccb4\uc778 \uc804\uc1a1 \uacb0\uacfc\ub97c \uae30\ub2e4\ub9ac\uace0 \uc788\uc5b4\uc694.", english = "Waiting for the blockchain transfer result."),
    "transfer_confirmed_status_detail" to appTextEntry(korean = "\uc1a1\uae08\uc774 \uc815\uc0c1\uc801\uc73c\ub85c \uc644\ub8cc\ub418\uc5c8\uc5b4\uc694.", english = "The transfer completed successfully."),
    "transfer_failed_status_detail" to appTextEntry(korean = "\uc1a1\uae08\uc744 \uc644\ub8cc\ud558\uc9c0 \ubabb\ud588\uc5b4\uc694.", english = "Couldn't complete the transfer."),
    "transfer_timeout_status_detail" to appTextEntry(korean = "\ube14\ub85d\uccb4\uc778 \ud655\uc778 \uc2dc\uac04\uc774 \uc9c0\uc5f0\ub418\uace0 \uc788\uc5b4\uc694.", english = "Blockchain confirmation is taking longer than expected."),
    "transfer_sending_account" to appTextEntry(korean = "\ubcf4\ub0b4\ub294 \uacc4\uc88c", english = "Sending Account"),
    "transfer_add_recipient" to appTextEntry(korean = "\uc218\uc2e0\uc790 \ucd94\uac00", english = "Add Recipient"),
    "transfer_scan_account_number" to appTextEntry(korean = "\uacc4\uc88c\ubc88\ud638 \ucd2c\uc601", english = "Scan Account Number"),
    "transfer_register_allowed_recipient_first" to appTextEntry(korean = "\ud5c8\uc6a9 \ubaa9\ub85d \uc218\uc2e0\uc790\ub97c \uba3c\uc800 \ub4f1\ub85d\ud574 \uc8fc\uc138\uc694.", english = "Register an allowed-list recipient first."),
    "transfer_register_recipient" to appTextEntry(korean = "\uc218\uc2e0\uc790 \ub4f1\ub85d", english = "Register Recipient"),
    "transfer_no_search_results" to appTextEntry(korean = "\uac80\uc0c9 \uacb0\uacfc\uac00 \uc5c6\uc2b5\ub2c8\ub2e4", english = "No Search Results"),
    "transfer_from_my_account_format" to appTextEntry(korean = "\ub0b4 %s\uc5d0\uc11c", english = "From My %s"),
    "transfer_balance_format" to appTextEntry(korean = "\uc794\uc561 %s", english = "Balance %s"),
    "transfer_confirming" to appTextEntry(korean = "\ud655\uc778 \uc911...", english = "Confirming..."),
    "transfer_next" to appTextEntry(korean = "\ub2e4\uc74c", english = "Next"),
    "transfer_leave_memo" to appTextEntry(korean = "\uba54\ubaa8 \ub0a8\uae30\uae30", english = "Leave a Memo"),
    "transfer_withdraw_wallet" to appTextEntry(korean = "\ucd9c\uae08 \uc9c0\uac11", english = "Source Wallet"),
    "transfer_withdraw_account" to appTextEntry(korean = "\ucd9c\uae08 \uacc4\uc88c", english = "Source Account"),
    "transfer_sending_now" to appTextEntry(korean = "\ubcf4\ub0b4\ub294 \uc911...", english = "Sending..."),
    "transfer_moving_now" to appTextEntry(korean = "\uc62e\uae30\ub294 \uc911...", english = "Moving..."),
    "transfer_done" to appTextEntry(korean = "\uc644\ub8cc", english = "Done"),
    "transfer_withdraw_from_wallet" to appTextEntry(korean = "\uc544\ub798 \uc9c0\uac11\uc5d0\uc11c \ucd9c\uae08\ub3fc\uc694", english = "It will be withdrawn from the wallet below"),
    "transfer_withdraw_from_account" to appTextEntry(korean = "\uc544\ub798 \uacc4\uc88c\uc5d0\uc11c \ucd9c\uae08\ub3fc\uc694", english = "It will be withdrawn from the account below"),
    "transfer_my_account_name_format" to appTextEntry(korean = "\ub0b4 %s", english = "My %s"),
    "transfer_confirm" to appTextEntry(korean = "\ud655\uc778", english = "Confirm"),
    "transfer_send_to_wallet" to appTextEntry(korean = "\uc544\ub798 \uc9c0\uac11\uc73c\ub85c \ubcf4\ub0b4\uc694", english = "Send to the wallet below"),
    "transfer_wallet_address_copied" to appTextEntry(korean = "\uc9c0\uac11 \uc8fc\uc18c\ub97c \ubcf5\uc0ac\ud588\uc5b4\uc694.", english = "Wallet address copied."),
    "transfer_copy_address" to appTextEntry(korean = "\uc8fc\uc18c \ubcf5\uc0ac", english = "Copy Address"),
    "transfer_enter_wallet_address" to appTextEntry(korean = "\uc9c0\uac11\uc8fc\uc18c \uc785\ub825", english = "Enter Wallet Address"),
    "transfer_search_again_account" to appTextEntry(korean = "\ub2e4\ub978 \uc774\ub984\uc774\ub098 \uacc4\uc88c\ubc88\ud638 \ud615\uc2dd\uc73c\ub85c \ub2e4\uc2dc \ucc3e\uc544\ubcf4\uc138\uc694.", english = "Search again with another name or account number."),
    "transfer_search_again_wallet" to appTextEntry(korean = "\ub2e4\ub978 \uc774\ub984\uc774\ub098 \uc9c0\uac11 \uc8fc\uc18c \ud615\uc2dd\uc73c\ub85c \ub2e4\uc2dc \ucc3e\uc544\ubcf4\uc138\uc694.", english = "Search again with another name or wallet address."),
    "transfer_frequent_account" to appTextEntry(korean = "\uc790\uc8fc \ubcf4\ub0b4\ub294 \uacc4\uc88c", english = "Frequent Accounts"),
    "transfer_recent_account" to appTextEntry(korean = "\ucd5c\uadfc \ubcf4\ub0b8 \uacc4\uc88c", english = "Recent Accounts"),
    "transfer_to_my_account_format" to appTextEntry(korean = "\ub0b4 %s \uacc4\uc88c\ub85c", english = "To My %s Account"),
    "transfer_to_wallet_format" to appTextEntry(korean = "%s \uc9c0\uac11\uc73c\ub85c", english = "To %s's Wallet"),
    "transfer_how_much_move" to appTextEntry(korean = "\uc5bc\ub9c8\ub098 \uc62e\uae38\uae4c\uc694?", english = "How much should you move?"),
    "transfer_how_much_send" to appTextEntry(korean = "\uc5bc\ub9c8\ub098 \ubcf4\ub0bc\uae4c\uc694?", english = "How much should you send?"),
    "transfer_balance_input_format" to appTextEntry(korean = "\uc794\uc561 \u00b7 %s \uc785\ub825", english = "Balance · Enter %s"),
    "transfer_moving_in_progress" to appTextEntry(korean = "\uc62e\uae30\ub294 \uc911\uc774\uc5d0\uc694", english = "Moving in Progress"),
    "transfer_sending_in_progress" to appTextEntry(korean = "\ubcf4\ub0b4\ub294 \uc911\uc774\uc5d0\uc694", english = "Sending in Progress"),
    "transfer_moved" to appTextEntry(korean = "\uc62e\uacbc\uc5b4\uc694", english = "Moved"),
    "transfer_sent" to appTextEntry(korean = "\ubcf4\ub0c8\uc5b4\uc694", english = "Sent"),
    "transfer_failed" to appTextEntry(korean = "\uc2e4\ud328\ud588\uc5b4\uc694", english = "Failed"),
    "transfer_checking" to appTextEntry(korean = "\ud655\uc778\ud558\uace0 \uc788\uc5b4\uc694", english = "Checking"),
    "transfer_move_question" to appTextEntry(korean = "\uc62e\uae38\uae4c\uc694?", english = "Move it?"),
    "transfer_send_question" to appTextEntry(korean = "\ubcf4\ub0bc\uae4c\uc694?", english = "Send it?"),
    "transfer_display_to_recipient" to appTextEntry(korean = "\ubc1b\ub294 \ubd84\uc5d0\uac8c \ud45c\uc2dc", english = "Shown to Recipient"),
    "transfer_deposit_account" to appTextEntry(korean = "\uc785\uae08 \uacc4\uc88c", english = "Deposit Account"),
    "transfer_move_action" to appTextEntry(korean = "\uc62e\uae30\uae30", english = "Move"),
    "transfer_send_action" to appTextEntry(korean = "\ubcf4\ub0b4\uae30", english = "Send"),
    "transfer_wallet_address_label" to appTextEntry(korean = "\uc9c0\uac11 \uc8fc\uc18c", english = "Wallet Address"),
    "korean" to appTextEntry(korean = "\ud55c\uad6d\uc5b4", english = "Korean"),
    "english" to appTextEntry(korean = "\uc601\uc5b4", english = "English"),
    "vault_yield" to appTextEntry(korean = "\uc608\uce58 \uc774\uc790", english = "Vault yield"),
    "vault_balance" to appTextEntry(korean = "\uc608\uce58 \uc794\uc561", english = "Vault balance"),
    "accrued_yield_est" to appTextEntry(korean = "\ub204\uc801 \uc774\uc790(\ucd94\uc815)", english = "Accrued yield (est.)"),
    "estimated_apy" to appTextEntry(korean = "\uc608\uc0c1 \uc5f0\uc774\uc728", english = "Estimated APY"),
    "advance" to appTextEntry(korean = "\ubbf8\ub9ac\ubc1b\uae30", english = "Advance"),
    "amount_received_this_month" to appTextEntry(korean = "\uc774\ubc88 \ub2ec \ubc1b\uc740 \uae08\uc561", english = "Amount received this month"),
    "additional_requestable_amount" to appTextEntry(korean = "\ucd94\uac00 \uc2e0\uccad \uac00\ub2a5 \uae08\uc561", english = "Additional requestable amount"),
    "settlement_date" to appTextEntry(korean = "\uc815\uc0b0\uc77c", english = "Settlement date"),
    "attendance_reflection" to appTextEntry(korean = "\uadfc\ubb34 \ubc18\uc601", english = "Attendance reflection"),
    "not_recorded" to appTextEntry(korean = "\ubbf8\uae30\ub85d", english = "Not recorded"),
    "clock_in_only" to appTextEntry(korean = "\ucd9c\uadfc\ub9cc", english = "Clock-in only"),
    "completed" to appTextEntry(korean = "\uc644\ub8cc", english = "Completed"),
    "edited" to appTextEntry(korean = "\uc218\uc815", english = "Edited"),
    "receivable_amount" to appTextEntry(korean = "\ubc1b\uc744 \uae08\uc561", english = "Receivable amount"),
    "planned_payout" to appTextEntry(korean = "\uc774\ubc88 \uc218\ub839 \uc608\uc815", english = "Planned payout"),
    "fee" to appTextEntry(korean = "\uc218\uc218\ub8cc", english = "Fee"),
    "selected_amount" to appTextEntry(korean = "\uc120\ud0dd \uae08\uc561", english = "Selected amount"),
    "request_limits" to appTextEntry(korean = "\uc2e0\uccad \uc81c\ud55c", english = "Request limits"),
    "this_month" to appTextEntry(korean = "\uc774\ubc88 \ub2ec \uc774\ub825", english = "This month"),
    "loading_request_details" to appTextEntry(korean = "\uc2e0\uccad \uc0c1\uc138\ub97c \ubd88\ub7ec\uc624\ub294 \uc911\uc774\uc5d0\uc694.", english = "Loading request details."),
    "details" to appTextEntry(korean = "\uc0c1\uc138 \uc815\ubcf4", english = "Details"),
    "payout_status" to appTextEntry(korean = "\uc9c0\uae09 \uc0c1\ud0dc", english = "Payout status"),
    "settlement_status" to appTextEntry(korean = "\uc815\uc0b0 \uc0c1\ud0dc", english = "Settlement status"),
    "transaction_check" to appTextEntry(korean = "\uac70\ub798 \ud655\uc778", english = "Transaction check"),
    "blockchain_transaction_check" to appTextEntry(korean = "\ube14\ub85d\uccb4\uc778 \uac70\ub798 \ud655\uc778", english = "Blockchain transaction check"),
    "requested_at" to appTextEntry(korean = "\uc2e0\uccad \uc2dc\uac01", english = "Requested at"),
    "calculation_basis" to appTextEntry(korean = "\uacc4\uc0b0 \uae30\uc900", english = "Calculation basis"),
    "available_amount_at_request_time" to appTextEntry(korean = "\ub2f9\uc2dc \uc2e0\uccad \uac00\ub2a5 \uae08\uc561", english = "Available amount at request time"),
    "reflected_work" to appTextEntry(korean = "\ubc18\uc601 \uadfc\ubb34", english = "Reflected work"),
    "needs_review" to appTextEntry(korean = "\ud655\uc778 \ud544\uc694 \uae30\ub85d", english = "Needs review"),
    "my_wallet_balance" to appTextEntry(korean = "\ub0b4 \uc9c0\uac11 \uc794\uc561", english = "My wallet balance"),
    "current_vault_balance" to appTextEntry(korean = "\ud604\uc7ac \uc608\uce58 \uc794\uc561", english = "Current vault balance"),
    "request_type" to appTextEntry(korean = "\uc694\uccad \uc885\ub958", english = "Request type"),
    "deposit" to appTextEntry(korean = "\uc608\uce58", english = "Deposit"),
    "withdraw" to appTextEntry(korean = "\ucd9c\uae08", english = "Withdraw"),
    "requested_amount" to appTextEntry(korean = "\uc694\uccad \uae08\uc561", english = "Requested amount"),
    "currently_selected" to appTextEntry(korean = "\uc9c0\uae08 \uc120\ud0dd\ub428", english = "Currently selected"),
    "available_to_withdraw_now" to appTextEntry(korean = "\uc9c0\uae08 \ucd9c\uae08 \uac00\ub2a5", english = "Available to withdraw now"),
    "available_to_deposit_now" to appTextEntry(korean = "\uc9c0\uae08 \uc608\uce58 \uac00\ub2a5", english = "Available to deposit now"),
    "estimated_yield" to appTextEntry(korean = "\uc608\uc0c1 \uc218\uc775", english = "Estimated yield"),
    "monthly_yield" to appTextEntry(korean = "\uc6d4 \uc608\uc0c1 \uc774\uc790", english = "Monthly yield"),
    "daily_yield" to appTextEntry(korean = "\uc77c \uc608\uc0c1 \uc774\uc790", english = "Daily yield"),
    "amount_available_to_deposit_now" to appTextEntry(korean = "\uc9c0\uae08 \uc608\uce58 \uac00\ub2a5\ud55c \uae08\uc561", english = "Amount available to deposit now"),
    "deposit_request_amount" to appTextEntry(korean = "\uc608\uce58 \uc2e0\uccad \uae08\uc561", english = "Deposit request amount"),
    "monthly_yield_mix_est" to appTextEntry(korean = "\uc6d4 \uc218\uc775 \uad6c\uc131(\ucd94\uc815)", english = "Monthly yield mix (est.)"),
    "defi_yield" to appTextEntry(korean = "DeFi \uc6b4\uc6a9 \uc218\uc775", english = "DeFi yield"),
    "advance_fee_contribution" to appTextEntry(korean = "\uac00\ubd88 \uc218\uc218\ub8cc \uae30\uc5ec\ubd84", english = "Advance fee contribution"),
    "total" to appTextEntry(korean = "\ud569\uacc4", english = "Total"),
    "pool_utilization" to appTextEntry(korean = "\ud480 \uc6b4\uc6a9 \ud604\ud669", english = "Pool utilization"),
    "defi_allocation" to appTextEntry(korean = "DeFi \uc6b4\uc6a9 \ube44\uc728", english = "DeFi allocation"),
    "advance_pool_ratio" to appTextEntry(korean = "\uac00\ubd88 \ud480 \ube44\uc728", english = "Advance pool ratio"),
    "advance_pool_usage" to appTextEntry(korean = "\uac00\ubd88 \ud480 \uc0ac\uc6a9\ub960", english = "Advance pool usage"),
    "limit_tier_guide" to appTextEntry(korean = "\ud55c\ub3c4 \uad6c\uac04 \uc548\ub0b4", english = "Limit tier guide"),
    "current_limit" to appTextEntry(korean = "\ud604\uc7ac \ub0b4 \ud55c\ub3c4", english = "Current limit"),
    "your_limit_can_increase_within_the_same_month_as_more_attendance_is_reflected" to appTextEntry(
        korean = "\ucd9c\uadfc \uae30\ub85d\uc774 \ub354 \ubc18\uc601\ub418\uba74 \uac19\uc740 \ub2ec\uc5d0\ub3c4 \ud55c\ub3c4\uac00 \ub298\uc5b4\ub0a0 \uc218 \uc788\uc5b4\uc694.",
        english = "Your limit can increase within the same month as more attendance is reflected."
    ),
    "maximum_limit_by_tier" to appTextEntry(korean = "\uad6c\uac04\ubcc4 \ucd5c\ub300 \ud55c\ub3c4", english = "Maximum limit by tier"),
    "text_0_4_reflected_work_days" to appTextEntry(korean = "\ubc18\uc601 \uadfc\ubb34 0~4\uc77c", english = "0-4 reflected work days"),
    "not_available_yet" to appTextEntry(korean = "\uc544\uc9c1 \uc2e0\uccad\ud560 \uc218 \uc5c6\uc5b4\uc694", english = "Not available yet"),
    "text_5_reflected_work_days" to appTextEntry(korean = "\ubc18\uc601 \uadfc\ubb34 5\uc77c \uc774\uc0c1", english = "5+ reflected work days"),
    "up_to_10_of_earned_wages" to appTextEntry(korean = "\uc77c\ud55c \uae08\uc561\uc758 10%\uae4c\uc9c0", english = "Up to 10% of earned wages"),
    "max_50_000" to appTextEntry(korean = "\ucd5c\ub300 5\ub9cc\uc6d0", english = "Max \u20a950,000"),
    "text_10_reflected_work_days" to appTextEntry(korean = "\ubc18\uc601 \uadfc\ubb34 10\uc77c \uc774\uc0c1", english = "10+ reflected work days"),
    "up_to_20_of_earned_wages" to appTextEntry(korean = "\uc77c\ud55c \uae08\uc561\uc758 20%\uae4c\uc9c0", english = "Up to 20% of earned wages"),
    "max_150_000" to appTextEntry(korean = "\ucd5c\ub300 15\ub9cc\uc6d0", english = "Max \u20a9150,000"),
    "text_20_reflected_work_days" to appTextEntry(korean = "\ubc18\uc601 \uadfc\ubb34 20\uc77c \uc774\uc0c1", english = "20+ reflected work days"),
    "up_to_30_of_earned_wages" to appTextEntry(korean = "\uc77c\ud55c \uae08\uc561\uc758 30%\uae4c\uc9c0", english = "Up to 30% of earned wages"),
    "max_300_000" to appTextEntry(korean = "\ucd5c\ub300 30\ub9cc\uc6d0", english = "Max \u20a9300,000"),
    "remaining_additional_requestable_amount" to appTextEntry(korean = "\ub0a8\uc740 \ucd94\uac00 \uc2e0\uccad \uac00\ub2a5 \uae08\uc561", english = "Remaining additional requestable amount"),
    "loading_transaction_history" to appTextEntry(korean = "\uac70\ub798\ub0b4\uc5ed\uc744 \ubd88\ub7ec\uc624\ub294 \uc911\uc785\ub2c8\ub2e4.", english = "Loading transaction history."),
    "failed_to_load_transaction_history" to appTextEntry(korean = "\uac70\ub798\ub0b4\uc5ed\uc744 \ubd88\ub7ec\uc624\uc9c0 \ubabb\ud588\uc2b5\ub2c8\ub2e4.", english = "Failed to load transaction history."),
    "search" to appTextEntry(korean = "\uac80\uc0c9", english = "Search"),
    "no_search_results_found" to appTextEntry(korean = "\uac80\uc0c9 \uacb0\uacfc\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.", english = "No search results found."),
    "transaction_not_found" to appTextEntry(korean = "\uac70\ub798 \uc815\ubcf4\ub97c \ucc3e\uc9c0 \ubabb\ud588\uc2b5\ub2c8\ub2e4.", english = "Transaction not found."),
    "edit" to appTextEntry(korean = "\uc218\uc815\ud558\uae30", english = "Edit"),
    "category" to appTextEntry(korean = "\uce74\ud14c\uace0\ub9ac", english = "Category"),
    "memo" to appTextEntry(korean = "\uba54\ubaa8", english = "Memo"),
    "no_memo" to appTextEntry(korean = "\uba54\ubaa8 \uc5c6\uc74c", english = "No memo"),
    "direction" to appTextEntry(korean = "\uac70\ub798 \uc720\ud615", english = "Direction"),
    "method" to appTextEntry(korean = "\uac70\ub798 \uc218\ub2e8", english = "Method"),
    "date_time" to appTextEntry(korean = "\uac70\ub798 \uc77c\uc2dc", english = "Date & time"),
    "wallet" to appTextEntry(korean = "\uc9c0\uac11", english = "Wallet"),
    "wallet_transfer" to appTextEntry(korean = "지갑 송금", english = "Wallet Transfer"),
    "advance_payout" to appTextEntry(korean = "미리받기 지급", english = "Advance Payout"),
    "unknown_income" to appTextEntry(korean = "알 수 없는 입금", english = "Unknown Income"),
    "unknown_expense" to appTextEntry(korean = "알 수 없는 출금", english = "Unknown Expense"),
    "enter_a_memo" to appTextEntry(korean = "\uba54\ubaa8\ub97c \uc785\ub825\ud558\uc138\uc694", english = "Enter a memo"),
    "save_2" to appTextEntry(korean = "\uc800\uc7a5", english = "Save"),
    "prev" to appTextEntry(korean = "\uc804\uc6d4", english = "Prev"),
    "next" to appTextEntry(korean = "\ub2e4\uc74c\uc6d4", english = "Next"),
    "all" to appTextEntry(korean = "\uc804\uccb4", english = "All"),
    "income" to appTextEntry(korean = "\uc785\uae08", english = "Income"),
    "expense" to appTextEntry(korean = "\ucd9c\uae08", english = "Expense"),
    "mon" to appTextEntry(korean = "\uc6d4", english = "Mon"),
    "tue" to appTextEntry(korean = "\ud654", english = "Tue"),
    "wed" to appTextEntry(korean = "\uc218", english = "Wed"),
    "thu" to appTextEntry(korean = "\ubaa9", english = "Thu"),
    "fri" to appTextEntry(korean = "\uae08", english = "Fri"),
    "sat" to appTextEntry(korean = "\ud1a0", english = "Sat"),
    "sun" to appTextEntry(korean = "\uc77c", english = "Sun")
)

private val legacyTextKeys: Map<String, AppTextKey> =
    appTextCatalog.entries.associate { (key, entry) -> entry.resolve(AppLanguage.KOREAN) to key }

fun AppLanguage.text(key: AppTextKey): String =
    appTextCatalog[key]?.resolve(this) ?: key

fun AppLanguage.text(key: AppTextKey, vararg args: Any): String =
    text(key).format(locale, *args)

fun AppLanguage.translate(text: String): String {
    if (this == AppLanguage.KOREAN) {
        return text
    }

    return when {
        text.startsWith(UPDATED_PREFIX) -> "Updated ${text.removePrefix(UPDATED_PREFIX)}"
        else -> legacyTextKeys[text]?.let { key -> this.text(key) } ?: text
    }
}
