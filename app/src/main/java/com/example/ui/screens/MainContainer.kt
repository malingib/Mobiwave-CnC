package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    viewModel: MobiWaveViewModel,
    signedInEmail: String? = null,
    accessToken: String? = null,
    onSignOut: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isTabletWidth = configuration.screenWidthDp >= 700
    val isWebDashboardMode by viewModel.isWebDashboardMode.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val showCreateBatchModal by viewModel.showCreateBatchModal.collectAsState()
    val showInvoiceDetailModal by viewModel.showInvoiceDetailModal.collectAsState()
    val showStkPushModal by viewModel.showStkPushModal.collectAsState()
    val notification by viewModel.notification.collectAsState()
    var showManualReviewQueue by remember { mutableStateOf(false) }
    var showFinanceOperations by remember { mutableStateOf(false) }

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
                        },
                        subtitle = signedInEmail ?: "Authenticated organisation session",
                        avatarUrl = viewModel.avatarUrl,
                        onAvatarClick = onSignOut,
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (configuration.screenWidthDp < 380) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("Authenticated session", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(onClick = { showFinanceOperations = true }) { Text("Finance") }
                                TextButton(onClick = { showManualReviewQueue = true }) { Text("Invoice review") }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Authenticated session", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { showFinanceOperations = true }) { Text("Finance") }
                                TextButton(onClick = { showManualReviewQueue = true }) { Text("Invoice review") }
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
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
                    }
                    }
                }
            }
        }

        if (showFinanceOperations) {
            FinanceOperationsScreen(
                accessToken = accessToken.orEmpty(),
                onClose = { showFinanceOperations = false }
            )
        }

        if (showManualReviewQueue) {
            ManualReviewQueueScreen(
                accessToken = accessToken.orEmpty(),
                onClose = { showManualReviewQueue = false }
            )
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
