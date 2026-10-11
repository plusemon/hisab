package com.plusemon.hisab.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Small status icon displayed in top app bars indicating network connectivity (online vs offline mode).
 */
@Composable
fun NetworkStatusIcon(
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val icon = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff
    val tintColor by animateColorAsState(
        targetValue = if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
        animationSpec = tween(300),
        label = "NetworkIconColor"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isConnected) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f),
        animationSpec = tween(300),
        label = "NetworkBgColor"
    )

    val contentDescription = if (isConnected) "Connected to network" else "Offline mode"

    Surface(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("network_status_icon"),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.3f)),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.size(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tintColor,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
