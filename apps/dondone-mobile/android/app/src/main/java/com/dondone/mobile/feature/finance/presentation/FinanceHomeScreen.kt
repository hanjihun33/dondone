package com.dondone.mobile.feature.finance.presentation

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
import com.dondone.mobile.core.designsystem.DonDoneCard
import com.dondone.mobile.core.designsystem.DonDoneProgressBar
import com.dondone.mobile.core.designsystem.MetricRow
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SectionPanel
import com.dondone.mobile.core.designsystem.SecondaryActionButton
import com.dondone.mobile.core.designsystem.StatusBadge

@Composable
fun FinanceHomeScreen(
    uiModel: FinanceHomeUiModel,
    onOpenWage: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenAccount: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DonDoneCard(kicker = "금융", title = "내 계좌") {
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
                PrimaryActionButton(text = "송금", onClick = onOpenTransfer)
                SecondaryActionButton(text = "계좌 관리", onClick = onOpenAccount)
            }
            Text(
                text = uiModel.account.hintText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DonDoneCard(kicker = "근무 기반 한도", title = "미리받기") {
            MetricRow(
                leftLabel = "가능 금액",
                leftValue = uiModel.advance.availableText,
                rightLabel = "수수료",
                rightValue = uiModel.advance.feeText
            )
            SectionPanel {
                DonDoneProgressBar(progress = uiModel.advance.progress)
                Text(
                    text = uiModel.advance.progressHintText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        DonDoneCard(kicker = "급여 점검", title = "급여 점검 요약") {
            StatusBadge(
                text = uiModel.wage.statusText,
                tone = uiModel.wage.statusTone
            )
            MetricRow(
                leftLabel = "추정",
                leftValue = uiModel.wage.estimatedText,
                rightLabel = "실제",
                rightValue = uiModel.wage.actualText
            )
            Text(
                text = uiModel.wage.hintText,
                style = MaterialTheme.typography.bodyMedium
            )
            PrimaryActionButton(text = "급여 점검 열기", onClick = onOpenWage)
        }

        uiModel.moneySplit?.let { moneySplit ->
            DonDoneCard(kicker = "돈 나누기", title = "이번 달 돈을 바로 나눠보세요") {
                MetricRow(
                    leftLabel = "생활비",
                    leftValue = moneySplit.livingCostText,
                    rightLabel = "가족 송금",
                    rightValue = moneySplit.familySendText
                )
                MetricRow(
                    leftLabel = "보관 / 이자",
                    leftValue = moneySplit.saveAmountText,
                    rightLabel = "기준",
                    rightValue = moneySplit.basisText
                )
            }
        }

        DonDoneCard(kicker = "보관 / 이자", title = "남는 돈 보관") {
            MetricRow(
                leftLabel = "권장 보관 금액",
                leftValue = uiModel.vault.suggestedDepositText,
                rightLabel = "월 예상 이자",
                rightValue = uiModel.vault.monthlyInterestText
            )
            Text(
                text = "데모 시뮬레이션 기준으로 하루 예상 이자는 ${uiModel.vault.dailyInterestText} 입니다.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
