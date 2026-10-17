package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.ui.MainTab
import com.example.ui.MobiWaveViewModel
import com.example.ui.components.MobileBottomNavBar
import com.example.ui.components.MobileTopAppBar
import com.example.ui.components.ToastNotification

@Composable
fun MainContainer(
    viewModel: MobiWaveViewModel
) {
    val configuration = LocalConfiguration.current
    val isTabletWidth = configuration.screenWidthDp >= 700
    val isWebDashboardMode by viewModel.isWebDashboardMode.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val showCreateBatchModal by viewModel.showCreateBatchModal.collectAsState()
    val showInvoiceDetailModal by viewModel.showInvoiceDetailModal.collectAsState()
    val showStkPushModal by viewModel.showStkPushModal.collectAsState()
    val notification by viewModel.notification.collectAsState()

    // Render Web Dashboard when in expanded width or when explicitly toggled
    val displayWeb = isWebDashboardMode || isTabletWidth

    Box(modifier = Modifier.fillMaxSize()) {
        if (displayWeb) {
            WebDashboardScreen(viewModel = viewModel)
        } else {
            Scaffold(
                topBar = {
                    MobileTopAppBar(
                        title = when (currentTab) {
                            MainTab.HOME -> "Home"
                            MainTab.EXPENSES -> "My Expenses"
                            MainTab.CAPTURE -> "Capture Receipt"
                            MainTab.ADVANCES -> "My Advances"
                            MainTab.APPROVALS -> "Approvals"
                            MainTab.ACCOUNT -> "Account & profile"
                        },
                        subtitle = "DEMO DATA • Not connected to live account",
                        avatarUrl = viewModel.avatarUrl,
                        onAvatarClick = {
                            viewModel.switchTab(MainTab.ACCOUNT)
                        },
                        onBackClick = if (currentTab != MainTab.HOME) {
                            { viewModel.switchTab(MainTab.HOME) }
                        } else null,
                        onToggleWebMode = {
                            viewModel.toggleWebDashboardMode(true)
                            viewModel.postNotification("Switched to Controllership Web Dashboard")
                        }
                    )
                },
                bottomBar = {
                    MobileBottomNavBar(
                        currentTab = currentTab,
                        onTabSelected = { viewModel.switchTab(it) },
                        onScanClick = { viewModel.switchTab(MainTab.CAPTURE) }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentTab) {
                        MainTab.HOME -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToScan = { viewModel.switchTab(MainTab.CAPTURE) },
                            onNavigateToExpenses = { viewModel.switchTab(MainTab.EXPENSES) },
                            onNavigateToAdvances = { viewModel.switchTab(MainTab.ADVANCES) }
                        )
                        MainTab.EXPENSES -> ExpensesScreen(
                            viewModel = viewModel,
                            onNavigateToScan = { viewModel.switchTab(MainTab.CAPTURE) }
                        )
                        MainTab.CAPTURE -> CaptureReceiptScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.switchTab(MainTab.HOME) }
                        )
                        MainTab.ADVANCES -> AdvancesScreen(
                            viewModel = viewModel,
                            onNavigateToScan = { viewModel.switchTab(MainTab.CAPTURE) }
                        )
                        MainTab.APPROVALS -> ApprovalsScreen(
                            viewModel = viewModel
                        )
                        MainTab.ACCOUNT -> AccountScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Modals (Available across all screen types)
        if (showCreateBatchModal) {
            CreateBatchModal(viewModel = viewModel)
        }

        if (showInvoiceDetailModal) {
            InvoiceVerificationModal(viewModel = viewModel)
        }

        if (showStkPushModal) {
            StkPushModal(viewModel = viewModel)
        }

        // Animated Toast Overlay
        AnimatedVisibility(
            visible = notification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            notification?.let { notif ->
                ToastNotification(
                    message = notif.message,
                    isError = notif.isError,
                    onDismiss = { viewModel.dismissNotification() }
                )
            }
        }
    }
}
