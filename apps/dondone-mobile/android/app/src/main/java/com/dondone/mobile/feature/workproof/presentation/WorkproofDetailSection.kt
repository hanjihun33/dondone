package com.dondone.mobile.feature.workproof.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dondone.mobile.R
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimaryDeep
import com.dondone.mobile.core.designsystem.DawnSurfaceAlt
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SecondaryActionButton
import com.dondone.mobile.core.designsystem.pressableScale
import com.dondone.mobile.core.designsystem.rememberDonDoneGrayRipple

@Composable
internal fun WorkproofDetailPage(
    displayedMonthText: String,
    calendarCountText: String,
    calendarCells: List<WorkproofCalendarCellUiModel>,
    weekdays: List<String>,
    records: List<WorkproofRecordUiModel>,
    auditItems: List<WorkproofAuditUiModel>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onBack: () -> Unit,
    onEditRecord: (WorkproofRecordUiModel) -> Unit,
    onOpenPdfCreation: () -> Unit
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
            onEditRecord = onEditRecord,
            onOpenPdfCreation = onOpenPdfCreation
        )
        WorkproofSectionDivider()
        WorkproofAuditCard(
            auditItems = auditItems
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
    onEditRecord: (WorkproofRecordUiModel) -> Unit,
    onOpenPdfCreation: () -> Unit
) {
    WorkproofSurfaceCard {
        WorkproofSectionHeader(
            title = "최근 기록",
            trailing = {
                WorkproofDocumentCreateHeaderAction(onClick = onOpenPdfCreation)
            }
        )

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
private fun WorkproofDocumentCreateHeaderAction(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DawnSurfaceAlt)
            .border(1.dp, DawnBorder, RoundedCornerShape(14.dp))
            .pressableScale(
                interactionSource = interactionSource,
                pressedScale = 0.985f
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberDonDoneGrayRipple(),
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = DawnPrimaryDeep,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = "문서 생성",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = DawnText
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = DawnTextSubtle,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun WorkproofAuditCard(
    auditItems: List<WorkproofAuditUiModel>
) {
    WorkproofSurfaceCard {
        WorkproofSectionHeader(title = "변경 기록")

        if (auditItems.isEmpty()) {
            WorkproofAuditPreviewRow(
                uiModel = WorkproofAuditUiModel(
                    dateText = "",
                    changeText = stringResource(R.string.workproof_empty_audit_title),
                    attachmentCount = 0,
                    reasonText = stringResource(R.string.workproof_empty_audit_description)
                )
            )
        } else {
            auditItems.forEachIndexed { index, audit ->
                if (index > 0) {
                    HorizontalDivider(color = WorkproofDivider)
                }
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
                text = formatAttachmentText(record.attachmentCount),
                style = MaterialTheme.typography.bodySmall,
                color = DawnTextSubtle
            )
            record.detailText?.let { reason ->
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnTextSubtle
                )
            }
        }
        WorkproofInlineActionButton(
            text = stringResource(R.string.workproof_action_edit),
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
            if (uiModel.attachmentCount > 0) {
                Text(
                    text = formatAttachmentText(uiModel.attachmentCount),
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
internal fun WorkproofSurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
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
internal fun WorkproofSectionHeader(
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
internal fun WorkproofKeyValueRow(
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
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(32.dp)
            .pressableScale(
                interactionSource = interactionSource,
                pressedScale = 0.94f
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberDonDoneGrayRipple(bounded = false),
                onClick = onClick
            ),
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
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, DawnBorder, RoundedCornerShape(12.dp))
            .pressableScale(
                interactionSource = interactionSource,
                pressedScale = 0.98f
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberDonDoneGrayRipple(),
                onClick = onClick
            )
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
internal fun WorkproofSelectionField(
    value: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, DawnBorder, RoundedCornerShape(18.dp))
            .pressableScale(
                interactionSource = interactionSource,
                pressedScale = 0.99f
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberDonDoneGrayRipple(),
                onClick = onClick
            )
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
internal fun WorkproofEditSheet(
    record: WorkproofRecordUiModel,
    selectedReasonLabel: String,
    reasonMenuExpanded: Boolean,
    memo: String,
    requestedClockInText: String,
    requestedClockOutText: String,
    selectedAttachmentName: String?,
    onToggleReasonMenu: () -> Unit,
    onDismissReasonMenu: () -> Unit,
    onSelectReason: (WorkproofEditReasonOption) -> Unit,
    onMemoChange: (String) -> Unit,
    onRequestedClockInChange: (String) -> Unit,
    onRequestedClockOutChange: (String) -> Unit,
    onPickAttachment: () -> Unit,
    onClearAttachment: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit
) {
    val isTimeInputValid = remember(requestedClockInText, requestedClockOutText) {
        requestedClockInText.isValidWorkproofTimeInput() &&
            requestedClockOutText.isValidWorkproofTimeInput()
    }
    val timeParts = remember(record.clockInText, record.clockOutText) {
        record.clockInText to record.clockOutText
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "닫기",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    color = DawnTextSubtle
                )
            }
        }

        WorkproofKeyValueRow(label = "출근", value = timeParts.first)
        WorkproofKeyValueRow(label = "퇴근", value = timeParts.second)
        HorizontalDivider(color = WorkproofDivider)
        WorkproofEditSheetSection(title = "요청 시간") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = requestedClockInText,
                    onValueChange = onRequestedClockInChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(text = "출근") },
                    placeholder = { Text(text = "09:00") }
                )
                OutlinedTextField(
                    value = requestedClockOutText,
                    onValueChange = onRequestedClockOutChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(text = "퇴근") },
                    placeholder = { Text(text = "18:00") }
                )
            }
            if (!isTimeInputValid && (requestedClockInText.isNotBlank() || requestedClockOutText.isNotBlank())) {
                Text(
                    text = "요청 시간은 HH:mm 형식으로 입력해 주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WorkproofModifiedText
                )
            }
        }
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
            text = "요청 보내기",
            onClick = onSave,
            enabled = selectedReasonLabel.isNotBlank() && isTimeInputValid,
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
    if (uiModel.dayLabel.isBlank()) {
        Box(modifier = modifier.aspectRatio(1f))
        return
    }

    val background = when (uiModel.tone) {
        null,
        WorkproofCalendarTone.UNAVAILABLE -> Color.Transparent
        WorkproofCalendarTone.MISSING -> WorkproofMissingBackground
        WorkproofCalendarTone.PARTIAL -> WorkproofPartialBackground
        WorkproofCalendarTone.COMPLETE -> WorkproofCompleteBackground
        WorkproofCalendarTone.MODIFIED -> WorkproofModifiedBackground
    }
    val border = when (uiModel.tone) {
        null,
        WorkproofCalendarTone.UNAVAILABLE -> Color.Transparent
        WorkproofCalendarTone.MISSING -> WorkproofMissingBorder
        WorkproofCalendarTone.PARTIAL -> WorkproofPartialBorder
        WorkproofCalendarTone.COMPLETE -> WorkproofCompleteBorder
        WorkproofCalendarTone.MODIFIED -> WorkproofModifiedBorder
    }
    val textColor = when (uiModel.tone) {
        null,
        WorkproofCalendarTone.UNAVAILABLE -> WorkproofInactiveText
        WorkproofCalendarTone.MISSING -> WorkproofMissingText
        WorkproofCalendarTone.PARTIAL -> WorkproofPartialText
        WorkproofCalendarTone.COMPLETE -> WorkproofCompleteText
        WorkproofCalendarTone.MODIFIED -> WorkproofModifiedText
    }

    val isRecordedDay = isRecordedCalendarTone(uiModel.tone)
    val cellShape = if (isRecordedDay) CircleShape else RoundedCornerShape(999.dp)

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(cellShape)
                .background(background)
                .then(
                    if (uiModel.isCurrent || isRecordedDay) {
                        Modifier.border(
                            width = if (uiModel.isCurrent) 2.dp else 1.dp,
                            color = if (uiModel.isCurrent) WorkproofCurrentDayBorder else border,
                            shape = cellShape
                        )
                    } else {
                        Modifier
                    }
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
}

@Composable
private fun WorkproofLegendItem(
    label: String,
    tone: WorkproofCalendarTone
) {
    val color = when (tone) {
        WorkproofCalendarTone.UNAVAILABLE -> Color.Transparent
        WorkproofCalendarTone.MISSING -> WorkproofMissingBackground
        WorkproofCalendarTone.PARTIAL -> WorkproofPartialBackground
        WorkproofCalendarTone.COMPLETE -> WorkproofCompleteBackground
        WorkproofCalendarTone.MODIFIED -> WorkproofModifiedBackground
    }
    val border = when (tone) {
        WorkproofCalendarTone.UNAVAILABLE -> Color.Transparent
        WorkproofCalendarTone.MISSING -> WorkproofGhostBorder
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
                .clip(if (tone == WorkproofCalendarTone.MISSING || tone == WorkproofCalendarTone.UNAVAILABLE) RoundedCornerShape(999.dp) else CircleShape)
                .background(color)
                .border(
                    width = if (tone == WorkproofCalendarTone.MISSING || tone == WorkproofCalendarTone.UNAVAILABLE) 1.dp else 0.dp,
                    color = border,
                    shape = if (tone == WorkproofCalendarTone.MISSING || tone == WorkproofCalendarTone.UNAVAILABLE) RoundedCornerShape(999.dp) else CircleShape
                )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = DawnTextSubtle
        )
    }
}

internal fun isRecordedCalendarTone(tone: WorkproofCalendarTone?): Boolean {
    return tone == WorkproofCalendarTone.PARTIAL ||
        tone == WorkproofCalendarTone.COMPLETE ||
        tone == WorkproofCalendarTone.MODIFIED
}

@Composable
private fun formatAttachmentText(attachmentCount: Int): String {
    return if (attachmentCount > 0) {
        stringResource(R.string.workproof_value_attachment_count, attachmentCount)
    } else {
        stringResource(R.string.workproof_value_attachment_none)
    }
}

@Composable
private fun formatRecordTimeLine(timeText: String): String {
    val parts = timeText.split(" - ")
    if (parts.size != 2) return timeText
    return stringResource(R.string.workproof_record_time_line, parts[0], parts[1])
}
