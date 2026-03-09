package com.dondone.mobile.app

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
import com.dondone.mobile.core.designsystem.DawnBackground
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnSecondary
import com.dondone.mobile.core.designsystem.DawnSurface
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.DonDoneWordmark

@Composable
fun DonDoneApp(
    viewModel: DemoSessionViewModel
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route.orEmpty()
    val chrome = resolveScreenChrome(currentRoute, uiState.remittance.flowStep)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DawnBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DawnBackground)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (chrome.showRootTabs) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (currentRoute == Route.HOME) {
                                DonDoneWordmark()
                            } else {
                                Text(
                                    text = chrome.title,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = "${uiState.demo.year}.${uiState.demo.month.toString().padStart(2, '0')}.${uiState.demo.asOfDay.toString().padStart(2, '0')}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DawnTextSubtle
                                )
                            }
                        }
                        if (chrome.showSettingsAction) {
                            IconButton(onClick = { navController.navigate(Route.MENU) }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "설정",
                                    tint = DawnTextSubtle
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
                                .background(DawnSurface)
                                .border(1.dp, DawnBorder, RoundedCornerShape(16.dp))
                                .clickable { navController.navigateUp() }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로",
                                tint = DawnTextSubtle
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = chrome.title, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "${uiState.demo.year}.${uiState.demo.month.toString().padStart(2, '0')}.${uiState.demo.asOfDay.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.labelLarge,
                                color = DawnTextSubtle
                            )
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
                    color = DawnSurface,
                    shadowElevation = 14.dp
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
                                    .background(if (selected) DawnSecondary else Color.Transparent)
                                    .border(
                                        width = if (selected) 1.dp else 0.dp,
                                        color = if (selected) DawnBorder else Color.Transparent,
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
                                    tint = if (selected) DawnPrimary else DawnTextSubtle
                                )
                                Text(
                                    text = tab.label,
                                    color = if (selected) DawnPrimary else DawnTextSubtle,
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
