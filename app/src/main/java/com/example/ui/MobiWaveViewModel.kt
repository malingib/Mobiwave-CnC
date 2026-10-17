package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MobiWaveRepository
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab(val title: String) {
    HOME("Home"),
    EXPENSES("Expenses"),
    CAPTURE("Capture"),
    ADVANCES("Advances"),
    APPROVALS("Approvals"),
    ACCOUNT("Account")
}

data class UiNotification(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val isError: Boolean = false
)

class MobiWaveViewModel(
    private val repository: MobiWaveRepository = MobiWaveRepository()
) : ViewModel() {

    // Active View Mode (true = Desktop/Web Controllership Dashboard, false = Mobile Stack)
    private val _isWebDashboardMode = MutableStateFlow(false)
    val isWebDashboardMode: StateFlow<Boolean> = _isWebDashboardMode.asStateFlow()

    // Active Mobile Tab
    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    // Repository flows
    val expenses = repository.expenses
    val advances = repository.advances
    val offlineQueue = repository.offlineQueue
    val pendingApprovals = repository.pendingApprovals
    val batchRecipients = repository.batchRecipients
    val mPesaFloatBalance = repository.mPesaFloatBalance
    val batchDispatched = repository.batchDispatched
    val simulationLog = repository.simulationLog
    val zoneMetrics = repository.zoneMetrics

    // Store manager context
    val managerName = repository.managerName
    val employeeId = repository.employeeId
    val managerRole = repository.managerRole
    val shopName = repository.shopName
    val kraPin = repository.kraPin
    val avatarUrl = repository.avatarUrl
    val allocationLimit = repository.allocationLimit
    val totalDisbursed = repository.totalDisbursed
    val accountedFor = repository.accountedFor
    val toAccountFor = repository.toAccountFor
    val headroomRemaining = repository.headroomRemaining

    // Filter states for Expenses screen
    private val _expenseFilter = MutableStateFlow("all")
    val expenseFilter: StateFlow<String> = _expenseFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Advances screen sub-view: "reconciliation" vs "offline_diagnostics"
    private val _advancesSubTab = MutableStateFlow("reconciliation")
    val advancesSubTab: StateFlow<String> = _advancesSubTab.asStateFlow()

    // UI Dialogs and Modals
    private val _showCreateBatchModal = MutableStateFlow(false)
    val showCreateBatchModal: StateFlow<Boolean> = _showCreateBatchModal.asStateFlow()

    private val _createBatchStep = MutableStateFlow(3) // Defaults to Step 3 or 4 to showcase the rich UI!
    val createBatchStep: StateFlow<Int> = _createBatchStep.asStateFlow()

    private val _showInvoiceDetailModal = MutableStateFlow(false)
    val showInvoiceDetailModal: StateFlow<Boolean> = _showInvoiceDetailModal.asStateFlow()

    private val _selectedInvoice = MutableStateFlow<ExpenseVoucher?>(null)
    val selectedInvoice: StateFlow<ExpenseVoucher?> = _selectedInvoice.asStateFlow()

    private val _showStkPushModal = MutableStateFlow(false)
    val showStkPushModal: StateFlow<Boolean> = _showStkPushModal.asStateFlow()

    // Toast notifications
    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    // Offline sync pause state
    private val _isSyncPaused = MutableStateFlow(false)
    val isSyncPaused: StateFlow<Boolean> = _isSyncPaused.asStateFlow()

    fun switchTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun toggleWebDashboardMode(enabled: Boolean) {
        _isWebDashboardMode.value = enabled
    }

    fun setExpenseFilter(filter: String) {
        _expenseFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAdvancesSubTab(subTab: String) {
        _advancesSubTab.value = subTab
    }

    fun openCreateBatchModal(step: Int = 3) {
        _createBatchStep.value = step
        _showCreateBatchModal.value = true
    }

    fun closeCreateBatchModal() {
        _showCreateBatchModal.value = false
    }

    fun setCreateBatchStep(step: Int) {
        _createBatchStep.value = step
    }

    fun openInvoiceDetailModal(voucher: ExpenseVoucher? = null) {
        _selectedInvoice.value = voucher ?: expenses.value.firstOrNull()
        _showInvoiceDetailModal.value = true
    }

    fun closeInvoiceDetailModal() {
        _showInvoiceDetailModal.value = false
    }

    fun openStkPushModal() {
        _showStkPushModal.value = true
    }

    fun closeStkPushModal() {
        _showStkPushModal.value = false
    }

    fun triggerStkPinSuccess() {
        _showStkPushModal.value = false
        postNotification("Demo only: no STK request was sent and no funds were transferred. Configure a verified payment backend first.", isError = true)
    }

    fun runSimulation() {
        repository.runDryRunSimulation()
        postNotification("Demo dry run completed locally. No gateway or payment provider was contacted.", isError = true)
    }

    fun submitNewExpense(
        merchant: String,
        amount: Double,
        category: String,
        justification: String,
        isOffline: Boolean
    ) {
        repository.addExpense(merchant, amount, category, justification, isOfflineMode = isOffline)
        if (isOffline) {
            postNotification("Demo only: expense saved to temporary app memory. It has not synced to a server.")
        } else {
            postNotification("Demo only: expense KES ${String.format("%,.2f", amount)} was added locally. No server submission or KRA validation occurred.", isError = true)
        }
        _currentTab.value = MainTab.EXPENSES
    }

    fun approveVoucher(id: String, branch: String) {
        repository.approveApprovalItem(id)
        postNotification("Demo only: $branch approval changed locally. No payment or official approval was issued.", isError = true)
    }

    fun rejectVoucher(id: String, branch: String) {
        repository.rejectApprovalItem(id)
        postNotification("Demo only: $branch item was removed from this preview. No official decision was recorded.", isError = true)
    }

    fun batchApproveClean() {
        repository.batchApproveAllClean()
        postNotification("Demo only: preview items changed locally. No biometric sign-off or official approval occurred.", isError = true)
    }

    fun resolveConflict(itemId: String, action: String) {
        repository.resolveConflict(itemId, action)
        when (action) {
            "adjust" -> postNotification("Amount auto-adjusted to KES 7,200. Re-queued for sync.")
            "escalate" -> postNotification("Escalation ticket dispatched to Area Manager via SMS.")
            "discard" -> postNotification("Draft discarded from encrypted vault.")
        }
    }

    fun toggleSyncPause() {
        _isSyncPaused.update { !it }
        val msg = if (_isSyncPaused.value) "Sync paused." else "Sync resumed."
        postNotification(msg)
    }

    fun forcePushSync() {
        viewModelScope.launch {
            postNotification("Sync unavailable: this build has no authenticated server connection.", isError = true)
        }
    }

    fun postNotification(message: String, isError: Boolean = false) {
        _notification.value = UiNotification(message = message, isError = isError)
        viewModelScope.launch {
            delay(3500)
            if (_notification.value?.message == message) {
                _notification.value = null
            }
        }
    }

    fun dismissNotification() {
        _notification.value = null
    }
}
