package com.dondone.mobile.feature.workproof.presentation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnPrimaryDeep
import com.dondone.mobile.core.designsystem.DawnSurface
import com.dondone.mobile.core.designsystem.DawnSurfaceAlt
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SecondaryActionButton
import java.time.YearMonth

private val WorkproofCanvas = Color.White
private val WorkproofDivider = Color(0xFFE8EBF0)
private val WorkproofRowAccentBackground = Color(0xFFF2F3FF)
private val WorkproofRowAccentTint = Color(0xFF6D68F5)
private val WorkproofMissingBackground = Color(0xFFF1F5F9)
private val WorkproofMissingBorder = Color(0xFFDBE3EE)
private val WorkproofMissingText = Color(0xFF64748B)
private val WorkproofPartialBackground = Color(0xFFFEF3C7)
private val WorkproofPartialBorder = Color(0xFFF8D26D)
private val WorkproofPartialText = Color(0xFF92400E)
private val WorkproofCompleteBackground = Color(0xFFBBF7D0)
private val WorkproofCompleteBorder = Color(0xFF74E4A2)
private val WorkproofCompleteText = Color(0xFF166534)
private val WorkproofModifiedBackground = Color(0xFFFECACA)
private val WorkproofModifiedBorder = Color(0xFFF59EA9)
private val WorkproofModifiedText = Color(0xFF9F1239)
private val WorkproofWeekdays = listOf("일", "월", "화", "수", "목", "금", "토")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkproofScreen(
    uiModel: WorkproofUiModel,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit,
    onSaveEdit: (String, String, String, Boolean) -> Unit,
    onDetailVisibilityChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val baseMonth = remember(uiModel.calendarBaseYear, uiModel.calendarBaseMonth) {
        YearMonth.of(uiModel.calendarBaseYear, uiModel.calendarBaseMonth)
    }
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var editingRecordId by rememberSaveable { mutableStateOf<String?>(null) }
    var editReasonKey by rememberSaveable { mutableStateOf("") }
    var editMemo by rememberSaveable { mutableStateOf("") }
    var selectedAttachmentName by rememberSaveable { mutableStateOf<String?>(null) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var reasonMenuExpanded by remember { mutableStateOf(false) }
    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedAttachmentName = uri?.let { resolveAttachmentName(context, it) }
    }
    val displayedMonth = remember(baseMonth, monthOffset) {
        baseMonth.plusMonths(monthOffset.toLong())
    }
    val isBaseMonth = remember(baseMonth, displayedMonth) {
        displayedMonth == baseMonth
    }
    val selectedReason = remember(editReasonKey) {
        WorkproofEditReasons.firstOrNull { it.key == editReasonKey }
    }
    val calendarCells = remember(displayedMonth, uiModel) {
        buildWorkproofCalendarCells(
            displayedMonth = displayedMonth,
            baseMonth = baseMonth,
            currentDay = uiModel.calendarCurrentDay,
            dayTones = uiModel.calendarDayTones
        )
    }
    val recordedDayCount = calendarCells.count {
        it.tone != null && it.tone != WorkproofCalendarTone.MISSING
    }
    val displayedRecentRecords = remember(isBaseMonth, uiModel.recentRecords) {
        if (isBaseMonth) uiModel.recentRecords else emptyList()
    }
    val displayedAuditPreview = remember(isBaseMonth, uiModel.auditPreview) {
        if (isBaseMonth) uiModel.auditPreview else null
    }
    val displayedAudits = remember(isBaseMonth, uiModel.audits) {
        if (isBaseMonth) uiModel.audits else emptyList()
    }
    val editingRecord = remember(displayedRecentRecords, editingRecordId) {
        displayedRecentRecords.firstOrNull { it.id == editingRecordId }
    }
    val clearEditSheet = {
        editingRecordId = null
        editReasonKey = ""
        editMemo = ""
        selectedAttachmentName = null
        reasonMenuExpanded = false
    }

    LaunchedEffect(showDetails) {
        onDetailVisibilityChange(showDetails)
    }

    if (editingRecord != null) {
        ModalBottomSheet(
            onDismissRequest = clearEditSheet,
            sheetState = editSheetState,
            containerColor = DawnSurface
        ) {
            WorkproofEditSheet(
                record = editingRecord,
                selectedReasonLabel = selectedReason?.label.orEmpty(),
                reasonMenuExpanded = reasonMenuExpanded,
                memo = editMemo,
                selectedAttachmentName = selectedAttachmentName,
                onToggleReasonMenu = { reasonMenuExpanded = !reasonMenuExpanded },
                onDismissReasonMenu = { reasonMenuExpanded = false },
                onSelectReason = { option ->
                    editReasonKey = option.key
                    reasonMenuExpanded = false
                },
                onMemoChange = { editMemo = it },
                onPickAttachment = { attachmentLauncher.launch(arrayOf("*/*")) },
                onClearAttachment = { selectedAttachmentName = null },
                onSave = {
                    val reasonLabel = selectedReason?.label ?: return@WorkproofEditSheet
                    onSaveEdit(
                        editingRecord.id,
                        reasonLabel,
                        editMemo.trim(),
                        selectedAttachmentName != null
                    )
                    clearEditSheet()
                },
                onClose = clearEditSheet
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkproofCanvas)
    ) {
        AnimatedContent(
            targetState = showDetails,
            transitionSpec = {
                if (targetState) {
                    slideInHorizontally(
                        animationSpec = tween(260),
                        initialOffsetX = { it / 3 }
                    ) togetherWith slideOutHorizontally(
                        animationSpec = tween(220),
                        targetOffsetX = { -it / 6 }
                    )
                } else {
                    slideInHorizontally(
                        animationSpec = tween(260),
                        initialOffsetX = { -it / 6 }
                    ) togetherWith slideOutHorizontally(
                        animationSpec = tween(220),
                        targetOffsetX = { it / 3 }
                    )
                }
            },
            label = "workproofContentSwitch"
        ) { detailVisible ->
            if (detailVisible) {
                WorkproofDetailPage(
                    displayedMonthText = formatMonthText(displayedMonth),
                    calendarCountText = "기록 ${recordedDayCount}일",
                    calendarCells = calendarCells,
                    weekdays = WorkproofWeekdays,
                    records = displayedRecentRecords,
                    preview = displayedAuditPreview,
                    audits = displayedAudits.drop(1),
                    onPreviousMonth = { monthOffset -= 1 },
                    onNextMonth = { monthOffset += 1 },
                    onBack = { showDetails = false },
                    onEditRecord = { record ->
                        editingRecordId = record.id
                        editReasonKey = ""
                        editMemo = ""
                        selectedAttachmentName = null
                        reasonMenuExpanded = false
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    WorkproofPunchCard(
                        uiModel = uiModel.summary,
                        onClockIn = onClockIn,
                        onClockOut = onClockOut,
                        onToggleDetails = { showDetails = true }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun WorkproofPunchCard(
    uiModel: WorkproofSummaryUiModel,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit,
    onToggleDetails: () -> Unit
) {
    WorkproofSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WorkproofSectionHeader(
                title = "오늘 근무",
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onToggleDetails),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "상세 기록 보기",
                            tint = DawnTextSubtle,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            Text(
                text = uiModel.todayMetaText,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryActionButton(
                    text = "출근",
                    onClick = onClockIn,
                    enabled = uiModel.canClockIn,
                    modifier = Modifier.weight(1f)
                )
                SecondaryActionButton(
                    text = "퇴근",
                    onClick = onClockOut,
                    enabled = uiModel.canClockOut,
                    modifier = Modifier.weight(1f)
                )
            }

            WorkproofKeyValueRow(label = "출근", value = uiModel.todayInText)
            WorkproofKeyValueRow(label = "퇴근", value = uiModel.todayOutText)
            WorkproofKeyValueRow(label = "근무일", value = uiModel.verifiedDaysText)
            WorkproofKeyValueRow(label = "수정", value = uiModel.auditCountText)
        }
    }
}

@Composable
private fun WorkproofDetailPage(
    displayedMonthText: String,
    calendarCountText: String,
    calendarCells: List<WorkproofCalendarCellUiModel>,
    weekdays: List<String>,
    records: List<WorkproofRecordUiModel>,
    preview: WorkproofAuditUiModel?,
    audits: List<WorkproofAuditUiModel>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onBack: () -> Unit,
    onEditRecord: (WorkproofRecordUiModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 14.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "뒤로",
                tint = DawnTextSubtle,
                modifier = Modifier.size(20.dp)
            )
        }
        WorkproofCalendarCard(
            displayedMonthText = displayedMonthText,
            calendarCountText = calendarCountText,
            calendarCells = calendarCells,
            weekdays = weekdays,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth
        )
        WorkproofSectionDivider()
        WorkproofRecentLogsCard(
            records = records,
            onEditRecord = onEditRecord
        )
        WorkproofSectionDivider()
        WorkproofAuditCard(
            preview = preview,
            audits = audits
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun WorkproofCalendarCard(
    displayedMonthText: String,
    calendarCountText: String,
    calendarCells: List<WorkproofCalendarCellUiModel>,
    weekdays: List<String>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    WorkproofSurfaceCard {
        WorkproofSectionHeader(
            title = "근무 달력",
            trailing = {
                Text(
                    text = calendarCountText,
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnTextSubtle
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkproofMonthButton(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "이전 달",
                        tint = DawnTextSubtle,
                        modifier = Modifier.size(28.dp)
                    )
                },
                onClick = onPreviousMonth
            )
            Text(
                text = displayedMonthText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = DawnText
            )
            WorkproofMonthButton(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "다음 달",
                        tint = DawnTextSubtle,
                        modifier = Modifier.size(28.dp)
                    )
                },
                onClick = onNextMonth
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            weekdays.forEach { label ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = DawnTextSubtle
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            calendarCells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    week.forEach { cell ->
                        WorkproofCalendarCell(
                            uiModel = cell,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WorkproofLegendItem(label = "미기록", tone = WorkproofCalendarTone.MISSING)
            WorkproofLegendItem(label = "출근만", tone = WorkproofCalendarTone.PARTIAL)
            WorkproofLegendItem(label = "완료", tone = WorkproofCalendarTone.COMPLETE)
            WorkproofLegendItem(label = "수정", tone = WorkproofCalendarTone.MODIFIED)
        }
    }
}

@Composable
private fun WorkproofRecentLogsCard(
    records: List<WorkproofRecordUiModel>,
    onEditRecord: (WorkproofRecordUiModel) -> Unit
) {
    WorkproofSurfaceCard {
        WorkproofSectionHeader(title = "최근 기록")

        records.forEachIndexed { index, record ->
            WorkproofRecentRecordRow(
                record = record,
                onEditRecord = onEditRecord
            )
            if (index != records.lastIndex) {
                HorizontalDivider(color = WorkproofDivider)
            }
        }
    }
}

@Composable
private fun WorkproofAuditCard(
    preview: WorkproofAuditUiModel?,
    audits: List<WorkproofAuditUiModel>
) {
    WorkproofSurfaceCard {
        WorkproofSectionHeader(title = "변경 기록")

        if (preview != null) {
            WorkproofAuditPreviewRow(uiModel = preview)
        } else {
            WorkproofAuditPreviewRow(
                uiModel = WorkproofAuditUiModel(
                    dateText = "",
                    changeText = "아직 쌓인 변경 기록이 없어요.",
                    attachmentText = "",
                    reasonText = "수정이 생기면 사유와 첨부 여부가 여기에 먼저 표시돼요."
                )
            )
        }

        if (audits.isNotEmpty()) {
            audits.forEachIndexed { index, audit ->
                HorizontalDivider(color = WorkproofDivider)
                WorkproofAuditPreviewRow(uiModel = audit)
            }
        }
    }
}

@Composable
private fun WorkproofRecentRecordRow(
    record: WorkproofRecordUiModel,
    onEditRecord: (WorkproofRecordUiModel) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(WorkproofRowAccentBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = WorkproofRowAccentTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.dateText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                    color = DawnText
                )
                WorkproofStatusPill(
                    text = record.statusText,
                    tone = record.tone
                )
            }
            Text(
                text = formatRecordTimeLine(record.timeText),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = DawnText
            )
            Text(
                text = record.attachmentText,
                style = MaterialTheme.typography.bodySmall,
                color = DawnTextSubtle
            )
            record.modifiedHintText?.let { hint ->
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnTextSubtle
                )
            }
        }
        WorkproofInlineActionButton(
            text = "수정",
            onClick = { onEditRecord(record) }
        )
    }
}

@Composable
private fun WorkproofAuditPreviewRow(
    uiModel: WorkproofAuditUiModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(WorkproofRowAccentBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SyncAlt,
                contentDescription = null,
                tint = WorkproofRowAccentTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (uiModel.dateText.isNotBlank()) {
                Text(
                    text = uiModel.dateText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                    color = DawnText
                )
            }
            Text(
                text = uiModel.changeText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = DawnText
            )
            if (uiModel.attachmentText.isNotBlank()) {
                Text(
                    text = uiModel.attachmentText,
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnTextSubtle
                )
            }
            Text(
                text = uiModel.reasonText,
                style = MaterialTheme.typography.bodySmall,
                color = DawnTextSubtle
            )
        }
    }
}

@Composable
private fun WorkproofSurfaceCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
private fun WorkproofSectionDivider() {
    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = WorkproofDivider)
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun WorkproofSectionHeader(
    title: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
        trailing?.invoke()
    }
}

@Composable
private fun WorkproofKeyValueRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextSubtle
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
    }
}

@Composable
private fun WorkproofMonthButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun WorkproofInlineActionButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, DawnBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
    }
}

@Composable
private fun WorkproofSelectionField(
    value: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, DawnBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isPlaceholder) DawnTextSubtle else DawnText
        )
        Text(
            text = "▾",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
            color = DawnTextSubtle
        )
    }
}

@Composable
private fun WorkproofEditSheet(
    record: WorkproofRecordUiModel,
    selectedReasonLabel: String,
    reasonMenuExpanded: Boolean,
    memo: String,
    selectedAttachmentName: String?,
    onToggleReasonMenu: () -> Unit,
    onDismissReasonMenu: () -> Unit,
    onSelectReason: (WorkproofEditReasonOption) -> Unit,
    onMemoChange: (String) -> Unit,
    onPickAttachment: () -> Unit,
    onClearAttachment: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit
) {
    val timeParts = remember(record.timeText) {
        record.timeText.split(" - ").let { parts ->
            (parts.getOrNull(0) ?: "-") to (parts.getOrNull(1) ?: "-")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "근무 시간 수정",
                    style = MaterialTheme.typography.titleLarge,
                    color = DawnText
                )
                Text(
                    text = record.dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnTextSubtle
                )
            }
            Text(
                text = "닫기",
                modifier = Modifier.clickable(onClick = onClose),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = DawnTextSubtle
            )
        }

        WorkproofKeyValueRow(label = "출근", value = timeParts.first)
        WorkproofKeyValueRow(label = "퇴근", value = timeParts.second)
        HorizontalDivider(color = WorkproofDivider)

        WorkproofEditSheetSection(title = "수정 사유") {
            Box(modifier = Modifier.fillMaxWidth()) {
                WorkproofSelectionField(
                    value = if (selectedReasonLabel.isBlank()) {
                        "선택하세요"
                    } else {
                        selectedReasonLabel
                    },
                    isPlaceholder = selectedReasonLabel.isBlank(),
                    onClick = onToggleReasonMenu
                )
                DropdownMenu(
                    expanded = reasonMenuExpanded,
                    onDismissRequest = onDismissReasonMenu,
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    WorkproofEditReasons.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option.label) },
                            onClick = { onSelectReason(option) }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = WorkproofDivider)

        WorkproofEditSheetSection(title = "메모") {
            OutlinedTextField(
                value = memo,
                onValueChange = onMemoChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = {
                    Text(
                        text = "추가 메모(선택)",
                        color = DawnTextSubtle
                    )
                }
            )
        }

        HorizontalDivider(color = WorkproofDivider)

        WorkproofEditSheetSection(title = "첨부") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecondaryActionButton(
                    text = if (selectedAttachmentName == null) {
                        "파일 선택"
                    } else {
                        "다시 선택"
                    },
                    onClick = onPickAttachment,
                    modifier = Modifier.weight(1f)
                )
                if (selectedAttachmentName != null) {
                    SecondaryActionButton(
                        text = "제거",
                        onClick = onClearAttachment,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(
                text = selectedAttachmentName ?: "선택된 파일 없음",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selectedAttachmentName == null) DawnTextSubtle else DawnText
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        PrimaryActionButton(
            text = "저장",
            onClick = onSave,
            enabled = selectedReasonLabel.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun WorkproofEditSheetSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = DawnText
        )
        content()
    }
}

@Composable
private fun WorkproofCalendarCell(
    uiModel: WorkproofCalendarCellUiModel,
    modifier: Modifier = Modifier
) {
    if (uiModel.dayLabel.isBlank() || uiModel.tone == null) {
        Box(modifier = modifier.aspectRatio(1f))
        return
    }

    val background = when (uiModel.tone) {
        WorkproofCalendarTone.MISSING -> WorkproofMissingBackground
        WorkproofCalendarTone.PARTIAL -> WorkproofPartialBackground
        WorkproofCalendarTone.COMPLETE -> WorkproofCompleteBackground
        WorkproofCalendarTone.MODIFIED -> WorkproofModifiedBackground
    }
    val border = when (uiModel.tone) {
        WorkproofCalendarTone.MISSING -> WorkproofMissingBorder
        WorkproofCalendarTone.PARTIAL -> WorkproofPartialBorder
        WorkproofCalendarTone.COMPLETE -> WorkproofCompleteBorder
        WorkproofCalendarTone.MODIFIED -> WorkproofModifiedBorder
    }
    val textColor = when (uiModel.tone) {
        WorkproofCalendarTone.MISSING -> WorkproofMissingText
        WorkproofCalendarTone.PARTIAL -> WorkproofPartialText
        WorkproofCalendarTone.COMPLETE -> WorkproofCompleteText
        WorkproofCalendarTone.MODIFIED -> WorkproofModifiedText
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(
                width = if (uiModel.isCurrent) 2.dp else 1.dp,
                color = if (uiModel.isCurrent) DawnPrimary else border,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = uiModel.dayLabel,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
            color = textColor
        )
    }
}

@Composable
private fun WorkproofLegendItem(
    label: String,
    tone: WorkproofCalendarTone
) {
    val color = when (tone) {
        WorkproofCalendarTone.MISSING -> WorkproofMissingBackground
        WorkproofCalendarTone.PARTIAL -> WorkproofPartialBackground
        WorkproofCalendarTone.COMPLETE -> WorkproofCompleteBackground
        WorkproofCalendarTone.MODIFIED -> WorkproofModifiedBackground
    }
    val border = when (tone) {
        WorkproofCalendarTone.MISSING -> WorkproofMissingBorder
        WorkproofCalendarTone.PARTIAL -> WorkproofPartialBorder
        WorkproofCalendarTone.COMPLETE -> WorkproofCompleteBorder
        WorkproofCalendarTone.MODIFIED -> WorkproofModifiedBorder
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
                .border(1.dp, border, RoundedCornerShape(3.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = DawnTextSubtle
        )
    }
}

@Composable
private fun WorkproofStatusPill(
    text: String,
    tone: WorkproofRecordTone
) {
    val background = when (tone) {
        WorkproofRecordTone.DEFAULT -> DawnSurfaceAlt
        WorkproofRecordTone.ACTIVE -> Color(0xFFFFF4DD)
        WorkproofRecordTone.MODIFIED -> Color(0xFFFFE6EA)
    }
    val color = when (tone) {
        WorkproofRecordTone.DEFAULT -> DawnPrimaryDeep
        WorkproofRecordTone.ACTIVE -> WorkproofPartialText
        WorkproofRecordTone.MODIFIED -> WorkproofModifiedText
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, DawnBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color.copy(alpha = 0.2f))
                    .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = color
            )
        }
    }
}

private fun buildWorkproofCalendarCells(
    displayedMonth: YearMonth,
    baseMonth: YearMonth,
    currentDay: Int,
    dayTones: Map<Int, WorkproofCalendarTone>
): List<WorkproofCalendarCellUiModel> {
    val firstWeekdayOffset = displayedMonth.atDay(1).dayOfWeek.value % 7

    return buildList {
        repeat(firstWeekdayOffset) {
            add(WorkproofCalendarCellUiModel(dayLabel = "", tone = null, isCurrent = false))
        }

        val isBaseMonth = displayedMonth == baseMonth
        for (day in 1..displayedMonth.lengthOfMonth()) {
            add(
                WorkproofCalendarCellUiModel(
                    dayLabel = day.toString(),
                    tone = if (isBaseMonth) {
                        dayTones[day] ?: WorkproofCalendarTone.MISSING
                    } else {
                        WorkproofCalendarTone.MISSING
                    },
                    isCurrent = isBaseMonth && day == currentDay
                )
            )
        }

        while (size % 7 != 0) {
            add(WorkproofCalendarCellUiModel(dayLabel = "", tone = null, isCurrent = false))
        }
    }
}

private fun formatMonthText(month: YearMonth): String {
    val year = month.year
    val monthValue = month.monthValue.toString().padStart(2, '0')
    return "$year.$monthValue"
}

private fun formatRecordTimeLine(timeText: String): String {
    val parts = timeText.split(" - ")
    if (parts.size != 2) return timeText
    return "출근 ${parts[0]} / 퇴근 ${parts[1]}"
}

private fun resolveAttachmentName(context: Context, uri: Uri): String {
    val cursor = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && it.moveToFirst()) {
            return it.getString(nameIndex)
        }
    }
    return uri.lastPathSegment ?: "선택한 첨부"
}

private data class WorkproofEditReasonOption(
    val key: String,
    val label: String
)

private val WorkproofEditReasons = listOf(
    WorkproofEditReasonOption(
        key = "late_tap",
        label = "출근/퇴근 탭을 늦게 눌렀어요"
    ),
    WorkproofEditReasonOption(
        key = "overtime",
        label = "연장근무가 있었어요"
    ),
    WorkproofEditReasonOption(
        key = "break",
        label = "휴게시간이 달랐어요"
    ),
    WorkproofEditReasonOption(
        key = "missing",
        label = "기록이 누락됐어요"
    ),
    WorkproofEditReasonOption(
        key = "other",
        label = "기타(직접 입력)"
    )
)
