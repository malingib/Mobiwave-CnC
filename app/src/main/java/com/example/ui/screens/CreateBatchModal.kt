package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.MobiWaveViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun CreateBatchModal(
    viewModel: MobiWaveViewModel
) {
    val step by viewModel.createBatchStep.collectAsState()
    var autoExcludeError by remember { mutableStateOf(true) }
    var makerAttestationChecked by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = { viewModel.closeCreateBatchModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Modal Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Create Non-Payroll Disbursement Batch",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = MobiDarkNavy,
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MobiTealFixed, modifier = Modifier.size(12.dp))
                                    Text(text = "Non-Payroll Policy Enforced", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 9.sp)
                                }
                            }
                        }
                        Text(
                            text = "Authorise store petty cash advances, route floats, and branch emergency liquidity across 170 retail shops",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { viewModel.closeCreateBatchModal() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // 4-Step Indicator Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModalStepItem(
                        stepNum = "1",
                        title = "1. Batch Setup",
                        subtitle = "Configured",
                        isCompleted = true,
                        isActive = step == 1,
                        modifier = Modifier.weight(1f)
                    )
                    ModalStepItem(
                        stepNum = "2",
                        title = "2. Ingestion",
                        subtitle = "38 Records",
                        isCompleted = true,
                        isActive = step == 2,
                        modifier = Modifier.weight(1f)
                    )
                    ModalStepItem(
                        stepNum = "3",
                        title = "3. Pre-Flight Validation",
                        subtitle = "37 Passed • 1 Quarantined",
                        isCompleted = step > 3,
                        isActive = step == 3,
                        modifier = Modifier.weight(1f)
                    )
                    ModalStepItem(
                        stepNum = "4",
                        title = "4. Maker 2-FA Sign-Off",
                        subtitle = "Dual-Approval Handover",
                        isCompleted = false,
                        isActive = step == 4,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (step == 3) {
                        // Section A: Batch Parameters Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "BATCH PARAMETERS & FLOAT SOURCE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Ref: MOBI-B2C-2024-W21-084",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(color = Color.White, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1.3f)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(text = "BATCH LABEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                            Text(text = "Nairobi Central & Rift Valley Weekly", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Surface(color = Color.White, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(text = "PURPOSE CATEGORY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                            Text(text = "Weekly Petty Cash Advance", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Surface(color = Color.White, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1.2f)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(text = "SOURCE UTILITY FLOAT (FLOAT SAFE)", style = MaterialTheme.typography.labelSmall, color = MobiTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(text = "Saf MMPS #600984: KES 12,660,250", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Surface(color = Color.White, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(text = "PAYMENT CHANNEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                            Text(text = "BusinessPayment (B2C)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Section B: Ingestion Summary & CSV File
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MobiTealContainer.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = MobiTeal)
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(text = "tumaini_b2c_disbursements_w21_draft.csv", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Text(text = "142 KB, 38 records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Text(text = "Ingested via secure Web Client • SHA-256 Verified", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = CircleShape
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                        Text(text = "7/7 columns mapped (MSISDN, Name, Emp ID, Branch, Amount, Purpose, National ID)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        // Section C: Pre-Flight Validation Telemetry Dashboard (4 Blocks)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "TOTAL PARSED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(text = "KES 542,000.00", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "38 Records Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "KYC & HEADROOM CLEAN", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    Text(text = "KES 492,000.00", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    Text(text = "35 Records Pre-Flight Passed", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                                }
                            }
                            Surface(color = MobiAmberContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "REVIEW EXCEPTIONS", style = MaterialTheme.typography.labelSmall, color = MobiAmberWarning, fontWeight = FontWeight.Bold)
                                    Text(text = "KES 38,000.00", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MobiAmberWarning)
                                    Text(text = "2 Warnings / Non-Fatal", style = MaterialTheme.typography.bodySmall, color = MobiAmberWarning)
                                }
                            }
                            Surface(color = MobiErrorContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "HARD BLOCKED", style = MaterialTheme.typography.labelSmall, color = MobiError, fontWeight = FontWeight.Bold)
                                    Text(text = "KES 12,000.00", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MobiError)
                                    Text(text = "1 Blocked / Hard Error", style = MaterialTheme.typography.bodySmall, color = MobiError)
                                }
                            }
                        }
                    } else {
                        // STEP 4: Maker 2-FA Sign-Off
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "FINALIZED BATCH PARAMETERS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Text(text = "Ref: MOBI-B2C-2024-W21-084", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(color = Color.White, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(text = "SOURCE MMPS FLOAT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp)
                                            Text(text = "Paybill #600984", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(text = "KES 12,660,250.00 Float Free", style = MaterialTheme.typography.bodySmall, color = MobiTeal)
                                        }
                                    }
                                    Surface(color = Color.White, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(text = "NET DISBURSABLE PAYLOAD", style = MaterialTheme.typography.labelSmall, color = MobiTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(text = "KES 530,000.00", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MobiTeal)
                                            Text(text = "37 Validated Recipient Accounts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Surface(color = Color.White, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(text = "QUARANTINED / EXCLUDED", style = MaterialTheme.typography.labelSmall, color = MobiError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(text = "KES 12,000.00 (1 User)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MobiError)
                                            Text(text = "Peter Mutua (Unregistered SIM)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Surface(color = Color.White, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(text = "PROJECTED FLOAT BALANCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp)
                                            Text(text = "KES 12,130,250.00", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(text = "95.8% Rail Liquidity Safe", style = MaterialTheme.typography.bodySmall, color = MobiTeal)
                                        }
                                    }
                                }
                            }
                        }

                        // Maker vs Checker Separation Matrix
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MobiTeal.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "1. Maker / Preparer (You)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        StatusBadge(text = "ACTIVE SIGNER", backgroundColor = MobiTealContainer.copy(alpha = 0.5f), textColor = Color(0xFF005049))
                                    }
                                    Text(text = "Finance Officer: Joseph Ochieng (#FO-109)", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Department: Central Treasury & Cash Operations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Whitelisted IP: 192.168.10.42 (Corporate VPN)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "2. Checker / Approver (Next Gate)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        StatusBadge(text = "QUEUED FOR DISPATCH", backgroundColor = MobiAmberContainer, textColor = MobiAmberWarning)
                                    }
                                    Text(text = "Designated Approver: Grace Muthoni (#FM-003)", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Credential: Head of Financial Controllership (ICPAK #9412)", style = MaterialTheme.typography.bodySmall, color = MobiTeal)
                                    Text(text = "Dual Auth Threshold: Tier-2 (Up to KES 5,000,000.00)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }

                        // Maker OTP Verification Block
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Enter 6-Digit Authenticator Token (Google Authenticator)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    StatusBadge(text = "TOKEN VERIFIED", icon = Icons.Default.CheckCircle, backgroundColor = Color(0xFFE8F5E9), textColor = Color(0xFF2E7D32))
                                }

                                // 6 PIN boxes (5 9 2 8 1 6)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("5", "9", "2", "8", "1", "6").forEach { digit ->
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                                .border(1.5.dp, MobiTeal, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = digit, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = "Code expires in 38s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }

                                // Attestation checkbox
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MobiAmberContainer.copy(alpha = 0.3f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = makerAttestationChecked,
                                        onCheckedChange = { makerAttestationChecked = it },
                                        colors = CheckboxDefaults.colors(checkedColor = MobiTeal)
                                    )
                                    Text(
                                        text = "Maker Attestation & Statutory Certification: I, Joseph Ochieng (#FO-109), hereby certify under company controllership rules that all 37 disbursements comply with Tumaini Retail non-payroll petty cash policies.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Modal Footer
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step == 3) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = autoExcludeError,
                                    onCheckedChange = { autoExcludeError = it },
                                    colors = CheckboxDefaults.colors(checkedColor = MobiTeal)
                                )
                                Text(
                                    text = "Auto-exclude 1 blocked error row (Peter Mutua)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "NET BATCH DISBURSABLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(text = "KES 530,000.00 (37 Recipients)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MobiTeal)
                                }

                                OutlinedButton(onClick = { viewModel.closeCreateBatchModal() }) {
                                    Text("Save as Draft")
                                }

                                Button(
                                    onClick = { viewModel.setCreateBatchStep(4) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MobiDarkNavy)
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MobiTealFixed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Proceed to Maker Sign-Off & Dual Approval", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.setCreateBatchStep(3) }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Back to Pre-Flight Audit")
                                }
                                OutlinedButton(onClick = { viewModel.closeCreateBatchModal() }) {
                                    Text("Save as Signed Draft")
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { viewModel.closeCreateBatchModal() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MobiError)
                                ) {
                                    Text("Reject / Abort Batch")
                                }

                                Button(
                                    onClick = {
                                        viewModel.closeCreateBatchModal()
                                        viewModel.postNotification("Batch signed & dispatched to Approver Grace Muthoni for release.")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MobiDarkNavy)
                                ) {
                                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = MobiTealFixed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sign Batch & Dispatch to Approver (Grace Muthoni)", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
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
fun ModalStepItem(
    stepNum: String,
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = when {
            isActive -> MobiDarkNavy
            isCompleted -> Color(0xFFE8F5E9)
            else -> Color.White
        },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isActive) MobiTeal else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> MobiTealFixed
                            isCompleted -> Color(0xFF2E7D32)
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                } else {
                    Text(
                        text = stepNum,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) MobiDarkNavy else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    fontSize = 10.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) MobiTealFixed else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    fontSize = 9.sp
                )
            }
        }
    }
}
