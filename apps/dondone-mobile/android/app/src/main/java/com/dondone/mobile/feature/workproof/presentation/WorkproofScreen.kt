package com.dondone.mobile.feature.workproof.presentation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.dondone.mobile.core.designsystem.SectionPanel
import java.time.YearMonth

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkproofScreen(
    uiModel: WorkproofUiModel,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit,
    onSaveEdit: (String, String, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val weekdays = listOf(
        "\uC77C",
        "\uC6D4",
        "\uD654",
        "\uC218",
        "\uBAA9",
        "\uAE08",
        "\uD1A0"
    )
    val baseMonth = remember(uiModel.calendarBaseYear, uiModel.calendarBaseMonth) {
        YearMonth.of(uiModel.calendarBaseYear, uiModel.calendarBaseMonth)
    }
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var editingRecordId by rememberSaveable { mutableStateOf<String?>(null) }
    var editReasonKey by rememberSaveable { mutableStateOf("") }
    var editMemo by rememberSaveable { mutableStateOf("") }
    var selectedAttachmentName by rememberSaveable { mutableStateOf<String?>(null) }
    var reasonMenuExpanded by remember { mutableStateOf(false) }
    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedAttachmentName = uri?.let { resolveAttachmentName(context, it) }
    }
    val displayedMonth = remember(baseMonth, monthOffset) {
        baseMonth.plusMonths(monthOffset.toLong())
    }
    val editingRecord = remember(uiModel.recentRecords, editingRecordId) {
        uiModel.recentRecords.firstOrNull { it.id == editingRecordId }
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
    val clearEditSheet = {
        editingRecordId = null
        editReasonKey = ""
        editMemo = ""
        selectedAttachmentName = null
        reasonMenuExpanded = false
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WorkproofPunchCard(
            uiModel = uiModel.summary,
            onClockIn = onClockIn,
            onClockOut = onClockOut
        )

        WorkproofCalendarCard(
            displayedMonthText = formatMonthText(displayedMonth),
            calendarCountText = "\uAE30\uB85D ${recordedDayCount}\uC77C",
            calendarCells = calendarCells,
            weekdays = weekdays,
            onPreviousMonth = { monthOffset -= 1 },
            onNextMonth = { monthOffset += 1 }
        )

        WorkproofRecentLogsCard(
            records = uiModel.recentRecords,
            onEditRecord = { record ->
                editingRecordId = record.id
                editReasonKey = ""
                editMemo = ""
                selectedAttachmentName = null
                reasonMenuExpanded = false
            }
        )
        WorkproofAuditCard(
            preview = uiModel.auditPreview,
            audits = uiModel.audits.drop(1)
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun WorkproofPunchCard(
    uiModel: WorkproofSummaryUiModel,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit
) {
    WorkproofSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "ONE TAP",
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnPrimaryDeep
                )
                Text(
                    text = "\uCD9C\uD1F4\uADFC \uC6D0\uD0ED \uAE30\uB85D",
                    style = MaterialTheme.typography.titleLarge,
                    color = DawnText
                )
                Text(
                    text = "\uAE30\uB85D \uC2DC\uAC04\uACFC \uC704\uCE58 \uC2A4\uB0C5\uC0F7\uC744 \uD55C \uBC88\uC5D0 \uC800\uC7A5\uD574\uC694.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(DawnSurfaceAlt),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkHistory,
                    contentDescription = null,
                    tint = DawnPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryActionButton(
                text = "\uCD9C\uADFC",
                onClick = onClockIn,
                enabled = uiModel.canClockIn,
                modifier = Modifier.weight(1f)
            )
            SecondaryActionButton(
                text = "\uD1F4\uADFC",
                onClick = onClockOut,
                enabled = uiModel.canClockOut,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WorkproofMiniStat(
                label = "\uADFC\uBB34\uC77C",
                value = uiModel.verifiedDaysText,
                modifier = Modifier.weight(1f)
            )
            WorkproofMiniStat(
                label = "\uC218\uC815",
                value = uiModel.auditCountText,
                modifier = Modifier.weight(1f)
            )
        }

        SectionPanel {
            Text(
                text = uiModel.todayMetaText,
                style = MaterialTheme.typography.labelLarge,
                color = DawnPrimaryDeep
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WorkproofTimePill(
                    label = "\uCD9C\uADFC",
                    value = uiModel.todayInText,
                    modifier = Modifier.weight(1f)
                )
                WorkproofTimePill(
                    label = "\uD1F4\uADFC",
                    value = uiModel.todayOutText,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = uiModel.todayImpactText,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnText
            )
        }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "\uADFC\uBB34 \uCE98\uB9B0\uB354",
                    style = MaterialTheme.typography.titleLarge,
                    color = DawnText
                )
                Text(
                    text = "\uAE30\uB85D \uC0C1\uD0DC\uB97C \uD55C \uB2EC \uB2E8\uC704\uB85C \uD655\uC778\uD560 \uC218 \uC788\uC5B4\uC694.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle
                )
            }
            Text(
                text = calendarCountText,
                style = MaterialTheme.typography.labelLarge,
                color = DawnTextSubtle
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkproofMonthButton(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "\uC774\uC804 \uB2EC",
                        tint = DawnTextSubtle
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "\uB2E4\uC74C \uB2EC",
                        tint = DawnTextSubtle
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
            WorkproofLegendItem(label = "\uBBF8\uAE30\uB85D", tone = WorkproofCalendarTone.MISSING)
            WorkproofLegendItem(label = "\uCD9C\uADFC\uB9CC", tone = WorkproofCalendarTone.PARTIAL)
            WorkproofLegendItem(label = "\uC644\uB8CC", tone = WorkproofCalendarTone.COMPLETE)
            WorkproofLegendItem(label = "\uC218\uC815", tone = WorkproofCalendarTone.MODIFIED)
        }
    }
}

@Composable
private fun WorkproofRecentLogsCard(
    records: List<WorkproofRecordUiModel>,
    onEditRecord: (WorkproofRecordUiModel) -> Unit
) {
    WorkproofSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\uCD5C\uADFC \uAE30\uB85D",
                style = MaterialTheme.typography.titleLarge,
                color = DawnText
            )
            Text(
                text = "\uC218\uC815 \uC0AC\uC720\uB97C \uB0A8\uACA8\uC8FC\uC138\uC694",
                style = MaterialTheme.typography.labelMedium,
                color = DawnTextSubtle
            )
        }

        records.forEach { record ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, DawnBorder, RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = DawnText
                        )
                    }
                    WorkproofInlineActionButton(
                        text = "\uC218\uC815",
                        onClick = { onEditRecord(record) }
                    )
                }

                record.modifiedHintText?.let { hint ->
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = DawnTextSubtle
                    )
                }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "\uBCC0\uACBD \uAE30\uB85D",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = DawnText
                )
                Text(
                    text = "\uBCC0\uACBD \uC774\uB825\uC740 \uBCF4\uAD00\uB418\uBA70 Proof Pack\uACFC \uADFC\uAC70 \uC790\uB8CC \uBB36\uC74C\uC5D0 \uD568\uAED8 \uD3EC\uD568\uB3FC\uC694.",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    color = DawnTextSubtle
                )
            }
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = DawnPrimaryDeep,
                modifier = Modifier.size(22.dp)
            )
        }

        if (preview != null) {
            WorkproofAuditPreviewBox(uiModel = preview)
        } else {
            WorkproofAuditPreviewBox(
                uiModel = WorkproofAuditUiModel(
                    changeText = "\uC544\uC9C1 \uC313\uC778 \uBCC0\uACBD \uAE30\uB85D\uC774 \uC5C6\uC5B4\uC694.",
                    reasonText = "\uC218\uC815\uC774 \uC0DD\uAE30\uBA74 \uC0AC\uC720\uC640 \uCCA8\uBD80 \uC5EC\uBD80\uAC00 \uC5EC\uAE30\uC5D0 \uBA3C\uC800 \uD45C\uC2DC\uB3FC\uC694.",
                    metaText = "\uD604\uC7AC \uBBF8\uB9AC\uBCF4\uAE30 \uD56D\uBAA9 \uC5C6\uC74C"
                )
            )
        }

        if (audits.isNotEmpty()) {
            Text(
                text = "\uCD5C\uADFC \uBCC0\uACBD ${audits.size + if (preview != null) 1 else 0}\uAC74",
                style = MaterialTheme.typography.labelMedium,
                color = DawnTextSubtle
            )
        }
    }
}

@Composable
private fun WorkproofAuditPreviewBox(
    uiModel: WorkproofAuditUiModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE7EDF5), RoundedCornerShape(16.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = uiModel.changeText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF334155)
        )
        Text(
            text = uiModel.reasonText,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
        )
        Text(
            text = uiModel.metaText,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
private fun WorkproofSurfaceCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = DawnSurface),
        border = BorderStroke(1.dp, DawnBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFFCFAFF)
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
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
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DawnSurfaceAlt)
            .border(1.dp, DawnBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun WorkproofMiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, DawnBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = DawnTextSubtle
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = DawnText
        )
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
            text = "\u25BE",
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "WORKPROOF EDIT",
                style = MaterialTheme.typography.labelMedium,
                color = DawnPrimaryDeep
            )
            Text(
                text = "\uADFC\uBB34 \uC2DC\uAC04 \uC218\uC815",
                style = MaterialTheme.typography.titleLarge,
                color = DawnText
            )
            Text(
                text = "${record.dateText} / ${formatRecordTimeLine(record.timeText)}",
                style = MaterialTheme.typography.bodySmall,
                color = DawnTextSubtle
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "\uC218\uC815 \uC0AC\uC720",
                style = MaterialTheme.typography.labelLarge,
                color = DawnText
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                WorkproofSelectionField(
                    value = if (selectedReasonLabel.isBlank()) {
                        "\uC120\uD0DD\uD558\uC138\uC694"
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

        OutlinedTextField(
            value = memo,
            onValueChange = onMemoChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = {
                Text(
                    text = "\uCD94\uAC00 \uBA54\uBAA8(\uC120\uD0DD)",
                    color = DawnTextSubtle
                )
            }
        )

        SectionPanel {
            Text(
                text = "\uCD94\uAC00 \uC99D\uAC70(\uC120\uD0DD)",
                style = MaterialTheme.typography.labelLarge,
                color = DawnText
            )
            Text(
                text = "\uC0AC\uC9C4/\uBA54\uBAA8 \uCCA8\uBD80 \uAC00\uB2A5 / \uCCA8\uBD80 \uC5C6\uC74C\uB3C4 \uAE30\uB85D\uB429\uB2C8\uB2E4.",
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecondaryActionButton(
                    text = if (selectedAttachmentName == null) {
                        "\uD30C\uC77C \uC120\uD0DD"
                    } else {
                        "\uB2E4\uC2DC \uC120\uD0DD"
                    },
                    onClick = onPickAttachment,
                    modifier = Modifier.weight(1f)
                )
                if (selectedAttachmentName != null) {
                    SecondaryActionButton(
                        text = "\uC81C\uAC70",
                        onClick = onClearAttachment,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(
                text = selectedAttachmentName ?: "\uC120\uD0DD\uB41C \uD30C\uC77C \uC5C6\uC74C",
                style = MaterialTheme.typography.bodyMedium,
                color = DawnText
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryActionButton(
                text = "\uC800\uC7A5",
                onClick = onSave,
                enabled = selectedReasonLabel.isNotBlank(),
                modifier = Modifier.weight(1f)
            )
            SecondaryActionButton(
                text = "\uB2EB\uAE30",
                onClick = onClose,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun WorkproofTimePill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, DawnBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = DawnTextSubtle
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = DawnText
        )
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
    return "\uCD9C\uADFC ${parts[0]} / \uD1F4\uADFC ${parts[1]}"
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
    return uri.lastPathSegment ?: "\uC120\uD0DD\uD55C \uCCA8\uBD80"
}

private data class WorkproofEditReasonOption(
    val key: String,
    val label: String
)

private val WorkproofEditReasons = listOf(
    WorkproofEditReasonOption(
        key = "late_tap",
        label = "\uCD9C\uADFC/\uD1F4\uADFC \uD0ED\uC744 \uB2A6\uAC8C \uB20C\uB800\uC5B4\uC694"
    ),
    WorkproofEditReasonOption(
        key = "overtime",
        label = "\uC5F0\uC7A5\uADFC\uBB34\uAC00 \uC788\uC5C8\uC5B4\uC694"
    ),
    WorkproofEditReasonOption(
        key = "break",
        label = "\uD734\uAC8C\uC2DC\uAC04\uC774 \uB2EC\uB790\uC5B4\uC694"
    ),
    WorkproofEditReasonOption(
        key = "missing",
        label = "\uAE30\uB85D\uC774 \uB204\uB77D\uB410\uC5B4\uC694"
    ),
    WorkproofEditReasonOption(
        key = "other",
        label = "\uAE30\uD0C0(\uC9C1\uC811 \uC785\uB825)"
    )
)
