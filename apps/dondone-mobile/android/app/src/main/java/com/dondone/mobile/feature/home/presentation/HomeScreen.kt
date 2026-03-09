package com.dondone.mobile.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.designsystem.DonDoneCard
import com.dondone.mobile.core.designsystem.DonDoneProgressBar
import com.dondone.mobile.core.designsystem.MetricRow
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SectionPanel
import com.dondone.mobile.core.designsystem.SecondaryActionButton
import com.dondone.mobile.core.designsystem.StatusBadge

@Composable
fun HomeScreen(
    uiModel: HomeUiModel,
    onOpenTransfer: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenWage: () -> Unit,
    onOpenWorkproof: () -> Unit,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DonDoneCard(kicker = "계좌", title = "내 계좌") {
            Text(
                text = uiModel.account.balanceText,
                style = MaterialTheme.typography.displaySmall
            )
            MetricRow(
                leftLabel = "송금 가능",
                leftValue = uiModel.account.sendableAmountText,
                rightLabel = "선택 계좌",
                rightValue = uiModel.account.selectedAccountText
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryActionButton(text = "계좌 관리", onClick = onOpenAccount)
                PrimaryActionButton(text = "송금하기", onClick = onOpenTransfer)
            }
            Text(
                text = uiModel.account.hintText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DonDoneCard(kicker = "오늘 근무", title = "오늘 출퇴근") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = uiModel.work.dateText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    StatusBadge(text = uiModel.work.statusText, tone = uiModel.work.statusTone)
                }
                SecondaryActionButton(text = "기록 보기", onClick = onOpenWorkproof)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryActionButton(
                    text = "출근",
                    onClick = onClockIn,
                    enabled = uiModel.work.canClockIn
                )
                SecondaryActionButton(
                    text = "퇴근",
                    onClick = onClockOut,
                    enabled = uiModel.work.canClockOut
                )
            }
            Text(
                text = uiModel.work.clockSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = uiModel.work.impactText,
                style = MaterialTheme.typography.bodyMedium
            )
            SectionPanel {
                Text(
                    text = "일한 만큼 한도가 열리고 있어요",
                    style = MaterialTheme.typography.titleMedium
                )
                MetricRow(
                    leftLabel = "지금 가능 금액",
                    leftValue = uiModel.work.advanceAvailableText,
                    rightLabel = "다음 구간 진행도",
                    rightValue = uiModel.work.advanceProgressText
                )
                DonDoneProgressBar(progress = uiModel.work.advanceProgress)
                Text(
                    text = uiModel.work.advanceHintText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        DonDoneCard(kicker = "차이 확인", title = "이번 달 돈 상태") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = uiModel.money.summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(
                    text = uiModel.money.statusText,
                    tone = uiModel.money.statusTone
                )
            }
            MetricRow(
                leftLabel = "추정",
                leftValue = uiModel.money.estimatedText,
                rightLabel = "실제",
                rightValue = uiModel.money.actualText
            )
            Text(
                text = uiModel.money.differenceText,
                style = MaterialTheme.typography.bodyMedium
            )
            SectionPanel {
                Text(
                    text = uiModel.money.hintText,
                    style = MaterialTheme.typography.bodyMedium
                )
                PrimaryActionButton(text = "급여 점검", onClick = onOpenWage)
            }
        }
    }
}
