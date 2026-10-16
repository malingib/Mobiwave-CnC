package com.example.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.MobiWaveViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun InvoiceVerificationModal(
    viewModel: MobiWaveViewModel
) {
    var check1 by remember { mutableStateOf(true) }
    var check2 by remember { mutableStateOf(true) }
    var check3 by remember { mutableStateOf(true) }
    var check4 by remember { mutableStateOf(true) }
    var auditNote by remember { mutableStateOf("Generator run confirmed against Branch Security Log for black-out period 13-MAY...") }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = { viewModel.closeInvoiceDetailModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Modal Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "VOUCHER #EXP-2025-0514-089",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Westlands Branch #018",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = MobiTealContainer.copy(alpha = 0.4f),
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = MobiTeal, modifier = Modifier.size(12.dp))
                                Text(
                                    text = "KRA eTIMS QR: Cryptographically Validated",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF005049),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    IconButton(onClick = { viewModel.closeInvoiceDetailModal() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerLow)

                // Split Body (Left: Receipt Scan, Right: Comparison Matrix)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Panel: Physical Scan & Vision AI (Width ~42%)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Scan Card
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
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, tint = MobiTeal, modifier = Modifier.size(18.dp))
                                        Text(text = "eTIMS Physical Scan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Text(text = "PIN: DEMO-PIN", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                }

                                // Receipt Visual Simulation
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = "TOTALENERGIES KENYA PLC", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(text = "Westlands Service Station • Retail Branch #04", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "PIN: DEMO-PIN • VAT REG NO: 0102934-M", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "CU INVOICE: INV-2025-09812", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            Text(text = "13-MAY-2025 18:22 EAT", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "BUYER: TUMAINI SUPERMARKET LTD", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "BUYER PIN: DEMO-PIN", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column {
                                                Text(text = "LOW SULPHUR DIESEL (AGO)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                Text(text = "78.53 Litres @ KES 191.00 / L", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                            Text(text = "15,000.00", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "Taxable Subtotal (16%):", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "KES 12,931.03", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "VAT Charged (16%):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MobiTeal)
                                            Text(text = "KES 2,068.97", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MobiTeal)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "TOTAL AMOUNT:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(text = "KES 15,000.00", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MobiTeal)
                                        }

                                        // QR Code
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MobiDarkNavy),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(imageVector = Icons.Default.QrCode2, contentDescription = null, tint = MobiTealFixed, modifier = Modifier.size(36.dp))
                                                }
                                                Column {
                                                    Text(text = "SHA-256 SIGNATURE VALID", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MobiTeal)
                                                    Text(text = "CU Serial: KRA-CU-098234812", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                                    Text(text = "Signed by KRA Security Hardware Module", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Vision AI & Tamper Verification Audit
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Vision AI & Tamper Verification", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    StatusBadge(text = "99.4% CONFIDENCE", backgroundColor = MobiTealContainer.copy(alpha = 0.5f), textColor = Color(0xFF005049))
                                }
                                Text(
                                    text = "Receipt captured on 13-MAY 18:24 via Mobile POS. Metadata geotag (-1.2644, 36.8041) corresponds to TotalEnergies Karuna Close, Westlands.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Right Panel: Data Harmonization Matrix & Attestation (Width ~58%)
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Sample Data Comparison — Not Verified",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(text = "Illustrative values only. No live KRA lookup or supplier verification has occurred.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                HarmonizationRow(field = "Supplier Name", claimed = "TotalEnergies Westlands", verified = "TotalEnergies Kenya PLC", verdict = "Fuzzy 98%")
                                HarmonizationRow(field = "Supplier PIN", claimed = "DEMO-PIN", verified = "DEMO-PIN", verdict = "100% Exact")
                                HarmonizationRow(field = "Fiscal Invoice No.", claimed = "INV-2025-09812", verified = "INV-2025-09812", verdict = "100% Exact")
                                HarmonizationRow(field = "Total Gross Amount", claimed = "KES 15,000.00", verified = "KES 15,000.00", verdict = "Zero Discrepancy")
                                HarmonizationRow(field = "Standard Rate 16% VAT", claimed = "KES 2,068.97", verified = "KES 2,068.97", verdict = "100% Deductible")
                                HarmonizationRow(field = "General Ledger Code", claimed = "#GL-6410", verified = "#GL-6410 Verified", verdict = "Aligned")
                            }
                        }

                        // Tax Fiscal Summary Blocks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = "INPUT TAX CREDIT (ITC)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp)
                                    Text(text = "KES 2,068.97", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MobiTeal)
                                    Text(text = "Claimable on KRA portal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                            }
                            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = "NET ODEMO-PIN EXPENSE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp)
                                    Text(text = "KES 12,931.03", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(text = "Debited to Westlands #018", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                            }
                            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = "WITHHOLDING VAT (2%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp)
                                    Text(text = "KES 0.00", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(text = "Exempt petroleum tag", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                            }
                        }

                        // Controller Statutory Attestation 4-Checklist
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Controller Statutory Attestation",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    AttestationCheckItem(title = "Valid eTIMS Tax Registry", subtitle = "PIN confirmed on iTax", checked = check1, onToggle = { check1 = it }, modifier = Modifier.weight(1f))
                                    AttestationCheckItem(title = "Genuine Cryptographic KRA QR", subtitle = "Public key verified", checked = check2, onToggle = { check2 = it }, modifier = Modifier.weight(1f))
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    AttestationCheckItem(title = "Valid GL & Deductibility", subtitle = "Section 17 compliant", checked = check3, onToggle = { check3 = it }, modifier = Modifier.weight(1f))
                                    AttestationCheckItem(title = "Delegation Limit Authority", subtitle = "Threshold respected", checked = check4, onToggle = { check4 = it }, modifier = Modifier.weight(1f))
                                }

                                OutlinedTextField(
                                    value = auditNote,
                                    onValueChange = { auditNote = it },
                                    label = { Text("Internal Audit Log Annotation") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Modal Footer Actions
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.closeInvoiceDetailModal()
                                    viewModel.postNotification("Voucher rejected and returned to branch.", isError = true)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MobiError)
                            ) {
                                Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Reject & Recover Float", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.closeInvoiceDetailModal()
                                    viewModel.postNotification("Query flagged to branch manager.")
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "Flag Query to Branch", fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.closeInvoiceDetailModal()
                                viewModel.postNotification("Voucher #EXP-2025-0514-089 accepted and certified for KRA tax filing!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MobiTeal),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Accept & Certify for KRA Tax Filing", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HarmonizationRow(
    field: String,
    claimed: String,
    verified: String,
    verdict: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = field, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Text(text = claimed, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.2f))
        Text(text = verified, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MobiTeal, modifier = Modifier.weight(1.2f))
        StatusBadge(text = verdict, backgroundColor = MobiTealContainer.copy(alpha = 0.5f), textColor = Color(0xFF005049))
    }
}

@Composable
fun AttestationCheckItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.clickable { onToggle(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(checkedColor = MobiTeal)
            )
            Column {
                Text(text = title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}
