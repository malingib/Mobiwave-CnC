package com.example.model

enum class VoucherStatus(val label: String) {
    AWAITING_FINANCE("Awaiting Finance"),
    APPROVED_RECONCILED("Approved & Reconciled"),
    NEEDS_REVIEW("Needs Review"),
    QUEUED_OFFLINE("Queued Offline")
}

enum class EtimsStatus(val label: String) {
    AWAITING_VERIFICATION("Awaiting Verification"),
    VERIFIED("eTIMS Verified"),
    DATA_EXTRACTED("Data Extracted"),
    NEEDS_REVIEW("Needs Review"),
    UNVERIFIED("Unverified"),
    MATCHED("eTIMS Matched"),
    OFFLINE("Offline Saved")
}

data class ExpenseVoucher(
    val id: String,
    val merchantName: String,
    val date: String,
    val grossAmount: Double,
    val vatAmount: Double = 0.0,
    val category: String,
    val categoryIcon: String = "receipt",
    val cuNumber: String,
    val supplierPin: String = "",
    val status: VoucherStatus,
    val etimsStatus: EtimsStatus,
    val auditFeedback: String? = null,
    val receiptImageUrl: String? = null,
    val branchName: String = "Tumaini Shop #042",
    val purpose: String = ""
)

data class CashAdvance(
    val id: String,
    val title: String,
    val date: String,
    val refCode: String,
    val amount: Double,
    val status: String = "Successful",
    val channel: String = "Safaricom B2C"
)

data class OfflineSyncItem(
    val id: String,
    val branch: String,
    val amount: Double,
    val title: String,
    val progress: Int,
    val status: SyncItemStatus,
    val conflictReason: String? = null,
    val payloadSize: String = "480 KB • SHA-256 Verified",
    val retryCount: Int = 2
)

enum class SyncItemStatus {
    UPLOADING,
    DESYNC_CONFLICT,
    QUEUED
}

data class PendingApprovalItem(
    val id: String,
    val branch: String,
    val requester: String,
    val timeAgo: String,
    val amount: Double,
    val tag: String,
    val isEmergency: Boolean = false,
    val subject: String,
    val cuRef: String,
    val kraStatus: String = "KRA QR OK",
    val linkedAdvance: String = "Linked Advance #ADV-042 (KES 0.00 Variance)",
    val avatarUrl: String,
    val receiptThumbUrl: String,
    val isAuditFlagged: Boolean = false,
    val flagNote: String? = null,
    val headroomUsedPercent: Int = 20
)

data class BatchRecipientItem(
    val empId: String,
    val name: String,
    val role: String,
    val branchNumber: String,
    val branchName: String,
    val region: String,
    val phone: String,
    val kycName: String,
    val kycMatchPercent: Int = 100,
    val purposeCategory: String,
    val amount: Double,
    val headroomPercent: Int,
    val limitText: String,
    val makerNote: String,
    val checkerStatus: String,
    val isQuarantined: Boolean = false,
    val isWarningOverridden: Boolean = false
)

data class ZoneMetric(
    val zoneName: String,
    val shopCount: Int,
    val disbursedAmount: Double,
    val allocationCeiling: Double,
    val cuComplianceRate: Int,
    val bufferPool: Double,
    val isLeadPerformer: Boolean = false
)
