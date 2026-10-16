package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MobiWaveRepository {

    // Store profile & financial allocations
    val managerName = "David Kiprop"
    val employeeId = "#EMP-2041"
    val managerRole = "Shop Manager (Permanent) & Acting ATD"
    val shopName = "Tumaini Shop #042 (Nairobi East)"
    val kraPin = "DEMO ONLY — configure via secured organisation profile"
    val avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDz6a-BRpBuaOXCqLQaa3Ec7_oh5Lf3lW2DYdPxgBWXvHZf2tgfT0C28dwgndYjUUGl635YqqOyy1lXc6TZWP5RLygUptPtNPRThSft_npZ03DHc11pVKt6YTE15Gt2F8ZRuAyhCmywgb6i6AgV8fIu-P5kTYV-yuPcS_ynj2yl2Oe2c8-SgmJXHEzsUZOrA8QWLIo7vbqDN2C0qm5wfZ_k5uRhs9LuIY8odp-PcZFn36GiYz5PWkro"

    val allocationLimit = 45000.0
    val totalDisbursed = 25000.0
    val accountedFor = 19800.0
    val toAccountFor = 5200.0
    val headroomRemaining = 18200.0

    // Expenses State
    private val _expenses = MutableStateFlow<List<ExpenseVoucher>>(initialExpenses())
    val expenses: StateFlow<List<ExpenseVoucher>> = _expenses.asStateFlow()

    // Advances History
    private val _advances = MutableStateFlow<List<CashAdvance>>(initialAdvances())
    val advances: StateFlow<List<CashAdvance>> = _advances.asStateFlow()

    // Offline Queue items
    private val _offlineQueue = MutableStateFlow<List<OfflineSyncItem>>(initialOfflineQueue())
    val offlineQueue: StateFlow<List<OfflineSyncItem>> = _offlineQueue.asStateFlow()

    // Operations Approvals
    private val _pendingApprovals = MutableStateFlow<List<PendingApprovalItem>>(initialApprovals())
    val pendingApprovals: StateFlow<List<PendingApprovalItem>> = _pendingApprovals.asStateFlow()

    // Enterprise Batch Recipient Items for Dual-Approval Queue
    private val _batchRecipients = MutableStateFlow<List<BatchRecipientItem>>(initialBatchRecipients())
    val batchRecipients: StateFlow<List<BatchRecipientItem>> = _batchRecipients.asStateFlow()

    // System Telemetry
    private val _mPesaFloatBalance = MutableStateFlow(0.00) // No live balance is available in prototype mode.
    val mPesaFloatBalance: StateFlow<Double> = _mPesaFloatBalance.asStateFlow()

    private val _batchDispatched = MutableStateFlow(false)
    val batchDispatched: StateFlow<Boolean> = _batchDispatched.asStateFlow()

    private val _simulationLog = MutableStateFlow<String?>(null)
    val simulationLog: StateFlow<String?> = _simulationLog.asStateFlow()

    fun addExpense(
        merchantName: String,
        amount: Double,
        category: String,
        justification: String,
        cuInvoice: String = "UNVERIFIED-CU-REFERENCE",
        supplierPin: String = "",
        isOfflineMode: Boolean = false,
        recordId: String? = null,
        displayDate: String? = null,
        vatAmountOverride: Double? = null,
        demoReceiptImage: Boolean = true
    ) {
        val nextId = recordId ?: "EXP-${UUID.randomUUID()}"
        val newVoucher = ExpenseVoucher(
            id = nextId,
            merchantName = merchantName,
            date = displayDate ?: SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
            grossAmount = amount,
            vatAmount = vatAmountOverride ?: (amount * 0.16) / 1.16,
            category = category,
            cuNumber = cuInvoice,
            supplierPin = supplierPin,
            status = if (isOfflineMode) VoucherStatus.QUEUED_OFFLINE else VoucherStatus.AWAITING_FINANCE,
            etimsStatus = if (isOfflineMode) EtimsStatus.OFFLINE else EtimsStatus.AWAITING_VERIFICATION,
            purpose = justification,
            receiptImageUrl = if (demoReceiptImage) "https://lh3.googleusercontent.com/aida-public/AB6AXuCimcCaeRGv9A0Gki93h4up-IQQ9gZeuoLPVrThXSJJiGiD8iskprTns7UBhWjXtdq9ru1gobmlVZbEKUGBljo2bnLTmJucHER5SuTOfjUe7a8hISZph7BauX_3j29oay2iAazlHzo3tDgfYt7H_cqGbYWfjfCdezNDKoev5hEfeRCE7t-_rzGGn18rVsUPinfpwfv_DUqnXxQBD2SaS1iBNA1TIGqkbMkmr0mb1eBVH500VQlBcX0b" else null
        )
        _expenses.update { listOf(newVoucher) + it }

        if (isOfflineMode) {
            val syncItem = OfflineSyncItem(
                id = "#VCH-OFFLINE-${newVoucher.id}",
                branch = "Tumaini Shop #042",
                amount = amount,
                title = "Expense upload: $merchantName",
                progress = 0,
                status = SyncItemStatus.QUEUED
            )
            _offlineQueue.update { listOf(syncItem) + it }
        }
    }

    fun approveApprovalItem(id: String) {
        _pendingApprovals.update { list -> list.filterNot { it.id == id } }
    }

    fun rejectApprovalItem(id: String) {
        _pendingApprovals.update { list -> list.filterNot { it.id == id } }
    }

    fun batchApproveAllClean() {
        _pendingApprovals.update { list -> list.filter { it.isAuditFlagged } }
    }

    fun resolveConflict(itemId: String, action: String) {
        when (action) {
            "adjust" -> {
                _offlineQueue.update { list ->
                    list.map {
                        if (it.id == itemId) it.copy(
                            status = SyncItemStatus.QUEUED,
                            conflictReason = "Adjustment requested locally; finance review and backend sync are required."
                        ) else it
                    }
                }
            }
            "escalate" -> {
                _offlineQueue.update { list ->
                    list.map {
                        if (it.id == itemId) it.copy(
                            status = SyncItemStatus.QUEUED,
                            conflictReason = "Escalation requested locally; no SMS was sent because backend integration is not configured."
                        ) else it
                    }
                }
            }
            "discard" -> {
                _offlineQueue.update { list -> list.filterNot { it.id == itemId } }
            }
        }
    }

    fun forcePushAllOffline() {
        // No network client is configured yet. Keep items queued instead of simulating an upload.
        _simulationLog.value = "SYNC BLOCKED — no authenticated backend session is configured. Queue items remain local."
    }

    fun runDryRunSimulation() {
        _simulationLog.value = "SIMULATION ONLY — no Safaricom connection was made, no certificate was checked, and no funds were reserved. Configure the secured server-side Daraja integration before live testing."
    }

    fun dispatchBatch() {
        // Safety guard: never mark a local UI action as a real payment.
        // Live B2C submissions must go through the authenticated server-side gateway.
        _batchDispatched.value = false
        _simulationLog.value = "LIVE DISPATCH BLOCKED — this prototype has no configured Daraja gateway. No payment was submitted and the displayed float was not changed."
    }

    private fun initialExpenses(): List<ExpenseVoucher> = listOf(
        ExpenseVoucher(
            id = "EXP-9042",
            merchantName = "TotalEnergies Kilimani",
            date = "14 May 2025",
            grossAmount = 3500.00,
            vatAmount = 482.76,
            category = "Transport & Fuel",
            categoryIcon = "local_gas_station",
            cuNumber = "KRA-CU #9182",
            supplierPin = "DEMO-PIN",
            status = VoucherStatus.AWAITING_FINANCE,
            etimsStatus = EtimsStatus.DATA_EXTRACTED,
            receiptImageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCXm4gCvjC5zGFKf_Ugpv1TYm0oI0u3utzBXcjIk2aHxm3jILIbDb7FtqvfweKf_xAWCsdXwv2Cf-bl-95k7-mBlH2Vz54m5oZOUmeKvb1t4NRDzWKynJI2JCsoBls8b4V19Q-WH4mr3I2KBzIfQXgVC-GDux9s_-2Q1M8hqv74NOn35UYIEoXmvECwTXdnkC_6RFFpK0UFMe2YtLVAkARn8b2x02Rd-09KeQuQQ9rnXCwPLFRgoU2v",
            purpose = "Fuel for emergency diesel generator and stock transfer delivery to Eastleigh branch."
        ),
        ExpenseVoucher(
            id = "EXP-8910",
            merchantName = "Naivas Supermarket",
            date = "12 May 2025",
            grossAmount = 1850.00,
            vatAmount = 255.17,
            category = "Shop Supplies",
            categoryIcon = "inventory_2",
            cuNumber = "KRA-CU #7712",
            supplierPin = "DEMO-PIN",
            status = VoucherStatus.APPROVED_RECONCILED,
            etimsStatus = EtimsStatus.DATA_EXTRACTED,
            receiptImageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuB74ZfWWSToYthB79qNF_z6A7nRS0cOy0I4XLfKbq2DqME82fo4ZLiE2Opttmultq429nz30etWhXD7MBqJLRqfianVlPfDmBbruaQi3eGiUnb9YV8dt8Wv7qq0zSpjCKNMPO2jJJN2u9VGvmNxQ4OXJlZuzLDdc1jXIdJMC2TPxdNEgEQRnD3Oy7cnHVDcV780zeQPNZQl08SzpRiFvC84rVEVSfxtoIx_sbmg8D_PnriAM9E-FxTH",
            purpose = "Store paper towels, sanitizer and checkout receipt rolls."
        ),
        ExpenseVoucher(
            id = "EXP-8821",
            merchantName = "Boma Hardware & Electricals",
            date = "10 May 2025",
            grossAmount = 4200.00,
            vatAmount = 0.0,
            category = "Shop Maintenance",
            categoryIcon = "build",
            cuNumber = "CU Slip Unverified",
            supplierPin = "DEMO-PIN",
            status = VoucherStatus.NEEDS_REVIEW,
            etimsStatus = EtimsStatus.NEEDS_REVIEW,
            auditFeedback = "Missing legible eTIMS invoice breakdown; please upload full slip with KRA QR.",
            receiptImageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCXm4gCvjC5zGFKf_Ugpv1TYm0oI0u3utzBXcjIk2aHxm3jILIbDb7FtqvfweKf_xAWCsdXwv2Cf-bl-95k7-mBlH2Vz54m5oZOUmeKvb1t4NRDzWKynJI2JCsoBls8b4V19Q-WH4mr3I2KBzIfQXgVC-GDux9s_-2Q1M8hqv74NOn35UYIEoXmvECwTXdnkC_6RFFpK0UFMe2YtLVAkARn8b2x02Rd-09KeQuQQ9rnXCwPLFRgoU2v",
            purpose = "Replacement circuit breaker and high-amp fuse for chilled dairy rack."
        ),
        ExpenseVoucher(
            id = "EXP-DRAFT-04",
            merchantName = "Quick Courier Services",
            date = "14 May 2025",
            grossAmount = 850.00,
            vatAmount = 117.24,
            category = "Logistics",
            categoryIcon = "local_shipping",
            cuNumber = "Local SQLite Cached",
            supplierPin = "DEMO-PIN",
            status = VoucherStatus.QUEUED_OFFLINE,
            etimsStatus = EtimsStatus.OFFLINE,
            purpose = "Urgent transfer of POS receipt roll stock from Central depot."
        ),
        ExpenseVoucher(
            id = "EXP-8705",
            merchantName = "Quick Mart Lavington",
            date = "08 May 2025",
            grossAmount = 950.00,
            vatAmount = 131.03,
            category = "Operations / Meals",
            categoryIcon = "restaurant",
            cuNumber = "KRA-CU #6104",
            supplierPin = "DEMO-PIN",
            status = VoucherStatus.NEEDS_REVIEW,
            etimsStatus = EtimsStatus.NEEDS_REVIEW,
            purpose = "Associate meal stocktake overnight session."
        )
    )

    private fun initialAdvances(): List<CashAdvance> = listOf(
        CashAdvance(
            id = "#ADV-4412",
            title = "Weekly Petty Cash Advance",
            date = "09 May 2025 • 09:14 AM",
            refCode = "SDH91KZ84L",
            amount = 10000.0,
            status = "Successful",
            channel = "Safaricom B2C"
        ),
        CashAdvance(
            id = "#ADV-4109",
            title = "Monthly Shop Launch Advance",
            date = "02 May 2025 • 08:30 AM",
            refCode = "SDB34TY90X",
            amount = 15000.0,
            status = "Successful",
            channel = "Safaricom B2C"
        )
    )

    private fun initialOfflineQueue(): List<OfflineSyncItem> = listOf(
        OfflineSyncItem(
            id = "#VCH-OFFLINE-8921",
            branch = "Westlands #018",
            amount = 15000.0,
            title = "KRA eTIMS Receipt Upload: TotalEnergies Diesel",
            progress = 72,
            status = SyncItemStatus.UPLOADING,
            payloadSize = "480 KB • SHA-256 Verified",
            retryCount = 2
        ),
        OfflineSyncItem(
            id = "#ADV-OFFLINE-8840",
            branch = "Eldoret #042",
            amount = 10000.0,
            title = "Advance Request: Store Petty Float",
            progress = 0,
            status = SyncItemStatus.DESYNC_CONFLICT,
            conflictReason = "HQ live ledger reports KES 7,200 remaining monthly allocation, but local cached limit was KES 15,000. Current voucher exceeds limit by KES 2,800.",
            payloadSize = "18 KB • Metadata Delta",
            retryCount = 1
        ),
        OfflineSyncItem(
            id = "#EXP-OFFLINE-8944",
            branch = "Machakos #063",
            amount = 4500.0,
            title = "Local Handyman Emergency Plumbing",
            progress = 0,
            status = SyncItemStatus.QUEUED,
            payloadSize = "320 KB • Image + EXIF Geotag",
            retryCount = 0
        )
    )

    private fun initialApprovals(): List<PendingApprovalItem> = listOf(
        PendingApprovalItem(
            id = "voucher-card-1",
            branch = "Westlands Branch #018",
            requester = "Faith Wanjiku (Store Lead)",
            timeAgo = "14m ago",
            amount = 15000.00,
            tag = "Emergency Float",
            isEmergency = true,
            subject = "Emergency Generator Diesel (KPLC Grid Outage)",
            cuRef = "TotalEnergies CU-098234812",
            kraStatus = "DEMO — NOT VERIFIED",
            linkedAdvance = "Linked Advance #ADV-042 (KES 0.00 Variance)",
            avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBlJM7PXzyt3de0kFmxH24nkq1BRLzAmVSQb6io3ujS8-mmqH9epBzh4z17px4KRSsmEiCjci_pn1rJChuufhVUCvVt_P-8O2VM0U3whfqEf1DYRUq8WbwDUww1HxItBs6i5iTnObqEEZvYbAUwU4Hn9Ppxb6kXa74Le0J-KvhK52SP6mSNA05XFHCHY9nB3Av4ikKqP3yfpR2ZY94xTHU29LlYvja5B1AQkOXdpjsbYKQZ9PuAaxlL",
            receiptThumbUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuC3Nqjyu4F49dNAleAHdFgxE30OGEi-AuydvZs6oND3iyuBcWs2k7cvsZeVZe8hX5HPXHh8NNLAMkyQDUZj90sxCpPFFATA6RnzxVXyhInICCmUDfwcWOUkIigBgPqmbZG1LK8tAg4hqWlpzFaChgUNHwPJ5YMlSU589s8OuMMEeOVm8H2XD3QEC3Q5ImK9QJee78aLLV9PzRFNqc-mzy_xaRDttTL0ng2y6k1rhsA1Tt6oHPIEvoUI"
        ),
        PendingApprovalItem(
            id = "voucher-card-2",
            branch = "Machakos Branch #063",
            requester = "Kevin Otieno (Logistics)",
            timeAgo = "1h ago",
            amount = 8000.00,
            tag = "Dispatched",
            isEmergency = false,
            subject = "Dispatch Fragile Packaging & Boxes",
            cuRef = "Carton Co Ltd CU-889102",
            kraStatus = "DEMO — NOT VERIFIED",
            linkedAdvance = "20% Utilized (KES 8k / 40k)",
            avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCw0b7peAC4OKgqCW2jEScGrnocmVOZ5-cWSPTKln5BPQ0Sw6Jo3Kh5EVWeAqGjdm1Dm5yjE7qRe3gljkA9SG2Rd1gNCgXHtZ9xKK9OYmQq1rjhDpwKBsWA3pziO2Qx8tc79mEczy42P4udushCg7kuDLe_sVHKwp6wumF3hxX4yMSLdCZWttKCXg2GSgP2eKoU6oqDuGuBD4PUqO71vfFP8C08yv3yhd9f-vxzBvFcXi0Znpv3A2tt",
            receiptThumbUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuB74ZfWWSToYthB79qNF_z6A7nRS0cOy0I4XLfKbq2DqME82fo4ZLiE2Opttmultq429nz30etWhXD7MBqJLRqfianVlPfDmBbruaQi3eGiUnb9YV8dt8Wv7qq0zSpjCKNMPO2jJJN2u9VGvmNxQ4OXJlZuzLDdc1jXIdJMC2TPxdNEgEQRnD3Oy7cnHVDcV780zeQPNZQl08SzpRiFvC84rVEVSfxtoIx_sbmg8D_PnriAM9E-FxTH",
            headroomUsedPercent = 20
        ),
        PendingApprovalItem(
            id = "voucher-card-3",
            branch = "Thika Road Branch #034",
            requester = "Mercy Nyambura",
            timeAgo = "2h ago",
            amount = 12500.00,
            tag = "Cold-Chain",
            isEmergency = false,
            subject = "Perishable Cold-Chain Dry Ice Consumables",
            cuRef = "DryIce Kenya Ltd CU-4491021",
            kraStatus = "DEMO — NOT VERIFIED",
            linkedAdvance = "Routine allotment",
            avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDff6NUbXGy2WbSSA5-KrMqnierdD56bP5HWKaD_bYPnqqq0sYfpNHJ8N1cz0gyYEjAGQO2PNrTHxo2_e3L27ky0RNSlwWaE8uFlfUZtWZPAbpjOKSvaRBCdPgpHP_vUX8NcDtsOHNZamAT4eG82d_y_rd8OdO1gVKQ7f-kaOEcChlFOmX0tu8fAnn4kZ9B-ala6D8sSORGxZKFQcMgGdWzONx4f602Yf9L6_iIy3y-tB-y0-lLDwsC",
            receiptThumbUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCXm4gCvjC5zGFKf_Ugpv1TYm0oI0u3utzBXcjIk2aHxm3jILIbDb7FtqvfweKf_xAWCsdXwv2Cf-bl-95k7-mBlH2Vz54m5oZOUmeKvb1t4NRDzWKynJI2JCsoBls8b4V19Q-WH4mr3I2KBzIfQXgVC-GDux9s_-2Q1M8hqv74NOn35UYIEoXmvECwTXdnkC_6RFFpK0UFMe2YtLVAkARn8b2x02Rd-09KeQuQQ9rnXCwPLFRgoU2v"
        ),
        PendingApprovalItem(
            id = "voucher-card-4",
            branch = "Kisumu Mega #112",
            requester = "Peter Mutua",
            timeAgo = "3h ago",
            amount = 3200.00,
            tag = "Audit Flag",
            isEmergency = false,
            subject = "Missing Official eTIMS CU Receipt",
            cuRef = "Unregistered Hand Receipt",
            kraStatus = "FLAGGED NON-ETIMS",
            linkedAdvance = "Messenger Repair",
            avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuChuBqZiaOJ0UZfl0Jhx4A73qP-p8R2qoDiv_UYnIlUFX8UgzstbLQpfaKoGhnjnnfHgncUva0HqTXqiRaWZyxt3GizFvsXgrDfgSWwdQyEPxOoDk_aifUkFliqLHEJvclWLbWYmmSfjYEO8lWW7bZV8XMT3-FW12lm0z4h7YL-CTWWzJz_KsoFv3PlCYBDyoic09Jboe-6SYKcDh0-w6dPl_RMmnhZbnEpDzCXwEFs9AreC8VBm83d",
            receiptThumbUrl = "",
            isAuditFlagged = true,
            flagNote = "Handwritten paper chit detected by OCR. Requires statutory compliance exception justification prior to regional controllership override."
        )
    )

    private fun initialBatchRecipients(): List<BatchRecipientItem> = listOf(
        BatchRecipientItem(
            empId = "#EMP-2041",
            name = "David Kiprop",
            role = "Store Lead",
            branchNumber = "#042 Eldoret",
            branchName = "Eldoret Central",
            region = "Rift North Region",
            phone = "+254 712 *** 891",
            kycName = "DAVID K. CHERUIYOT",
            kycMatchPercent = 100,
            purposeCategory = "Weekly Petty Cash",
            amount = 15000.00,
            headroomPercent = 33,
            limitText = "Limit KES 45,000.00",
            makerNote = "Routine weekly field allotment",
            checkerStatus = "Demo — review required",
            isQuarantined = false,
            isWarningOverridden = false
        ),
        BatchRecipientItem(
            empId = "#EMP-1822",
            name = "Faith Wanjiku",
            role = "Operations Supervisor",
            branchNumber = "#018 Westlands",
            branchName = "Westlands Super Center",
            region = "Nairobi West",
            phone = "+254 722 *** 409",
            kycName = "F. W. GICHOYA",
            kycMatchPercent = 98,
            purposeCategory = "Emergency Generator Fuel",
            amount = 15000.00,
            headroomPercent = 58,
            limitText = "Limit KES 60,000.00",
            makerNote = "14h blackout, diesel bought via Total Energies Westlands.",
            checkerStatus = "Demo — review required",
            isQuarantined = false,
            isWarningOverridden = true
        ),
        BatchRecipientItem(
            empId = "#EMP-3310",
            name = "Kevin Otieno",
            role = "Logistics Clerk",
            branchNumber = "#063 Machakos",
            branchName = "Machakos Depot",
            region = "Eastern Central",
            phone = "+254 705 *** 122",
            kycName = "K. O. AMOLO",
            kycMatchPercent = 100,
            purposeCategory = "Store Operating Float",
            amount = 8000.00,
            headroomPercent = 20,
            limitText = "Limit KES 40,000.00",
            makerNote = "Packaging & carrier dispatch bags",
            checkerStatus = "Demo — review required",
            isQuarantined = false,
            isWarningOverridden = false
        ),
        BatchRecipientItem(
            empId = "#EMP-2914",
            name = "Mary Mutua",
            role = "Regional Cashier",
            branchNumber = "#005 Mombasa",
            branchName = "Coast Super Hub",
            region = "Coast Super Hub",
            phone = "+254 791 *** 554",
            kycName = "M. N. MUTUA",
            kycMatchPercent = 100,
            purposeCategory = "Route Float Top-up",
            amount = 25000.00,
            headroomPercent = 44,
            limitText = "Limit KES 80,000.00",
            makerNote = "Toll & municipal market van entry levies",
            checkerStatus = "Demo — review required",
            isQuarantined = false,
            isWarningOverridden = false
        ),
        BatchRecipientItem(
            empId = "#EMP-1120",
            name = "Peter Mutua",
            role = "Merchandiser",
            branchNumber = "#112 Kisumu",
            branchName = "Kisumu North",
            region = "Lake Basin North",
            phone = "+254 748 *** 091",
            kycName = "Unregistered MSISDN / No KYC",
            kycMatchPercent = 0,
            purposeCategory = "Bicycle Fleet Repair",
            amount = 12000.00,
            headroomPercent = 0,
            limitText = "BLOCKED BY MAKER",
            makerNote = "Line reassignment incomplete. Re-route to voucher.",
            checkerStatus = "Quarantined",
            isQuarantined = true,
            isWarningOverridden = false
        )
    )

    val zoneMetrics: List<ZoneMetric> = listOf(
        ZoneMetric("Nairobi Central", 45, 12400000.0, 15000000.0, 96, 2600000.0),
        ZoneMetric("Rift Valley", 38, 8200000.0, 11000000.0, 93, 2800000.0),
        ZoneMetric("Coast Hub", 28, 6100000.0, 8500000.0, 95, 2400000.0),
        ZoneMetric("Western & Nyanza", 34, 5800000.0, 8000000.0, 91, 2200000.0),
        ZoneMetric("Mount Kenya", 25, 4300000.0, 6000000.0, 97, 1700000.0, isLeadPerformer = true)
    )
}
