package com.dondone.mobile.feature.finance.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle

private val AccountManageCanvas = Color.White
private val AccountManageDivider = Color(0xFFE8EBF0)

@Composable
fun AccountManageScreen(
    uiModel: AccountManageUiModel,
    onSelectAccount: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AccountManageCanvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        ManageSection(
            title = "내 계좌",
            actionText = "계좌 추가"
        ) {
            uiModel.accounts.forEachIndexed { index, account ->
                ManageRow(
                    title = account.name,
                    subtitle = "${account.number} · ${account.balanceText}",
                    selected = account.selected,
                    onClick = { onSelectAccount(account.id) }
                )
                if (index != uiModel.accounts.lastIndex) {
                    HorizontalDivider(color = AccountManageDivider)
                }
            }
        }

        ManageSectionDivider()

        ManageSection(
            title = "수신 지갑",
            actionText = "지갑 추가"
        ) {
            uiModel.recipientWallets.forEachIndexed { index, wallet ->
                ManageRow(
                    title = wallet.name,
                    subtitle = wallet.address,
                    selected = wallet.selected,
                    onClick = {}
                )
                if (index != uiModel.recipientWallets.lastIndex) {
                    HorizontalDivider(color = AccountManageDivider)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ManageSection(
    title: String,
    actionText: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = DawnText
                )
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnPrimary
                )
            }
            content()
        }
    )
}

@Composable
private fun ManageSectionDivider() {
    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = AccountManageDivider)
    Spacer(modifier = Modifier.height(14.dp))
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
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) DawnPrimary else DawnText
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = DawnPrimary
            )
        }
    }
}
