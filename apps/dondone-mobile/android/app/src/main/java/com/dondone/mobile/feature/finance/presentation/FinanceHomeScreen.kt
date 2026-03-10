package com.dondone.mobile.feature.finance.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnSurface
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.DonDoneProgressBar
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FinanceAccountCard(
            uiModel = uiModel,
            onOpenTransfer = onOpenTransfer,
            onOpenAccount = onOpenAccount
        )

        uiModel.moneySplit?.let { moneySplit ->
            FinanceBlockCard(
                kicker = "돈 나누기",
                title = "이번 달 돈을 바로 나눠보세요",
                description = "입금이 들어오면 생활비, 가족 송금, 보관 금액을 바로 정리할 수 있어요.",
                trailing = { FinanceCapsule(text = moneySplit.basisText) }
            ) {
                FinanceMetricRow(
                    metrics = listOf(
                        "생활비" to moneySplit.livingCostText,
                        "가족 송금" to moneySplit.familySendText,
                        "보관 / 이자" to moneySplit.saveAmountText
                    )
                )
                Text(
                    text = "원터치 자동 분배가 아니라, 이번 달 계획을 빠르게 잡기 위한 제안 카드예요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnTextSubtle
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinancePrimaryButton(
                        text = "가족 송금 열기",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTransfer
                    )
                    FinanceGhostButton(
                        text = "보관 보기",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAccount
                    )
                }
            }
        }

        FinanceBlockCard(
            kicker = "PAYCHECK ADVANCE",
            title = "미리받기",
            description = uiModel.advance.statusText,
            actionText = "자세히",
            onAction = onOpenAccount
        ) {
            FinanceMetricRow(
                metrics = listOf(
                    "지금 가능 금액" to uiModel.advance.availableText,
                    "상환 예정" to uiModel.advance.repaymentDueText
                )
            )
            FinanceInnerPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "다음 구간 진행도",
                        style = MaterialTheme.typography.labelSmall,
                        color = DawnTextSubtle
                    )
                    Text(
                        text = "${(uiModel.advance.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = DawnText
                    )
                }
                DonDoneProgressBar(progress = uiModel.advance.progress)
                Text(
                    text = uiModel.advance.progressHintText,
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnTextSubtle
                )
            }
            FinancePrimaryButton(
                text = "미리받기 신청",
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenAccount
            )
        }

        FinanceBlockCard(
            kicker = "VAULT YIELD",
            title = "예치 이자",
            description = if (uiModel.vault.depositStatusText == "미신청") {
                "아직 예치를 시작하지 않았어요."
            } else {
                uiModel.vault.helperText
            },
            actionText = "상세",
            onAction = onOpenAccount
        ) {
            FinanceMetricRow(
                metrics = listOf(
                    "예치 잔액" to uiModel.vault.depositStatusText,
                    "누적 이자(추정)" to uiModel.vault.accruedInterestText
                )
            )
            Text(
                text = "예상 연이율 ${uiModel.vault.aprText}",
                style = MaterialTheme.typography.bodySmall,
                color = DawnTextSubtle
            )
            Text(
                text = uiModel.vault.helperText,
                style = MaterialTheme.typography.bodySmall,
                color = DawnTextSubtle
            )
        }

        FinanceWageSummaryCard(
            uiModel = uiModel.wage,
            onOpenWage = onOpenWage
        )

        Text(
            text = "현재는 데모 환경입니다. 실제 자금 이동이 발생하지 않습니다.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = DawnTextSubtle
        )
    }
}

@Composable
private fun FinanceAccountCard(
    uiModel: FinanceHomeUiModel,
    onOpenTransfer: () -> Unit,
    onOpenAccount: () -> Unit
) {
    FinanceSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "내 계좌",
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnPrimary
                )
                Text(
                    text = uiModel.account.balanceText,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = DawnText
                )
            }

            FinanceMetricRow(
                metrics = listOf(
                    "송금 가능" to uiModel.account.sendableAmountText,
                    "선택 계좌" to uiModel.account.selectedAccountText
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancePrimaryButton(
                    text = "계좌 관리",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAccount
                )
                FinanceGhostButton(
                    text = "송금하기",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenTransfer
                )
            }

            Text(
                text = uiModel.account.hintText,
                style = MaterialTheme.typography.bodySmall,
                color = DawnTextSubtle
            )
        }
    }
}

@Composable
private fun FinanceWageSummaryCard(
    uiModel: FinanceWageUiModel,
    onOpenWage: () -> Unit
) {
    val diffColor = when (uiModel.statusTone) {
        BadgeTone.Success -> Color(0xFF2E8B57)
        BadgeTone.Warning -> Color(0xFFB97711)
        BadgeTone.Info -> DawnPrimary
    }

    FinanceSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "WAGE CHECK",
                        style = MaterialTheme.typography.labelMedium,
                        color = DawnPrimary
                    )
                    Text(
                        text = "급여 점검 요약",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                FinanceLinkButton(text = "급여 점검", onClick = onOpenWage)
            }

            Text(
                text = "차액",
                style = MaterialTheme.typography.labelSmall,
                color = DawnTextSubtle
            )
            Text(
                text = uiModel.differenceText,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = diffColor
            )
            Text(
                text = uiModel.hintText,
                style = MaterialTheme.typography.bodySmall,
                color = DawnTextSubtle
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "추정 ${uiModel.estimatedText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnTextSubtle
                )
                Text(
                    text = "· 실제 ${uiModel.actualText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnTextSubtle
                )
            }
            StatusBadge(text = uiModel.statusText, tone = uiModel.statusTone)
        }
    }
}

@Composable
private fun FinanceBlockCard(
    kicker: String,
    title: String,
    description: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    FinanceSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = kicker,
                        style = MaterialTheme.typography.labelMedium,
                        color = DawnPrimary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = DawnTextSubtle
                    )
                }
                when {
                    trailing != null -> trailing()
                    actionText != null && onAction != null -> FinanceLinkButton(text = actionText, onClick = onAction)
                }
            }
            content()
        }
    }
}

@Composable
private fun FinanceSurfaceCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(28.dp))
            .border(1.dp, DawnBorder, RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
private fun FinanceMetricRow(metrics: List<Pair<String, String>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        metrics.forEach { (label, value) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(DawnSurface, RoundedCornerShape(20.dp))
                    .border(1.dp, DawnBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = DawnTextSubtle
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = DawnText
                )
            }
        }
    }
}

@Composable
private fun FinanceInnerPanel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DawnSurface, RoundedCornerShape(20.dp))
            .border(1.dp, DawnBorder, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun FinanceCapsule(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .background(Color(0xFFF2F4F7), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelSmall,
        color = DawnTextSubtle
    )
}

@Composable
private fun FinanceLinkButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DawnBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = DawnPrimary
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun FinancePrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DawnPrimary,
            contentColor = Color.White
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun FinanceGhostButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, DawnBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFFF8FAFC),
            contentColor = DawnText
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
