package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.EtimsStatus
import com.example.model.VoucherStatus
import com.example.ui.MainTab
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

fun formatKes(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "KES ${formatter.format(amount)}"
}

fun formatKesNoDecimals(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    return "KES ${formatter.format(amount)}"
}

@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color? = null
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (borderColor != null) Modifier.border(1.dp, borderColor, CircleShape)
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun VoucherStatusPill(status: VoucherStatus) {
    when (status) {
        VoucherStatus.AWAITING_FINANCE -> {
            StatusBadge(
                text = "Awaiting Finance",
                icon = Icons.Default.Schedule,
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        VoucherStatus.APPROVED_RECONCILED -> {
            StatusBadge(
                text = "Approved & Reconciled",
                icon = Icons.Default.CheckCircle,
                backgroundColor = MobiTealContainer.copy(alpha = 0.5f),
                textColor = Color(0xFF005049)
            )
        }
        VoucherStatus.NEEDS_REVIEW -> {
            StatusBadge(
                text = "Needs Review",
                icon = Icons.Default.Warning,
                backgroundColor = MobiAmberContainer,
                textColor = MobiAmberWarning
            )
        }
        VoucherStatus.QUEUED_OFFLINE -> {
            StatusBadge(
                text = "Queued Offline",
                icon = Icons.Default.Sync,
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EtimsStatusPill(status: EtimsStatus) {
    when (status) {
        EtimsStatus.VERIFIED, EtimsStatus.MATCHED -> {
            StatusBadge(
                text = "eTIMS Verified",
                icon = Icons.Default.Verified,
                backgroundColor = Color(0xFFE8F5E9),
                textColor = Color(0xFF2E7D32)
            )
        }
        EtimsStatus.DATA_EXTRACTED -> {
            StatusBadge(
                text = "Data Extracted",
                icon = Icons.Default.DocumentScanner,
                backgroundColor = Color(0xFFE0F2FE),
                textColor = Color(0xFF0284C7)
            )
        }
        EtimsStatus.NEEDS_REVIEW -> {
            StatusBadge(
                text = "Needs Review",
                icon = Icons.Default.PriorityHigh,
                backgroundColor = MobiAmberContainer,
                textColor = MobiAmberWarning
            )
        }
        EtimsStatus.UNVERIFIED -> {
            StatusBadge(
                text = "Unverified",
                icon = Icons.Default.ErrorOutline,
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                textColor = MaterialTheme.colorScheme.outline
            )
        }
        EtimsStatus.OFFLINE -> {
            StatusBadge(
                text = "Offline Saved",
                icon = Icons.Default.CloudQueue,
                backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MobileTopAppBar(
    title: String,
    subtitle: String,
    avatarUrl: String,
    onAvatarClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    onToggleWebMode: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    // MobiWave Logo Badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MobiDarkNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "MobiWave",
                            tint = MobiTealFixed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "MobiWave",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Online & Synced Pill
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MobiTealContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MobiTeal)
                    )
                    Text(
                        text = "ONLINE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF005049),
                        fontSize = 9.sp
                    )
                }

                // Web/Desktop Hub Toggle Button
                FilledTonalIconButton(
                    onClick = onToggleWebMode,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = "Toggle Desktop Hub View",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Profile Avatar
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable(onClick = onAvatarClick)
                )
            }
        }
    }
}

@Composable
fun MobileBottomNavBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onScanClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavItem(
                icon = Icons.Default.Storefront,
                label = "Home",
                isSelected = currentTab == MainTab.HOME,
                onClick = { onTabSelected(MainTab.HOME) },
                testTag = "nav_home"
            )

            NavItem(
                icon = Icons.Default.ReceiptLong,
                label = "Expenses",
                isSelected = currentTab == MainTab.EXPENSES,
                onClick = { onTabSelected(MainTab.EXPENSES) },
                testTag = "nav_expenses"
            )

            // Elevated Center Capture Button
            Box(
                modifier = Modifier
                    .offset(y = (-8).dp)
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MobiTeal)
                    .clickable(onClick = onScanClick)
                    .testTag("nav_scan_center"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Scan Receipt",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            NavItem(
                icon = Icons.Default.Payments,
                label = "Advances",
                isSelected = currentTab == MainTab.ADVANCES,
                onClick = { onTabSelected(MainTab.ADVANCES) },
                testTag = "nav_advances"
            )

            NavItem(
                icon = Icons.Default.Rule,
                label = "Approvals",
                isSelected = currentTab == MainTab.APPROVALS,
                onClick = { onTabSelected(MainTab.APPROVALS) },
                testTag = "nav_approvals"
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MobiTeal else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MobiTeal else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp
        )
    }
}

@Composable
fun ToastNotification(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Surface(
        color = MobiInverseSurface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isError) Icons.Default.Cancel else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isError) Color(0xFFFF8A80) else MobiTealFixed,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
