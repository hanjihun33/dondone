package com.dondone.mobile.feature.menu.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.DonDoneCard
import com.dondone.mobile.core.designsystem.PillButton
import com.dondone.mobile.core.designsystem.SectionPanel
import com.dondone.mobile.core.designsystem.StatusBadge

@Composable
fun MenuScreen(
    uiModel: MenuUiModel,
    onShiftAsOf: (Int) -> Unit,
    onResetSeed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DonDoneCard(kicker = "문서", title = "문서와 영수증") {
            uiModel.documents.forEach { document ->
                SectionPanel {
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = document.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = document.updatedAtText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(
                            text = document.statusText,
                            tone = document.statusTone
                        )
                    }
                }
            }
        }

        DonDoneCard(kicker = "데모", title = "Time Travel") {
            Text(
                text = "현재 기준일: ${uiModel.currentDateText}",
                style = MaterialTheme.typography.bodyLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(text = "전날", onClick = { onShiftAsOf(-1) })
                PillButton(text = "다음날", onClick = { onShiftAsOf(1) })
                PillButton(text = "시드 초기화", onClick = onResetSeed)
            }
        }
    }
}
