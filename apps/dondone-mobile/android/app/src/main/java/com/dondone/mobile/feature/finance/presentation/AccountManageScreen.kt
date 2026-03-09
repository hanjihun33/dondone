package com.dondone.mobile.feature.finance.presentation

import androidx.compose.foundation.clickable
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
import com.dondone.mobile.core.designsystem.MetricRow
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SectionPanel
import com.dondone.mobile.core.designsystem.StatusBadge

@Composable
fun AccountManageScreen(
    uiModel: AccountManageUiModel,
    onSelectAccount: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DonDoneCard(kicker = "송금", title = "계좌 선택") {
            Text(
                text = "송금에 사용할 계좌를 먼저 고른 뒤 받는 사람 선택으로 넘어갑니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SectionPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "현재 선택", style = MaterialTheme.typography.labelMedium)
                        Text(text = uiModel.selectedAccountName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = uiModel.selectedAccountNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadge(text = "STEP 1", tone = BadgeTone.Info)
                }
                MetricRow(
                    leftLabel = "잔액",
                    leftValue = uiModel.selectedBalanceText,
                    rightLabel = "송금 초안",
                    rightValue = uiModel.draftAmountText
                )
            }
        }

        DonDoneCard(kicker = "보내는 계좌", title = "계좌 목록") {
            uiModel.accounts.forEach { account ->
                SectionPanel(
                    modifier = Modifier.clickable { onSelectAccount(account.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = account.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = account.number,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "잔액 ${account.balanceText}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (account.selected) {
                            StatusBadge(text = "선택됨", tone = BadgeTone.Info)
                        }
                    }
                }
            }
            PrimaryActionButton(text = "다음: 받는 사람", onClick = onContinue)
        }
    }
}
