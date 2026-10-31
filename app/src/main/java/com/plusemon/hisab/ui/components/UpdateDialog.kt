package com.plusemon.hisab.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.plusemon.hisab.data.model.UpdateInfo
import com.plusemon.hisab.domain.util.MarkdownUtils
import com.plusemon.hisab.domain.util.VersionUtils
import com.plusemon.hisab.ui.viewmodel.UpdateUiState

@Composable
fun UpdateDialog(
    updateUiState: UpdateUiState,
    isBangla: Boolean,
    onDownloadAndInstall: (UpdateInfo) -> Unit,
    onDismiss: () -> Unit,
    onEnablePermission: () -> Unit,
    onRetryInstall: (UpdateInfo) -> Unit
) {
    when (updateUiState) {
        is UpdateUiState.Checking -> {
            Dialog(onDismissRequest = onDismiss) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isBangla) "আপডেট চেক করা হচ্ছে..." else "Checking for updates...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        is UpdateUiState.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = if (isBangla) "অ্যাপ সর্বশেষ ভার্সনে আছে" else "You're Up to Date",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    val displayVer = VersionUtils.formatDisplayVersion(updateUiState.currentVersion)
                    Text(
                        text = if (isBangla)
                            "আপনি হিসাব-এর সর্বশেষ সংস্করণ ($displayVer) ব্যবহার করছেন।"
                        else
                            "You are using the latest version of Hisab ($displayVer)."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isBangla) "ঠিক আছে" else "OK")
                    }
                }
            )
        }

        is UpdateUiState.Available -> {
            // Only show modal dialog when triggered manually (e.g. from Settings).
            // Silent startup checks use the inline UpdateCard on the Dashboard.
            if (!updateUiState.isManual) return

            val info = updateUiState.updateInfo
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    val displayVer = VersionUtils.formatDisplayVersion(info.version)
                    Text(
                        text = if (isBangla) "নতুন আপডেট এসেছে: $displayVer" else "Update available: $displayVer",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = if (isBangla) "হিসাব অ্যাপের একটি নতুন সংস্করণ পাওয়া গেছে।" else "A newer version of Hisab is available.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (info.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = MarkdownUtils.parseMarkdown(info.releaseNotes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onDownloadAndInstall(info) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBangla) "ডাউনলোড ও ইন্সটল" else "Download & Install")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(if (isBangla) "পরে" else "Later")
                    }
                }
            )
        }

        is UpdateUiState.Downloading -> {
            if (!updateUiState.isManual) return

            val progressPercent = (updateUiState.progress * 100).toInt().coerceIn(0, 100)
            val displayVer = VersionUtils.formatDisplayVersion(updateUiState.updateInfo.version)
            Dialog(onDismissRequest = {}) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Downloading,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isBangla) "আপডেট ডাউনলোড হচ্ছে ($displayVer)" else "Downloading $displayVer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { updateUiState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBangla) "ডাউনলোড শেষে স্বয়ংক্রিয়ভাবে ইন্সটল হবে" else "Install starts automatically when ready",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$progressPercent%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        is UpdateUiState.PermissionNeeded -> {
            val info = updateUiState.updateInfo
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = if (isBangla) "ইন্সটল করার অনুমতি প্রয়োজন" else "Permission Required",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (isBangla)
                            "অ্যাপ আপডেট ইনস্টল করতে অ্যান্ড্রয়েড সেটিংস থেকে 'অপরিচিত অ্যাপ ইনস্টল' করার অনুমতি প্রদান করুন।"
                        else
                            "To install the updated version, please grant 'Install unknown apps' permission for Hisab in Android settings."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onEnablePermission,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isBangla) "অনুমতি দিন" else "Enable Permission")
                    }
                },
                dismissButton = {
                    Row {
                        OutlinedButton(
                            onClick = { onRetryInstall(info) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isBangla) "পুনরায় চেষ্টা করুন" else "Retry")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onDismiss) {
                            Text(if (isBangla) "পরে" else "Later")
                        }
                    }
                }
            )
        }

        is UpdateUiState.Downloaded -> {
            // Only show modal dialog when triggered manually.
            // Dashboard already displays UpdateReadyCard inline.
            if (!updateUiState.isManual) return

            val info = updateUiState.updateInfo
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    val displayVer = VersionUtils.formatDisplayVersion(info.version)
                    Text(
                        text = if (isBangla) "আপডেট প্রস্তুত: $displayVer" else "Update Ready: $displayVer",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    val displayVer = VersionUtils.formatDisplayVersion(info.version)
                    Text(
                        text = if (isBangla)
                            "নতুন ভার্সন ফাইল ডাউনলোড সম্পন্ন হয়েছে। এখনই ইন্সটল করতে নিচের বাটনে ট্যাপ করুন।"
                        else
                            "The update $displayVer has been downloaded and is ready to install."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onRetryInstall(info) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isBangla) "এখনই ইন্সটল করুন" else "Install Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(if (isBangla) "পরে" else "Later")
                    }
                }
            )
        }

        is UpdateUiState.Idle -> {
            // Nothing to show
        }
    }
}
