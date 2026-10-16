package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ExpenseVoucher
import com.example.model.VoucherStatus
import com.example.ui.MobiWaveViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ExpensesScreen(
    viewModel: MobiWaveViewModel,
    onNavigateToScan: () -> Unit
) {
    val expenses by viewModel.expenses.collectAsState()
    val activeFilter by viewModel.expenseFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSyncPaused by viewModel.isSyncPaused.collectAsState()

    val filteredExpenses = remember(expenses, activeFilter, searchQuery) {
        expenses.filter { item ->
            val matchesFilter = when (activeFilter) {
                "pending" -> item.status == VoucherStatus.AWAITING_FINANCE
                "verified" -> item.status == VoucherStatus.APPROVED_RECONCILED
                "needs-review" -> item.status == VoucherStatus.NEEDS_REVIEW
                "offline" -> item.status == VoucherStatus.QUEUED_OFFLINE
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                item.merchantName.contains(searchQuery, ignoreCase = true) ||
                item.id.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true) ||
                item.cuNumber.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp)
    ) {
        // 1. Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "Search voucher, merchant, PIN, or KES...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MobiTeal,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_search_input")
            )
        }

        // 2. Horizontal Filter Pills
        item {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterPillItem(
                    label = "All",
                    count = "14",
                    isSelected = activeFilter == "all",
                    onClick = { viewModel.setExpenseFilter("all") }
                )
                FilterPillItem(
                    label = "Pending Review",
                    count = "2",
                    isSelected = activeFilter == "pending",
                    onClick = { viewModel.setExpenseFilter("pending") }
                )
                FilterPillItem(
                    label = "Externally Verified",
                    count = "8",
                    dotColor = MobiTeal,
                    isSelected = activeFilter == "verified",
                    onClick = { viewModel.setExpenseFilter("verified") }
                )
                FilterPillItem(
                    label = "Needs Review",
                    count = "1",
                    dotColor = MobiAmberWarning,
                    isSelected = activeFilter == "needs-review",
                    onClick = { viewModel.setExpenseFilter("needs-review") }
                )
                FilterPillItem(
                    label = "Offline Queued",
                    count = "1",
                    icon = Icons.Default.CloudOff,
                    isSelected = activeFilter == "offline",
                    onClick = { viewModel.setExpenseFilter("offline") }
                )
            }
        }

        // 3. Offline Sync Warning Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MobiTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1 local voucher queued",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "STORAGE V2.4",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "1 expense queued locally. Will automatically sync when 4G/Wi-Fi connection is restored.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable { viewModel.forcePushSync() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MobiTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Retry Now",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MobiTeal
                                )
                            }
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Text(
                                text = "Queue: KES 850.00",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4. Section Date & Swipe Hint
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "MAY 2025 LEDGER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        letterSpacing = 0.5.sp
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwipeLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Swipe left to view QR / Actions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // 5. Expense Voucher Cards
        items(filteredExpenses, key = { it.id }) { voucher ->
            ExpenseVoucherCard(
                voucher = voucher,
                onClick = { viewModel.openInvoiceDetailModal(voucher) },
                onUpdateReceipt = onNavigateToScan
            )
        }

        // 6. May 2025 Reconciliation Summary Footer
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
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
                        Text(
                            text = "MAY 2025 RECONCILIATION SUMMARY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "85% Processed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MobiTeal
                        )
                    }

                    // Progress Track
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.85f)
                                .fillMaxHeight()
                                .background(MobiTeal)
                        )
                        Box(
                            modifier = Modifier
                                .weight(0.15f)
                                .fillMaxHeight()
                                .background(Color(0xFF565E74))
                        )
                    }

                    // Metric breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Submitted",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "KES 23,300",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "14 vouchers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column {
                            Text(
                                text = "Accepted",
                                style = MaterialTheme.typography.labelSmall,
                                color = MobiTeal,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "KES 19,800",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MobiTeal
                            )
                            Text(
                                text = "Reconciled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column {
                            Text(
                                text = "Pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "KES 3,500",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Audit stage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tumaini Retail Branch #042",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable {
                                viewModel.postNotification("Tax Report PDF generated and saved.")
                            }
                        ) {
                            Text(
                                text = "Download Tax Report",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MobiTeal
                            )
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = MobiTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterPillItem(
    label: String,
    count: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    dotColor: Color? = null,
    icon: ImageVector? = null
) {
    Surface(
        color = if (isSelected) MobiDarkNavy else MaterialTheme.colorScheme.surfaceContainer,
        shape = CircleShape,
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = if (isSelected) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = CircleShape
            ) {
                Text(
                    text = count,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun ExpenseVoucherCard(
    voucher: ExpenseVoucher,
    onClick: () -> Unit,
    onUpdateReceipt: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (voucher.status == VoucherStatus.QUEUED_OFFLINE)
                MaterialTheme.colorScheme.surfaceContainerLow
            else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("voucher_item_${voucher.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Code, Date, Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (voucher.status == VoucherStatus.NEEDS_REVIEW)
                                MobiAmberContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = voucher.id,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (voucher.status == VoucherStatus.NEEDS_REVIEW)
                                    MobiAmberWarning
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = voucher.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = voucher.merchantName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatKes(voucher.grossAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (voucher.status == VoucherStatus.APPROVED_RECONCILED) "Reconciled"
                        else if (voucher.status == VoucherStatus.QUEUED_OFFLINE) "Offline cached"
                        else if (voucher.status == VoucherStatus.NEEDS_REVIEW) "ACTION REQUIRED"
                        else "Incl. 16% VAT",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (voucher.status == VoucherStatus.NEEDS_REVIEW) FontWeight.Bold else FontWeight.Normal,
                        color = if (voucher.status == VoucherStatus.NEEDS_REVIEW) MobiAmberWarning
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Mid Row: Category & Thumbnail
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
                        imageVector = when (voucher.category) {
                            "Transport & Fuel" -> Icons.Default.LocalGasStation
                            "Shop Supplies" -> Icons.Default.Inventory2
                            "Shop Maintenance" -> Icons.Default.Build
                            "Logistics" -> Icons.Default.LocalShipping
                            else -> Icons.Default.Receipt
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = voucher.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (voucher.receiptImageUrl != null) {
                        AsyncImage(
                            model = voucher.receiptImageUrl,
                            contentDescription = "Receipt Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                    Text(
                        text = voucher.cuNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Audit Feedback Box if present
            if (voucher.auditFeedback != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MobiAmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Audit Feedback from Controllership",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = voucher.auditFeedback,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Badges & Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VoucherStatusPill(voucher.status)
                    EtimsStatusPill(voucher.etimsStatus)
                }

                if (voucher.status == VoucherStatus.NEEDS_REVIEW) {
                    Button(
                        onClick = onUpdateReceipt,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MobiDarkNavy),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Update Receipt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
