package com.dondone.mobile.domain.model

import java.time.LocalDate

data class DemoInfo(
    val year: Int,
    val month: Int,
    val monthLength: Int,
    val asOfDay: Int
)

data class TodayWork(
    val clockIn: String? = null,
    val clockOut: String? = null
)

data class WorkRecord(
    val id: String,
    val workDate: LocalDate,
    val day: Int,
    val inTime: String,
    val outTime: String,
    val modified: Boolean,
    val attachments: Int,
    val reflectionStatus: String = "PENDING",
    val recognizedInTime: String? = null,
    val recognizedOutTime: String? = null
)

data class WorkAudit(
    val id: String,
    val before: String,
    val after: String,
    val reason: String,
    val attachments: Int,
    val at: String
)

data class WorkproofData(
    val workplaceName: String,
    val workplaceAddress: String,
    val workplaceLatitude: Double,
    val workplaceLongitude: Double,
    val currentLatitude: Double,
    val currentLongitude: Double,
    val today: TodayWork,
    val records: List<WorkRecord>,
    val audit: List<WorkAudit>,
    val workplaceId: Long? = null,
    val allowedRadiusMeters: Int = 100
)

data class WageData(
    val workDays: Int,
    val totalHours: Int,
    val overtimeHours: Int,
    val nightHours: Int,
    val hourly: Int,
    val deductionsKnown: Boolean,
    val paydayDay: Int,
    val actualDepositRecordedDay: Int?,
    val actualDeposit: Int
)

data class Account(
    val id: String,
    val name: String,
    val number: String,
    val balance: Int
)

data class Recipient(
    val id: String,
    val name: String,
    val relationship: String,
    val address: String
)

enum class TransferStatus {
    IDLE, REVIEWING, SUBMITTED, CONFIRMED, FAILED
}

enum class TransferFlowStep {
    ACCOUNT, RECIPIENT, AMOUNT
}

enum class TransferDestinationMode {
    ACCOUNT, WALLET
}

data class RemittanceData(
    val accounts: List<Account>,
    val selectedAccountId: String,
    val recipients: List<Recipient>,
    val transactions: List<TransactionRecord> = emptyList(),
    val selectedRecipientId: String,
    val recipientDisplayNameOverride: String? = null,
    val draftAmountUsd: Int,
    val txHash: String,
    val status: TransferStatus,
    val flowStep: TransferFlowStep,
    val destinationMode: TransferDestinationMode = TransferDestinationMode.ACCOUNT,
    val stepReturnTarget: TransferFlowStep? = null
) {
    fun selectedAccountOrNull(): Account? = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()

    fun selectedAccount(): Account = requireNotNull(selectedAccountOrNull()) { "No account available" }

    fun selectedRecipientOrNull(): Recipient? = recipients.firstOrNull { it.id == selectedRecipientId } ?: recipients.firstOrNull()

    fun selectedRecipient(): Recipient = requireNotNull(selectedRecipientOrNull()) { "No recipient available" }

    fun displayedRecipientName(): String = recipientDisplayNameOverride?.takeIf { it.isNotBlank() } ?: selectedRecipient().name

    fun setSelectedAccountBalance(balance: Int): RemittanceData {
        return copy(
            accounts = accounts.map {
                if (it.id == selectedAccountId) it.copy(balance = balance) else it
            }
        )
    }

    fun changeSelectedAccountBalanceBy(delta: Int): RemittanceData {
        return copy(
            accounts = accounts.map {
                if (it.id == selectedAccountId) {
                    it.copy(balance = (it.balance + delta).coerceAtLeast(0))
                } else {
                    it
                }
            }
        )
    }
}

data class AdvanceData(
    val maxCap: Int,
    val limitRate: Double,
    val used: Int,
    val flatFee: Int,
    val selectedRequest: Int,
    val previousRepaymentGood: Boolean,
    val bonusLimit: Int
)

data class VaultData(
    val enabled: Boolean,
    val userDeposit: Int,
    val apr: Double,
    val accruedInterest: Int,
    val totalPool: Int,
    val advanceRatio: Double,
    val advanceUtilization: Double,
    val monthlyFeeRevenue: Int
)

data class DocumentItem(
    val id: String,
    val title: String,
    val status: String,
    val updatedAt: String?
)

data class DemoState(
    val demo: DemoInfo,
    val workproof: WorkproofData,
    val wage: WageData,
    val remittance: RemittanceData,
    val advance: AdvanceData,
    val vault: VaultData,
    val documents: List<DocumentItem>
)
