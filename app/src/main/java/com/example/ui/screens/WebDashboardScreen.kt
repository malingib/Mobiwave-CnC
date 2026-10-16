package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BatchRecipientItem
import com.example.ui.MobiWaveViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun WebDashboardScreen(
    viewModel: MobiWaveViewModel
) {
    var webSubTab by remember { mutableStateOf("dual_approval") } // "dual_approval" vs "overview_matrix"
    var tableFilter by remember { mutableStateOf("all") } // "all", "warn", "quar"
    var checkerRemarks by remember {
        mutableStateOf("Demo-only placeholder. Add a finance review note after verifying the selected recipients, allocation limits and supporting records.")
    }
    var isAttested by remember { mutableStateOf(false) }
    var selectedAuthMethod by remember { mutableStateOf("stk") }

    val batchRecipients by viewModel.batchRecipients.collectAsState()
    val floatBalance by viewModel.mPesaFloatBalance.collectAsState()
    val isDispatched by viewModel.batchDispatched.collectAsState()
    val simulationLog by viewModel.simulationLog.collectAsState()

    val filteredRecipients = remember(batchRecipients, tableFilter) {
        when (tableFilter) {
            "warn" -> batchRecipients.filter { it.isWarningOverridden }
            "quar" -> batchRecipients.filter { it.isQuarantined }
            else -> batchRecipients
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MobiSurface)
    ) {
        // Left Corporate Sidebar (240dp fixed width on desktop/tablet)
        Surface(
            color = MobiDarkNavy,
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Brand Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MobiTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MobiWave",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "EXPENSE & eTIMS HUB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MobiTealFixed,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Scope Box
                    Surface(
                        color = MobiInverseSurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "CURRENT SCOPE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "Tumaini Retail Network",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "All 170 Shops • Central Finance",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFBAC3DE),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Navigation Links
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "CORE OPERATIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7C839B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )

                        WebNavItem(
                            icon = Icons.Default.Dashboard,
                            title = "Overview / Matrix",
                            isSelected = webSubTab == "overview_matrix",
                            onClick = { webSubTab = "overview_matrix" }
                        )

                        Text(
                            text = "DISBURSEMENTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7C839B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        WebNavItem(
                            icon = Icons.Default.Layers,
                            title = "Dual Approvals Queue",
                            isSelected = webSubTab == "dual_approval",
                            badgeText = "W21",
                            onClick = { webSubTab = "dual_approval" }
                        )

                        WebNavItem(
                            icon = Icons.Default.ReceiptLong,
                            title = "Invoice Verification",
                            isSelected = false,
                            onClick = { viewModel.openInvoiceDetailModal() }
                        )

                        WebNavItem(
                            icon = Icons.Default.PostAdd,
                            title = "Create New Batch",
                            isSelected = false,
                            onClick = { viewModel.openCreateBatchModal(3) }
                        )
                    }
                }

                // Bottom Gateway telemetry in sidebar
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = MobiInverseSurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MobiTealFixed)
                                )
                                Text(
                                    text = "SAFARICOM B2C GATEWAY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MobiTealFixed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Working Balance:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFBAC3DE),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = formatKes(floatBalance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Grace Muthoni profile row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MobiTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "GM",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Grace Muthoni",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Finance Mgr • Approver",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFBAC3DE),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Main Desktop Workspace
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // Top Header Bar
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Finance & Operations",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Retail Network",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = MobiTeal,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "DEMO MODE — synthetic sample data",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "CYCLE:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "W3-OCT-2024",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Switch to Mobile Mode button
                        Button(
                            onClick = { viewModel.toggleWebDashboardMode(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = MobiTeal),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Mobile View", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.openCreateBatchModal(3) },
                            colors = ButtonDefaults.buttonColors(containerColor = MobiDarkNavy),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MobiTealFixed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Disburse Batch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (webSubTab == "dual_approval") {
                    // Top Operational Banner
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MobiInverseSurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = MobiTealFixed,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Dual Approval Queue / Review Batch MOBI-B2C-2024-W21-084",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Surface(
                                                color = if (isDispatched) MobiTealContainer else MobiTealContainer.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            ) {
                                                Text(
                                                    text = if (isDispatched) "DISPATCHED" else "AWAITING CHECKER RELEASE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF005049),
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Maker Attestation: Demo placeholder — not signed",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(text = "•", color = MaterialTheme.colorScheme.outlineVariant)
                                            Text(
                                                text = "ICPAK Dual Sign-Off: CAP 531 §14 Compliance",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(text = "•", color = MaterialTheme.colorScheme.outlineVariant)
                                            Text(
                                                text = "Idempotency: IDEMP-MW-20250514-W21-084-77A",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.runSimulation() },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = MobiTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Run simulation (no live connection)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Surface(
                                        color = MobiDarkNavy,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(MobiTealFixed)
                                            )
                                            Column {
                                                Text(
                                                    text = "CHECKER ROLE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFBAC3DE),
                                                    fontSize = 8.sp
                                                )
                                                Text(
                                                    text = "Grace Muthoni (ICPAK #9412)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4 Bento Metric Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DesktopBentoCard(
                                title = "NET BATCH DISBURSABLE",
                                mainFigure = "KES 530,000.00",
                                subtitle = "37 Approved Recipients • Ready to Route",
                                footer = "Channel: M-PESA B2C • Avg: KES 14,324.32",
                                modifier = Modifier.weight(1f)
                            )
                            DesktopBentoCard(
                                title = "QUARANTINED / EXCLUDED",
                                mainFigure = "KES 12,000.00",
                                isError = true,
                                subtitle = "1 User Flagged (P. Mutua) • SIM Unregistered",
                                footer = "Maker Hard-Lock • Zero Float Impact",
                                modifier = Modifier.weight(1f)
                            )
                            DesktopBentoCard(
                                title = "SAFARICOM MMPS FLOAT #600984",
                                mainFigure = formatKes(floatBalance),
                                subtitle = "No live balance connected • simulation only",
                                footer = "Reserve Buffer: KES 2.5M • Clearance 100%",
                                modifier = Modifier.weight(1f)
                            )
                            DesktopBentoCard(
                                title = "POST-DISBURSEMENT RAIL",
                                mainFigure = "KES 12,130,250.00",
                                subtitle = "Liquidity Headroom • 95.8% Safe",
                                footer = "Safety Threshold: KES 5M Auto-Sweep",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Maker vs Checker Attestation Grid
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Step 1: Maker Proof (Locked)
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MobiTeal,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Step 1: Maker Attestation (Not confirmed)",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "TOTP not configured — demo only",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = "AUTHORING OFFICER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                            Text(text = "Joseph Ochieng (#FO-109)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(text = "Treasury & Field Operations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column {
                                            Text(text = "CRYPTOGRAPHIC STAMP & IP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                            Text(text = "14 May 2025 10:15:24 EAT", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(text = "IP: 192.168.10.42 (Internal LAN)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "Maker Review Log:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Demo audit placeholder — sample decisions are not persisted, and no recipient has been verified or quarantined by a live system.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            // Step 2: Checker Authorization Status (Active)
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LockOpen,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Step 2: Checker Dual-Sign Authorization (In Session)",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "TIER-2 GATE (MAX KES 5,000,000)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MobiTeal
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = "ACTIVE CONTROLLERSHIP CHECKER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                            Text(text = "Grace Muthoni (#FM-003)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(text = "Head of Financial Controllership • ICPAK #9412", style = MaterialTheme.typography.bodySmall, color = MobiTeal)
                                        }
                                        Column {
                                            Text(text = "SEGREGATION CLEARANCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                            Text(text = "Full Independent Review", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(text = "Daraja credentials: Not configured (demo mode)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = MobiTeal,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Zero commonality detected between Maker token signature and Checker terminal. Dual-control strictly preserved.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Audit Table Card with Tab Filters
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Filter Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterPillItem(
                                            label = "Demo Recipients",
                                            count = "37",
                                            isSelected = tableFilter == "all",
                                            onClick = { tableFilter = "all" }
                                        )
                                        FilterPillItem(
                                            label = "Maker Overridden",
                                            count = "1",
                                            dotColor = MobiAmberWarning,
                                            isSelected = tableFilter == "warn",
                                            onClick = { tableFilter = "warn" }
                                        )
                                        FilterPillItem(
                                            label = "Quarantined / Excluded",
                                            count = "1",
                                            dotColor = MobiError,
                                            isSelected = tableFilter == "quar",
                                            onClick = { tableFilter = "quar" }
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Demo data — no live KYC validation performed",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(text = "•", color = MaterialTheme.colorScheme.outlineVariant)
                                        Text(
                                            text = "DISBURSEMENT CUTOFF: 11:30 AM EAT",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MobiTeal
                                        )
                                    }
                                }

                                // Table Content
                                Column(modifier = Modifier.padding(14.dp)) {
                                    filteredRecipients.forEach { item ->
                                        DesktopRecipientRow(
                                            item = item,
                                            onInspect = { viewModel.openInvoiceDetailModal() }
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerLow)
                                    }
                                }

                                // Table Footer
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Illustrative payload only — sample recipient rows are not live records; KES 530,000.00 is not a verified batch total.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = null,
                                            tint = MobiTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "eTIMS provider not configured",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MobiTeal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2-Factor Sign-Off Gate & Remarks
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Left 2-FA Mode selector & Simulation box
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhonelinkLock,
                                            contentDescription = null,
                                            tint = MobiTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Checker 2-Factor Sign-Off Gate",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = "Demo workflow only — no cryptographic approval or payment is performed here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Auth options
                                    AuthOptionRow(
                                        title = "Safaricom STK Secure Prompt",
                                        subtitle = "Controllership Registered SIM: +254 722 *** 002",
                                        isSelected = selectedAuthMethod == "stk",
                                        badge = "Primary",
                                        onClick = { selectedAuthMethod = "stk" }
                                    )

                                    AuthOptionRow(
                                        title = "Hardware Token (FIDO2 / YubiKey 5C)",
                                        subtitle = "Slot 1: ICPAK #9412 Certified Chip",
                                        isSelected = selectedAuthMethod == "fido",
                                        badge = "Hardware",
                                        onClick = { selectedAuthMethod = "fido" }
                                    )

                                    AuthOptionRow(
                                        title = "Corporate Authenticator App",
                                        subtitle = "6-digit time-based dynamic OTP",
                                        isSelected = selectedAuthMethod == "totp",
                                        badge = "Backup",
                                        onClick = { selectedAuthMethod = "totp" }
                                    )

                                    // Simulation Output Box
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "SIMULATED GATEWAY STATUS",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 9.sp
                                                )
                                                Text(
                                                    text = "Simulate Payout Dry Run",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MobiTeal,
                                                    modifier = Modifier.clickable { viewModel.runSimulation() }
                                                )
                                            }
                                            Surface(
                                                color = MobiDarkNavy,
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 6.dp)
                                            ) {
                                                Text(
                                                    text = simulationLog ?: "SIMULATION MODE — no live Daraja connection, certificate check or float reservation has occurred.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MobiTealFixed,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Right: Checker Remarks & Statutory Attestation
                                Column(
                                    modifier = Modifier.weight(1.3f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Demo Review Notes (not a permanent audit record)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    OutlinedTextField(
                                        value = checkerRemarks,
                                        onValueChange = { checkerRemarks = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                            focusedBorderColor = MobiTeal,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )

                                    // Attestation Checkbox
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Checkbox(
                                                checked = isAttested,
                                                onCheckedChange = { isAttested = it },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = MobiTeal
                                                )
                                            )
                                            Column {
                                                Text(
                                                    text = "Mandatory ICPAK Statutory Attestation:",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Prototype placeholder only. A real authenticated maker, approver and submitter must independently validate the batch, recipients, expense-only purpose and funding before any live request. This prototype cannot authorise a payment.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Session Token: STK-SES-20250514-9982",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = null,
                                                tint = MobiTeal,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Encrypted with HSM AES-256-GCM",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MobiTeal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Sticky Action Bar
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.postNotification("Batch rejected. Joseph Ochieng notified.", isError = true)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MobiError)
                                    ) {
                                        Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Reject Batch with Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.postNotification("Clarification request sent to Preparer Joseph Ochieng.")
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ContactSupport, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Request Maker Clarification", fontSize = 11.sp)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.runSimulation() },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Re-Verify Payload", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            if (!isAttested) {
                                                viewModel.postNotification("Please check the mandatory ICPAK statutory dual-control compliance checkbox.", isError = true)
                                            } else {
                                                viewModel.openStkPushModal()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MobiTeal),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isDispatched) "Dispatched to Safaricom B2C"
                                            else "Live B2C disabled — configure backend first",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Overview Matrix Tab (Image 27)
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Regional Distribution Architecture (5 Zones / 20 Regions)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    viewModel.zoneMetrics.forEach { zone ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = zone.zoneName,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = "${zone.cuComplianceRate}% CU",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MobiTeal
                                                    )
                                                }
                                                Text(
                                                    text = "${zone.shopCount} Retail Outlets",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = formatKes(zone.disbursedAmount),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                                Text(
                                                    text = "Cap: ${formatKes(zone.allocationCeiling)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopBentoCard(
    title: String,
    mainFigure: String,
    subtitle: String,
    footer: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                text = mainFigure,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isError) MobiError else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MobiError else MobiTeal
            )
            Surface(
                color = if (isError) MobiErrorContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = footer,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = if (isError) MobiOnErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DesktopRecipientRow(
    item: BatchRecipientItem,
    onInspect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInspect)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Recipient
        Column(modifier = Modifier.weight(1.2f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isQuarantined) MobiError else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.isQuarantined) TextDecoration.LineThrough else TextDecoration.None
                )
                if (item.isWarningOverridden) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MobiAmberWarning,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = "${item.empId} • ${item.role}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Branch
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.branchNumber,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.region,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Phone & KYC
        Column(modifier = Modifier.weight(1.3f)) {
            Text(
                text = item.phone,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                textDecoration = if (item.isQuarantined) TextDecoration.LineThrough else TextDecoration.None
            )
            Text(
                text = item.kycName,
                style = MaterialTheme.typography.bodySmall,
                color = if (item.isQuarantined) MobiError else MobiTeal,
                fontSize = 11.sp
            )
        }

        // Purpose
        Surface(
            color = if (item.isWarningOverridden) MobiAmberContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.weight(1.1f)
        ) {
            Text(
                text = item.purposeCategory,
                style = MaterialTheme.typography.labelSmall,
                color = if (item.isWarningOverridden) MobiAmberWarning else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }

        // Amount
        Text(
            text = formatKes(item.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (item.isQuarantined) MobiError else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (item.isQuarantined) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f)
        )

        // Checker Action Badge
        Surface(
            color = when {
                item.isQuarantined -> MobiErrorContainer
                item.isWarningOverridden -> MobiAmberContainer.copy(alpha = 0.5f)
                else -> MobiTealContainer.copy(alpha = 0.5f)
            },
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.weight(1.2f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when {
                        item.isQuarantined -> Icons.Default.RemoveDone
                        item.isWarningOverridden -> Icons.Default.FactCheck
                        else -> Icons.Default.Verified
                    },
                    contentDescription = null,
                    tint = when {
                        item.isQuarantined -> MobiError
                        item.isWarningOverridden -> MobiAmberWarning
                        else -> Color(0xFF005049)
                    },
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.checkerStatus,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        item.isQuarantined -> MobiError
                        item.isWarningOverridden -> MobiAmberWarning
                        else -> Color(0xFF005049)
                    }
                )
            }
        }
    }
}

@Composable
fun AuthOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    badge: String,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerLow else Color.White,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) MobiTeal else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = MobiTeal)
                )
                Column {
                    Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun WebNavItem(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MobiTealContainer.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MobiTealFixed else Color(0xFFBAC3DE),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color(0xFFBAC3DE)
                )
            }

            if (badgeText != null) {
                Surface(
                    color = MobiTeal,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
