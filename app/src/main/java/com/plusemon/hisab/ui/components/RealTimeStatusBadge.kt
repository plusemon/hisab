package com.plusemon.hisab.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

internal data class UnifiedStatusVisuals(
    val bgColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val icon: ImageVector,
    val shortLabel: String,
    val fullLabel: String,
    val isLoading: Boolean,
    val isAttentionState: Boolean
)

/**
 * Resolves the unified connectivity + cloud sync status state for the mobile header pill.
 */
internal fun resolveUnifiedStatusVisuals(
    status: AppSyncStatus,
    isNetworkConnected: Boolean,
    isBn: Boolean,
    isDark: Boolean = false
): UnifiedStatusVisuals {
    if (!isNetworkConnected) {
        val fg = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
        val bg = fg.copy(alpha = if (isDark) 0.18f else 0.10f)
        return UnifiedStatusVisuals(
            bgColor = bg,
            contentColor = fg,
            borderColor = fg.copy(alpha = 0.32f),
            icon = Icons.Default.WifiOff,
            shortLabel = if (isBn) "অফলাইন" else "Offline",
            fullLabel = if (isBn) "অফলাইন মোড" else "Offline mode",
            isLoading = false,
            isAttentionState = true
        )
    }

    return when (status) {
        is AppSyncStatus.Saving -> {
            val fg = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
            val bg = fg.copy(alpha = if (isDark) 0.18f else 0.12f)
            UnifiedStatusVisuals(
                bgColor = bg,
                contentColor = fg,
                borderColor = fg.copy(alpha = 0.32f),
                icon = Icons.Default.Save,
                shortLabel = if (isBn) "সেভ" else "Saving",
                fullLabel = if (isBn) "সংরক্ষণ হচ্ছে..." else "Saving...",
                isLoading = true,
                isAttentionState = true
            )
        }
        is AppSyncStatus.Syncing -> {
            val fg = if (isDark) Color(0xFF818CF8) else Color(0xFF4338CA)
            val bg = fg.copy(alpha = if (isDark) 0.18f else 0.12f)
            UnifiedStatusVisuals(
                bgColor = bg,
                contentColor = fg,
                borderColor = fg.copy(alpha = 0.32f),
                icon = Icons.Default.CloudSync,
                shortLabel = if (isBn) "সিঙ্ক" else "Sync",
                fullLabel = if (isBn) "সিঙ্ক হচ্ছে..." else "Syncing...",
                isLoading = true,
                isAttentionState = true
            )
        }
        is AppSyncStatus.CheckingUpdates -> {
            val fg = if (isDark) Color(0xFFC084FC) else Color(0xFF7E22CE)
            val bg = fg.copy(alpha = if (isDark) 0.18f else 0.12f)
            UnifiedStatusVisuals(
                bgColor = bg,
                contentColor = fg,
                borderColor = fg.copy(alpha = 0.32f),
                icon = Icons.Default.Refresh,
                shortLabel = if (isBn) "আপডেট" else "Update",
                fullLabel = if (isBn) "আপডেট খোঁজা হচ্ছে..." else "Checking updates...",
                isLoading = true,
                isAttentionState = true
            )
        }
        is AppSyncStatus.Error -> {
            val fg = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
            val bg = fg.copy(alpha = if (isDark) 0.18f else 0.12f)
            UnifiedStatusVisuals(
                bgColor = bg,
                contentColor = fg,
                borderColor = fg.copy(alpha = 0.35f),
                icon = Icons.Default.ErrorOutline,
                shortLabel = if (isBn) "ত্রুটি" else "Error",
                fullLabel = if (isBn) "সিঙ্ক ত্রুটি" else "Sync error",
                isLoading = false,
                isAttentionState = true
            )
        }
        is AppSyncStatus.Saved,
        is AppSyncStatus.Synced,
        is AppSyncStatus.Idle -> {
            val fg = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
            val bg = fg.copy(alpha = if (isDark) 0.16f else 0.10f)
            UnifiedStatusVisuals(
                bgColor = bg,
                contentColor = fg,
                borderColor = fg.copy(alpha = 0.28f),
                icon = Icons.Default.CloudDone,
                shortLabel = if (isBn) "সিঙ্কড" else "Synced",
                fullLabel = if (isBn) "অনলাইন ও সিঙ্ক হয়েছে" else "Online & Synced",
                isLoading = false,
                isAttentionState = false
            )
        }
    }
}

/**
 * Unified mobile-responsive status pill combining network connectivity and cloud sync state.
 * Designed to sit cleanly in the header action bar without crowding or truncating user greeting text.
 */
@Composable
fun RealTimeStatusBadge(
    status: AppSyncStatus,
    isBn: Boolean,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    isNetworkConnected: Boolean = true,
    allowCompactLabel: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val visuals = resolveUnifiedStatusVisuals(
        status = status,
        isNetworkConnected = isNetworkConnected,
        isBn = isBn,
        isDark = isDark
    )

    val animatedBg by animateColorAsState(
        targetValue = visuals.bgColor,
        animationSpec = tween(250),
        label = "UnifiedStatusBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = visuals.borderColor,
        animationSpec = tween(250),
        label = "UnifiedStatusBorder"
    )

    Surface(
        modifier = modifier
            .height(36.dp)
            .defaultMinSize(minWidth = 36.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSyncClick)
            .testTag("realtime_status_badge"),
        color = animatedBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, animatedBorder),
        shadowElevation = 0.dp
    ) {
        AnimatedContent(
            targetState = visuals,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "StatusBadgeAnim"
        ) { currentVisuals ->
            // In compact mobile header mode:
            // - When online & synced, display a clean icon-only pill (36x36dp) so the header stays uncluttered.
            // - When offline, syncing, saving, or error, show the concise 1-word label if screen width allows.
            val showText = if (compact) {
                allowCompactLabel && currentVisuals.isAttentionState
            } else {
                true
            }

            Row(
                modifier = Modifier.padding(
                    horizontal = if (showText) 9.dp else 8.dp,
                    vertical = 6.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (currentVisuals.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(15.dp),
                        strokeWidth = 1.8.dp,
                        color = currentVisuals.contentColor
                    )
                } else {
                    Icon(
                        imageVector = currentVisuals.icon,
                        contentDescription = currentVisuals.fullLabel,
                        tint = currentVisuals.contentColor,
                        modifier = Modifier.size(17.dp)
                    )
                }

                if (showText) {
                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = if (compact) currentVisuals.shortLabel else currentVisuals.fullLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = currentVisuals.contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
