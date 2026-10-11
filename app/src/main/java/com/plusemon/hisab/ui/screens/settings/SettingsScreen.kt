package com.plusemon.hisab.ui.screens.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.domain.util.VersionUtils
import com.plusemon.hisab.ui.components.DeleteConfirmationDialog
import com.plusemon.hisab.ui.components.DetailTopAppBar
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.viewmodel.HisabViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HisabViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val isBn = settings.language == "bn"
    val currentUser by viewModel.currentUser.collectAsState()
    val isNetworkConnected by viewModel.isNetworkConnected.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

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

    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        DeleteConfirmationDialog(
            title = if (isBn) "সব তথ্য মুছে ফেলবেন?" else "Clear All Data?",
            message = if (isBn)
                "আপনার সমস্ত লেনদেন, অ্যাকাউন্ট, বাজেট, দেনা-পাওনা ও সঞ্চয় লক্ষ্য মুছে ফেলা হবে। এই কাজটি পুনরায় ফিরিয়ে আনা যাবে না।"
            else
                "All your transactions, accounts, budgets, debts and savings goals will be permanently deleted. This action cannot be undone.",
            itemDetail = if (isBn) "সতর্কতা: সমস্ত ডেটা রিসেট হয়ে যাবে" else "Warning: All local and synced data will be wiped",
            isBangla = isBn,
            onConfirm = {
                viewModel.clearAllData()
                showClearDataDialog = false
            },
            onDismiss = {
                showClearDataDialog = false
            }
        )
    }

    // Delete Account Confirmation Dialog
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

    Scaffold(
        topBar = {
            DetailTopAppBar(
                title = Localization.getString(Localization.Key.SETTINGS, isBn),
                onNavigateBack = onNavigateBack,
                isNetworkConnected = isNetworkConnected
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Section: Language & Display
            Text(
                text = if (isBn) "ভাষা ও প্রদর্শন" else "Language & Display",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.Language,
                        title = Localization.getString(Localization.Key.LANGUAGE, isBn),
                        value = if (isBn) "বাংলা" else "English",
                        onClick = { viewModel.toggleLanguage() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowItem(
                        icon = Icons.Default.Language,
                        title = Localization.getString(Localization.Key.NUMERAL_SYSTEM, isBn),
                        value = if (settings.numeralSystem == "bn") "বাংলা (১, ২, ৩)" else "English (1, 2, 3)",
                        onClick = { viewModel.toggleNumeralSystem() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    // Dark Mode Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleDarkMode() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (settings.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = Localization.getString(Localization.Key.DARK_MODE, isBn),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Switch(
                            checked = settings.isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: Security & Preferences
            Text(
                text = if (isBn) "নিরাপত্তা ও পছন্দসমূহ" else "Security & Preferences",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    // PIN Lock Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = Localization.getString(Localization.Key.PIN_LOCK, isBn),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
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

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Recurring Transactions
                    SettingsRowItem(
                        icon = Icons.Default.Repeat,
                        title = Localization.getString(Localization.Key.RECURRING_TRANSACTIONS, isBn),
                        onClick = onNavigateToRecurring
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: App & Updates
            Text(
                text = if (isBn) "অ্যাপ ও সংস্করণ" else "App & Updates",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.SystemUpdate,
                        title = Localization.getString(Localization.Key.CHECK_UPDATES, isBn),
                        value = VersionUtils.formatDisplayVersion(com.plusemon.hisab.BuildConfig.VERSION_NAME),
                        onClick = { viewModel.checkForUpdates(isManual = true) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: Danger Zone & Sign Out
            Text(
                text = if (isBn) "বিপদজনক জোন ও লগআউট" else "Danger Zone & Account",
                style = MaterialTheme.typography.labelLarge,
                color = ExpenseRed,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.Delete,
                        title = if (isBn) "সব তথ্য পরিষ্কার করুন" else "Clear All Data",
                        titleColor = ExpenseRed,
                        onClick = { showClearDataDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowItem(
                        icon = Icons.Default.DeleteForever,
                        title = Localization.getString(Localization.Key.DELETE_ACCOUNT, isBn),
                        titleColor = ExpenseRed,
                        onClick = { showDeleteAccountDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowItem(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        title = Localization.getString(Localization.Key.SIGN_OUT, isBn),
                        titleColor = ExpenseRed,
                        onClick = { viewModel.signOut() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SettingsRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
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
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SetPinDialog(
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


