package com.dondone.mobile.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.dondone.mobile.app.navigation.Route
import com.dondone.mobile.app.navigation.mainTabs
import com.dondone.mobile.core.designsystem.DonDoneWordmark
import com.dondone.mobile.core.designsystem.rememberDonDoneGrayRipple

private val ChromeTextStrong = Color(0xFF191F28)
private val ChromeTextMuted = Color(0xFFB0B8C1)
private val ChromeAccent = Color(0xFF9AA4B2)
private val ChromeBorder = Color(0xFFF0F2F5)
internal val ChromeSurface = Color.White

internal enum class AppTopBarLayout {
    COLLAPSED_ROOT,
    ROOT,
    CHILD
}

internal data class AppTopBarState(
    val layout: AppTopBarLayout,
    val showWordmark: Boolean,
    val headerTitle: String?,
    val headerDateText: String?,
    val showSettingsAction: Boolean
) {
    fun rootState(): RootTopBarState {
        return RootTopBarState(
            showWordmark = showWordmark,
            headerTitle = headerTitle,
            headerDateText = headerDateText,
            showSettingsAction = showSettingsAction
        )
    }
}

internal data class RootTopBarState(
    val showWordmark: Boolean,
    val headerTitle: String?,
    val headerDateText: String?,
    val showSettingsAction: Boolean
)

internal data class RootTabUiState(
    val rootRoute: String,
    val label: String,
    val selected: Boolean,
    val icon: ImageVector,
    val colors: TabColors
)

internal data class TabColors(
    val iconTint: Color,
    val labelTint: Color
)

internal fun resolveAppTopBarState(
    currentRoute: String,
    showRootTabs: Boolean,
    showSettingsAction: Boolean,
    headerTitle: String?,
    headerDateText: String?
): AppTopBarState {
    val layout = when {
        showRootTabs && headerTitle == null && headerDateText == null && !showSettingsAction -> {
            AppTopBarLayout.COLLAPSED_ROOT
        }
        showRootTabs -> AppTopBarLayout.ROOT
        else -> AppTopBarLayout.CHILD
    }

    return AppTopBarState(
        layout = layout,
        showWordmark = currentRoute == Route.HOME,
        headerTitle = headerTitle,
        headerDateText = headerDateText,
        showSettingsAction = showSettingsAction
    )
}

internal fun NavDestination?.toRootTabUiStates(
    currentRoute: String
): List<RootTabUiState> {
    return mainTabs.map { tab ->
        val selected = isSelectedTab(
            rootRoute = tab.rootRoute,
            currentRoute = currentRoute
        )
        RootTabUiState(
            rootRoute = tab.rootRoute,
            label = tab.label,
            selected = selected,
            icon = tabIcon(tab.rootRoute),
            colors = selected.toTabColors()
        )
    }
}

@Composable
internal fun AppTopBar(
    state: AppTopBarState,
    onBack: () -> Unit,
    onMenuClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChromeSurface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                horizontal = 16.dp,
                vertical = if (state.layout == AppTopBarLayout.COLLAPSED_ROOT) {
                    0.dp
                } else {
                    10.dp
                }
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (state.layout) {
            AppTopBarLayout.COLLAPSED_ROOT -> {
                Box(modifier = Modifier.heightIn(min = 4.dp))
            }
            AppTopBarLayout.ROOT -> {
                RootTopBar(
                    state = state.rootState(),
                    onMenuClick = onMenuClick
                )
            }
            AppTopBarLayout.CHILD -> {
                ChildTopBar(
                    headerTitle = state.headerTitle,
                    headerDateText = state.headerDateText,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun RootTopBar(
    state: RootTopBarState,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 42.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (state.showWordmark) {
                DonDoneWordmark()
            } else {
                HeaderTextBlock(
                    title = state.headerTitle,
                    dateText = state.headerDateText,
                    titleStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (state.showSettingsAction) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "설정",
                    tint = ChromeTextMuted
                )
            }
        }
    }
}

@Composable
private fun ChildTopBar(
    headerTitle: String?,
    headerDateText: String?,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackButton(onClick = onBack)

        HeaderTextBlock(
            title = headerTitle,
            dateText = headerDateText,
            titleStyle = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BackButton(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
        contentDescription = "뒤로",
        modifier = Modifier
            .size(32.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberDonDoneGrayRipple(bounded = false),
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        tint = ChromeTextMuted
    )
}

@Composable
internal fun RootBottomBar(
    visible: Boolean,
    tabs: List<RootTabUiState>,
    onTabClick: (String) -> Unit
) {
    if (!visible) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = ChromeSurface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChromeSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ChromeBorder)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { tab ->
                    RootTabItem(
                        tab = tab,
                        onClick = { onTabClick(tab.rootRoute) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.RootTabItem(
    tab: RootTabUiState,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = rememberDonDoneGrayRipple(),
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tab.colors.iconTint
        )
        Text(
            text = tab.label,
            color = tab.colors.labelTint,
            style = if (tab.selected) {
                MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
            } else {
                MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
            }
        )
    }
}

@Composable
private fun HeaderTextBlock(
    title: String?,
    dateText: String?,
    titleStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    if (title == null && dateText == null) {
        Box(modifier = modifier)
        return
    }

    Column(modifier = modifier) {
        title?.let { Text(text = it, style = titleStyle) }
        dateText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = ChromeTextMuted
            )
        }
    }
}

private fun NavDestination?.isSelectedTab(
    rootRoute: String,
    currentRoute: String
): Boolean =
    this?.hierarchy?.any { it.route == rootRoute } == true ||
        currentRoute.startsWith(rootRoute)

private fun tabIcon(route: String): ImageVector = when (route) {
    Route.HOME -> Icons.Default.Home
    Route.FINANCE_HOME -> Icons.Default.AccountBalanceWallet
    Route.WORKPROOF -> Icons.Default.WorkHistory
    else -> Icons.Default.Description
}

private fun Boolean.toTabColors(): TabColors =
    if (this) {
        TabColors(
            iconTint = ChromeTextStrong,
            labelTint = ChromeTextStrong
        )
    } else {
        TabColors(
            iconTint = ChromeAccent,
            labelTint = ChromeTextMuted
        )
    }
