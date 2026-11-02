package com.plusemon.hisab.ui.screens.more

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.domain.util.VersionUtils
import com.plusemon.hisab.ui.components.DeleteConfirmationDialog
import com.plusemon.hisab.ui.components.UserAvatar
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.viewmodel.HisabViewModel

@Composable
fun MoreScreen(
    viewModel: HisabViewModel,
    onNavigateToReports: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val isBn = settings.language == "bn"
    val currentUser by viewModel.currentUser.collectAsState()
    val accounts by viewModel.accountsWithBalances.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val recurringRules by viewModel.recurringRules.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    // PIN Setup Dialog
    if (showPinDialog) {
        SetPinDialog(
            isBangla = isBn,
            onDismiss = { showPinDialog = false },
            onSetPin = { pin ->
                viewModel.setPin(pin)
                showPinDialog = false
                Toast.makeText(context, if (isBn) "পিন সুরক্ষা চালু হয়েছে" else "PIN lock enabled", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Account Confirmation Dialog (purges all cloud Firestore & local Room data)
    if (showDeleteAccountDialog) {
        DeleteConfirmationDialog(
            title = Localization.getString(Localization.Key.DELETE_ACCOUNT, isBn),
            message = Localization.getString(Localization.Key.DELETE_ACCOUNT_CONFIRM, isBn),
            itemDetail = currentUser?.email ?: (if (isBn) "বর্তমান অ্যাকাউন্ট" else "Current Account"),
            isBangla = isBn,
            onConfirm = {
                viewModel.deleteAccount()
                showDeleteAccountDialog = false
            },
            onDismiss = {
                showDeleteAccountDialog = false
            }
        )
    }

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

        // Profile Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
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

        // SECTION 3: PREFERENCES & SECURITY (Moved directly from Settings)
        SectionHeader(
            title = if (isBn) "পছন্দসমূহ ও নিরাপত্তা" else "Preferences & Security"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Column {
                // Language Selection
                SettingsActionRowItem(
                    icon = Icons.Default.Language,
                    title = Localization.getString(Localization.Key.LANGUAGE, isBn),
                    value = if (isBn) "বাংলা" else "English",
                    onClick = { viewModel.toggleLanguage() },
                    testTag = "more_language_tile"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Dark Mode Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleDarkMode() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("more_dark_mode_tile"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (settings.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = Localization.getString(Localization.Key.DARK_MODE, isBn),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Switch(
                        checked = settings.isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() }
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // PIN Code Protection Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("more_pin_tile"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = Localization.getString(Localization.Key.PIN_LOCK, isBn),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Switch(
                        checked = settings.pinEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                showPinDialog = true
                            } else {
                                viewModel.disablePin()
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 4: APP & UPDATES
        SectionHeader(
            title = if (isBn) "অ্যাপ ও সংস্করণ" else "App & Updates"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Column {
                SettingsActionRowItem(
                    icon = Icons.Default.SystemUpdate,
                    title = Localization.getString(Localization.Key.CHECK_UPDATES, isBn),
                    value = VersionUtils.formatDisplayVersion(com.plusemon.hisab.BuildConfig.VERSION_NAME),
                    onClick = { viewModel.checkForUpdates(isManual = true) },
                    testTag = "more_updates_tile"
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 5: DANGER ZONE & ACCOUNT (Only Delete Account & Sign Out)
        SectionHeader(
            title = if (isBn) "বিপদজনক জোন ও অ্যাকাউন্ট" else "Danger Zone & Account",
            titleColor = ExpenseRed
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.35f))
        ) {
            Column {
                // Delete Account & Data
                SettingsActionRowItem(
                    icon = Icons.Default.DeleteForever,
                    title = Localization.getString(Localization.Key.DELETE_ACCOUNT, isBn),
                    titleColor = ExpenseRed,
                    iconBgColor = ExpenseRed.copy(alpha = 0.12f),
                    iconTint = ExpenseRed,
                    onClick = { showDeleteAccountDialog = true },
                    testTag = "more_delete_account_tile"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Sign Out / Log Out
                SettingsActionRowItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = Localization.getString(Localization.Key.SIGN_OUT, isBn),
                    titleColor = ExpenseRed,
                    iconBgColor = ExpenseRed.copy(alpha = 0.12f),
                    iconTint = ExpenseRed,
                    onClick = { viewModel.signOut() },
                    testTag = "more_sign_out_tile"
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

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
private fun SectionHeader(
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = titleColor,
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

@Composable
private fun SettingsActionRowItem(
    icon: ImageVector,
    title: String,
    value: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconBgColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
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

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SetPinDialog(
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSetPin: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = Localization.getString(Localization.Key.SET_PIN, isBangla),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pin = it },
                    label = { Text(if (isBangla) "৪ ডিজিটের পিন" else "4-Digit PIN") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) confirmPin = it },
                    label = { Text(Localization.getString(Localization.Key.CONFIRM_PIN, isBangla)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Text(
                        text = if (isBangla) "পিন দুটি মিলছে না বা ৪ ডিজিট নয়" else "PINs do not match or are not 4 digits",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Localization.getString(Localization.Key.CANCEL, isBangla))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (pin.length == 4 && pin == confirmPin) {
                                onSetPin(pin)
                            } else {
                                isError = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(Localization.getString(Localization.Key.SAVE, isBangla), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
