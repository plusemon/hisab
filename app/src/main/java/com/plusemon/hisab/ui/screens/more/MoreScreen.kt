package com.plusemon.hisab.ui.screens.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plusemon.hisab.data.model.AppSyncStatus
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.domain.util.VersionUtils
import com.plusemon.hisab.ui.components.UserAvatar
import com.plusemon.hisab.ui.viewmodel.HisabViewModel

@Composable
fun MoreScreen(
    viewModel: HisabViewModel,
    onNavigateToReports: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isBn = settings.language == "bn"
    val currentUser by viewModel.currentUser.collectAsState()
    val syncStatus by viewModel.appSyncStatus.collectAsState()
    val accounts by viewModel.accountsWithBalances.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val recurringRules by viewModel.recurringRules.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 96.dp)
    ) {
        // Top Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBn) "আরও মেনু" else "More Menu",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Profile Card with Edit/Settings shortcut
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSettings() }
                .testTag("more_profile_banner"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    photoUrl = currentUser?.photoUrl,
                    displayName = currentUser?.displayName,
                    size = 52.dp,
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = currentUser?.displayName ?: (if (isBn) "ব্যবহারকারী" else "User"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentUser?.isGoogleUser == true) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF4285F4).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Google",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A73E8),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = currentUser?.email ?: (if (isBn) "অফলাইন অ্যাকাউন্ট" else "Offline Account"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Edit / Settings shortcut button
                FilledTonalIconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("more_profile_edit_btn"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = if (isBn) "সেটিংস খুলুন" else "Edit Settings",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 1: INSIGHTS & ANALYTICS
        SectionHeader(
            title = if (isBn) "অন্তর্দৃষ্টি ও বিশ্লেষণ" else "Insights & Analytics"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Column {
                MoreTileItem(
                    icon = Icons.Default.BarChart,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = if (isBn) "রিপোর্ট ও বিবরণী" else "Reports & Insights",
                    subtitle = if (isBn) "আয়-ব্যয়ের চিত্রলৈখিক বিশ্লেষণ ও ট্রেন্ড" else "Income vs Expense, category breakdowns & trends",
                    onClick = onNavigateToReports,
                    testTag = "more_reports_tile"
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 2: MANAGEMENT
        SectionHeader(
            title = if (isBn) "ব্যবস্থাপনা" else "Management"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Column {
                MoreTileItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.14f),
                    iconTint = Color(0xFF1D4ED8),
                    title = if (isBn) "অ্যাকাউন্ট ও ওয়ালেট" else "Accounts & Wallets",
                    subtitle = if (isBn) "ব্যাংক, মোবাইল ব্যাংকিং (বিকাশ, নগদ) ও নগদ টাকা" else "Bank, Mobile Banking, Cash accounts",
                    badgeText = if (accounts.isNotEmpty()) "${accounts.size}" else null,
                    onClick = onNavigateToAccounts,
                    testTag = "more_accounts_tile"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                MoreTileItem(
                    icon = Icons.Default.Category,
                    iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.14f),
                    iconTint = Color(0xFF6D28D9),
                    title = if (isBn) "ক্যাটাগরি ব্যবস্থাপনা" else "Manage Categories",
                    subtitle = if (isBn) "আয় ও ব্যয়ের ক্যাটাগরি, আইকন এবং রং পরিবর্তন" else "Custom expense & income categories, icons & colors",
                    badgeText = if (categories.isNotEmpty()) "${categories.size}" else null,
                    onClick = onNavigateToCategories,
                    testTag = "more_categories_tile"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                MoreTileItem(
                    icon = Icons.Default.Repeat,
                    iconBgColor = Color(0xFF0D9488).copy(alpha = 0.14f),
                    iconTint = Color(0xFF0F766E),
                    title = if (isBn) "স্বয়ংক্রিয় লেনদেন" else "Recurring Transactions",
                    subtitle = if (isBn) "নিয়মিত বিল, বেতন ও পুনরাবৃত্ত লেনদেনের নিয়ম" else "Automated bills, subscriptions & scheduled rules",
                    badgeText = if (recurringRules.isNotEmpty()) "${recurringRules.size}" else null,
                    onClick = onNavigateToRecurring,
                    testTag = "more_recurring_tile"
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 3: APP SETTINGS & CLOUD (Clean navigation tiles without duplicate controls)
        SectionHeader(
            title = if (isBn) "অ্যাপ সেটিংস ও ক্লাউড" else "Settings & Cloud"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Column {
                // Navigation tile to Settings & Security
                MoreTileItem(
                    icon = Icons.Default.Settings,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    title = if (isBn) "সেটিংস ও নিরাপত্তা" else "Settings & Security",
                    subtitle = if (isBn) "ভাষা, ডার্ক মোড, পিন কোড ও পছন্দসমূহ" else "Language, dark mode, PIN protection & preferences",
                    onClick = onNavigateToSettings,
                    testTag = "more_settings_tile"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Cloud Backup & Sync (simple list tile showing last sync time, tapping initiates manual sync)
                val syncSubtitle = when (syncStatus) {
                    is AppSyncStatus.Syncing -> if (isBn) "ক্লাউড সিঙ্ক চলছে..." else "Syncing with cloud..."
                    is AppSyncStatus.Synced -> {
                        val timeStr = Formatters.formatTime((syncStatus as AppSyncStatus.Synced).timestamp)
                        if (isBn) "সর্বশেষ সিঙ্ক: $timeStr • ট্যাপ করে সিঙ্ক করুন" else "Last synced: $timeStr • Tap to sync"
                    }
                    is AppSyncStatus.Saved -> {
                        val timeStr = Formatters.formatTime((syncStatus as AppSyncStatus.Saved).timestamp)
                        if (isBn) "সর্বশেষ সংরক্ষণ: $timeStr • ট্যাপ করে সিঙ্ক করুন" else "Last saved: $timeStr • Tap to sync"
                    }
                    is AppSyncStatus.Error -> {
                        if (isBn) "সিঙ্কে সমস্যা হয়েছে • ট্যাপ করে পুনরায় চেষ্টা করুন" else "Sync failed • Tap to retry"
                    }
                    else -> if (isBn) "ক্লাউড ব্যাকআপ • ট্যাপ করে সিঙ্ক করুন" else "Cloud backup • Tap to sync now"
                }

                MoreTileItem(
                    icon = Icons.Default.CloudSync,
                    iconBgColor = Color(0xFF0F766E).copy(alpha = 0.14f),
                    iconTint = Color(0xFF0F766E),
                    title = if (isBn) "ক্লাউড ব্যাকআপ ও সিঙ্ক" else "Cloud Backup & Sync",
                    subtitle = syncSubtitle,
                    onClick = { viewModel.triggerManualSync() },
                    testTag = "more_sync_tile"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Footer / Version Info
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "হিসাব • Hisab Tracker",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Version ${VersionUtils.formatDisplayVersion(com.plusemon.hisab.BuildConfig.VERSION_NAME)} • Offline-first with Cloud Sync",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 2.dp, top = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun MoreTileItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (badgeText != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
