package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AppRepository
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AnyWatchShieldsModal(
    repository: AppRepository,
    currentUrl: String = "",
    onDismiss: () -> Unit,
    onReloadRequested: () -> Unit,
    onClearDataRequested: () -> Unit = {}
) {
    val shieldsEnabled by repository.anywatchShieldsEnabled.collectAsState()
    val blockedCount by repository.blockedAdsCount.collectAsState()
    val whitelistedDomains by repository.whitelistedDomains.collectAsState()
    val desktopMode by repository.desktopMode.collectAsState()
    val is18Plus by repository.is18PlusEnabled.collectAsState()
    val unblocker by repository.unblockerEnabled.collectAsState()

    val currentHost = remember(currentUrl) {
        try {
            Uri.parse(currentUrl).host?.lowercase()?.removePrefix("www.") ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    val isCurrentSiteWhitelisted = currentHost.isNotBlank() && whitelistedDomains.contains(currentHost)

    var isModalVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isModalVisible = true }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = isModalVisible,
            enter = fadeIn(animationSpec = tween(220)) + slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(animationSpec = tween(140)) + slideOutVertically(targetOffsetY = { it / 4 })
        ) {
            Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .border(
                    1.dp,
                    Brush.horizontalGradient(listOf(NeonCyan, NeonViolet, NeonPink)),
                    RoundedCornerShape(20.dp)
                )
                .testTag("anywatch_shields_modal"),
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: "AnyWatch Shields UP"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(NeonCyan.copy(alpha = 0.4f), NeonViolet.copy(alpha = 0.2f))
                                    )
                                )
                                .border(1.2.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "AnyWatch Shields UP",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.3.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (shieldsEnabled) NeonCyan.copy(alpha = 0.2f) else AlertRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        if (shieldsEnabled) "ACTIVE" else "OFF",
                                        color = if (shieldsEnabled) NeonCyan else AlertRed,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                "Brave-Grade 3-Level Defense System",
                                color = NeonCyan.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // DYNAMIC COUNTER CARD: "Ads & Trackers Blocked: [Number]"
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.7f), NeonViolet.copy(alpha = 0.5f)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Ads & Trackers Blocked",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$blockedCount",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonViolet.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f))
                        ) {
                            Text(
                                "BRAVE SHIELD",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SINGLE UNIFIED ANYWATCH SHIELD MASTER TOGGLE
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (shieldsEnabled) NeonCyan.copy(alpha = 0.12f) else ObsidianCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (shieldsEnabled) NeonCyan else Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (shieldsEnabled) NeonCyan else TextSecondary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (shieldsEnabled) ObsidianBlack else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "AnyWatch Shield",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (shieldsEnabled) "3-Level Protection Active" else "Shield Paused",
                                    color = if (shieldsEnabled) NeonCyan else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = shieldsEnabled,
                            onCheckedChange = {
                                repository.setAnywatchShieldsEnabled(it)
                                onReloadRequested()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBlack,
                                checkedTrackColor = NeonCyan
                            )
                        )
                    }
                }

                // PER-SITE WHITELIST TOGGLE
                if (currentHost.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurrentSiteWhitelisted) NeonPink.copy(alpha = 0.15f) else ObsidianCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCurrentSiteWhitelisted) NeonPink else Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrentSiteWhitelisted) NeonPink.copy(alpha = 0.25f) else NeonViolet.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = null,
                                        tint = if (isCurrentSiteWhitelisted) NeonPink else NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Whitelist $currentHost",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (isCurrentSiteWhitelisted) "Shield bypassed on this domain" else "Toggle if video player fails to load",
                                        color = if (isCurrentSiteWhitelisted) NeonPink else TextSecondary,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }

                            Switch(
                                checked = isCurrentSiteWhitelisted,
                                onCheckedChange = { whitelisted ->
                                    repository.setDomainWhitelisted(currentHost, whitelisted)
                                    onReloadRequested()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ObsidianBlack,
                                    checkedTrackColor = NeonPink
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3-LEVEL SHIELD PROTECTION BREAKDOWN
                Text(
                    "3-LEVEL SHIELD ARCHITECTURE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                ShieldFeatureItem(
                    level = "LEVEL 1",
                    title = "Native Network Interception",
                    desc = "Directly drops requests to ad networks, trackers, and crypto-miners at the socket level.",
                    active = shieldsEnabled && !isCurrentSiteWhitelisted
                )

                ShieldFeatureItem(
                    level = "LEVEL 2",
                    title = "DOM & Popup Destruction",
                    desc = "MutationObserver continuously scrubs ad banners, iframe popups, and forces window.open = null.",
                    active = shieldsEnabled && !isCurrentSiteWhitelisted
                )

                ShieldFeatureItem(
                    level = "LEVEL 3",
                    title = "Smart Clickjacking Protection",
                    desc = "Neutralizes transparent overlay click-traps while preserving real video play and episode buttons.",
                    active = shieldsEnabled && !isCurrentSiteWhitelisted
                )

                ShieldFeatureItem(
                    level = "GUARANTEE",
                    title = "Safe Stream Delivery",
                    desc = ".m3u8, .mp4, and video CDNs (MegaCloud, Rabbit, Dood, etc.) are always whitelisted and never blocked.",
                    active = true
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    "PREFERENCES",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 18+ Content Toggle (Simplified, Clean)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertRed.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(AlertRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("18+", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("18+ Content", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Unlock adult streaming sources", color = TextSecondary, fontSize = 10.sp)
                            }
                        }

                        Switch(
                            checked = is18Plus,
                            onCheckedChange = { repository.set18PlusEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBlack,
                                checkedTrackColor = AlertRed
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Desktop Mode Toggle
                SettingToggleRow(
                    icon = Icons.Default.DesktopWindows,
                    title = "Desktop Chrome User-Agent",
                    subtitle = "Bypasses mobile-only player restrictions",
                    checked = desktopMode,
                    tint = NeonCyan,
                    onCheckedChange = {
                        repository.setDesktopMode(it)
                        onReloadRequested()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Unblocker Proxy Gateway
                SettingToggleRow(
                    icon = Icons.Default.VpnKey,
                    title = "Unblocker Gateway",
                    subtitle = "Bypasses ISP blockades & SSL cert filters",
                    checked = unblocker,
                    tint = NeonViolet,
                    onCheckedChange = {
                        repository.setUnblockerEnabled(it)
                        onReloadRequested()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Button(
                    onClick = {
                        onReloadRequested()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Apply & Reload Page", color = ObsidianBlack, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onClearDataRequested()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianCard)
                ) {
                    Text("Clear Shield Cache & Cookies", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        }
    }
}

@Composable
private fun ShieldFeatureItem(
    level: String,
    title: String,
    desc: String,
    active: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ObsidianCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (active) NeonCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (active) NeonCyan.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level,
                    color = if (active) NeonCyan else Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(desc, color = TextSecondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    tint: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(subtitle, color = TextSecondary, fontSize = 10.sp)
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianBlack,
                    checkedTrackColor = tint
                )
            )
        }
    }
}
