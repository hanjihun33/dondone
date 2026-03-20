package com.dondone.mobile.feature.workproof.presentation

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dondone.mobile.BuildConfig
import com.dondone.mobile.R
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimaryDeep
import com.dondone.mobile.core.designsystem.DawnSurface
import com.dondone.mobile.core.designsystem.DawnSurfaceAlt
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.DonDoneErrorPanel
import com.dondone.mobile.core.designsystem.DonDoneLoadingPanel
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SecondaryActionButton
import com.dondone.mobile.core.designsystem.pressableScale
import com.dondone.mobile.core.designsystem.rememberDonDoneGrayRipple
import com.dondone.mobile.core.map.KakaoMapSupport
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.shape.DotPoints
import com.kakao.vectormap.shape.PolygonOptions
import com.kakao.vectormap.shape.PolygonStyles
import com.kakao.vectormap.shape.PolygonStylesSet
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val WorkproofCanvas = Color.White
private val WorkproofDivider = Color(0xFFE8EBF0)
private val WorkproofGhostBorder = Color(0xFFF4F6F8)
private val WorkproofCurrentDayBorder = Color(0xFFDDE4FF)
private val WorkproofRowAccentBackground = Color(0xFFF2F3FF)
private val WorkproofRowAccentTint = Color(0xFF6D68F5)
private val WorkproofMissingBackground = Color.White
private val WorkproofMissingBorder = Color.Transparent
private val WorkproofMissingText = Color(0xFF64748B)
private val WorkproofInactiveText = Color(0xFFCBD5E1)
private val WorkproofPartialBackground = Color(0xFFEEE5FF)
private val WorkproofPartialBorder = Color(0xFFC3A6FF)
private val WorkproofPartialText = Color(0xFF6F42D9)
private val WorkproofCompleteBackground = Color(0xFFE9DEFF)
private val WorkproofCompleteBorder = Color(0xFFB89BFF)
private val WorkproofCompleteText = Color(0xFF5E3CC5)
private val WorkproofModifiedBackground = Color(0xFFF7E4F4)
private val WorkproofModifiedBorder = Color(0xFFD98FD0)
private val WorkproofModifiedText = Color(0xFFAA3E96)
private val WorkproofMapWorkplacePin = DawnText
private val WorkproofMapCurrentPin = DawnPrimaryDeep
private val WorkproofPdfPresetSelectedBackground = Color(0xFFF1ECFF)
private val WorkproofPdfPresetSelectedBorder = Color(0xFFB89BFF)
private val WorkproofWeekdays = listOf("일", "월", "화", "수", "목", "금", "토")
private val WorkproofPdfDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkproofScreen(
    uiModel: WorkproofUiModel,
    pdfPreviewUiState: WorkproofPdfPreviewUiState,
    pdfCreateUiState: WorkproofPdfCreateUiState,
    pdfFileUiState: WorkproofPdfFileUiState,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit,
    onSaveEdit: (String, String, String, Boolean) -> Unit,
    onRefreshPdfPreview: (LocalDate, LocalDate) -> Unit,
    onClearPdfPreview: () -> Unit,
    onCreateWorkproofPdf: (LocalDate, LocalDate) -> Unit,
    onClearPdfCreateState: () -> Unit,
    onOpenWorkproofPdf: (Long) -> Unit,
    onShareWorkproofPdf: (Long) -> Unit,
    onClearPdfFileState: () -> Unit,
    resetVersion: Int,
    onDetailVisibilityChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val baseMonth = remember(uiModel.calendarBaseYear, uiModel.calendarBaseMonth) {
        YearMonth.of(uiModel.calendarBaseYear, uiModel.calendarBaseMonth)
    }
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var monthOffset by remember(resetVersion) { mutableIntStateOf(0) }
    var editingRecordId by remember(resetVersion) { mutableStateOf<String?>(null) }
    var editReasonKey by remember(resetVersion) { mutableStateOf("") }
    var editMemo by remember(resetVersion) { mutableStateOf("") }
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
            WorkproofPdfFileAction.OPEN -> openWorkproofPdfFile(context, uri)
            WorkproofPdfFileAction.SHARE -> shareWorkproofPdfFile(context, uri, pdfFileUiState.fileName)
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
                onOpenDocuments = { showWorkproofPdfDocumentBoxToast(context) },
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
                calendarCountText = stringResource(R.string.workproof_calendar_recorded_count, recordedDayCount),
                calendarCells = calendarCells,
                weekdays = WorkproofWeekdays,
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

internal fun shouldInterceptWorkproofBack(
    showDetails: Boolean,
    editingRecordId: String?
): Boolean = showDetails && editingRecordId == null

@Composable
private fun WorkproofPunchCard(
    uiModel: WorkproofSummaryUiModel,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit,
    onToggleDetails: () -> Unit
) {
    val context = LocalContext.current
    val canClockIn = uiModel.canClockIn && uiModel.isWithinWorkplaceRadius
    val canClockOut = uiModel.canClockOut && uiModel.isWithinWorkplaceRadius
    val showClockInRadiusFeedback = uiModel.canClockIn && !uiModel.isWithinWorkplaceRadius
    val showClockOutRadiusFeedback = uiModel.canClockOut && !uiModel.isWithinWorkplaceRadius

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
                            contentDescription = "상세 기록 보기",
                            tint = DawnTextSubtle,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WorkproofActionButtonWithFeedback(
                    enabled = canClockIn,
                    showDisabledFeedback = showClockInRadiusFeedback,
                    onDisabledClick = { showWorkproofRadiusToast(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    PrimaryActionButton(
                        text = stringResource(R.string.workproof_label_clock_in),
                        onClick = onClockIn,
                        enabled = canClockIn,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WorkproofActionButtonWithFeedback(
                    enabled = canClockOut,
                    showDisabledFeedback = showClockOutRadiusFeedback,
                    onDisabledClick = { showWorkproofRadiusToast(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    SecondaryActionButton(
                        text = stringResource(R.string.workproof_label_clock_out),
                        onClick = onClockOut,
                        enabled = canClockOut,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            HorizontalDivider(color = WorkproofDivider)
            WorkproofWorkplaceMapCard(uiModel = uiModel)
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

@Composable
private fun WorkproofWorkplaceMapCard(
    uiModel: WorkproofSummaryUiModel
) {
    val isKakaoMapAvailable = remember {
        KakaoMapSupport.isMapAvailable(BuildConfig.KAKAO_NATIVE_APP_KEY)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "위치",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )

        if (!isKakaoMapAvailable) {
            WorkproofMapFallbackCard(
                hasApiKey = BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank(),
                isRuntimeSupported = KakaoMapSupport.isRuntimeSupported()
            )
        } else {
            KakaoWorkplaceMapView(
                workplaceLatitude = uiModel.workplaceLatitude,
                workplaceLongitude = uiModel.workplaceLongitude,
                currentLatitude = uiModel.currentLatitude,
                currentLongitude = uiModel.currentLongitude,
                workplaceRadiusMeters = uiModel.workplaceRadiusMeters
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WorkproofMapLegendItem(
                color = WorkproofMapCurrentPin,
                label = "현재 내 위치"
            )
            WorkproofMapLegendItem(
                color = WorkproofMapWorkplacePin,
                label = "근무지 위치"
            )
        }
    }
}

@Composable
private fun WorkproofMapFallbackCard(
    hasApiKey: Boolean,
    isRuntimeSupported: Boolean
) {
    val message = when {
        !hasApiKey -> "KAKAO_NATIVE_APP_KEY를 설정하면 지도가 표시돼요."
        !isRuntimeSupported -> "현재 실행 환경에서는 카카오 지도를 지원하지 않아 지도 없이 표시돼요."
        else -> "지도를 불러올 수 없어요."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFFF7F9FC))
            .border(1.dp, DawnBorder, RoundedCornerShape(28.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = DawnTextSubtle
        )
    }
}

@Composable
private fun KakaoWorkplaceMapView(
    workplaceLatitude: Double,
    workplaceLongitude: Double,
    currentLatitude: Double,
    currentLongitude: Double,
    workplaceRadiusMeters: Int
) {
    var retryToken by rememberSaveable { mutableIntStateOf(0) }
    val mapView = rememberKakaoMapViewWithLifecycle(retryToken)
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var isCameraInitialized by rememberSaveable { mutableStateOf(false) }
    var isWorkplacePinAdded by rememberSaveable { mutableStateOf(false) }
    var isCurrentPinAdded by rememberSaveable { mutableStateOf(false) }
    var isRadiusCircleAdded by rememberSaveable { mutableStateOf(false) }
    var mapErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val workplacePosition = remember(workplaceLatitude, workplaceLongitude) {
        LatLng.from(workplaceLatitude, workplaceLongitude)
    }
    val currentPosition = remember(currentLatitude, currentLongitude) {
        LatLng.from(currentLatitude, currentLongitude)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, DawnBorder, RoundedCornerShape(28.dp))
        ) {
            if (mapErrorMessage != null) {
                DonDoneErrorPanel(
                    title = "지도를 불러오지 못했어요",
                    message = mapErrorMessage ?: "잠시 후 다시 시도해 주세요.",
                    actionLabel = "다시 시도",
                    onAction = {
                        mapErrorMessage = null
                        kakaoMap = null
                        isCameraInitialized = false
                        isWorkplacePinAdded = false
                        isCurrentPinAdded = false
                        isRadiusCircleAdded = false
                        retryToken += 1
                    },
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                AndroidView(
                    factory = {
                        mapView.apply {
                            start(
                                object : MapLifeCycleCallback() {
                                    override fun onMapDestroy() = Unit

                                    override fun onMapError(error: Exception) {
                                        mapErrorMessage = error.message ?: "카카오 지도 초기화에 실패했습니다."
                                    }
                                },
                                object : KakaoMapReadyCallback() {
                                    override fun onMapReady(map: KakaoMap) {
                                        mapErrorMessage = null
                                        kakaoMap = map
                                        if (!isWorkplacePinAdded) {
                                            map.labelManager?.let { labelManager ->
                                                val labelStyles = labelManager.addLabelStyles(
                                                    LabelStyles.from(
                                                        LabelStyle.from(R.drawable.ic_workplace_pin)
                                                    )
                                                )
                                                labelManager.layer?.let { labelLayer ->
                                                    labelLayer.addLabel(
                                                        LabelOptions.from(workplacePosition)
                                                            .setStyles(labelStyles)
                                                    )
                                                    isWorkplacePinAdded = true
                                                }
                                            }
                                        }
                                        if (!isCurrentPinAdded) {
                                            map.labelManager?.let { labelManager ->
                                                val currentLabelStyles = labelManager.addLabelStyles(
                                                    LabelStyles.from(
                                                        LabelStyle.from(R.drawable.ic_current_location_pin)
                                                    )
                                                )
                                                labelManager.layer?.let { labelLayer ->
                                                    labelLayer.addLabel(
                                                        LabelOptions.from(currentPosition)
                                                            .setStyles(currentLabelStyles)
                                                    )
                                                    isCurrentPinAdded = true
                                                }
                                            }
                                        }
                                        if (!isRadiusCircleAdded) {
                                            val radiusStyles = PolygonStylesSet.from(
                                                PolygonStyles.from(
                                                    android.graphics.Color.parseColor("#1A7B68EE"),
                                                    1.0f,
                                                    android.graphics.Color.parseColor("#7B68EE")
                                                )
                                            )
                                            map.shapeManager?.layer?.let { shapeLayer ->
                                                shapeLayer.addPolygon(
                                                    PolygonOptions.from(
                                                        DotPoints.fromCircle(
                                                            workplacePosition,
                                                            workplaceRadiusMeters.toFloat()
                                                        )
                                                    ).setStylesSet(radiusStyles)
                                                )
                                                isRadiusCircleAdded = true
                                            }
                                        }
                                        if (!isCameraInitialized) {
                                            map.moveCamera(
                                                CameraUpdateFactory.newCenterPosition(workplacePosition)
                                            )
                                            isCameraInitialized = true
                                        }
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, DawnBorder, RoundedCornerShape(16.dp))
                .clickable {
                    kakaoMap?.moveCamera(
                        CameraUpdateFactory.newCenterPosition(currentPosition)
                    )
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "현재 내 위치로 핀 이동",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = DawnTextSubtle
            )
        }
    }
}

@Composable
private fun WorkproofMapLegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = DawnText
        )
    }
}

@Composable
private fun rememberKakaoMapViewWithLifecycle(
    retryToken: Int
): MapView {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember(retryToken) { MapView(context) }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.pause()
            mapView.finish()
        }
    }

    return mapView
}

@Composable
private fun WorkproofDetailPage(
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
private fun WorkproofPdfDateRangeSheet(
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
private fun WorkproofPdfGenerationResultSheet(
    periodText: String,
    createUiState: WorkproofPdfCreateUiState,
    fileName: String,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onOpenDocuments: () -> Unit,
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
        isActionable -> "열기 또는 공유를 누르면 선택한 기간의 근무 기록 문서를 바로 생성해요."
        else -> "선택한 기간의 근무 기록 문서 생성 요청이 접수됐어요. 문서를 열 때 바로 생성해요."
    }
    val statusLabel = when (createUiState.status) {
        "QUEUED" -> if (isActionable) "열기 시 생성" else "요청 접수"
        "RUNNING" -> "생성 중"
        "READY" -> "준비 완료"
        "FAILED" -> "실패"
        null -> "요청 접수"
        else -> createUiState.status
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
            WorkproofKeyValueRow(label = "생성 상태", value = statusLabel)
            WorkproofKeyValueRow(label = "요청 ID", value = createUiState.requestId.orEmpty())
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
            text = "문서함에서 보기",
            onClick = onOpenDocuments,
            modifier = Modifier.fillMaxWidth()
        )
        SecondaryActionButton(
            text = "닫기",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
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
    val interactionSource = remember { MutableInteractionSource() }

    WorkproofSurfaceCard {
        WorkproofSectionHeader(
            title = "최근 기록",
            trailing = {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .pressableScale(
                            interactionSource = interactionSource,
                            pressedScale = 0.98f
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = rememberDonDoneGrayRipple(),
                            onClick = onOpenPdfCreation
                        )
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "문서 생성",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = WorkproofRowAccentTint
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = WorkproofRowAccentTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
                    text = recordStatusText(record.tone),
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
            record.modifiedReason?.let { reason ->
                Text(
                    text = stringResource(R.string.workproof_value_modified_reason, reason),
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
private fun WorkproofSurfaceCard(
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
private fun WorkproofSelectionField(
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

private fun isRecordedCalendarTone(tone: WorkproofCalendarTone?): Boolean {
    return tone == WorkproofCalendarTone.PARTIAL ||
        tone == WorkproofCalendarTone.COMPLETE ||
        tone == WorkproofCalendarTone.MODIFIED
}

@Composable
private fun recordStatusText(tone: WorkproofRecordTone): String = when (tone) {
    WorkproofRecordTone.DEFAULT -> stringResource(R.string.workproof_status_default)
    WorkproofRecordTone.ACTIVE -> stringResource(R.string.workproof_status_active)
    WorkproofRecordTone.MODIFIED -> stringResource(R.string.workproof_status_modified)
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

private fun showWorkproofRadiusToast(context: Context) {
    Toast.makeText(
        context,
        "근무지 반경 밖에서는 출퇴근할 수 없어요.",
        Toast.LENGTH_SHORT
    ).show()
}

private fun showWorkproofPdfDateRangeSavedToast(
    context: Context,
    startDate: LocalDate,
    endDate: LocalDate
) {
    Toast.makeText(
        context,
        "선택 기간 ${formatWorkproofPdfDateRange(startDate, endDate)}",
        Toast.LENGTH_SHORT
    ).show()
}

private fun showWorkproofPdfDocumentBoxToast(context: Context) {
    Toast.makeText(
        context,
        "문서함 연결은 다음 단계에서 메뉴 탭과 연결됩니다.",
        Toast.LENGTH_SHORT
    ).show()
}

private fun showWorkproofDatePicker(
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

private fun formatWorkproofPdfDateRange(startDate: LocalDate, endDate: LocalDate): String =
    "${formatWorkproofPdfDate(startDate)} - ${formatWorkproofPdfDate(endDate)}"

private fun buildWorkproofPdfFileName(startDate: LocalDate, endDate: LocalDate): String =
    "workproof-${startDate.toString().replace("-", "")}-${endDate.toString().replace("-", "")}.pdf"

private fun openWorkproofPdfFile(
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

private fun shareWorkproofPdfFile(
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

private enum class WorkproofPdfDatePreset {
    THIS_MONTH,
    LAST_MONTH,
    CUSTOM
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
