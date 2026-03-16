package com.dondone.mobile.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dondone.mobile.app.navigation.DonDoneNavGraph
import com.dondone.mobile.app.navigation.Route
import com.dondone.mobile.app.navigation.mainTabs
import com.dondone.mobile.app.navigation.resolveScreenChrome
import com.dondone.mobile.app.session.DemoSessionViewModel
import com.dondone.mobile.core.designsystem.DonDoneWordmark
import com.dondone.mobile.core.designsystem.pressableScale
import com.dondone.mobile.core.designsystem.rememberDonDoneGrayRipple
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus

private val ChromeTextStrong = Color(0xFF191F28)
private val ChromeTextMuted = Color(0xFFB0B8C1)
private val ChromeAccent = Color(0xFF9AA4B2)
private val ChromeBorder = Color(0xFFF0F2F5)
private val ChromeSurface = Color.White

@Composable
fun DonDoneApp(
    viewModel: DemoSessionViewModel
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentRoute = currentDestination?.route.orEmpty()
    val remittance = uiState.remittance
    var isWorkproofDetailVisible by rememberSaveable { mutableStateOf(false) }

    val chrome = resolveScreenChrome(
        route = currentRoute,
        transferStep = remittance.flowStep,
        transferStatus = remittance.status,
        isWorkproofDetailVisible = isWorkproofDetailVisible
    )

    val headerTitle = chrome.title.takeIf(String::isNotBlank)
    val headerDateText = uiState.demo.run {
        "$year.${month.toString().padStart(2, '0')}.${asOfDay.toString().padStart(2, '0')}"
    }.takeIf { chrome.showDate }

    fun navigateToRootTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun handleBack() {
        if (currentRoute != Route.TRANSFER) {
            navController.navigateUp()
            return
        }

        when (remittance.status) {
            TransferStatus.REVIEWING -> {
                viewModel.dismissTransferConfirmation()
                return
            }
            TransferStatus.SUBMITTED,
            TransferStatus.CONFIRMED -> {
                navController.navigateUp()
                return
            }
            TransferStatus.IDLE -> Unit
        }

        val previousStep = remittance.flowStep.previousStep(remittance.stepReturnTarget)
            ?: run {
                navController.navigateUp()
                return
            }

        viewModel.showTransferStep(previousStep)
    }

    BackHandler(
        enabled = currentRoute == Route.TRANSFER,
        onBack = ::handleBack
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ChromeSurface,
        topBar = {
            AppTopBar(
                showRootTabs = chrome.showRootTabs,
                showSettingsAction = chrome.showSettingsAction,
                currentRoute = currentRoute,
                headerTitle = headerTitle,
                headerDateText = headerDateText,
                onBack = ::handleBack,
                onMenuClick = { navController.navigate(Route.MENU) }
            )
        },
        bottomBar = {
            RootBottomBar(
                visible = chrome.showRootTabs,
                currentDestination = currentDestination,
                currentRoute = currentRoute,
                onTabClick = ::navigateToRootTab
            )
        }
    ) { innerPadding ->
        DonDoneNavGraph(
            modifier = Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ),
            navController = navController,
            viewModel = viewModel,
            onWorkproofDetailVisibilityChange = { visible ->
                isWorkproofDetailVisible = visible
            }
        )
    }
}

@Composable
private fun AppTopBar(
    showRootTabs: Boolean,
    showSettingsAction: Boolean,
    currentRoute: String,
    headerTitle: String?,
    headerDateText: String?,
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
                vertical = if (showRootTabs && headerTitle == null && headerDateText == null && !showSettingsAction) {
                    0.dp
                } else {
                    10.dp
                }
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showRootTabs && headerTitle == null && headerDateText == null && !showSettingsAction) {
            Box(modifier = Modifier.heightIn(min = 4.dp))
        } else if (showRootTabs) {
            RootTopBar(
                currentRoute = currentRoute,
                headerTitle = headerTitle,
                headerDateText = headerDateText,
                showSettingsAction = showSettingsAction,
                onMenuClick = onMenuClick
            )
        } else {
            ChildTopBar(
                headerTitle = headerTitle,
                headerDateText = headerDateText,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun RootTopBar(
    currentRoute: String,
    headerTitle: String?,
    headerDateText: String?,
    showSettingsAction: Boolean,
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
            when (currentRoute) {
                Route.HOME -> DonDoneWordmark()
                else -> HeaderTextBlock(
                    title = headerTitle,
                    dateText = headerDateText,
                    titleStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showSettingsAction) {
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
            .pressableScale(
                interactionSource = interactionSource,
                pressedScale = 0.94f
            )
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
private fun RootBottomBar(
    visible: Boolean,
    currentDestination: NavDestination?,
    currentRoute: String,
    onTabClick: (String) -> Unit
) {
    if (!visible) return

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
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
                mainTabs.forEach { tab ->
                    RootTabItem(
                        rootRoute = tab.rootRoute,
                        label = tab.label,
                        selected = currentDestination.isSelectedTab(
                            rootRoute = tab.rootRoute,
                            currentRoute = currentRoute
                        ),
                        onClick = { onTabClick(tab.rootRoute) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.RootTabItem(
    rootRoute: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = selected.toTabColors()
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .pressableScale(
                interactionSource = interactionSource,
                pressedScale = 0.96f
            )
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
            imageVector = tabIcon(rootRoute),
            contentDescription = label,
            tint = colors.iconTint
        )
        Text(
            text = label,
            color = colors.labelTint,
            style = if (selected) {
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

private fun DemoSessionViewModel.showTransferStep(step: TransferFlowStep) =
    when (step) {
        TransferFlowStep.AMOUNT -> showAmountStep()
        TransferFlowStep.RECIPIENT -> showRecipientStep()
        TransferFlowStep.ACCOUNT -> showAccountStep()
    }

private fun TransferFlowStep.previousStep(
    target: TransferFlowStep?
): TransferFlowStep? =
    when (this) {
        TransferFlowStep.AMOUNT -> TransferFlowStep.RECIPIENT
        TransferFlowStep.RECIPIENT -> when (target) {
            TransferFlowStep.AMOUNT -> TransferFlowStep.AMOUNT
            TransferFlowStep.ACCOUNT -> TransferFlowStep.ACCOUNT
            else -> null
        }
        TransferFlowStep.ACCOUNT -> when (target) {
            TransferFlowStep.RECIPIENT -> TransferFlowStep.RECIPIENT
            TransferFlowStep.AMOUNT -> TransferFlowStep.AMOUNT
            else -> null
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

private data class TabColors(
    val iconTint: Color,
    val labelTint: Color
)
