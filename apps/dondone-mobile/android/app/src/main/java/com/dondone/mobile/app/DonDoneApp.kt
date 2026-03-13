package com.dondone.mobile.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus

private val ChromeTextMuted = Color(0xFF8B95A1)
private val ChromeAccent = Color(0xFF6D68F5)
private val ChromeAccentSoft = Color(0xFFF2F3FF)
private val ChromeBorder = Color(0xFFE8EBF0)
private val ChromeBackSurface = Color(0xFFF7F8FA)

@Composable
fun DonDoneApp(
    viewModel: DemoSessionViewModel
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route.orEmpty()
    val chrome = resolveScreenChrome(
        route = currentRoute,
        transferStep = uiState.remittance.flowStep,
        transferStatus = uiState.remittance.status
    )
    val handleBack: () -> Unit = {
        if (currentRoute == Route.TRANSFER) {
            when {
                uiState.remittance.status == TransferStatus.REVIEWING -> {
                    viewModel.dismissTransferConfirmation()
                }
                uiState.remittance.status == TransferStatus.SUBMITTED ||
                    uiState.remittance.status == TransferStatus.CONFIRMED -> navController.navigateUp()
                uiState.remittance.flowStep == TransferFlowStep.AMOUNT -> viewModel.showRecipientStep()
                uiState.remittance.flowStep == TransferFlowStep.RECIPIENT -> {
                    if (uiState.remittance.stepReturnTarget == TransferFlowStep.AMOUNT) {
                        viewModel.showAmountStep()
                    } else {
                        viewModel.showAccountStep()
                    }
                }
                uiState.remittance.flowStep == TransferFlowStep.ACCOUNT -> {
                    when (uiState.remittance.stepReturnTarget) {
                        TransferFlowStep.RECIPIENT -> viewModel.showRecipientStep()
                        TransferFlowStep.AMOUNT -> viewModel.showAmountStep()
                        null -> navController.navigateUp()
                        TransferFlowStep.ACCOUNT -> navController.navigateUp()
                    }
                }
            }
        } else {
            navController.navigateUp()
        }
    }

    BackHandler(enabled = currentRoute == Route.TRANSFER) {
        handleBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (chrome.showRootTabs) {
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
                            if (currentRoute == Route.HOME) {
                                DonDoneWordmark()
                            } else {
                                Text(
                                    text = chrome.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
                                )
                                if (chrome.showDate) {
                                    Text(
                                        text = "${uiState.demo.year}.${uiState.demo.month.toString().padStart(2, '0')}.${uiState.demo.asOfDay.toString().padStart(2, '0')}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ChromeTextMuted
                                    )
                                }
                            }
                        }
                        if (chrome.showSettingsAction) {
                            IconButton(onClick = { navController.navigate(Route.MENU) }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "설정",
                                    tint = ChromeTextMuted
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(ChromeBackSurface)
                                .border(1.dp, ChromeBorder, RoundedCornerShape(16.dp))
                                .clickable(onClick = handleBack)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로",
                                tint = ChromeTextMuted
                            )
                        }
                        if (chrome.title.isNotBlank() || chrome.showDate) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (chrome.title.isNotBlank()) {
                                    Text(text = chrome.title, style = MaterialTheme.typography.titleLarge)
                                }
                                if (chrome.showDate) {
                                    Text(
                                        text = "${uiState.demo.year}.${uiState.demo.month.toString().padStart(2, '0')}.${uiState.demo.asOfDay.toString().padStart(2, '0')}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ChromeTextMuted
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (chrome.showRootTabs) {
                Surface(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mainTabs.forEach { tab ->
                            val selected = currentDestination
                                ?.hierarchy
                                ?.any { destination ->
                                    destination.route == tab.rootRoute || currentRoute.startsWith(tab.rootRoute)
                                } == true

                            val icon = when (tab.rootRoute) {
                                Route.HOME -> Icons.Default.Home
                                Route.FINANCE_HOME -> Icons.Default.AccountBalanceWallet
                                Route.WORKPROOF -> Icons.Default.WorkHistory
                                else -> Icons.Default.Description
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) ChromeAccentSoft else Color.Transparent)
                                    .border(
                                        width = if (selected) 1.dp else 0.dp,
                                        color = if (selected) ChromeBorder else Color.Transparent,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        navController.navigate(tab.rootRoute) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.label,
                                    tint = if (selected) ChromeAccent else ChromeTextMuted
                                )
                                Text(
                                    text = tab.label,
                                    color = if (selected) ChromeAccent else ChromeTextMuted,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding: PaddingValues ->
        DonDoneNavGraph(
            modifier = Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ),
            navController = navController,
            viewModel = viewModel
        )
    }
}
