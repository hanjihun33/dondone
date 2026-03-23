package com.dondone.mobile.feature.finance.presentation

import com.dondone.mobile.app.session.TransactionMetadataOverride
import com.dondone.mobile.data.remittance.RemittanceRemoteMode
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TransactionCategory
import com.dondone.mobile.domain.model.TransactionDirection
import com.dondone.mobile.domain.model.TransactionRecord
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
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
    val label: String
) {
    ALL("전체"),
    INCOME("입금"),
    EXPENSE("출금")
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
    isAuthenticated: Boolean,
    overrides: Map<String, TransactionMetadataOverride>
): TransactionHistoryMainUiModel {
    if (isAuthenticated && accountId == "remote-wallet") {
        return remittanceRemoteState.toRemoteTransactionHistoryMainUiModel(overrides)
    }

    val account = remittance.accounts.firstOrNull { it.id == accountId } ?: remittance.selectedAccount()
    val transactions = remittance.transactions
        .filter { it.walletId == account.id }
        .map { record ->
            val override = overrides[record.id]
            record.toUiModel(
                category = override?.category ?: record.category,
                memo = override?.memo ?: record.memo,
                currencyUnit = "KRW"
            )
        }
        .sortedByDescending { it.occurredAt }

    val initialMonth = YearMonth.of(demo.year, demo.month)
    return TransactionHistoryMainUiModel(
        walletId = account.id,
        walletName = account.name,
        walletSubtitle = account.number,
        screenState = if (transactions.isEmpty()) TransactionHistoryScreenState.EMPTY else TransactionHistoryScreenState.CONTENT,
        monthOptions = buildMonthOptions(initialMonth),
        initialMonth = initialMonth,
        initialAnchorDay = LocalDate.of(demo.year, demo.month, demo.asOfDay),
        items = transactions,
        emptyMessage = "이번 기간에 거래내역이 없어요.",
        errorMessage = null
    )
}

fun DemoState.toTransactionHistoryDetailUiModel(
    accountId: String,
    transactionId: String,
    remittanceRemoteState: RemittanceRemoteState,
    isAuthenticated: Boolean,
    overrides: Map<String, TransactionMetadataOverride>
): TransactionHistoryDetailUiModel? {
    val source = toTransactionHistoryMainUiModel(accountId, remittanceRemoteState, isAuthenticated, overrides)
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
        directionLabel = if (item.direction == TransactionDirection.INCOME) "입금" else "출금",
        category = item.category,
        categoryLabel = item.categoryLabel,
        memo = item.memo,
        dateText = item.occurredAt.format(TransactionDetailDateFormatter),
        methodLabel = item.methodLabel
    )
}

fun DemoState.toTransactionHistoryEditUiModel(
    accountId: String,
    transactionId: String,
    remittanceRemoteState: RemittanceRemoteState,
    isAuthenticated: Boolean,
    overrides: Map<String, TransactionMetadataOverride>
): TransactionHistoryEditUiModel? {
    val detail = toTransactionHistoryDetailUiModel(accountId, transactionId, remittanceRemoteState, isAuthenticated, overrides)
        ?: return null
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
    overrides: Map<String, TransactionMetadataOverride>
): TransactionHistoryMainUiModel {
    val payload = payload
    if (mode == RemittanceRemoteMode.LOADING || payload == null && mode == RemittanceRemoteMode.UNAUTHENTICATED) {
        val currentMonth = YearMonth.now()
        return TransactionHistoryMainUiModel(
            walletId = "remote-wallet",
            walletName = "DonDone Wallet",
            walletSubtitle = null,
            screenState = TransactionHistoryScreenState.LOADING,
            monthOptions = buildMonthOptions(currentMonth),
            initialMonth = currentMonth,
            initialAnchorDay = currentMonth.atDay(LocalDate.now().dayOfMonth.coerceAtMost(currentMonth.lengthOfMonth())),
            items = emptyList(),
            emptyMessage = "거래내역을 불러오고 있어요.",
            errorMessage = errorMessage
        )
    }
    if (mode == RemittanceRemoteMode.ERROR || payload == null) {
        val currentMonth = YearMonth.now()
        return TransactionHistoryMainUiModel(
            walletId = "remote-wallet",
            walletName = "DonDone Wallet",
            walletSubtitle = payload?.wallet?.walletAddress?.toShortWalletAddress(),
            screenState = TransactionHistoryScreenState.ERROR,
            monthOptions = buildMonthOptions(currentMonth),
            initialMonth = currentMonth,
            initialAnchorDay = currentMonth.atDay(LocalDate.now().dayOfMonth.coerceAtMost(currentMonth.lengthOfMonth())),
            items = emptyList(),
            emptyMessage = "거래내역이 없어요.",
            errorMessage = errorMessage ?: "거래내역을 불러오지 못했어요."
        )
    }

    val currentMonth = YearMonth.now()
    val currencyUnit = payload.transfers.firstOrNull()?.assetSymbol ?: "USDC"
    val currencyDecimals = payload.balance?.assetDecimals ?: assetDecimalsFor(currencyUnit)
    val items = payload.transfers.map { transfer ->
        val timestamp = transfer.updatedAt ?: LocalDateTime.now()
        val override = overrides[transfer.transferId]
        val category = override?.category ?: inferRemoteCategory(transfer.recipientAlias)
        val memo = override?.memo ?: ""
        val amount = atomicToDisplayAmount(transfer.amountAtomic, currencyDecimals)
        val direction = transfer.direction.toTransactionDirection()
        val counterpartyName = when (direction) {
            TransactionDirection.INCOME -> transfer.senderName ?: transfer.senderAddress.toShortWalletAddress()
            TransactionDirection.EXPENSE -> transfer.recipientAlias ?: transfer.recipientAddress.toShortWalletAddress()
        }
        val counterpartyAddress = when (direction) {
            TransactionDirection.INCOME -> transfer.senderAddress
            TransactionDirection.EXPENSE -> transfer.recipientAddress
        }
        TransactionHistoryItemUiModel(
            id = transfer.transferId,
            walletId = "remote-wallet",
            occurredAt = timestamp,
            day = timestamp.toLocalDate(),
            counterpartyName = counterpartyName,
            counterpartyAddress = counterpartyAddress,
            amountValue = amount,
            amountText = formatAtomicMoney(transfer.amountAtomic, direction, currencyUnit, currencyDecimals),
            feeText = formatRemoteFee(transfer.networkFeeWei, transfer.networkFeeAssetSymbol),
            direction = direction,
            category = category,
            categoryLabel = category.label,
            memo = memo,
            methodLabel = "지갑 송금",
            timeText = timestamp.format(TransactionTimeFormatter),
            currencyUnit = currencyUnit
        )
    }.sortedByDescending { it.occurredAt }

    return TransactionHistoryMainUiModel(
        walletId = "remote-wallet",
        walletName = "DonDone Wallet",
        walletSubtitle = payload.wallet.walletAddress.toShortWalletAddress(),
        screenState = if (items.isEmpty()) TransactionHistoryScreenState.EMPTY else TransactionHistoryScreenState.CONTENT,
        monthOptions = buildMonthOptions(currentMonth),
        initialMonth = currentMonth,
        initialAnchorDay = currentMonth.atDay(LocalDate.now().dayOfMonth.coerceAtMost(currentMonth.lengthOfMonth())),
        items = items,
        emptyMessage = "이번 기간에 거래내역이 없어요.",
        errorMessage = errorMessage
    )
}

private fun String.toTransactionDirection(): TransactionDirection =
    if (equals("INCOME", ignoreCase = true)) TransactionDirection.INCOME else TransactionDirection.EXPENSE

private fun TransactionRecord.toUiModel(
    category: TransactionCategory,
    memo: String,
    currencyUnit: String
): TransactionHistoryItemUiModel {
    return TransactionHistoryItemUiModel(
        id = id,
        walletId = walletId,
        occurredAt = occurredAt,
        day = occurredAt.toLocalDate(),
        counterpartyName = counterpartyName,
        counterpartyAddress = counterpartyAddress,
        amountValue = amount,
        amountText = formatMoney(amount, direction, currencyUnit),
        feeText = formatFee(feeAmount, currencyUnit),
        direction = direction,
        category = category,
        categoryLabel = category.label,
        memo = memo,
        methodLabel = methodLabel,
        timeText = occurredAt.format(TransactionTimeFormatter),
        currencyUnit = currencyUnit
    )
}

private fun inferRemoteCategory(alias: String?): TransactionCategory {
    val normalized = alias.orEmpty().lowercase(Locale.ROOT)
    return when {
        normalized.contains("payroll") -> TransactionCategory.SALARY
        normalized.contains("cafe") -> TransactionCategory.CAFE
        normalized.contains("mart") -> TransactionCategory.LIVING
        else -> TransactionCategory.TRANSFER
    }
}

private fun buildMonthOptions(center: YearMonth): List<TransactionMonthOptionUiModel> {
    return (-12..12)
        .map { offset -> center.plusMonths(offset.toLong()) }
        .sortedDescending()
        .map { month -> TransactionMonthOptionUiModel(month, month.format(TransactionMonthFormatter)) }
}

private fun atomicToDisplayAmount(amountAtomic: Long, decimals: Int): Int =
    scaleAtomicAmount(amountAtomic, decimals).toInt()

private fun formatAtomicMoney(
    amountAtomic: Long,
    direction: TransactionDirection,
    unit: String,
    decimals: Int
): String {
    val sign = if (direction == TransactionDirection.INCOME) "+" else "-"
    return "$sign${formatScaledAmount(amountAtomic, unit, decimals, minimumFractionDigits = 0)}"
}

private fun formatMoney(
    value: Int,
    direction: TransactionDirection,
    unit: String
): String {
    val sign = if (direction == TransactionDirection.INCOME) "+" else "-"
    val number = NumberFormat.getNumberInstance(Locale.KOREA).format(value)
    return if (unit == "KRW") {
        "${sign}${number}원"
    } else {
        "$sign$number $unit"
    }
}

private fun formatFee(value: Int, unit: String): String {
    val number = NumberFormat.getNumberInstance(Locale.KOREA).format(value)
    return if (unit == "KRW") {
        "${number}원"
    } else {
        "$number $unit"
    }
}

private fun unavailableFeeText(): String = "-"

private fun formatRemoteFee(networkFeeWei: String?, feeAssetSymbol: String?): String {
    val parsed = networkFeeWei?.toBigIntegerOrNull() ?: return unavailableFeeText()
    if (parsed.signum() == 0) return unavailableFeeText()
    val symbol = feeAssetSymbol ?: "ETH"
    val ethValue = BigDecimal(parsed).movePointLeft(18)
    if (ethValue.compareTo(BigDecimal("0.000001")) < 0) {
        return "< 0.000001 $symbol"
    }
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA).apply {
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
    minimumFractionDigits: Int
): String {
    val scaled = scaleAtomicAmount(amountAtomic, decimals)
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA).apply {
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
        "${number}원"
    } else {
        "$number $unit"
    }
}

private val TransactionMonthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)
private val TransactionTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val TransactionDetailDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm", Locale.KOREAN)
