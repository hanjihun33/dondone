package com.dondone.mobile.feature.workproof.presentation

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnSurfaceAlt
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.DonDoneErrorPanel
import com.dondone.mobile.core.designsystem.DonDoneLoadingPanel
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SecondaryActionButton
import java.time.LocalDate

@Composable
internal fun WorkproofPdfDateRangeSheet(
    selectedPreset: WorkproofPdfDatePreset,
    startDate: LocalDate,
    endDate: LocalDate,
    isDateRangeValid: Boolean,
    previewUiState: WorkproofPdfPreviewUiState,
    createUiState: WorkproofPdfCreateUiState,
    onSelectPreset: (WorkproofPdfDatePreset) -> Unit,
    onOpenStartDatePicker: () -> Unit,
    onOpenEndDatePicker: () -> Unit,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "근무 기록 PDF 생성",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
        Text(
            text = "문서로 정리할 기간을 먼저 선택해 주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextSubtle
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WorkproofPdfPresetChip(
                label = "이번 달",
                selected = selectedPreset == WorkproofPdfDatePreset.THIS_MONTH,
                onClick = { onSelectPreset(WorkproofPdfDatePreset.THIS_MONTH) }
            )
            WorkproofPdfPresetChip(
                label = "지난 달",
                selected = selectedPreset == WorkproofPdfDatePreset.LAST_MONTH,
                onClick = { onSelectPreset(WorkproofPdfDatePreset.LAST_MONTH) }
            )
            WorkproofPdfPresetChip(
                label = "직접 선택",
                selected = selectedPreset == WorkproofPdfDatePreset.CUSTOM,
                onClick = { onSelectPreset(WorkproofPdfDatePreset.CUSTOM) }
            )
        }

        WorkproofPdfDateField(
            label = "시작일",
            value = formatWorkproofPdfDate(startDate),
            onClick = onOpenStartDatePicker
        )
        WorkproofPdfDateField(
            label = "종료일",
            value = formatWorkproofPdfDate(endDate),
            onClick = onOpenEndDatePicker
        )

        if (!isDateRangeValid) {
            Text(
                text = "종료일은 시작일보다 빠를 수 없어요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        when {
            previewUiState.isLoading -> {
                DonDoneLoadingPanel(
                    title = "미리보기 불러오는 중",
                    message = "선택한 기간의 근무 기록 요약을 확인하고 있어요.",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            previewUiState.errorMessage != null -> {
                DonDoneErrorPanel(
                    title = "미리보기를 불러오지 못했어요",
                    message = previewUiState.errorMessage,
                    actionLabel = "기간 다시 선택",
                    onAction = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            previewUiState.preview != null -> {
                WorkproofPdfPreviewCard(preview = previewUiState.preview)
            }
        }

        if (createUiState.errorMessage != null) {
            DonDoneErrorPanel(
                title = "문서 생성 요청을 접수하지 못했어요",
                message = createUiState.errorMessage,
                actionLabel = "다시 시도",
                onAction = onGenerate,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (createUiState.isSubmitting) {
            DonDoneLoadingPanel(
                title = "PDF 생성 요청 접수 중",
                message = "선택한 기간의 근무 기록 문서 생성을 요청하고 있어요.",
                modifier = Modifier.fillMaxWidth()
            )
        }

        SecondaryActionButton(
            text = if (createUiState.isSubmitting) "PDF 생성 요청 중..." else "PDF 생성",
            onClick = onGenerate,
            enabled = isDateRangeValid && !createUiState.isSubmitting && previewUiState.preview != null,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "선택한 기간 기준 실제 근무 기록 요약을 확인한 뒤 문서를 생성할 수 있어요.",
            style = MaterialTheme.typography.bodySmall,
            color = DawnTextSubtle
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun WorkproofPdfPreviewCard(
    preview: WorkproofPdfPreviewUiModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DawnSurfaceAlt)
            .border(1.dp, DawnBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "문서 미리보기",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
        WorkproofKeyValueRow(
            label = "근무지",
            value = preview.workplaceName
        )
        WorkproofKeyValueRow(
            label = "선택 기간",
            value = preview.periodText
        )
        WorkproofKeyValueRow(
            label = "기록 수",
            value = preview.totalRecordCountText
        )
        WorkproofKeyValueRow(
            label = "수정 기록",
            value = preview.editedCountText
        )
        WorkproofKeyValueRow(
            label = "첨부 수",
            value = preview.attachmentCountText
        )
        WorkproofKeyValueRow(
            label = "총 근무시간",
            value = preview.totalWorkedHoursText
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "문서 포함 항목",
                style = MaterialTheme.typography.labelLarge,
                color = DawnTextSubtle
            )
            Text(
                text = preview.sectionSummaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnText
            )
        }
    }
}

@Composable
private fun WorkproofPdfPresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) WorkproofPdfPresetSelectedBackground else WorkproofCanvas)
            .border(
                width = 1.dp,
                color = if (selected) WorkproofPdfPresetSelectedBorder else DawnBorder,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (selected) WorkproofRowAccentTint else DawnTextSubtle
        )
    }
}

@Composable
private fun WorkproofPdfDateField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = DawnTextSubtle
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(WorkproofCanvas)
                .border(1.dp, DawnBorder, RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = DawnText
            )
        }
    }
}

@Composable
internal fun WorkproofPdfGenerationResultSheet(
    periodText: String,
    createUiState: WorkproofPdfCreateUiState,
    fileName: String,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val isActionable = createUiState.isActionable
    val isFailed = createUiState.isFailed
    val title = when {
        isFailed -> "PDF 생성 실패"
        isActionable -> "PDF 열기 준비 완료"
        else -> "PDF 생성 요청 완료"
    }
    val description = when {
        isFailed -> createUiState.errorMessage ?: "문서 생성에 실패했어요."
        isActionable -> "열기 또는 공유를 눌러 문서를 확인해 주세요."
        else -> "선택한 기간의 근무기록 문서를 준비하고 있어요."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(WorkproofPdfPresetSelectedBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isActionable -> Icons.Default.CheckCircle
                        isFailed -> Icons.Default.SyncAlt
                        else -> Icons.Default.Schedule
                    },
                    contentDescription = null,
                    tint = WorkproofRowAccentTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = DawnText
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DawnSurfaceAlt)
                .border(1.dp, DawnBorder, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WorkproofKeyValueRow(label = "선택 기간", value = periodText)
            WorkproofKeyValueRow(label = "파일명", value = fileName)
            if (createUiState.errorMessage != null) {
                Text(
                    text = createUiState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryActionButton(
                text = "열기",
                onClick = onOpen,
                enabled = isActionable,
                modifier = Modifier.weight(1f)
            )
            SecondaryActionButton(
                text = "공유",
                onClick = onShare,
                enabled = isActionable,
                modifier = Modifier.weight(1f)
            )
        }
        SecondaryActionButton(
            text = "닫기",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

internal fun showWorkproofDatePicker(
    context: Context,
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth
    ).show()
}

private fun formatWorkproofPdfDate(date: LocalDate): String = date.format(WorkproofPdfDateFormatter)

internal fun formatWorkproofPdfDateRange(startDate: LocalDate, endDate: LocalDate): String =
    "${formatWorkproofPdfDate(startDate)} - ${formatWorkproofPdfDate(endDate)}"

internal fun buildWorkproofPdfFileName(startDate: LocalDate, endDate: LocalDate): String =
    "workproof-${startDate.toString().replace("-", "")}-${endDate.toString().replace("-", "")}.pdf"

internal fun openWorkproofPdfFile(
    context: Context,
    uri: Uri
) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }.onFailure {
        Toast.makeText(context, "PDF를 열 수 없어요.", Toast.LENGTH_SHORT).show()
    }
}

internal fun shareWorkproofPdfFile(
    context: Context,
    uri: Uri,
    fileName: String?
) {
    runCatching {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TITLE, fileName ?: "근무 기록 문서")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "근무 기록 문서 공유"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }.onFailure {
        Toast.makeText(context, "PDF를 공유할 수 없어요.", Toast.LENGTH_SHORT).show()
    }
}

internal enum class WorkproofPdfDatePreset {
    THIS_MONTH,
    LAST_MONTH,
    CUSTOM
}
