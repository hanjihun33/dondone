package com.dondone.mobile.feature.finance.presentation

import com.dondone.mobile.app.session.TransactionMetadataOverride
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.numberFormat
import com.dondone.mobile.core.i18n.text
import com.dondone.mobile.data.remittance.RemittanceLedgerItemPayload
import com.dondone.mobile.data.remittance.RemittanceRemoteMode
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.data.remittance.RemittanceTransferSummaryPayload
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TransactionCategory
import com.dondone.mobile.domain.model.TransactionDirection
import com.dondone.mobile.domain.model.TransactionRecord
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TransactionHistoryScreenState {
    LOADING,
    ERROR,
    EMPTY,
    CONTENT
}

enum class TransactionLedgerFilter(
    val labelKey: String
) {
    ALL("all"),
    INCOME("income"),
    EXPENSE("expense")
}

data class TransactionMonthOptionUiModel(
    val value: YearMonth,
    val label: String
)

data class TransactionHistoryItemUiModel(
    val id: String,
    val walletId: String,
    val occurredAt: LocalDateTime,
    val day: LocalDate,
    val counterpartyName: String,
    val counterpartyAddress: String?,
    val amountValue: Int,
    val amountText: String,
    val feeText: String,
    val direction: TransactionDirection,
    val category: TransactionCategory,
    val categoryLabel: String,
    val memo: String,
    val methodLabel: String,
    val timeText: String,
    val currencyUnit: String
)

data class TransactionHistoryMainUiModel(
    val walletId: String,
    val walletName: String,
    val walletSubtitle: String?,
    val screenState: TransactionHistoryScreenState,
    val monthOptions: List<TransactionMonthOptionUiModel>,
    val initialMonth: YearMonth,
    val initialAnchorDay: LocalDate,
    val items: List<TransactionHistoryItemUiModel>,
    val emptyMessage: String,
    val errorMessage: String?
)

data class TransactionHistoryDetailUiModel(
    val walletId: String,
    val walletName: String,
    val transactionId: String,
    val counterpartyName: String,
    val counterpartyAddress: String?,
    val amountText: String,
    val amountValue: Int,
    val feeText: String,
    val direction: TransactionDirection,
    val directionLabel: String,
    val category: TransactionCategory,
    val categoryLabel: String,
    val memo: String,
    val dateText: String,
    val methodLabel: String
)

data class TransactionHistoryEditUiModel(
    val walletId: String,
    val walletName: String,
    val transactionId: String,
    val counterpartyName: String,
    val amountText: String,
    val selectedCategory: TransactionCategory,
    val memo: String,
    val categories: List<TransactionCategory>
)

fun DemoState.toTransactionHistoryMainUiModel(
    accountId: String,
    remittanceRemoteState: RemittanceRemoteState,
    language: AppLanguage = AppLanguage.KOREAN,
    isAuthenticated: Boolean,
    overrides: Map<String, TransactionMetadataOverride>
): TransactionHistoryMainUiModel {
    if (isAuthenticated && accountId == "remote-wallet") {
        return remittanceRemoteState.toRemoteTransactionHistoryMainUiModel(overrides, language)
    }

    val account = remittance.accounts.firstOrNull { it.id == accountId } ?: remittance.selectedAccount()
    val transactions = remittance.transactions
        .filter { it.walletId == account.id }
        .map { record ->
            val override = overrides[record.id]
            record.toUiModel(
                category = override?.category ?: record.category,
                memo = override?.memo ?: record.memo,
                currencyUnit = "KRW",
                language = language
            )
        }
        .sortedByDescending { it.occurredAt }

    val initialMonth = YearMonth.of(demo.year, demo.month)
    return TransactionHistoryMainUiModel(
        walletId = account.id,
        walletName = account.name,
        walletSubtitle = account.number,
        screenState = if (transactions.isEmpty()) TransactionHistoryScreenState.EMPTY else TransactionHistoryScreenState.CONTENT,
        monthOptions = buildMonthOptions(initialMonth, language),
        initialMonth = initialMonth,
        initialAnchorDay = LocalDate.of(demo.year, demo.month, demo.asOfDay),
        items = transactions,
        emptyMessage = language.text("finance_no_history_this_month"),
        errorMessage = null
    )
}

fun DemoState.toTransactionHistoryDetailUiModel(
    accountId: String,
    transactionId: String,
    remittanceRemoteState: RemittanceRemoteState,
    language: AppLanguage = AppLanguage.KOREAN,
    isAuthenticated: Boolean,
    overrides: Map<String, TransactionMetadataOverride>
): TransactionHistoryDetailUiModel? {
    val source = toTransactionHistoryMainUiModel(accountId, remittanceRemoteState, language, isAuthenticated, overrides)
    val item = source.items.firstOrNull { it.id == transactionId } ?: return null
    return TransactionHistoryDetailUiModel(
        walletId = source.walletId,
        walletName = if (source.walletId == "remote-wallet") {
            item.counterpartyAddress ?: source.walletName
        } else {
            source.walletName
        },
        transactionId = item.id,
        counterpartyName = item.counterpartyName,
        counterpartyAddress = item.counterpartyAddress,
        amountText = item.amountText,
        amountValue = item.amountValue,
        feeText = item.feeText,
        direction = item.direction,
        directionLabel = directionLabel(item.direction, language),
        category = item.category,
        categoryLabel = item.categoryLabel,
        memo = item.memo,
        dateText = item.occurredAt.format(transactionDetailDateFormatter(language)),
        methodLabel = item.methodLabel
    )
}

fun DemoState.toTransactionHistoryEditUiModel(
    accountId: String,
    transactionId: String,
    remittanceRemoteState: RemittanceRemoteState,
    language: AppLanguage = AppLanguage.KOREAN,
    isAuthenticated: Boolean,
    overrides: Map<String, TransactionMetadataOverride>
): TransactionHistoryEditUiModel? {
    val detail = toTransactionHistoryDetailUiModel(
        accountId = accountId,
        transactionId = transactionId,
        remittanceRemoteState = remittanceRemoteState,
        language = language,
        isAuthenticated = isAuthenticated,
        overrides = overrides
    ) ?: return null
    return TransactionHistoryEditUiModel(
        walletId = detail.walletId,
        walletName = detail.walletName,
        transactionId = detail.transactionId,
        counterpartyName = detail.counterpartyName,
        amountText = detail.amountText,
        selectedCategory = detail.category,
        memo = detail.memo,
        categories = TransactionCategory.values().toList()
    )
}

private fun RemittanceRemoteState.toRemoteTransactionHistoryMainUiModel(
    overrides: Map<String, TransactionMetadataOverride>,
    language: AppLanguage
): TransactionHistoryMainUiModel {
    val payload = payload
    val currentMonth = YearMonth.now()
    if (mode == RemittanceRemoteMode.LOADING || (payload == null && mode == RemittanceRemoteMode.UNAUTHENTICATED)) {
        return TransactionHistoryMainUiModel(
            walletId = "remote-wallet",
            walletName = "DonDone Wallet",
            walletSubtitle = null,
            screenState = TransactionHistoryScreenState.LOADING,
            monthOptions = buildMonthOptions(currentMonth, language),
            initialMonth = currentMonth,
            initialAnchorDay = currentMonth.atDay(LocalDate.now().dayOfMonth.coerceAtMost(currentMonth.lengthOfMonth())),
            items = emptyList(),
            emptyMessage = language.text("loading_transaction_history"),
            errorMessage = errorMessage
        )
    }
    if (mode == RemittanceRemoteMode.ERROR || payload == null) {
        return TransactionHistoryMainUiModel(
            walletId = "remote-wallet",
            walletName = "DonDone Wallet",
            walletSubtitle = payload?.wallet?.walletAddress?.toShortWalletAddress(),
            screenState = TransactionHistoryScreenState.ERROR,
            monthOptions = buildMonthOptions(currentMonth, language),
            initialMonth = currentMonth,
            initialAnchorDay = currentMonth.atDay(LocalDate.now().dayOfMonth.coerceAtMost(currentMonth.lengthOfMonth())),
            items = emptyList(),
            emptyMessage = language.text("finance_no_history_this_month"),
            errorMessage = errorMessage ?: language.text("failed_to_load_transaction_history")
        )
    }

    val currencyUnit = payload.ledgerItems.firstOrNull()?.assetSymbol ?: payload.balance?.assetSymbol ?: "USDC"
    val currencyDecimals = payload.balance?.assetDecimals ?: assetDecimalsFor(currencyUnit)
    val items = payload.ledgerItems.map { entry ->
        val timestamp = entry.occurredAt ?: LocalDateTime.now()
        val override = overrides[entry.entryId]
        val transfer = payload.transfers.firstOrNull { it.transferId == entry.entryId }
        val direction = entry.direction.toTransactionDirection()
        val amount = atomicToDisplayAmount(entry.amountAtomic, currencyDecimals)
        val category = override?.category ?: inferRemoteCategory(entry, transfer)
        val memo = override?.memo ?: entry.memo.orEmpty()
        TransactionHistoryItemUiModel(
            id = entry.entryId,
            walletId = "remote-wallet",
            occurredAt = timestamp,
            day = timestamp.toLocalDate(),
            counterpartyName = counterpartyName(entry, transfer, direction, language),
            counterpartyAddress = counterpartyAddress(entry, transfer, direction),
            amountValue = amount,
            amountText = formatAtomicMoney(entry.amountAtomic, direction, entry.assetSymbol, currencyDecimals, language),
            feeText = formatRemoteFee(transfer?.networkFeeWei, transfer?.networkFeeAssetSymbol, language),
            direction = direction,
            category = category,
            categoryLabel = categoryLabel(category, language),
            memo = memo,
            methodLabel = methodLabel(entry, language),
            timeText = timestamp.format(TransactionTimeFormatter),
            currencyUnit = entry.assetSymbol
        )
    }.sortedByDescending { it.occurredAt }

    return TransactionHistoryMainUiModel(
        walletId = "remote-wallet",
        walletName = "DonDone Wallet",
        walletSubtitle = payload.wallet.walletAddress.toShortWalletAddress(),
        screenState = if (items.isEmpty()) TransactionHistoryScreenState.EMPTY else TransactionHistoryScreenState.CONTENT,
        monthOptions = buildMonthOptions(currentMonth, language),
        initialMonth = currentMonth,
        initialAnchorDay = currentMonth.atDay(LocalDate.now().dayOfMonth.coerceAtMost(currentMonth.lengthOfMonth())),
        items = items,
        emptyMessage = language.text("finance_no_history_this_month"),
        errorMessage = errorMessage
    )
}

private fun counterpartyName(
    entry: RemittanceLedgerItemPayload,
    transfer: RemittanceTransferSummaryPayload?,
    direction: TransactionDirection,
    language: AppLanguage
): String {
    return when (entry.entryType.uppercase(Locale.ROOT)) {
        "ADVANCE_PAYOUT" -> language.text("advance_payout")
        else -> when (direction) {
            TransactionDirection.INCOME -> {
                transfer?.senderName
                    ?: entry.counterpartyLabel
                    ?: transfer?.senderAddress?.toShortWalletAddress()
                    ?: language.text("unknown_income")
            }

            TransactionDirection.EXPENSE -> {
                transfer?.recipientAlias
                    ?: entry.counterpartyLabel
                    ?: transfer?.recipientAddress?.toShortWalletAddress()
                    ?: language.text("unknown_expense")
            }
        }
    }
}

private fun counterpartyAddress(
    entry: RemittanceLedgerItemPayload,
    transfer: RemittanceTransferSummaryPayload?,
    direction: TransactionDirection
): String? {
    return when (entry.entryType.uppercase(Locale.ROOT)) {
        "ADVANCE_PAYOUT" -> null
        else -> when (direction) {
            TransactionDirection.INCOME -> transfer?.senderAddress
            TransactionDirection.EXPENSE -> transfer?.recipientAddress
        }
    }
}

private fun methodLabel(
    entry: RemittanceLedgerItemPayload,
    language: AppLanguage
): String {
    return when (entry.entryType.uppercase(Locale.ROOT)) {
        "ADVANCE_PAYOUT" -> language.text("finance_advance_title")
        else -> language.text("wallet_transfer")
    }
}

private fun String.toTransactionDirection(): TransactionDirection =
    if (equals("INCOME", ignoreCase = true) || equals("INBOUND", ignoreCase = true)) {
        TransactionDirection.INCOME
    } else {
        TransactionDirection.EXPENSE
    }

private fun TransactionRecord.toUiModel(
    category: TransactionCategory,
    memo: String,
    currencyUnit: String,
    language: AppLanguage
): TransactionHistoryItemUiModel {
    return TransactionHistoryItemUiModel(
        id = id,
        walletId = walletId,
        occurredAt = occurredAt,
        day = occurredAt.toLocalDate(),
        counterpartyName = counterpartyName,
        counterpartyAddress = counterpartyAddress,
        amountValue = amount,
        amountText = formatMoney(amount, direction, currencyUnit, language),
        feeText = formatFee(feeAmount, currencyUnit, language),
        direction = direction,
        category = category,
        categoryLabel = categoryLabel(category, language),
        memo = memo,
        methodLabel = language.text("wallet_transfer"),
        timeText = occurredAt.format(TransactionTimeFormatter),
        currencyUnit = currencyUnit
    )
}

private fun inferRemoteCategory(
    entry: RemittanceLedgerItemPayload,
    transfer: RemittanceTransferSummaryPayload?
): TransactionCategory {
    if (entry.entryType.equals("ADVANCE_PAYOUT", ignoreCase = true)) {
        return TransactionCategory.SALARY
    }

    val normalized = (transfer?.recipientAlias ?: entry.counterpartyLabel).orEmpty().lowercase(Locale.ROOT)
    return when {
        normalized.contains("payroll") -> TransactionCategory.SALARY
        normalized.contains("cafe") -> TransactionCategory.CAFE
        normalized.contains("mart") -> TransactionCategory.LIVING
        else -> TransactionCategory.TRANSFER
    }
}

private fun buildMonthOptions(
    center: YearMonth,
    language: AppLanguage
): List<TransactionMonthOptionUiModel> {
    return (-12..12)
        .map { offset -> center.plusMonths(offset.toLong()) }
        .sortedDescending()
        .map { month -> TransactionMonthOptionUiModel(month, month.format(transactionMonthFormatter(language))) }
}

private fun categoryLabel(
    category: TransactionCategory,
    language: AppLanguage
): String = when (category) {
    TransactionCategory.SALARY -> if (language == AppLanguage.ENGLISH) "Salary" else "급여"
    TransactionCategory.FOOD -> if (language == AppLanguage.ENGLISH) "Food" else "식비"
    TransactionCategory.CAFE -> if (language == AppLanguage.ENGLISH) "Cafe/Snack" else "카페/간식"
    TransactionCategory.SHOPPING -> if (language == AppLanguage.ENGLISH) "Shopping" else "쇼핑"
    TransactionCategory.TRANSPORT -> if (language == AppLanguage.ENGLISH) "Transport" else "교통"
    TransactionCategory.LIVING -> if (language == AppLanguage.ENGLISH) "Living" else "생활"
    TransactionCategory.TRANSFER -> if (language == AppLanguage.ENGLISH) "Transfer" else "이체"
    TransactionCategory.ETC -> if (language == AppLanguage.ENGLISH) "Other" else "기타"
}

private fun directionLabel(
    direction: TransactionDirection,
    language: AppLanguage
): String = when (direction) {
    TransactionDirection.INCOME -> language.text("income")
    TransactionDirection.EXPENSE -> language.text("expense")
}

private fun atomicToDisplayAmount(amountAtomic: Long, decimals: Int): Int =
    scaleAtomicAmount(amountAtomic, decimals).toInt()

private fun formatAtomicMoney(
    amountAtomic: Long,
    direction: TransactionDirection,
    unit: String,
    decimals: Int,
    language: AppLanguage
): String {
    val sign = if (direction == TransactionDirection.INCOME) "+" else "-"
    return "$sign${formatScaledAmount(amountAtomic, unit, decimals, minimumFractionDigits = 0, language = language)}"
}

private fun formatMoney(
    value: Int,
    direction: TransactionDirection,
    unit: String,
    language: AppLanguage
): String {
    val sign = if (direction == TransactionDirection.INCOME) "+" else "-"
    val number = language.numberFormat().format(value)
    return if (unit == "KRW") {
        "$sign₩$number"
    } else {
        "$sign$number $unit"
    }
}

private fun formatFee(
    value: Int,
    unit: String,
    language: AppLanguage
): String {
    val number = language.numberFormat().format(value)
    return if (unit == "KRW") {
        "₩$number"
    } else {
        "$number $unit"
    }
}

private fun unavailableFeeText(): String = "-"

private fun formatRemoteFee(
    networkFeeWei: String?,
    feeAssetSymbol: String?,
    language: AppLanguage
): String {
    val parsed = networkFeeWei?.toBigIntegerOrNull() ?: return unavailableFeeText()
    if (parsed.signum() == 0) return unavailableFeeText()
    val symbol = feeAssetSymbol ?: "ETH"
    val ethValue = BigDecimal(parsed).movePointLeft(18)
    if (ethValue.compareTo(BigDecimal("0.000001")) < 0) {
        return "< 0.000001 $symbol"
    }
    val formatter = language.numberFormat().apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 6
        roundingMode = RoundingMode.DOWN
    }
    return "${formatter.format(ethValue)} $symbol"
}

private fun String.toShortWalletAddress(): String {
    return if (length <= 14) this else "${take(8)}...${takeLast(6)}"
}

private fun assetDecimalsFor(assetSymbol: String): Int = when (assetSymbol.uppercase(Locale.ROOT)) {
    "KRW" -> 0
    "BTC" -> 8
    "ETH", "SEPOLIA_ETH" -> 18
    else -> 6
}

private fun scaleAtomicAmount(amountAtomic: Long, decimals: Int): BigDecimal =
    BigDecimal.valueOf(amountAtomic).movePointLeft(decimals)

private fun formatScaledAmount(
    amountAtomic: Long,
    unit: String,
    decimals: Int,
    minimumFractionDigits: Int,
    language: AppLanguage
): String {
    val scaled = scaleAtomicAmount(amountAtomic, decimals)
    val formatter = language.numberFormat().apply {
        this.minimumFractionDigits = minimumFractionDigits
        maximumFractionDigits = when {
            unit == "KRW" -> 0
            scaled.compareTo(BigDecimal.ONE) >= 0 -> minOf(decimals, 2)
            else -> minOf(decimals, 6)
        }
        roundingMode = RoundingMode.DOWN
    }
    val number = formatter.format(scaled)
    return if (unit == "KRW") {
        "₩$number"
    } else {
        "$number $unit"
    }
}

private fun transactionMonthFormatter(language: AppLanguage): DateTimeFormatter =
    when (language) {
        AppLanguage.KOREAN -> DateTimeFormatter.ofPattern("yyyy년 M월", language.locale)
        AppLanguage.ENGLISH -> DateTimeFormatter.ofPattern("MMMM yyyy", language.locale)
    }

private fun transactionDetailDateFormatter(language: AppLanguage): DateTimeFormatter =
    when (language) {
        AppLanguage.KOREAN -> DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm", language.locale)
        AppLanguage.ENGLISH -> DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", language.locale)
    }

private val TransactionTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
