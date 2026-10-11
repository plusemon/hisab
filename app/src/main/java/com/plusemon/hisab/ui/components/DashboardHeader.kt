package com.plusemon.hisab.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plusemon.hisab.data.model.AppSyncStatus
import com.plusemon.hisab.data.model.User
import com.plusemon.hisab.data.model.UserSettings
import com.plusemon.hisab.domain.util.Localization

/**
 * Modern Dashboard Header displayed on the root tabs (Home/Dashboard).
 * Features:
 * 1. Profile row: Avatar + Greeting & User Name with compact sync status.
 * 2. Header actions:
 *    - Language Switch button styled as uploaded pill design [ 文A EN / বাং ]
 *    - Theme Switcher with System Default (default), Light, and Dark modes.
 */
@Composable
fun DashboardHeader(
    user: User?,
    settings: UserSettings,
    syncStatus: AppSyncStatus = AppSyncStatus.Synced(),
    isNetworkConnected: Boolean = true,
    onSyncClick: () -> Unit = {},
    onToggleDarkMode: () -> Unit = {},
    onSetThemeMode: (useSystem: Boolean, isDark: Boolean) -> Unit = { _, _ -> },
    onToggleLanguage: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = onProfileClick,
    modifier: Modifier = Modifier
) {
    val isBn = settings.language == "bn"

    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        currentHour in 5..11 -> if (isBn) "শুভ সকাল" else "Good Morning"
        currentHour in 12..16 -> if (isBn) "শুভ অপরাহ্ন" else "Good Afternoon"
        currentHour in 17..20 -> if (isBn) "শুভ সন্ধ্যা" else "Good Evening"
        else -> if (isBn) "শুভ রাত্রি" else "Good Night"
    }

    var showThemeMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: User Avatar + Greeting & Name + Compact Sync Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onProfileClick() }
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                ) {
                    UserAvatar(
                        photoUrl = user?.photoUrl,
                        displayName = user?.displayName,
                        size = 38.dp,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            RealTimeStatusBadge(
                                status = syncStatus,
                                isBn = isBn,
                                onSyncClick = onSyncClick,
                                compact = true
                            )
                        }

                        Text(
                            text = user?.displayName ?: (if (isBn) "স্বাগতম" else "Welcome"),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right side: Action Controls (Network Status, Language Pill, Theme Switcher)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NetworkStatusIcon(isConnected = isNetworkConnected)

                    // Language Switch Button (Pill Design matching uploaded screenshot: [ 文A EN / বাং ])
                    Surface(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onToggleLanguage() }
                            .testTag("header_language_btn"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = Localization.getString(Localization.Key.LANGUAGE, isBn),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isBn) "বাং" else "EN",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Theme Switcher Button with System Default, Light & Dark options
                    Box {
                        val currentThemeIcon = when {
                            settings.useSystemTheme -> Icons.Default.BrightnessAuto
                            settings.isDarkMode -> Icons.Default.DarkMode
                            else -> Icons.Default.LightMode
                        }

                        Surface(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showThemeMenu = true }
                                .testTag("header_theme_btn"),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            shadowElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = currentThemeIcon,
                                    contentDescription = Localization.getString(Localization.Key.THEME, isBn),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            // System Default (Default option)
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = Localization.getString(Localization.Key.SYSTEM_DEFAULT, isBn),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (settings.useSystemTheme) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isBn) "সিস্টেম অনুযায়ী (ডিফল্ট)" else "Follows system (Default)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.BrightnessAuto,
                                        contentDescription = null,
                                        tint = if (settings.useSystemTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (settings.useSystemTheme) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    onSetThemeMode(true, false)
                                    showThemeMenu = false
                                }
                            )

                            // Light Mode
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = Localization.getString(Localization.Key.LIGHT_MODE, isBn),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (!settings.useSystemTheme && !settings.isDarkMode) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = if (!settings.useSystemTheme && !settings.isDarkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (!settings.useSystemTheme && !settings.isDarkMode) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    onSetThemeMode(false, false)
                                    showThemeMenu = false
                                }
                            )

                            // Dark Mode
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = Localization.getString(Localization.Key.DARK_MODE, isBn),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (!settings.useSystemTheme && settings.isDarkMode) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = if (!settings.useSystemTheme && settings.isDarkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (!settings.useSystemTheme && settings.isDarkMode) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    onSetThemeMode(false, true)
                                    showThemeMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
