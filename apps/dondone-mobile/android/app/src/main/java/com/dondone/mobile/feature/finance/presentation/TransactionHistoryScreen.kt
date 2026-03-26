package com.dondone.mobile.feature.finance.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.LocalAppLanguage
import com.dondone.mobile.core.i18n.text
import com.dondone.mobile.domain.model.TransactionCategory
import com.dondone.mobile.domain.model.TransactionDirection
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

private val HistoryCanvas = Color.White
private val HistoryMutedSurface = Color(0xFFF6F7FB)
private val HistoryIncome = Color(0xFF2A6DF5)
private val HistoryExpense = Color(0xFF191F28)
private val DragThreshold = 60f

@Composable
fun TransactionHistoryMainScreen(uiModel: TransactionHistoryMainUiModel, onOpenTransaction: (String) -> Unit) {
    val language = LocalAppLanguage.current
    if (uiModel.screenState == TransactionHistoryScreenState.LOADING) {
        StateMessage(language.text("loading_transaction_history"), true)
        return
    }
    if (uiModel.screenState == TransactionHistoryScreenState.ERROR) {
        StateMessage(uiModel.errorMessage ?: language.text("failed_to_load_transaction_history"))
        return
    }
    var query by remember(uiModel.walletId) { mutableStateOf("") }
    var filter by remember(uiModel.walletId) { mutableStateOf(TransactionLedgerFilter.ALL) }
    var month by remember(uiModel.walletId) { mutableStateOf(uiModel.initialMonth) }
    var selectedDay by remember(uiModel.walletId) { mutableStateOf<LocalDate?>(null) }
    var anchorDay by remember(uiModel.walletId) { mutableStateOf(uiModel.initialAnchorDay) }
    val monthItems = remember(uiModel.items, month) { uiModel.items.filter { it.day.year == month.year && it.day.month == month.month } }
    val filtered = remember(monthItems, query, filter, selectedDay) {
        monthItems.filter { item ->
            val queryMatch = query.isBlank() || item.counterpartyName.contains(query, true) || item.memo.contains(query, true) || item.categoryLabel.contains(query, true)
            val filterMatch = when (filter) {
                TransactionLedgerFilter.ALL -> true
                TransactionLedgerFilter.INCOME -> item.direction == TransactionDirection.INCOME
                TransactionLedgerFilter.EXPENSE -> item.direction == TransactionDirection.EXPENSE
            }
            val dayMatch = selectedDay == null || item.day == selectedDay
            queryMatch && filterMatch && dayMatch
        }
    }

    Column(Modifier.fillMaxSize().background(HistoryCanvas).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(uiModel.walletName, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = DawnText)
        uiModel.walletSubtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = DawnTextSubtle) }
        MonthHeader(uiModel.monthOptions, month, { month = it; selectedDay = null; anchorDay = it.atDay(1) })
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(language.text("search")) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = DawnTextSubtle) },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = DawnBorder, focusedBorderColor = DawnPrimary, unfocusedContainerColor = HistoryMutedSurface, focusedContainerColor = Color.White)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TransactionLedgerFilter.values().forEach { option ->
                Box(Modifier.background(if (option == filter) DawnPrimary else HistoryMutedSurface, RoundedCornerShape(999.dp)).clickable { filter = option }.padding(horizontal = 16.dp, vertical = 9.dp)) {
                    Text(language.text(option.labelKey), color = if (option == filter) Color.White else DawnTextSubtle)
                }
            }
        }
        WeekStrip(month, anchorDay, selectedDay, monthItems, onChangeWeek = { anchorDay = anchorDay.plusDays(it) }, onSelectDay = { selectedDay = if (selectedDay == it) null else it; anchorDay = it })
        if (filtered.isEmpty()) {
            StateMessage(if (monthItems.isEmpty()) uiModel.emptyMessage else language.text("no_search_results_found"))
        } else {
            filtered.groupBy { it.day }.toSortedMap(compareByDescending { it }).forEach { (day, items) ->
                Text(day.format(groupFormatter(language)), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = DawnText)
                items.forEachIndexed { index, item ->
                    TransactionListItem(item, onOpenTransaction)
                    if (index != items.lastIndex) HorizontalDivider(color = DawnBorder)
                }
            }
        }
    }
}

@Composable
fun TransactionHistoryDetailScreen(uiModel: TransactionHistoryDetailUiModel?, onEdit: () -> Unit) {
    val language = LocalAppLanguage.current
    if (uiModel == null) return StateMessage(language.text("transaction_not_found"))
    Column(Modifier.fillMaxSize().background(HistoryCanvas).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(uiModel.counterpartyName, style = MaterialTheme.typography.titleLarge, color = DawnTextSubtle)
                Text(uiModel.amountText, style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black), color = if (uiModel.direction == TransactionDirection.INCOME) HistoryIncome else HistoryExpense)
            }
            Button(onClick = onEdit, colors = ButtonDefaults.buttonColors(containerColor = DawnSecondary, contentColor = DawnPrimary)) { Text(language.text("edit")) }
        }
        DetailRow(language.text("category"), uiModel.categoryLabel)
        DetailRow(language.text("memo"), uiModel.memo.ifBlank { language.text("no_memo") })
        HorizontalDivider(color = DawnBorder)
        DetailRow(language.text("direction"), uiModel.directionLabel)
        DetailRow(language.text("method"), uiModel.methodLabel)
        DetailRow(language.text("date_time"), uiModel.dateText)
        DetailRow(language.text("fee"), uiModel.feeText)
        DetailRow(language.text("wallet"), uiModel.walletName)
    }
}

@Composable
fun TransactionHistoryEditScreen(uiModel: TransactionHistoryEditUiModel?, onSave: (TransactionCategory, String) -> Unit) {
    val language = LocalAppLanguage.current
    if (uiModel == null) return StateMessage(language.text("transaction_not_found"))
    var selectedCategory by rememberSaveable(uiModel.transactionId) { mutableStateOf(uiModel.selectedCategory) }
    var memo by rememberSaveable(uiModel.transactionId) { mutableStateOf(uiModel.memo) }
    Column(Modifier.fillMaxSize().background(HistoryCanvas).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(uiModel.amountText, style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black), color = DawnText)
        Text(uiModel.counterpartyName, style = MaterialTheme.typography.titleMedium, color = DawnTextSubtle)
        Text(language.text("category"), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = DawnText)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            uiModel.categories.forEach { category ->
                Box(Modifier.weight(1f).background(if (category == selectedCategory) DawnSecondary else HistoryMutedSurface, RoundedCornerShape(14.dp)).clickable { selectedCategory = category }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text(categoryLabel(category, language), color = if (category == selectedCategory) DawnPrimary else DawnText)
                }
            }
        }
        OutlinedTextField(
            value = memo,
            onValueChange = { memo = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            placeholder = { Text(language.text("enter_a_memo")) },
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = DawnBorder, focusedBorderColor = DawnPrimary, unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
        )
        Button(onClick = { onSave(selectedCategory, memo) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DawnPrimary)) {
            Text(language.text("save_2"))
        }
    }
}

@Composable
private fun MonthHeader(options: List<TransactionMonthOptionUiModel>, selected: java.time.YearMonth, onSelect: (java.time.YearMonth) -> Unit) {
    val language = LocalAppLanguage.current
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Box {
            Text(options.firstOrNull { it.value == selected }?.label ?: selected.toString(), modifier = Modifier.clickable { expanded = true }, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = DawnText)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = { expanded = false; onSelect(option.value) }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MonthChip(language.text("previous_month")) { onSelect(selected.minusMonths(1)) }
            MonthChip(language.text("next_month")) { onSelect(selected.plusMonths(1)) }
        }
    }
}

@Composable private fun MonthChip(label: String, onClick: () -> Unit) { Box(Modifier.background(DawnSecondary, RoundedCornerShape(999.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)) { Text(label, color = DawnPrimary) } }

@Composable
private fun WeekStrip(month: java.time.YearMonth, anchorDay: LocalDate, selectedDay: LocalDate?, items: List<TransactionHistoryItemUiModel>, onChangeWeek: (Long) -> Unit, onSelectDay: (LocalDate) -> Unit) {
    val language = LocalAppLanguage.current
    var dragAmount by remember { mutableFloatStateOf(0f) }
    val weekStart = anchorDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val amountByDay = items.groupBy { it.day }.mapValues { entry -> entry.value.sumOf { if (it.direction == TransactionDirection.INCOME) it.amountValue else -it.amountValue } }
    Row(Modifier.fillMaxWidth().pointerInput(month, anchorDay, selectedDay) { detectHorizontalDragGestures(onHorizontalDrag = { _, drag -> dragAmount += drag }, onDragEnd = { if (abs(dragAmount) > DragThreshold) onChangeWeek(if (dragAmount < 0) 7 else -7); dragAmount = 0f }, onDragCancel = { dragAmount = 0f }) }, horizontalArrangement = Arrangement.SpaceBetween) {
        (0..6).map { weekStart.plusDays(it.toLong()) }.forEach { day ->
            val inMonth = day.month == month.month && day.year == month.year
            val amountText = amountByDay[day]?.let { if (it > 0) "+$it" else if (it < 0) "$it" else "-" } ?: "-"
            Column(Modifier.width(42.dp).background(if (day == selectedDay) DawnSecondary else Color.Transparent, RoundedCornerShape(16.dp)).clickable(enabled = inMonth) { onSelectDay(day) }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(day.dayOfWeek.shortLabel(language), style = MaterialTheme.typography.labelSmall, color = if (inMonth) DawnTextSubtle else Color(0xFFCAD0D8))
                Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = if (inMonth) DawnText else Color(0xFFCAD0D8))
                Text(amountText, style = MaterialTheme.typography.labelSmall, color = if (amountText.startsWith("+")) HistoryIncome else HistoryExpense, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TransactionListItem(item: TransactionHistoryItemUiModel, onOpenTransaction: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onOpenTransaction(item.id) }.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(DawnSecondary, CircleShape), contentAlignment = Alignment.Center) { Icon(categoryIcon(item.category), null, tint = DawnPrimary) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.amountText, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = if (item.direction == TransactionDirection.INCOME) HistoryIncome else HistoryExpense)
            Text(item.counterpartyName, style = MaterialTheme.typography.bodyLarge, color = DawnText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.width(88.dp), style = MaterialTheme.typography.titleMedium, color = DawnTextSubtle)
        Text(value, modifier = Modifier.weight(1f).padding(start = 20.dp), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = DawnText)
    }
}

@Composable
private fun StateMessage(message: String, loading: Boolean = false) {
    Box(Modifier.fillMaxWidth().background(HistoryMutedSurface, RoundedCornerShape(24.dp)).padding(horizontal = 18.dp, vertical = 28.dp), contentAlignment = Alignment.Center) {
        if (loading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = DawnPrimary)
                Spacer(Modifier.height(12.dp))
                Text(message, color = DawnTextSubtle)
            }
        } else {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = DawnTextSubtle)
        }
    }
}

private fun DayOfWeek.shortLabel(language: AppLanguage): String = when (language) {
    AppLanguage.KOREAN -> when (this) { DayOfWeek.MONDAY -> "월"; DayOfWeek.TUESDAY -> "화"; DayOfWeek.WEDNESDAY -> "수"; DayOfWeek.THURSDAY -> "목"; DayOfWeek.FRIDAY -> "금"; DayOfWeek.SATURDAY -> "토"; DayOfWeek.SUNDAY -> "일" }
    AppLanguage.ENGLISH -> when (this) { DayOfWeek.MONDAY -> "Mon"; DayOfWeek.TUESDAY -> "Tue"; DayOfWeek.WEDNESDAY -> "Wed"; DayOfWeek.THURSDAY -> "Thu"; DayOfWeek.FRIDAY -> "Fri"; DayOfWeek.SATURDAY -> "Sat"; DayOfWeek.SUNDAY -> "Sun" }
}

private fun groupFormatter(language: AppLanguage): DateTimeFormatter = when (language) {
    AppLanguage.KOREAN -> DateTimeFormatter.ofPattern("d일 EEEE", Locale.KOREAN)
    AppLanguage.ENGLISH -> DateTimeFormatter.ofPattern("d EEEE", Locale.ENGLISH)
}

private fun categoryLabel(category: TransactionCategory, language: AppLanguage): String = when (category) {
    TransactionCategory.SALARY -> if (language == AppLanguage.ENGLISH) "Salary" else "급여"
    TransactionCategory.FOOD -> if (language == AppLanguage.ENGLISH) "Food" else "식비"
    TransactionCategory.CAFE -> if (language == AppLanguage.ENGLISH) "Cafe/Snack" else "카페/간식"
    TransactionCategory.SHOPPING -> if (language == AppLanguage.ENGLISH) "Shopping" else "쇼핑"
    TransactionCategory.TRANSPORT -> if (language == AppLanguage.ENGLISH) "Transport" else "교통"
    TransactionCategory.LIVING -> if (language == AppLanguage.ENGLISH) "Living" else "생활"
    TransactionCategory.TRANSFER -> if (language == AppLanguage.ENGLISH) "Transfer" else "이체"
    TransactionCategory.ETC -> if (language == AppLanguage.ENGLISH) "Other" else "기타"
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
