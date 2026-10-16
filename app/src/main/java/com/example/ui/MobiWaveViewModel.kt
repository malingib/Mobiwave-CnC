package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MobiWaveRepository
import com.example.data.SupabaseExpenseRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    APPROVALS("Approvals")
}

data class UiNotification(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val isError: Boolean = false
)

class MobiWaveViewModel(
    private val repository: MobiWaveRepository = MobiWaveRepository()
) : ViewModel() {

    private val liveExpenseRepository = SupabaseExpenseRepository()
    private var authenticatedAccessToken: String? = null
    private var authenticatedUserId: String? = null
    private val _isSubmittingExpense = MutableStateFlow(false)
    val isSubmittingExpense: StateFlow<Boolean> = _isSubmittingExpense.asStateFlow()

    fun setAuthenticatedSession(accessToken: String?, userId: String?) {
        authenticatedAccessToken = accessToken
        authenticatedUserId = userId
    }

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
        repository.dispatchBatch()
        postNotification("Demo only — no M-PESA prompt or payment occurred. Live dispatch is blocked until the secured backend is configured.", isError = true)
    }

    fun runSimulation() {
        repository.runDryRunSimulation()
        postNotification("Simulation only — no live gateway, certificate or float check was performed.")
    }

    fun submitNewExpense(
        merchant: String,
        amount: Double,
        category: String,
        justification: String,
        isOffline: Boolean
    ) {
        if (_isSubmittingExpense.value) return
        if (isOffline) {
            postNotification("Offline drafts are not enabled yet. Nothing was saved; connect to the internet and submit online.", isError = true)
            return
        }
        val token = authenticatedAccessToken
        val userId = authenticatedUserId
        if (token.isNullOrBlank() || userId.isNullOrBlank()) {
            postNotification("Sign in again before submitting an expense.", isError = true)
            return
        }
        if (merchant.isBlank() || amount <= 0 || justification.isBlank()) {
            postNotification("Enter a merchant, positive amount, and purpose.", isError = true)
            return
        }
        _isSubmittingExpense.value = true
        viewModelScope.launch {
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val created = liveExpenseRepository.createExpense(
                    accessToken = token, userId = userId, merchant = merchant, amount = amount,
                    category = category, purpose = justification, expenseDate = date
                )
                repository.addExpense(
                    merchantName = created.merchantName, amount = created.grossAmount, category = category,
                    justification = justification, isOfflineMode = false, recordId = created.id,
                    displayDate = created.expenseDate, vatAmountOverride = 0.0, demoReceiptImage = false
                )
                postNotification("Expense saved to the server. Receipt attachment and eTIMS verification are still pending.")
                _currentTab.value = MainTab.EXPENSES
            } catch (e: Exception) {
                postNotification(e.message ?: "Expense submission failed. No local success was recorded.", isError = true)
            } finally {
                _isSubmittingExpense.value = false
            }
        }
    }

    fun approveVoucher(id: String, branch: String) {
        repository.approveApprovalItem(id)
        postNotification("Demo only — local pending list changed for $branch; no approval was persisted and no settlement was authorised.")
    }

    fun rejectVoucher(id: String, branch: String) {
        repository.rejectApprovalItem(id)
        postNotification("Demo only — local pending list changed for $branch; no rejection or audit event was persisted.", isError = true)
    }

    fun batchApproveClean() {
        repository.batchApproveAllClean()
        postNotification("Demo only — the local list was updated. No biometric sign-off, persistent approval or payment occurred.")
    }

    fun resolveConflict(itemId: String, action: String) {
        repository.resolveConflict(itemId, action)
        when (action) {
            "adjust" -> postNotification("Demo only — adjustment requires finance review and backend sync; no financial record was changed.")
            "escalate" -> postNotification("Demo only — escalation was not sent by SMS; backend integration is required.")
            "discard" -> postNotification("Demo only — item removed from the temporary local list; no encrypted vault is configured.")
        }
    }

    fun toggleSyncPause() {
        _isSyncPaused.update { !it }
        val msg = if (_isSyncPaused.value) "Sync paused." else "Sync resumed."
        postNotification(msg)
    }

    fun forcePushSync() {
        viewModelScope.launch {
            repository.forcePushAllOffline()
            postNotification("Sync unavailable in prototype — no records were uploaded to a server.", isError = true)
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
