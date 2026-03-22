package com.dondone.mobile.feature.finance.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnSecondary
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.domain.model.TransactionCategory
import com.dondone.mobile.domain.model.TransactionDirection
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

private val HistoryCanvas = Color.White
private val HistoryMutedSurface = Color(0xFFF6F7FB)
private val HistoryIncome = Color(0xFF2A6DF5)
private val HistoryExpense = Color(0xFF191F28)
private val HistoryWeekExpense = Color(0xFFE25D5D)
private val HistorySelectedDay = DawnSecondary
private val DragThreshold = 60f

@Composable
fun TransactionHistoryMainScreen(
    uiModel: TransactionHistoryMainUiModel,
    onOpenTransaction: (String) -> Unit
) {
    when (uiModel.screenState) {
        TransactionHistoryScreenState.LOADING -> {
            LoadingState(message = "거래내역을 불러오는 중입니다.")
            return
        }

        TransactionHistoryScreenState.ERROR -> {
            FeedbackState(message = uiModel.errorMessage ?: "거래내역을 불러오지 못했습니다.")
            return
        }

        else -> Unit
    }

    var selectedMonthValue by rememberSaveable(uiModel.walletId) {
        mutableStateOf(uiModel.initialMonth.toString())
    }
    var searchQuery by rememberSaveable(uiModel.walletId) { mutableStateOf("") }
    var activeFilter by rememberSaveable(uiModel.walletId) { mutableStateOf(TransactionLedgerFilter.ALL.name) }
    var selectedDayValue by rememberSaveable(uiModel.walletId, selectedMonthValue) { mutableStateOf("") }
    var anchorDayValue by rememberSaveable(uiModel.walletId, selectedMonthValue) {
        mutableStateOf(uiModel.initialAnchorDay.toString())
    }

    val selectedMonth = remember(selectedMonthValue) { YearMonth.parse(selectedMonthValue) }
    val monthItems = remember(uiModel.items, selectedMonth) {
        uiModel.items.filter { YearMonth.from(it.occurredAt) == selectedMonth }
    }

    LaunchedEffect(selectedMonthValue) {
        selectedDayValue = ""
        val newMonth = YearMonth.parse(selectedMonthValue)
        val initialAnchor = if (newMonth == uiModel.initialMonth) {
            uiModel.initialAnchorDay.dayOfMonth.coerceAtMost(newMonth.lengthOfMonth())
        } else {
            1
        }
        anchorDayValue = newMonth.atDay(initialAnchor).toString()
    }

    val selectedDay = selectedDayValue.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
    val selectedFilter = remember(activeFilter) { TransactionLedgerFilter.valueOf(activeFilter) }
    val anchorDay = anchorDayValue.let(LocalDate::parse)
    val filteredItems = remember(monthItems, searchQuery, selectedDay, selectedFilter) {
        monthItems.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.counterpartyName.contains(searchQuery, ignoreCase = true) ||
                item.memo.contains(searchQuery, ignoreCase = true) ||
                item.categoryLabel.contains(searchQuery, ignoreCase = true)
            val matchesDay = selectedDay == null || item.day == selectedDay
            val matchesFilter = when (selectedFilter) {
                TransactionLedgerFilter.ALL -> true
                TransactionLedgerFilter.INCOME -> item.direction == TransactionDirection.INCOME
                TransactionLedgerFilter.EXPENSE -> item.direction == TransactionDirection.EXPENSE
            }
            matchesQuery && matchesDay && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HistoryCanvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WalletHeader(
            walletName = uiModel.walletName,
            walletSubtitle = uiModel.walletSubtitle
        )

        MonthSelector(
            options = uiModel.monthOptions,
            selectedMonth = selectedMonth,
            onSelect = { selectedMonthValue = it.toString() }
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            placeholder = { Text("검색") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = DawnTextSubtle
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = DawnBorder,
                focusedBorderColor = DawnPrimary,
                unfocusedContainerColor = HistoryMutedSurface,
                focusedContainerColor = Color.White
            )
        )

        LedgerFilterRow(
            activeFilter = selectedFilter,
            onSelect = { activeFilter = it.name }
        )

        WeekStrip(
            selectedMonth = selectedMonth,
            selectedDay = selectedDay,
            anchorDay = anchorDay,
            items = monthItems,
            onChangeWeek = { nextAnchor ->
                anchorDayValue = nextAnchor.toString()
            },
            onSelectDay = { day ->
                selectedDayValue = if (selectedDay == day) "" else day.toString()
                anchorDayValue = day.toString()
            }
        )

        when {
            filteredItems.isEmpty() && monthItems.isEmpty() -> FeedbackState(message = uiModel.emptyMessage)
            filteredItems.isEmpty() -> FeedbackState(message = "검색 결과가 없습니다.")
            else -> {
                val grouped = filteredItems.groupBy { it.day }.toSortedMap(compareByDescending { it })
                grouped.forEach { (day, items) ->
                    Text(
                        text = day.format(TransactionGroupDateFormatter),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DawnText
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items.forEachIndexed { index, item ->
                            TransactionRow(
                                item = item,
                                onClick = { onOpenTransaction(item.id) }
                            )
                            if (index != items.lastIndex) {
                                HorizontalDivider(color = DawnBorder)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun TransactionHistoryDetailScreen(
    uiModel: TransactionHistoryDetailUiModel?,
    onEdit: () -> Unit
) {
    if (uiModel == null) {
        FeedbackState(message = "거래 정보를 찾지 못했습니다.")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HistoryCanvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = uiModel.counterpartyName,
                    style = MaterialTheme.typography.titleLarge,
                    color = DawnTextSubtle
                )
                Text(
                    text = uiModel.amountText,
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                    color = if (uiModel.direction == TransactionDirection.INCOME) HistoryIncome else HistoryExpense
                )
            }
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DawnSecondary,
                    contentColor = DawnPrimary
                )
            ) {
                Text("수정하기")
            }
        }

        SimpleSection(spacing = 18.dp) {
            DetailRow(label = "카테고리", value = uiModel.categoryLabel, emphasize = true)
            DetailRow(label = "메모", value = uiModel.memo.ifBlank { "메모 없음" }, emphasize = true)
        }

        HorizontalDivider(color = DawnBorder)

        SimpleSection(spacing = 18.dp) {
            DetailRow(label = "거래 유형", value = uiModel.directionLabel, emphasize = true)
            DetailRow(label = "거래 수단", value = uiModel.methodLabel, emphasize = true)
            DetailRow(label = "거래 일시", value = uiModel.dateText, emphasize = true)
            DetailRow(label = "수수료", value = uiModel.feeText, emphasize = true)
            DetailRow(label = "지갑", value = uiModel.walletName, emphasize = true)
        }
    }
}

@Composable
fun TransactionHistoryEditScreen(
    uiModel: TransactionHistoryEditUiModel?,
    onSave: (TransactionCategory, String) -> Unit
) {
    if (uiModel == null) {
        FeedbackState(message = "거래 정보를 찾지 못했습니다.")
        return
    }

    var selectedCategory by rememberSaveable(uiModel.transactionId) { mutableStateOf(uiModel.selectedCategory.name) }
    var memo by rememberSaveable(uiModel.transactionId) { mutableStateOf(uiModel.memo) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HistoryCanvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SimpleSection(spacing = 10.dp) {
            Text(
                text = uiModel.amountText,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                color = DawnText
            )
            Text(
                text = uiModel.counterpartyName,
                style = MaterialTheme.typography.titleMedium,
                color = DawnTextSubtle
            )
        }

        SimpleSection(spacing = 16.dp) {
            Text(
                text = "카테고리",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = DawnText
            )
            CategoryGrid(
                categories = uiModel.categories,
                selectedCategory = selectedCategory,
                onSelect = { selectedCategory = it.name }
            )
        }

        SimpleSection(spacing = 16.dp) {
            Text(
                text = "메모",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = DawnText
            )
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                placeholder = { Text("메모를 입력하세요") },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = DawnBorder,
                    focusedBorderColor = DawnPrimary,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )
        }

        Button(
            onClick = { onSave(TransactionCategory.valueOf(selectedCategory), memo) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = DawnPrimary)
        ) {
            Text("저장")
        }
    }
}

@Composable
private fun WalletHeader(
    walletName: String,
    walletSubtitle: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = walletName,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
        walletSubtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
        }
    }
}

@Composable
private fun MonthSelector(
    options: List<TransactionMonthOptionUiModel>,
    selectedMonth: YearMonth,
    onSelect: (YearMonth) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selectedMonth }?.label ?: selectedMonth.toString()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Text(
                text = selectedLabel,
                modifier = Modifier.clickable { expanded = true },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = DawnText
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            expanded = false
                            onSelect(option.value)
                        }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MonthChip(
                label = "전월",
                onClick = { onSelect(selectedMonth.minusMonths(1)) }
            )
            MonthChip(
                label = "다음월",
                onClick = { onSelect(selectedMonth.plusMonths(1)) }
            )
        }
    }
}

@Composable
private fun MonthChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = DawnSecondary,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = DawnPrimary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun LedgerFilterRow(
    activeFilter: TransactionLedgerFilter,
    onSelect: (TransactionLedgerFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TransactionLedgerFilter.values().forEach { filter ->
            Box(
                modifier = Modifier
                    .background(
                        color = if (filter == activeFilter) DawnPrimary else HistoryMutedSurface,
                        shape = RoundedCornerShape(999.dp)
                    )
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    text = filter.label,
                    color = if (filter == activeFilter) Color.White else DawnTextSubtle,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun WeekStrip(
    selectedMonth: YearMonth,
    selectedDay: LocalDate?,
    anchorDay: LocalDate,
    items: List<TransactionHistoryItemUiModel>,
    onChangeWeek: (LocalDate) -> Unit,
    onSelectDay: (LocalDate) -> Unit
) {
    var dragAmount by remember { mutableFloatStateOf(0f) }
    val weekStart = anchorDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
    val amountByDay = items.groupBy { it.day }.mapValues { entry ->
        entry.value.sumOf { item ->
            if (item.direction == TransactionDirection.INCOME) item.amountValue else -item.amountValue
        }
    }
    val unit = items.firstOrNull()?.currencyUnit ?: "KRW"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(selectedMonth, selectedDay, anchorDay) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, drag -> dragAmount += drag },
                    onDragEnd = {
                        if (abs(dragAmount) > DragThreshold) {
                            val delta = if (dragAmount < 0) 7L else -7L
                            onChangeWeek(anchorDay.plusDays(delta))
                        }
                        dragAmount = 0f
                    },
                    onDragCancel = { dragAmount = 0f }
                )
            },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        weekDays.forEach { day ->
            val amount = amountByDay[day] ?: 0
            DayCell(
                day = day,
                inMonth = day.monthValue == selectedMonth.monthValue && day.year == selectedMonth.year,
                isSelected = day == selectedDay,
                amountText = dayAmountText(amount, unit),
                onClick = {
                    if (day.monthValue == selectedMonth.monthValue && day.year == selectedMonth.year) {
                        onSelectDay(day)
                    }
                }
            )
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    inMonth: Boolean,
    isSelected: Boolean,
    amountText: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(42.dp)
            .background(
                color = if (isSelected) HistorySelectedDay else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = inMonth, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = day.dayOfWeek.toKoreanShortLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = if (inMonth) DawnTextSubtle else Color(0xFFCAD0D8)
        )
        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (inMonth) DawnText else Color(0xFFCAD0D8)
        )
        Text(
            text = amountText,
            style = MaterialTheme.typography.labelSmall,
            color = when {
                !inMonth -> Color(0xFFCAD0D8)
                amountText.startsWith("+") -> HistoryIncome
                amountText.startsWith("-") -> HistoryWeekExpense
                else -> DawnTextSubtle
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TransactionRow(
    item: TransactionHistoryItemUiModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(DawnSecondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon(item.category),
                contentDescription = null,
                tint = DawnPrimary
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.amountText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (item.direction == TransactionDirection.INCOME) HistoryIncome else HistoryExpense
            )
            Text(
                text = item.counterpartyName,
                style = MaterialTheme.typography.bodyLarge,
                color = DawnText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<TransactionCategory>,
    selectedCategory: String,
    onSelect: (TransactionCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        categories.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { category ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (category.name == selectedCategory) DawnSecondary else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelect(category) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = categoryIcon(category),
                                contentDescription = null,
                                tint = if (category.name == selectedCategory) DawnPrimary else DawnTextSubtle
                            )
                            Text(
                                text = category.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (category.name == selectedCategory) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (category.name == selectedCategory) DawnPrimary else DawnText
                            )
                        }
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = DawnTextSubtle
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = value,
            style = if (emphasize) {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = DawnText,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SimpleSection(
    spacing: androidx.compose.ui.unit.Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
private fun LoadingState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HistoryCanvas),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = DawnPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, color = DawnTextSubtle)
        }
    }
}

@Composable
private fun FeedbackState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HistoryMutedSurface, RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextSubtle
        )
    }
}

private fun dayAmountText(amount: Int, unit: String): String {
    if (amount == 0) return "-"
    val sign = if (amount > 0) "+" else "-"
    val absolute = abs(amount)
    val number = NumberFormat.getNumberInstance(Locale.KOREA).format(absolute)
    return if (unit == "KRW") "$sign$number" else "$sign$number"
}

private fun DayOfWeek.toKoreanShortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "월"
    DayOfWeek.TUESDAY -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY -> "목"
    DayOfWeek.FRIDAY -> "금"
    DayOfWeek.SATURDAY -> "토"
    DayOfWeek.SUNDAY -> "일"
}

private fun categoryIcon(category: TransactionCategory): ImageVector = when (category) {
    TransactionCategory.SALARY -> Icons.Default.Payments
    TransactionCategory.FOOD -> Icons.Default.Fastfood
    TransactionCategory.CAFE -> Icons.Default.Coffee
    TransactionCategory.SHOPPING -> Icons.Default.ShoppingBag
    TransactionCategory.TRANSPORT -> Icons.Default.DirectionsBus
    TransactionCategory.LIVING -> Icons.Default.Home
    TransactionCategory.TRANSFER -> Icons.Default.SwapHoriz
    TransactionCategory.ETC -> Icons.Default.Checkroom
}

private val TransactionGroupDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d일 EEEE", Locale.KOREAN)
