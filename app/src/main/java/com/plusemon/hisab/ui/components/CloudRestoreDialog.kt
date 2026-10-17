package com.plusemon.hisab.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.plusemon.hisab.data.repository.CloudRestoreSummary
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.ui.viewmodel.HisabViewModel
import com.plusemon.hisab.ui.viewmodel.RestoreUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CloudRestoreDialog(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val restoreUiState by viewModel.restoreUiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isBn = settings.language == "bn"

    if (restoreUiState is RestoreUiState.Idle) {
        return
    }

    AlertDialog(
        onDismissRequest = {
            if (restoreUiState is RestoreUiState.Completed || restoreUiState is RestoreUiState.Error) {
                viewModel.dismissRestoreDialog()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = restoreUiState !is RestoreUiState.Restoring,
            dismissOnClickOutside = restoreUiState !is RestoreUiState.Restoring
        ),
        modifier = modifier.testTag("cloud_restore_dialog"),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (restoreUiState) {
                    is RestoreUiState.Restoring -> {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isBn) "তথ্য পুনরুদ্ধার হচ্ছে" else "Restoring Cloud Data",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    is RestoreUiState.Completed -> {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF1B5E20),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isBn) "তথ্য পুনরুদ্ধার সম্পন্ন!" else "Restore Complete!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20),
                            textAlign = TextAlign.Center
                        )
                    }

                    is RestoreUiState.Error -> {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isBn) "পুনরুদ্ধারে সমস্যা হয়েছে" else "Restoration Notice",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> Unit
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = restoreUiState) {
                    is RestoreUiState.Restoring -> {
                        Text(
                            text = if (isBn)
                                "ক্লাউড থেকে আপনার সমস্ত হিসাব, অ্যাকাউন্ট ও লেনদেন নামানো হচ্ছে..."
                            else
                                "Downloading your accounts, balances and transactions from cloud storage...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .testTag("restore_progress_bar"),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedContent(
                                targetState = state.step,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "restore_step_text",
                                modifier = Modifier.weight(1f)
                            ) { stepText ->
                                Text(
                                    text = stepText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            val percent = (state.progress * 100).toInt()
                            Text(
                                text = if (isBn) "${Formatters.toBanglaDigits(percent.toString())}%" else "$percent%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    is RestoreUiState.Completed -> {
                        val summary = state.summary
                        Text(
                            text = if (isBn)
                                "আপনার ক্লাউড ডাটা সফলভাবে বর্তমান ডিভাইসে যুক্ত হয়েছে।"
                            else
                                "Your cloud records have been cleanly restored and merged with your local storage.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Summary badges grid
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (summary.accountsCount > 0) {
                                RestoreBadge(
                                    icon = Icons.Default.AccountBalanceWallet,
                                    label = if (isBn) "${Formatters.toBanglaDigits(summary.accountsCount.toString())}টি অ্যাকাউন্ট" else "${summary.accountsCount} Accounts"
                                )
                            }
                            if (summary.transactionsCount > 0) {
                                RestoreBadge(
                                    icon = Icons.Default.ReceiptLong,
                                    label = if (isBn) "${Formatters.toBanglaDigits(summary.transactionsCount.toString())}টি লেনদেন" else "${summary.transactionsCount} Transactions"
                                )
                            }
                            if (summary.categoriesCount > 0) {
                                RestoreBadge(
                                    icon = Icons.Default.Category,
                                    label = if (isBn) "${Formatters.toBanglaDigits(summary.categoriesCount.toString())}টি ক্যাটাগরি" else "${summary.categoriesCount} Categories"
                                )
                            }
                            if (summary.budgetsCount > 0) {
                                RestoreBadge(
                                    icon = Icons.Default.PieChart,
                                    label = if (isBn) "${Formatters.toBanglaDigits(summary.budgetsCount.toString())}টি বাজেট" else "${summary.budgetsCount} Budgets"
                                )
                            }
                            if (summary.goalsCount > 0) {
                                RestoreBadge(
                                    icon = Icons.Default.Savings,
                                    label = if (isBn) "${Formatters.toBanglaDigits(summary.goalsCount.toString())}টি লক্ষ্য" else "${summary.goalsCount} Goals"
                                )
                            }
                            if (summary.debtsCount > 0) {
                                RestoreBadge(
                                    icon = Icons.Default.Handshake,
                                    label = if (isBn) "${Formatters.toBanglaDigits(summary.debtsCount.toString())}টি দেনা-পাওনা" else "${summary.debtsCount} Debts"
                                )
                            }
                        }
                    }

                    is RestoreUiState.Error -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> Unit
                }
            }
        },
        confirmButton = {
            when (restoreUiState) {
                is RestoreUiState.Completed -> {
                    Button(
                        onClick = { viewModel.dismissRestoreDialog() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_done_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isBn) "ড্যাশবোর্ডে প্রবেশ করুন" else "Go to Dashboard")
                    }
                }

                is RestoreUiState.Error -> {
                    Button(
                        onClick = { viewModel.triggerRestoreFromCloud(isManual = true) },
                        modifier = Modifier.testTag("restore_retry_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBn) "পুনরায় চেষ্টা করুন" else "Retry")
                    }
                }

                else -> Unit
            }
        },
        dismissButton = {
            if (restoreUiState is RestoreUiState.Error) {
                TextButton(
                    onClick = { viewModel.dismissRestoreDialog() },
                    modifier = Modifier.testTag("restore_cancel_button")
                ) {
                    Text(if (isBn) "বাতিল" else "Dismiss")
                }
            }
        }
    )
}

@Composable
private fun RestoreBadge(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
