package com.dondone.mobile.feature.workproof.presentation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dondone.mobile.app.session.WorkproofLaunchRequest
import com.dondone.mobile.app.session.WorkproofLaunchTarget
import com.dondone.mobile.R
import com.dondone.mobile.core.designsystem.DawnSurface
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.DonDoneNoticeBanner
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SecondaryActionButton
import com.dondone.mobile.core.designsystem.StatusBadge
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.LocalAppLanguage
import com.dondone.mobile.core.i18n.text
import com.dondone.mobile.core.i18n.translate
import java.time.LocalDate
import java.time.YearMonth


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkproofScreen(
    uiModel: WorkproofUiModel,
    pdfPreviewUiState: WorkproofPdfPreviewUiState,
    pdfCreateUiState: WorkproofPdfCreateUiState,
    pdfFileUiState: WorkproofPdfFileUiState,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit,
    onRefreshCurrentLocation: () -> Unit,
    onSaveEdit: (String, String, String, String, String, Boolean) -> Unit,
    onRefreshPdfPreview: (LocalDate, LocalDate) -> Unit,
    onClearPdfPreview: () -> Unit,
    onCreateWorkproofPdf: (LocalDate, LocalDate) -> Unit,
    onClearPdfCreateState: () -> Unit,
    onOpenWorkproofPdf: (Long) -> Unit,
    onShareWorkproofPdf: (Long) -> Unit,
    onClearPdfFileState: () -> Unit,
    launchRequest: WorkproofLaunchRequest?,
    onConsumeLaunchRequest: () -> Unit,
    resetVersion: Int,
    onDetailVisibilityChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val baseMonth = remember(uiModel.calendarBaseYear, uiModel.calendarBaseMonth) {
        YearMonth.of(uiModel.calendarBaseYear, uiModel.calendarBaseMonth)
    }
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var monthOffset by remember(resetVersion) { mutableIntStateOf(0) }
    var editingRecordId by remember(resetVersion) { mutableStateOf<String?>(null) }
    var editReasonKey by remember(resetVersion) { mutableStateOf("") }
    var editMemo by remember(resetVersion) { mutableStateOf("") }
    var requestedClockInText by remember(resetVersion) { mutableStateOf("") }
    var requestedClockOutText by remember(resetVersion) { mutableStateOf("") }
    var selectedAttachmentName by remember(resetVersion) { mutableStateOf<String?>(null) }
    var showDetails by remember(resetVersion) { mutableStateOf(false) }
    var showPdfDateRangeSheet by remember(resetVersion) { mutableStateOf(false) }
    var showPdfGenerationResultSheet by remember(resetVersion) { mutableStateOf(false) }
    var pdfDatePreset by remember(resetVersion) { mutableStateOf(WorkproofPdfDatePreset.THIS_MONTH) }
    var pdfStartDateEpochDay by remember(resetVersion) { mutableLongStateOf(baseMonth.atDay(1).toEpochDay()) }
    var pdfEndDateEpochDay by remember(resetVersion) { mutableLongStateOf(baseMonth.atEndOfMonth().toEpochDay()) }
    var reasonMenuExpanded by remember { mutableStateOf(false) }
    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedAttachmentName = uri?.let { resolveAttachmentName(context, it, language) }
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
    val recordedDayCount = calendarCells.count { cell ->
        isRecordedCalendarTone(cell.tone)
    }
    val displayedRecentRecords = remember(isBaseMonth, uiModel.recentRecords) {
        if (isBaseMonth) uiModel.recentRecords else emptyList()
    }
    val displayedAuditItems = remember(isBaseMonth, uiModel.audits) {
        if (isBaseMonth) uiModel.audits else emptyList()
    }
    val editingRecord = remember(displayedRecentRecords, editingRecordId) {
        displayedRecentRecords.firstOrNull { it.id == editingRecordId }
    }
    val pdfStartDate = remember(pdfStartDateEpochDay) { LocalDate.ofEpochDay(pdfStartDateEpochDay) }
    val pdfEndDate = remember(pdfEndDateEpochDay) { LocalDate.ofEpochDay(pdfEndDateEpochDay) }
    val isPdfDateRangeValid = remember(pdfStartDateEpochDay, pdfEndDateEpochDay) {
        pdfStartDateEpochDay <= pdfEndDateEpochDay
    }
    fun clearEditDraft() {
        editReasonKey = ""
        editMemo = ""
        requestedClockInText = ""
        requestedClockOutText = ""
        selectedAttachmentName = null
        reasonMenuExpanded = false
    }
    val resetEditDraft = {
        editingRecordId = null
        clearEditDraft()
    }
    fun openEditSheet(record: WorkproofRecordUiModel) {
        editingRecordId = record.id
        clearEditDraft()
        requestedClockInText = record.clockInText.takeUnless { it == "-" }.orEmpty()
        requestedClockOutText = record.clockOutText.takeUnless { it == "-" }.orEmpty()
    }

    LaunchedEffect(showDetails) {
        onDetailVisibilityChange(showDetails)
    }

    LaunchedEffect(showPdfDateRangeSheet, pdfStartDateEpochDay, pdfEndDateEpochDay, isPdfDateRangeValid) {
        if (!showPdfDateRangeSheet) {
            onClearPdfPreview()
            return@LaunchedEffect
        }
        if (isPdfDateRangeValid) {
            onRefreshPdfPreview(pdfStartDate, pdfEndDate)
        } else {
            onClearPdfPreview()
        }
    }

    LaunchedEffect(pdfCreateUiState.requestId) {
        if (pdfCreateUiState.requestId == null) {
            return@LaunchedEffect
        }
        showPdfDateRangeSheet = false
        showPdfGenerationResultSheet = true
    }

    LaunchedEffect(pdfFileUiState.fileUri, pdfFileUiState.pendingAction) {
        val fileUri = pdfFileUiState.fileUri ?: return@LaunchedEffect
        val action = pdfFileUiState.pendingAction ?: return@LaunchedEffect
        val uri = Uri.parse(fileUri)
        when (action) {
            WorkproofPdfFileAction.OPEN -> openWorkproofPdfFile(context, uri, language)
            WorkproofPdfFileAction.SHARE -> shareWorkproofPdfFile(context, uri, pdfFileUiState.fileName, language)
        }
        onClearPdfFileState()
    }

    LaunchedEffect(pdfFileUiState.errorMessage) {
        val message = pdfFileUiState.errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        onClearPdfFileState()
    }

    LaunchedEffect(resetVersion) {
        monthOffset = 0
        resetEditDraft()
        showDetails = false
        showPdfDateRangeSheet = false
        showPdfGenerationResultSheet = false
        pdfDatePreset = WorkproofPdfDatePreset.THIS_MONTH
        pdfStartDateEpochDay = baseMonth.atDay(1).toEpochDay()
        pdfEndDateEpochDay = baseMonth.atEndOfMonth().toEpochDay()
        onClearPdfPreview()
        onClearPdfCreateState()
        onClearPdfFileState()
    }

    LaunchedEffect(launchRequest) {
        val request = launchRequest ?: return@LaunchedEffect
        when (request.target) {
            WorkproofLaunchTarget.PDF_CREATION -> {
                showDetails = true
                showPdfGenerationResultSheet = false
                showPdfDateRangeSheet = true
            }
        }
        onConsumeLaunchRequest()
    }

    DisposableEffect(Unit) {
        onDispose {
            onDetailVisibilityChange(false)
        }
    }

    BackHandler(
        enabled = shouldInterceptWorkproofBack(
            showDetails = showDetails,
            editingRecordId = editingRecordId
        )
    ) {
        showDetails = false
    }

    if (editingRecord != null) {
        ModalBottomSheet(
            onDismissRequest = resetEditDraft,
            sheetState = editSheetState,
            containerColor = DawnSurface
        ) {
            WorkproofEditSheet(
                record = editingRecord,
                selectedReasonLabel = selectedReason?.label.orEmpty(),
                reasonMenuExpanded = reasonMenuExpanded,
                memo = editMemo,
                requestedClockInText = requestedClockInText,
                requestedClockOutText = requestedClockOutText,
                selectedAttachmentName = selectedAttachmentName,
                onToggleReasonMenu = { reasonMenuExpanded = !reasonMenuExpanded },
                onDismissReasonMenu = { reasonMenuExpanded = false },
                onSelectReason = { option ->
                    editReasonKey = option.key
                    reasonMenuExpanded = false
                },
                onMemoChange = { editMemo = it },
                onRequestedClockInChange = {
                    requestedClockInText = normalizeWorkproofTimeInput(
                        previousInput = requestedClockInText,
                        rawInput = it
                    )
                },
                onRequestedClockOutChange = {
                    requestedClockOutText = normalizeWorkproofTimeInput(
                        previousInput = requestedClockOutText,
                        rawInput = it
                    )
                },
                onPickAttachment = { attachmentLauncher.launch(arrayOf("*/*")) },
                onClearAttachment = { selectedAttachmentName = null },
                onSave = {
                    val selectedReasonKey = selectedReason?.key ?: return@WorkproofEditSheet
                    onSaveEdit(
                        editingRecord.id,
                        requestedClockInText,
                        requestedClockOutText,
                        selectedReasonKey,
                        editMemo.trim(),
                        selectedAttachmentName != null
                    )
                    resetEditDraft()
                },
                onClose = resetEditDraft
            )
        }
    }

    if (showPdfDateRangeSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showPdfDateRangeSheet = false
                onClearPdfPreview()
                onClearPdfCreateState()
            },
            containerColor = DawnSurface
        ) {
            WorkproofPdfDateRangeSheet(
                selectedPreset = pdfDatePreset,
                startDate = pdfStartDate,
                endDate = pdfEndDate,
                isDateRangeValid = isPdfDateRangeValid,
                previewUiState = pdfPreviewUiState,
                createUiState = pdfCreateUiState,
                onSelectPreset = { preset ->
                    pdfDatePreset = preset
                    when (preset) {
                        WorkproofPdfDatePreset.THIS_MONTH -> {
                            pdfStartDateEpochDay = baseMonth.atDay(1).toEpochDay()
                            pdfEndDateEpochDay = baseMonth.atEndOfMonth().toEpochDay()
                        }

                        WorkproofPdfDatePreset.LAST_MONTH -> {
                            val lastMonth = baseMonth.minusMonths(1)
                            pdfStartDateEpochDay = lastMonth.atDay(1).toEpochDay()
                            pdfEndDateEpochDay = lastMonth.atEndOfMonth().toEpochDay()
                        }

                        WorkproofPdfDatePreset.CUSTOM -> Unit
                    }
                },
                onOpenStartDatePicker = {
                    showWorkproofDatePicker(
                        context = context,
                        initialDate = pdfStartDate,
                        onDateSelected = { selectedDate ->
                            pdfDatePreset = WorkproofPdfDatePreset.CUSTOM
                            pdfStartDateEpochDay = selectedDate.toEpochDay()
                        }
                    )
                },
                onOpenEndDatePicker = {
                    showWorkproofDatePicker(
                        context = context,
                        initialDate = pdfEndDate,
                        onDateSelected = { selectedDate ->
                            pdfDatePreset = WorkproofPdfDatePreset.CUSTOM
                            pdfEndDateEpochDay = selectedDate.toEpochDay()
                        }
                    )
                },
                onGenerate = {
                    if (!isPdfDateRangeValid || pdfCreateUiState.isSubmitting || pdfPreviewUiState.preview == null) {
                        return@WorkproofPdfDateRangeSheet
                    }
                    onCreateWorkproofPdf(pdfStartDate, pdfEndDate)
                },
                onDismiss = {
                    showPdfDateRangeSheet = false
                    onClearPdfPreview()
                    onClearPdfCreateState()
                }
            )
        }
    }

    if (showPdfGenerationResultSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showPdfGenerationResultSheet = false
                onClearPdfCreateState()
                onClearPdfFileState()
            },
            containerColor = DawnSurface
        ) {
            WorkproofPdfGenerationResultSheet(
                periodText = formatWorkproofPdfDateRange(pdfStartDate, pdfEndDate),
                createUiState = pdfCreateUiState,
                fileName = buildWorkproofPdfFileName(pdfStartDate, pdfEndDate),
                onOpen = {
                    val documentId = pdfCreateUiState.documentId ?: return@WorkproofPdfGenerationResultSheet
                    onOpenWorkproofPdf(documentId)
                },
                onShare = {
                    val documentId = pdfCreateUiState.documentId ?: return@WorkproofPdfGenerationResultSheet
                    onShareWorkproofPdf(documentId)
                },
                onDismiss = {
                    showPdfGenerationResultSheet = false
                    onClearPdfCreateState()
                    onClearPdfFileState()
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkproofCanvas)
    ) {
        if (showDetails) {
            WorkproofDetailPage(
                displayedMonthText = formatMonthText(displayedMonth),
                calendarCountText = language.text("workproof_calendar_recorded_count", recordedDayCount),
                calendarCells = calendarCells,
                weekdays = workproofWeekdays(language),
                records = displayedRecentRecords,
                auditItems = displayedAuditItems,
                onPreviousMonth = { monthOffset -= 1 },
                onNextMonth = { monthOffset += 1 },
                onBack = { showDetails = false },
                onEditRecord = ::openEditSheet,
                onOpenPdfCreation = { showPdfDateRangeSheet = true }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                uiModel.fallbackNoticeMessage?.let { message ->
                    DonDoneNoticeBanner(
                        title = uiModel.fallbackNoticeTitle ?: "가상 예시 데이터",
                        message = message
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
                WorkproofPunchCard(
                    uiModel = uiModel.summary,
                    onClockIn = onClockIn,
                    onClockOut = onClockOut,
                    onRefreshCurrentLocation = onRefreshCurrentLocation,
                    onToggleDetails = { showDetails = true }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

internal fun shouldInterceptWorkproofBack(
    showDetails: Boolean,
    editingRecordId: String?
): Boolean = showDetails && editingRecordId == null

@Composable
private fun WorkproofPunchCard(
    uiModel: WorkproofSummaryUiModel,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit,
    onRefreshCurrentLocation: () -> Unit,
    onToggleDetails: () -> Unit
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val canClockIn = uiModel.canClockIn && uiModel.isWithinWorkplaceRadius
    val canClockOut = uiModel.canClockOut
    val showClockInRadiusFeedback = uiModel.canClockIn && !uiModel.isWithinWorkplaceRadius

    WorkproofSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WorkproofSectionHeader(
                title = "출퇴근",
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
                            contentDescription = language.text("workproof_view_detailed_records"),
                            tint = DawnTextSubtle,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiModel.dateText,
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnTextSubtle
                )
                StatusBadge(
                    text = uiModel.statusText,
                    tone = uiModel.statusTone
                )
            }
            WorkproofKeyValueRow(label = "출근", value = uiModel.clockInText)
            WorkproofKeyValueRow(label = "퇴근", value = uiModel.clockOutText)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WorkproofActionButtonWithFeedback(
                    enabled = canClockIn,
                    showDisabledFeedback = showClockInRadiusFeedback,
                    onDisabledClick = { showWorkproofRadiusToast(context, language) },
                    modifier = Modifier.weight(1f)
                ) {
                    PrimaryActionButton(
                        text = language.translate(stringResource(R.string.workproof_label_clock_in)),
                        onClick = onClockIn,
                        enabled = canClockIn,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WorkproofActionButtonWithFeedback(
                    enabled = canClockOut,
                    showDisabledFeedback = false,
                    onDisabledClick = { showWorkproofRadiusToast(context, language) },
                    modifier = Modifier.weight(1f)
                ) {
                    SecondaryActionButton(
                        text = language.translate(stringResource(R.string.workproof_label_clock_out)),
                        onClick = onClockOut,
                        enabled = canClockOut,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            uiModel.currentLocationStatus?.let { status ->
                DonDoneNoticeBanner(
                    title = language.translate(stringResource(R.string.workproof_location_banner_title)),
                    message = workproofCurrentLocationStatusMessage(status)
                )
            }
            if (uiModel.isUsingFallbackCoordinates) {
                DonDoneNoticeBanner(
                    title = language.text("workproof_workplace_location_check_required"),
                    message = language.translate("근무지 실연동 데이터가 없어 지도 좌표가 실제와 다를 수 있어요.")
                )
            }
            HorizontalDivider(color = WorkproofDivider)
            WorkproofWorkplaceMapCard(
                uiModel = uiModel,
                onRefreshCurrentLocation = onRefreshCurrentLocation
            )
        }
    }
}

@Composable
private fun WorkproofActionButtonWithFeedback(
    enabled: Boolean,
    showDisabledFeedback: Boolean,
    onDisabledClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
        if (!enabled && showDisabledFeedback) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onDisabledClick)
            )
        }
    }
}
internal fun buildWorkproofCalendarCells(
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
                    tone = when {
                        !isBaseMonth -> WorkproofCalendarTone.UNAVAILABLE
                        day > currentDay -> WorkproofCalendarTone.UNAVAILABLE
                        else -> dayTones[day] ?: WorkproofCalendarTone.MISSING
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
private fun resolveAttachmentName(context: Context, uri: Uri, language: AppLanguage): String {
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
    return uri.lastPathSegment ?: language.text("workproof_selected_attachment")
}

private fun showWorkproofRadiusToast(context: Context, language: AppLanguage) {
    Toast.makeText(
        context,
        language.text("workproof_outside_workplace_radius"),
        Toast.LENGTH_SHORT
    ).show()
}
