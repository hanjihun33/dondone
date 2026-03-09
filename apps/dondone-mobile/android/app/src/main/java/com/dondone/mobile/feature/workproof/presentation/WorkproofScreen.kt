package com.dondone.mobile.feature.workproof.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnSecondary
import com.dondone.mobile.core.designsystem.DonDoneCard
import com.dondone.mobile.core.designsystem.MetricRow
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SectionPanel
import com.dondone.mobile.core.designsystem.SecondaryActionButton
import com.dondone.mobile.core.designsystem.StatusBadge

@Composable
fun WorkproofScreen(
    uiModel: WorkproofUiModel,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit
) {
    val weekdays = listOf("일", "월", "화", "수", "목", "금", "토")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DonDoneCard(kicker = "원탭", title = "출퇴근 원탭 기록") {
            Text(
                text = "기록 시간 + 위치 스냅샷 1회 저장",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryActionButton(
                    text = "출근",
                    onClick = onClockIn,
                    enabled = uiModel.summary.canClockIn
                )
                SecondaryActionButton(
                    text = "퇴근",
                    onClick = onClockOut,
                    enabled = uiModel.summary.canClockOut
                )
            }
            MetricRow(
                leftLabel = "근무일",
                leftValue = uiModel.summary.verifiedDaysText,
                rightLabel = "수정",
                rightValue = uiModel.summary.auditCountText
            )
            Text(
                text = uiModel.summary.todayClockText,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        DonDoneCard(kicker = "근무 캘린더", title = "기록 상태를 한 달 단위로 확인할 수 있어요") {
            Text(
                text = uiModel.monthText,
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekdays.forEach { label ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiModel.calendarDays.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        week.forEach { day ->
                            val background = when (day.tone) {
                                CalendarDayTone.TODAY -> DawnPrimary
                                CalendarDayTone.MODIFIED -> Color(0xFFFFE9DA)
                                CalendarDayTone.COMPLETE -> DawnSecondary
                                CalendarDayTone.DEFAULT -> Color.Transparent
                            }
                            val foreground = when (day.tone) {
                                CalendarDayTone.TODAY -> Color.White
                                CalendarDayTone.MODIFIED -> Color(0xFFE58F2A)
                                CalendarDayTone.COMPLETE -> DawnPrimary
                                CalendarDayTone.DEFAULT -> MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(background)
                                    .border(1.dp, DawnBorder, RoundedCornerShape(14.dp))
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.day.toString(),
                                    color = foreground,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        repeat(7 - week.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(color = Color.Transparent, label = "없음")
                LegendItem(color = DawnSecondary, label = "완료")
                LegendItem(color = Color(0xFFFFE9DA), label = "수정")
            }
        }

        DonDoneCard(kicker = "최근 기록", title = "수정 사유를 남겨주세요") {
            uiModel.records.forEach { record ->
                SectionPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = record.dateText,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = record.timeText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (record.modifiedHintText != null) {
                                Text(
                                    text = record.modifiedHintText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusBadge(
                            text = if (record.modified) "수정됨" else "기록",
                            tone = record.statusTone()
                        )
                    }
                }
            }
        }

        DonDoneCard(kicker = "변경 기록", title = "감사 로그") {
            uiModel.audits.forEach { audit ->
                SectionPanel {
                    Text(text = audit.timeRangeText, style = MaterialTheme.typography.titleMedium)
                    Text(text = audit.reason, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = audit.metaText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (color == Color.Transparent) Color.White else color)
                .border(1.dp, DawnBorder, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
