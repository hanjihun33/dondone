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
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.AppTextKeys
import com.dondone.mobile.core.i18n.LocalAppLanguage
import com.dondone.mobile.core.i18n.text
import com.dondone.mobile.core.i18n.translate
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
    val language = LocalAppLanguage.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
            text = language.text("workproof_generate_pdf"),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
        Text(
            text = language.text("workproof_select_period_first"),
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextSubtle
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WorkproofPdfPresetChip(
                label = language.text("workproof_this_month"),
                selected = selectedPreset == WorkproofPdfDatePreset.THIS_MONTH,
                onClick = { onSelectPreset(WorkproofPdfDatePreset.THIS_MONTH) }
            )
            WorkproofPdfPresetChip(
                label = language.text("workproof_last_month"),
                selected = selectedPreset == WorkproofPdfDatePreset.LAST_MONTH,
                onClick = { onSelectPreset(WorkproofPdfDatePreset.LAST_MONTH) }
            )
            WorkproofPdfPresetChip(
                label = language.text("workproof_custom_range"),
                selected = selectedPreset == WorkproofPdfDatePreset.CUSTOM,
                onClick = { onSelectPreset(WorkproofPdfDatePreset.CUSTOM) }
            )
        }

        WorkproofPdfDateField(
            label = language.text("workproof_start_date"),
            value = formatWorkproofPdfDate(startDate),
            onClick = onOpenStartDatePicker
        )
        WorkproofPdfDateField(
            label = language.text("workproof_end_date"),
            value = formatWorkproofPdfDate(endDate),
            onClick = onOpenEndDatePicker
        )

        if (!isDateRangeValid) {
            Text(
                text = language.text("workproof_end_date_invalid"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        when {
            previewUiState.isLoading -> {
                DonDoneLoadingPanel(
                    title = language.text("workproof_loading_preview"),
                    message = language.text("workproof_loading_preview_message"),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            previewUiState.errorMessage != null -> {
                DonDoneErrorPanel(
                    title = language.text("workproof_preview_failed"),
                    message = previewUiState.errorMessage,
                    actionLabel = language.text("workproof_select_period_again"),
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
                title = language.text("workproof_failed_to_submit_pdf_request"),
                message = createUiState.errorMessage,
                actionLabel = language.text("workproof_try_again"),
                onAction = onGenerate,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (createUiState.isSubmitting) {
            DonDoneLoadingPanel(
                title = language.text("workproof_submitting_pdf_request"),
                message = language.text("workproof_submitting_pdf_request_message"),
                modifier = Modifier.fillMaxWidth()
            )
        }

        SecondaryActionButton(
            text = if (createUiState.isSubmitting) {
                language.text("workproof_submitting_pdf_request_in_progress")
            } else {
                language.text("workproof_generate_pdf")
            },
            onClick = onGenerate,
            enabled = isDateRangeValid && !createUiState.isSubmitting && previewUiState.preview != null,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = language.text("workproof_selected_period_summary_notice"),
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
    val language = LocalAppLanguage.current
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
            text = language.text("workproof_document_preview"),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
        WorkproofKeyValueRow(
            label = language.text("workproof_workplace"),
            value = preview.workplaceName
        )
        WorkproofKeyValueRow(
            label = language.text("workproof_selected_period"),
            value = preview.periodText
        )
        WorkproofKeyValueRow(
            label = language.text("workproof_record_count"),
            value = translateWorkproofPreviewValue(preview.totalRecordCountText, language)
        )
        WorkproofKeyValueRow(
            label = language.text("workproof_edited_records"),
            value = translateWorkproofPreviewValue(preview.editedCountText, language)
        )
        WorkproofKeyValueRow(
            label = language.text("workproof_attachment_count"),
            value = translateWorkproofPreviewValue(preview.attachmentCountText, language)
        )
        WorkproofKeyValueRow(
            label = language.text("workproof_total_work_hours"),
            value = translateWorkproofPreviewValue(preview.totalWorkedHoursText, language)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = language.text("workproof_document_includes"),
                style = MaterialTheme.typography.labelLarge,
                color = DawnTextSubtle
            )
            Text(
                text = translateWorkproofPreviewValue(preview.sectionSummaryText, language),
                style = MaterialTheme.typography.bodyMedium,
                color = DawnText
            )
        }
    }
}

private fun translateWorkproofPreviewValue(
    raw: String,
    language: AppLanguage
): String {
    if (language == AppLanguage.KOREAN) {
        return raw
    }

    val count = Regex("""^(\d+)건$""").matchEntire(raw.trim())?.groupValues?.get(1)?.toIntOrNull()
    val hoursAndMinutes = Regex("""^(\d+)시간\s+(\d+)분$""").matchEntire(raw.trim())
    val hoursOnly = Regex("""^(\d+)시간$""").matchEntire(raw.trim())?.groupValues?.get(1)?.toIntOrNull()
    val minutesOnly = Regex("""^(\d+)분$""").matchEntire(raw.trim())?.groupValues?.get(1)?.toIntOrNull()
    return when {
        count != null -> language.text("workproof_item_count", count)
        hoursAndMinutes != null -> language.text(
            "workproof_hours_minutes_format",
            hoursAndMinutes.groupValues[1].toInt(),
            hoursAndMinutes.groupValues[2].toInt()
        )
        hoursOnly != null -> language.text("workproof_hours_only_format", hoursOnly)
        minutesOnly != null -> language.text("workproof_minutes_only_format", minutesOnly)
        raw == "출퇴근 기록, 수정 이력, 기간 요약" -> language.text("workproof_section_summary_value")
        else -> language.translate(raw)
    }
}

@Composable
private fun WorkproofPdfPresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val language = LocalAppLanguage.current
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
            text = language.translate(label),
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
    val language = LocalAppLanguage.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = language.translate(label),
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
    val language = LocalAppLanguage.current
    val isActionable = createUiState.isActionable
    val isFailed = createUiState.isFailed
    val title = when {
        isFailed -> language.text("workproof_pdf_generation_failed")
        isActionable -> language.text("workproof_pdf_ready_to_open")
        else -> language.text("workproof_pdf_request_submitted")
    }
    val description = when {
        isFailed -> createUiState.errorMessage ?: language.text("workproof_document_generation_failed_message")
        isActionable -> language.text("workproof_open_or_share_message")
        else -> language.text("workproof_preparing_document_message")
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
            WorkproofKeyValueRow(label = language.text("workproof_selected_period"), value = periodText)
            WorkproofKeyValueRow(label = language.text("workproof_file_name"), value = fileName)
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
                text = language.text("workproof_open_pdf"),
                onClick = onOpen,
                enabled = isActionable,
                modifier = Modifier.weight(1f)
            )
            SecondaryActionButton(
                text = language.text("workproof_share_pdf"),
                onClick = onShare,
                enabled = isActionable,
                modifier = Modifier.weight(1f)
            )
        }
        SecondaryActionButton(
            text = language.text(AppTextKeys.CLOSE),
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
    uri: Uri,
    language: AppLanguage
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
        Toast.makeText(context, language.text("workproof_unable_to_open_pdf"), Toast.LENGTH_SHORT).show()
    }
}

internal fun shareWorkproofPdfFile(
    context: Context,
    uri: Uri,
    fileName: String?,
    language: AppLanguage
) {
    runCatching {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TITLE, fileName ?: language.text("workproof_work_record_document"))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                language.text("workproof_share_work_record_document")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }.onFailure {
        Toast.makeText(context, language.text("workproof_unable_to_share_pdf"), Toast.LENGTH_SHORT).show()
    }
}

internal enum class WorkproofPdfDatePreset {
    THIS_MONTH,
    LAST_MONTH,
    CUSTOM
}
