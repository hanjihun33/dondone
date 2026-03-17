package com.dondone.mobile.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dondone.mobile.app.navigation.AppBackAction
import com.dondone.mobile.app.navigation.DonDoneNavGraph
import com.dondone.mobile.app.navigation.Route
import com.dondone.mobile.app.navigation.navigateToRootTab
import com.dondone.mobile.app.navigation.resolveAppBackAction
import com.dondone.mobile.app.navigation.resolveScreenChrome
import com.dondone.mobile.app.navigation.showTransferStep
import com.dondone.mobile.app.navigation.shouldResetWorkproofUiState
import com.dondone.mobile.app.session.DemoSessionViewModel
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.designsystem.DonDoneToastHost
import com.dondone.mobile.core.designsystem.rememberDonDoneToastState
import com.dondone.mobile.domain.model.DemoInfo
import com.dondone.mobile.feature.auth.presentation.LoginLoadingScreen
import com.dondone.mobile.feature.auth.presentation.LoginScreen

@Composable
fun DonDoneApp(
    viewModel: DemoSessionViewModel
) {
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()

    if (authUiState.isRestoring) {
        LoginLoadingScreen()
        return
    }

    if (!authUiState.isAuthenticated) {
        LoginScreen(
            uiState = authUiState,
            onLogin = viewModel::login,
            onSignup = viewModel::signup,
            onFieldEdited = viewModel::clearAuthError
        )
        return
    }

    AuthenticatedDonDoneAppShell(viewModel = viewModel)
}

@Composable
private fun AuthenticatedDonDoneAppShell(
    viewModel: DemoSessionViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastState = rememberDonDoneToastState()
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentRoute = currentDestination?.route.orEmpty()
    val remittance = uiState.remittance
    val workproofShellState = rememberWorkproofShellState(currentRoute)

    val chrome = resolveScreenChrome(
        route = currentRoute,
        transferStep = remittance.flowStep,
        transferStatus = remittance.status,
        isWorkproofDetailVisible = workproofShellState.isDetailVisible
    )
    val topBarState = resolveAppTopBarState(
        currentRoute = currentRoute,
        showRootTabs = chrome.showRootTabs,
        showSettingsAction = chrome.showSettingsAction,
        headerTitle = chrome.headerTitle,
        headerDateText = uiState.demo.toHeaderDateTextOrNull(showDate = chrome.showDate)
    )
    val rootTabs = currentDestination.toRootTabUiStates(currentRoute)

    fun handleBack() {
        when (val action = resolveAppBackAction(currentRoute, remittance)) {
            AppBackAction.NavigateUp -> navController.navigateUp()
            AppBackAction.DismissTransferConfirmation -> {
                viewModel.dismissTransferConfirmation()
            }

            is AppBackAction.ShowTransferStep -> {
                viewModel.showTransferStep(action.step)
            }
        }
    }

    BackHandler(
        enabled = currentRoute == Route.TRANSFER,
        onBack = ::handleBack
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ChromeSurface,
            topBar = {
                AppTopBar(
                    state = topBarState,
                    onBack = ::handleBack,
                    onMenuClick = { navController.navigateToRootTab(Route.MENU) }
                )
            },
            bottomBar = {
                RootBottomBar(
                    visible = chrome.showRootTabs,
                    tabs = rootTabs,
                    onTabClick = { route -> navController.navigateToRootTab(route) }
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
                workproofResetVersion = workproofShellState.resetVersion,
                onNavigateToRootTab = { route -> navController.navigateToRootTab(route) },
                onWorkproofDetailVisibilityChange = workproofShellState.onDetailVisibilityChange,
                onShowToast = { message, tone ->
                    toastState.show(message = message, tone = tone)
                }
            )
        }

        DonDoneToastHost(
            state = toastState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 28.dp)
        )
    }
}

@Composable
private fun rememberWorkproofShellState(
    currentRoute: String
): WorkproofShellState {
    var isDetailVisible by rememberSaveable { mutableStateOf(false) }
    var resetVersion by rememberSaveable { mutableStateOf(0) }
    var previousRoute by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(currentRoute) {
        if (shouldResetWorkproofUiState(previousRoute, currentRoute)) {
            isDetailVisible = false
            resetVersion += 1
        }
        previousRoute = currentRoute
    }

    return WorkproofShellState(
        isDetailVisible = isDetailVisible,
        resetVersion = resetVersion,
        onDetailVisibilityChange = { visible ->
            isDetailVisible = visible
        }
    )
}

private fun DemoInfo.toHeaderDateTextOrNull(showDate: Boolean): String? {
    if (!showDate) {
        return null
    }

    return "$year.${month.toString().padStart(2, '0')}.${asOfDay.toString().padStart(2, '0')}"
}

private data class WorkproofShellState(
    val isDetailVisible: Boolean,
    val resetVersion: Int,
    val onDetailVisibilityChange: (Boolean) -> Unit
)
