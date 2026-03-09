package com.dondone.mobile.feature.wage.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.designsystem.DonDoneCard
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnSurface
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.MetricRow
import com.dondone.mobile.core.designsystem.PillButton
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SectionPanel
import com.dondone.mobile.core.designsystem.StatusBadge

@Composable
fun WageScreen(
    uiModel: WageUiModel,
    onRecordDeposit: () -> Unit,
    onIncreaseDeposit: () -> Unit,
    onDecreaseDeposit: () -> Unit,
    onOpenTransfer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DonDoneCard(kicker = "급여 점검", title = "급여 점검") {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiModel.chips.forEach { chip ->
                    SuggestionChip(text = chip)
                }
            }
            SectionPanel {
                Text(
                    text = "이 급여 계산은 참고용 추정입니다. 실제 지급과 공제는 근로계약, 급여명세서, 회사 규정에 따라 달라질 수 있어요.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "공제 항목은 아직 반영되지 않아 실제 수령액과 차이가 있을 수 있습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DonDoneCard(kicker = uiModel.summaryMonthText, title = "이번 달 급여 확인") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = uiModel.summaryHelperText, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = uiModel.auditSummaryText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(
                    text = uiModel.deposit.statusText,
                    tone = uiModel.deposit.statusTone
                )
            }
            SectionPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "급여일 체크", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = uiModel.deposit.headerText,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = uiModel.deposit.recordedDateText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!uiModel.deposit.recorded) {
                        PillButton(text = "적용", onClick = onRecordDeposit)
                    }
                }
            }
            SectionPanel {
                Text(text = "실제 입금액(실수령)", style = MaterialTheme.typography.labelMedium)
                Text(text = uiModel.deposit.actualDepositText, style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(text = "-5만", onClick = onDecreaseDeposit)
                    PillButton(text = "+5만", onClick = onIncreaseDeposit)
                }
                Text(
                    text = uiModel.deposit.helperText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MetricRow(
                leftLabel = "근무일",
                leftValue = uiModel.workDaysText,
                rightLabel = "총 근무",
                rightValue = uiModel.totalHoursText
            )
        }

        DonDoneCard(kicker = "참고용 추정 급여", title = "근거 자료 기반") {
            MetricRow(
                leftLabel = "기본",
                leftValue = uiModel.baseText,
                rightLabel = "연장 프리미엄",
                rightValue = uiModel.overtimePremiumText
            )
            MetricRow(
                leftLabel = "야간 프리미엄",
                leftValue = uiModel.nightPremiumText,
                rightLabel = "추정 합계",
                rightValue = uiModel.estimatedTotalText
            )
        }

        DonDoneCard(
            kicker = "차이 확인",
            title = uiModel.difference.title
        ) {
            if (uiModel.difference.locked) {
                SectionPanel {
                    Text(text = "실입금을 입력하면 다음 행동이 열려요", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "입금액을 입력한 뒤 차이 확인, 근거 정리, 송금 흐름을 이어서 진행할 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                StatusBadge(
                    text = uiModel.difference.statusText,
                    tone = uiModel.difference.statusTone
                )
                MetricRow(
                    leftLabel = "추정",
                    leftValue = uiModel.difference.estimatedText,
                    rightLabel = "실제",
                    rightValue = uiModel.difference.actualText
                )
                Text(
                    text = uiModel.difference.differenceText,
                    style = MaterialTheme.typography.titleMedium
                )
                SectionPanel {
                    Text(text = "함께 확인된 근거", style = MaterialTheme.typography.titleMedium)
                    uiModel.difference.evidenceLines.forEach { line ->
                        Text(text = "• $line", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                SectionPanel {
                    Text(text = "추천 경로(선택)", style = MaterialTheme.typography.labelLarge)
                    uiModel.difference.steps.forEach { step ->
                        StepRow(step = step.step, label = step.label, status = step.status)
                    }
                }
                SectionPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "다음 추천 행동", style = MaterialTheme.typography.labelLarge)
                            Text(text = uiModel.difference.nextActionText, style = MaterialTheme.typography.bodyMedium)
                        }
                        StatusBadge(text = "단계 3", tone = BadgeTone.Info)
                    }
                    PrimaryActionButton(text = "송금으로 이어가기", onClick = onOpenTransfer)
                }
            }
        }
    }
}

@Composable
private fun StepRow(step: String, label: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$step. $label", style = MaterialTheme.typography.bodyLarge)
        StatusBadge(text = status, tone = BadgeTone.Info)
    }
}

@Composable
private fun SuggestionChip(text: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(DawnSurface)
            .border(1.dp, DawnBorder, MaterialTheme.shapes.large)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = DawnTextSubtle
        )
    }
}
