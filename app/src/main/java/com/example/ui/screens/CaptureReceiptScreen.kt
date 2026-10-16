package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.MobiWaveViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun CaptureReceiptScreen(
    viewModel: MobiWaveViewModel,
    onNavigateBack: () -> Unit
) {
    var category by remember { mutableStateOf("Transport & Fuel") }
    var amountText by remember { mutableStateOf("3,500.00") }
    var justification by remember { mutableStateOf("Fuel for emergency diesel generator and stock transfer delivery to Eastleigh branch.") }
    var isOfflineMode by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
    ) {
        // 1. Stepper Progression Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Step 1: Capture
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MobiTeal),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "CAPTURE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MobiTeal
                            )
                        }

                        // Line
                        HorizontalDivider(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp),
                            thickness = 2.dp,
                            color = MobiTeal
                        )

                        // Step 2: eTIMS Check (Active)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MobiDarkNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "2",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "eTIMS Check",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Line
                        HorizontalDivider(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        )

                        // Step 3: Submit
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "3",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "Submit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "100% Optical Quality",
                            style = MaterialTheme.typography.bodySmall,
                            color = MobiTeal,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "KRA Fiscal Signature Verified",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Viewfinder & Optical Extraction Display
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .background(MobiDarkNavy)
                    ) {
                        // Real scanned receipt image from provided asset
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCimcCaeRGv9A0Gki93h4up-IQQ9gZeuoLPVrThXSJJiGiD8iskprTns7UBhWjXtdq9ru1gobmlVZbEKUGBljo2bnLTmJucHER5SuTOfjUe7a8hISZph7BauX_3j29oay2iAazlHzo3tDgfYt7H_cqGbYWfjfCdezNDKoev5hEfeRCE7t-_rzGGn18rVsUPinfpwfv_DUqnXxQBD2SaS1iBNA1TIGqkbMkmr0mb1eBVH500VQlBcX0b",
                            contentDescription = "Scanned Receipt Viewfinder",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                                    )
                                )
                        )

                        // Top controls in viewfinder
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        tint = MobiTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "TotalEnergies Kilimani",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.postNotification("Camera sensor recalibrated. Ready to capture.") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.9f)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Retake",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // eTIMS Targeting Laser Bracket over QR Code in Bottom Right
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 16.dp)
                                .size(90.dp)
                                .border(1.5.dp, MobiTealFixed, RoundedCornerShape(8.dp))
                                .background(MobiTeal.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Laser scan beam
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(MobiTealFixed)
                            )
                        }
                    }

                    // OCR Badge Strip below image
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(MobiTealContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF005049),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "KRA eTIMS QR DETECTED & MATCHED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF005049),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "Cryptographic signature valid on KRA Tax Core",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MobiTeal)
                        )
                    }
                }
            }
        }

        // 3. Extracted eTIMS Ledger Record Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = MobiTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Extracted eTIMS Ledger Record",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        StatusBadge(
                            text = "100% MATCH",
                            backgroundColor = MobiTealContainer.copy(alpha = 0.5f),
                            textColor = Color(0xFF005049)
                        )
                    }

                    // 2x2 Grid of Extracted Data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExtractedFieldBox(
                            title = "SUPPLIER TAX PIN",
                            primaryValue = "P051102381W",
                            secondaryValue = "TOTAL ENERGIES KENYA LTD",
                            modifier = Modifier.weight(1f)
                        )
                        ExtractedFieldBox(
                            title = "CU INVOICE NUMBER",
                            primaryValue = "INV-ETIMS-2025-08912",
                            secondaryValue = "Control Unit: Total Kilimani",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExtractedFieldBox(
                            title = "INVOICE DATE & TIME",
                            primaryValue = "14 May 2025",
                            secondaryValue = "14:22 EAT",
                            modifier = Modifier.weight(1f)
                        )
                        ExtractedFieldBox(
                            title = "STATUTORY VAT (16%)",
                            primaryValue = "KES 482.76",
                            secondaryValue = "Calculated Deductible",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Certified Total Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MobiTealContainer.copy(alpha = 0.35f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CERTIFIED INVOICE TOTAL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF005049)
                            )
                            Text(
                                text = "KES 3,500.00",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MobiTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Pre-Reconciled",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MobiTeal
                            )
                        }
                    }
                }
            }
        }

        // 4. Expense Classification Form Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Expense Ledger Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Category Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "OPERATIONAL COST CATEGORY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Delivery Run & Emergency Power",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Claimed Amount
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CLAIMED AMOUNT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SyncAlt,
                                    contentDescription = null,
                                    tint = MobiTeal,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Auto-synced to Receipt",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MobiTeal
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "KES",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = amountText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MobiTeal,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(end = 12.dp)
                            )
                        }
                    }

                    // Justification
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "BUSINESS JUSTIFICATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = justification,
                            onValueChange = { justification = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedBorderColor = MobiTeal,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }

                    // Cost Center Allocation Box
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "COST CENTER ASSIGNMENT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Tumaini Retail #042 Ops",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "BUDGET RESERVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "KES 18,200",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MobiTeal
                                    )
                                }
                            }

                            // Consumption bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(0.65f)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(0.192f)
                                        .fillMaxHeight()
                                        .background(MobiTeal)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(0.158f)
                                        .fillMaxHeight()
                                        .background(Color.Transparent)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Current Draw: 19.2% of Month Pool",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Within Authorized Threshold",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MobiTeal
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Offline Resilient Mode Toggle
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Offline-Resilient Mode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Save locally if Safaricom 4G fluctuates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isOfflineMode,
                        onCheckedChange = { isOfflineMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MobiTeal,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
            }
        }

        // 6. Action Submission Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        viewModel.submitNewExpense(
                            merchant = "TotalEnergies Kilimani",
                            amount = 3500.0,
                            category = category,
                            justification = justification,
                            isOffline = isOfflineMode
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_expense_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MobiDarkNavy)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submit Expense • KES 3,500.00",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = {
                        viewModel.submitNewExpense(
                            merchant = "TotalEnergies Kilimani",
                            amount = 3500.0,
                            category = category,
                            justification = justification,
                            isOffline = true
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_offline_queue_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save to Offline Queue (1 Pending)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ExtractedFieldBox(
    title: String,
    primaryValue: String,
    secondaryValue: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = secondaryValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
