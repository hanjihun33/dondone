package com.dondone.mobile.feature.finance.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

private val FinanceAdvanceSheetHero = Color(0xFFF5F0FF)
private val FinanceAdvanceSheetHeroBorder = Color(0xFFE9DFFF)
private val FinanceAdvanceSheetMutedBackground = Color(0xFFF8FAFC)
private val FinanceAdvanceSheetMutedBorder = Color(0xFFE2E8F0)
private val FinanceAdvanceSheetWarningBorder = Color(0xFFFDE68A)
private val FinanceAdvanceSheetDefaultText = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceHomeScreen(
    uiModel: FinanceHomeUiModel,
    onOpenWage: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenAccount: () -> Unit
) {
    val advanceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val vaultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAdvanceSheet by remember { mutableStateOf(false) }
    var showVaultSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
            kicker = "미리받기",
            title = "미리받기",
            description = uiModel.advance.statusText
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
                onClick = { showAdvanceSheet = true }
            )
        }

        FinanceBlockCard(
            kicker = "예치 이자",
            title = "예치 이자",
            description = if (uiModel.vault.depositStatusText == "미신청") {
                "아직 예치를 시작하지 않았어요."
            } else {
                uiModel.vault.helperText
            }
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
            FinancePrimaryButton(
                text = uiModel.vault.actionText,
                modifier = Modifier.fillMaxWidth(),
                onClick = { showVaultSheet = true }
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

        Spacer(modifier = Modifier.height(12.dp))
    }

    if (showAdvanceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAdvanceSheet = false },
            sheetState = advanceSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = null
        ) {
            FinanceAdvanceBottomSheet(
                uiModel = uiModel.advance.detail,
                onDismiss = { showAdvanceSheet = false },
                onRequestAdvance = { showAdvanceSheet = false }
            )
        }
    }

    if (showVaultSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVaultSheet = false },
            sheetState = vaultSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = null
        ) {
            FinanceVaultBottomSheet(
                uiModel = uiModel.vault.detail,
                onDismiss = { showVaultSheet = false },
                onAction = { showVaultSheet = false }
            )
        }
    }
}

@Composable
private fun FinanceAccountCard(
    uiModel: FinanceHomeUiModel,
    onOpenTransfer: () -> Unit,
    onOpenAccount: () -> Unit
) {
    FinanceSurfaceCard(hero = true) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "계좌",
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnPrimary
                )
                Text(
                    text = "내 계좌",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = DawnText
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
        }
    }
}

@Composable
private fun FinanceAdvanceBottomSheet(
    uiModel: FinanceAdvanceDetailUiModel,
    onDismiss: () -> Unit,
    onRequestAdvance: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "미리받기",
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnPrimary
                )
                Text(
                    text = "미리받기",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = DawnText
                )
                Text(
                    text = uiModel.subtitleText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = DawnTextSubtle
                )
            }
            FinanceLinkButton(text = "닫기", onClick = onDismiss)
        }

        FinanceAdvanceSummaryCard(uiModel = uiModel)
        FinanceAdvanceWorkproofCard(uiModel = uiModel)

        if (uiModel.amountOptions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "받을 금액 선택",
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnText
                )
                FinanceAmountOptionGrid(options = uiModel.amountOptions)
                Text(
                    text = uiModel.progressHintText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle
                )
            }
        }

        FinanceAdvanceReceiveCard(uiModel = uiModel)
        FinanceAdvanceHistoryCard(uiModel = uiModel)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FinancePrimaryButton(
                text = "미리받기 신청",
                modifier = Modifier.weight(1f),
                onClick = onRequestAdvance
            )
            FinanceGhostButton(
                text = "닫기",
                modifier = Modifier.weight(1f),
                onClick = onDismiss
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FinanceVaultBottomSheet(
    uiModel: FinanceVaultDetailUiModel,
    onDismiss: () -> Unit,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "예치 이자",
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnPrimary
                )
                Text(
                    text = "예치 이자",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = DawnText
                )
                Text(
                    text = uiModel.subtitleText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = DawnTextSubtle
                )
            }
            FinanceLinkButton(text = "닫기", onClick = onDismiss)
        }

        if (uiModel.isActive) {
            FinanceVaultActiveSection(uiModel = uiModel)
        } else {
            FinanceVaultOnboardingSection(uiModel = uiModel)
        }

        Text(
            text = uiModel.noteText,
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextSubtle
        )

        if (uiModel.isActive) {
            FinanceGhostButton(
                text = "닫기",
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinancePrimaryButton(
                    text = uiModel.actionText,
                    modifier = Modifier.weight(1f),
                    onClick = onAction
                )
                FinanceGhostButton(
                    text = "닫기",
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FinanceVaultOnboardingSection(uiModel: FinanceVaultDetailUiModel) {
    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetMutedBackground,
        borderColor = FinanceAdvanceSheetMutedBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "지금 예치 가능한 금액",
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnTextSubtle
                )
                Text(
                    text = uiModel.availableText,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = DawnText
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "예치 신청 금액",
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnText
                )
                FinanceAmountOptionGrid(options = uiModel.amountOptions)
            }
        }
    }

    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetHero,
        borderColor = FinanceAdvanceSheetHeroBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FinanceAdvanceLineItem(label = "예상 연이율", value = uiModel.aprText)
            FinanceAdvanceLineItem(label = "월 예상 이자", value = uiModel.monthlyInterestText)
            FinanceAdvanceLineItem(label = "일 예상 이자", value = uiModel.dailyInterestText)
        }
    }
}

@Composable
private fun FinanceVaultActiveSection(uiModel: FinanceVaultDetailUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FinanceSummaryMetricCard(
            label = "예치 잔액",
            value = uiModel.balanceText,
            modifier = Modifier.weight(1f)
        )
        FinanceSummaryMetricCard(
            label = "예상 연이율",
            value = uiModel.aprText,
            modifier = Modifier.weight(1f)
        )
    }

    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetHero,
        borderColor = FinanceAdvanceSheetHeroBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FinanceAdvanceLineItem(label = "누적 이자(추정)", value = uiModel.accruedInterestText)
            FinanceAdvanceLineItem(label = "월 예상 이자", value = uiModel.monthlyInterestText)
            FinanceAdvanceLineItem(label = "일 예상 이자", value = uiModel.dailyInterestText)
        }
    }

    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetMutedBackground,
        borderColor = FinanceAdvanceSheetMutedBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "월 수익 구성(추정)",
                style = MaterialTheme.typography.labelLarge,
                color = DawnText
            )
            FinanceAdvanceLineItem(label = "DeFi 운용 수익", value = uiModel.defiMonthlyText)
            FinanceAdvanceLineItem(label = "가불 수수료 기여분", value = uiModel.feeShareText)
            FinanceAdvanceLineItem(label = "합계", value = uiModel.totalMonthlyText)
        }
    }

    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetMutedBackground,
        borderColor = FinanceAdvanceSheetMutedBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "풀 운용 현황",
                style = MaterialTheme.typography.labelLarge,
                color = DawnText
            )
            FinanceRatioBar(
                label = "DeFi 운용 비율",
                value = uiModel.defiRatioText,
                progress = ratioTextToFloat(uiModel.defiRatioText)
            )
            FinanceRatioBar(
                label = "가불 풀 비율",
                value = uiModel.advanceRatioText,
                progress = ratioTextToFloat(uiModel.advanceRatioText)
            )
            FinanceAdvanceLineItem(label = "가불 풀 사용률", value = uiModel.advanceUsageText)
        }
    }
}

@Composable
private fun FinanceAdvanceSummaryCard(uiModel: FinanceAdvanceDetailUiModel) {
    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetHero,
        borderColor = FinanceAdvanceSheetHeroBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "지금 가능 금액",
                style = MaterialTheme.typography.labelLarge,
                color = DawnTextSubtle
            )
            Text(
                text = uiModel.availableText,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                color = DawnText
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinanceAdvanceInlineStat(
                    label = "누적 사용",
                    value = uiModel.usedText,
                    modifier = Modifier.weight(1f)
                )
                FinanceAdvanceInlineStat(
                    label = "상환 예정",
                    value = uiModel.repaymentDueText,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FinanceAdvanceWorkproofCard(uiModel: FinanceAdvanceDetailUiModel) {
    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetMutedBackground,
        borderColor = FinanceAdvanceSheetMutedBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "근무 캘린더 반영 현황",
                        style = MaterialTheme.typography.labelLarge,
                        color = DawnText
                    )
                    Text(
                        text = uiModel.calendarSummaryText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = DawnTextSubtle
                    )
                }
                Text(
                    text = "반영 기준 ${uiModel.updatedAtText}",
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnTextSubtle
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinanceAdvanceStatusCard(
                    label = "근무 반영",
                    value = uiModel.reflectedCountText,
                    borderColor = FinanceAdvanceSheetHeroBorder,
                    labelColor = DawnPrimary,
                    modifier = Modifier.weight(1f)
                )
                FinanceAdvanceStatusCard(
                    label = "확인 필요",
                    value = uiModel.reviewCountText,
                    borderColor = FinanceAdvanceSheetWarningBorder,
                    labelColor = Color(0xFFB97711),
                    modifier = Modifier.weight(1f)
                )
                FinanceAdvanceStatusCard(
                    label = "미반영",
                    value = uiModel.unreflectedCountText,
                    borderColor = FinanceAdvanceSheetMutedBorder,
                    labelColor = DawnTextSubtle,
                    modifier = Modifier.weight(1f)
                )
            }

            FinanceAdvanceCalendarGrid(days = uiModel.calendarDays)
        }
    }
}

@Composable
private fun FinanceAmountOptionGrid(options: List<FinanceAmountOptionUiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(4).forEach { optionRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                optionRow.forEach { option ->
                    val backgroundColor = if (option.selected) FinanceAdvanceSheetHero else Color.White
                    val borderColor = if (option.selected) DawnPrimary else DawnBorder
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(backgroundColor, RoundedCornerShape(18.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = DawnText
                        )
                    }
                }
                repeat(4 - optionRow.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FinanceAdvanceReceiveCard(uiModel: FinanceAdvanceDetailUiModel) {
    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetHero,
        borderColor = FinanceAdvanceSheetHeroBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FinanceAdvanceLineItem(
                label = "이번 수령 예정",
                value = uiModel.receiveAmountText
            )
            FinanceAdvanceLineItem(
                label = "수수료",
                value = uiModel.feeText
            )
            FinanceAdvanceLineItem(
                label = "선택 금액",
                value = uiModel.requestAmountText
            )
        }
    }
}

@Composable
private fun FinanceAdvanceHistoryCard(uiModel: FinanceAdvanceDetailUiModel) {
    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetMutedBackground,
        borderColor = FinanceAdvanceSheetMutedBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "이번 달 미리받기 이력",
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnText
                )
                Text(
                    text = uiModel.tierText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    color = DawnPrimary
                )
            }

            uiModel.historyItems.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = DawnText
                        )
                        Text(
                            text = item.metaText,
                            style = MaterialTheme.typography.labelMedium,
                            color = DawnTextSubtle
                        )
                    }
                    Text(
                        text = item.valueText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                        color = DawnText
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanceAdvanceCalendarGrid(days: List<FinanceAdvanceCalendarDayUiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                week.forEach { day ->
                    val backgroundColor = when (day.tone) {
                        FinanceAdvanceCalendarTone.COMPLETE -> Color.White
                        FinanceAdvanceCalendarTone.MODIFIED -> Color(0xFFFFFBEB)
                        FinanceAdvanceCalendarTone.TODAY -> FinanceAdvanceSheetHero
                        FinanceAdvanceCalendarTone.DEFAULT -> Color.White
                    }
                    val borderColor = when (day.tone) {
                        FinanceAdvanceCalendarTone.COMPLETE -> FinanceAdvanceSheetHeroBorder
                        FinanceAdvanceCalendarTone.MODIFIED -> FinanceAdvanceSheetWarningBorder
                        FinanceAdvanceCalendarTone.TODAY -> DawnPrimary
                        FinanceAdvanceCalendarTone.DEFAULT -> DawnBorder
                    }
                    val textColor = when (day.tone) {
                        FinanceAdvanceCalendarTone.COMPLETE -> DawnText
                        FinanceAdvanceCalendarTone.MODIFIED -> Color(0xFFB97711)
                        FinanceAdvanceCalendarTone.TODAY -> DawnPrimary
                        FinanceAdvanceCalendarTone.DEFAULT -> FinanceAdvanceSheetDefaultText
                    }
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor, RoundedCornerShape(14.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.day.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                        }
                    }
                }
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FinanceSheetPanel(
    backgroundColor: Color,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(24.dp))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun FinanceAdvanceInlineStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = DawnTextSubtle
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
    }
}

@Composable
private fun FinanceAdvanceStatusCard(
    label: String,
    value: String,
    borderColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
    }
}

@Composable
private fun FinanceAdvanceLineItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
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
private fun FinanceSummaryMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(FinanceAdvanceSheetMutedBackground, RoundedCornerShape(24.dp))
            .border(1.dp, FinanceAdvanceSheetMutedBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
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
private fun FinanceRatioBar(
    label: String,
    value: String,
    progress: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = DawnTextSubtle
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                color = DawnText
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(DawnPrimary, RoundedCornerShape(999.dp))
            )
        }
    }
}

private fun ratioTextToFloat(value: String): Float {
    val numeric = value.removeSuffix("%").toFloatOrNull() ?: return 0f
    return numeric / 100f
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
                        text = "급여 점검",
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
private fun FinanceSurfaceCard(
    hero: Boolean = false,
    content: @Composable () -> Unit
) {
    val backgroundBrush = if (hero) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF8F4FF)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFCFAFF)
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DawnSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, DawnBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
        ) {
            Column(
                modifier = Modifier.padding(if (hero) 20.dp else 18.dp),
                verticalArrangement = Arrangement.spacedBy(if (hero) 16.dp else 12.dp)
            ) {
                content()
            }
        }
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
