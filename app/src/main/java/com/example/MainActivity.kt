package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainTab
import com.example.ui.MobiWaveViewModel
import com.example.ui.screens.MainContainer
import com.example.ui.theme.MobiWaveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobiWaveTheme {
                val viewModel: MobiWaveViewModel = viewModel()
                val isWebDashboardMode by viewModel.isWebDashboardMode.collectAsState()
                val currentTab by viewModel.currentTab.collectAsState()
                val showCreateBatch by viewModel.showCreateBatchModal.collectAsState()
                val showInvoiceDetail by viewModel.showInvoiceDetailModal.collectAsState()
                val showStkPush by viewModel.showStkPushModal.collectAsState()

                // Predictive BackHandler for natural navigation
                BackHandler(enabled = showCreateBatch || showInvoiceDetail || showStkPush || isWebDashboardMode || currentTab != MainTab.HOME) {
                    when {
                        showStkPush -> viewModel.closeStkPushModal()
                        showInvoiceDetail -> viewModel.closeInvoiceDetailModal()
                        showCreateBatch -> viewModel.closeCreateBatchModal()
                        isWebDashboardMode -> viewModel.toggleWebDashboardMode(false)
                        currentTab != MainTab.HOME -> viewModel.switchTab(MainTab.HOME)
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    MainContainer(viewModel = viewModel)
                }
            }
        }
    }
}
