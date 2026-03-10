package com.dondone.mobile.feature.finance.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnSurface
import com.dondone.mobile.core.designsystem.DawnTextSubtle

@Composable
fun AccountManageScreen(
    uiModel: AccountManageUiModel,
    onSelectAccount: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ManageSection(
            title = "내 계좌",
            actionText = "계좌 추가"
        ) {
            uiModel.accounts.forEach { account ->
                ManageRow(
                    title = account.name,
                    subtitle = "${account.number} · ${account.balanceText}",
                    selected = account.selected,
                    onClick = { onSelectAccount(account.id) }
                )
            }
        }

        ManageSection(
            title = "수신 지갑",
            actionText = "지갑 추가"
        ) {
            uiModel.recipientWallets.forEach { wallet ->
                ManageRow(
                    title = wallet.name,
                    subtitle = wallet.address,
                    selected = wallet.selected,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun ManageSection(
    title: String,
    actionText: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(28.dp))
            .border(1.dp, DawnBorder, RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = DawnTextSubtle
            )
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = DawnPrimary
            )
        }
        content()
    }
}

@Composable
private fun ManageRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DawnSurface, RoundedCornerShape(22.dp))
            .border(
                width = 1.dp,
                color = if (selected) DawnPrimary.copy(alpha = 0.7f) else DawnBorder,
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) DawnPrimary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
        }
    }
}
