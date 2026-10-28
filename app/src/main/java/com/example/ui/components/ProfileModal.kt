package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.AdminConstants
import com.example.data.model.UserProfile
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Dedicated Glassmorphic Profile Modal with Real User Data,
 * Native Profile Picture Upload, Admin Mode Toggle, and Anti-Impersonation.
 */
@Composable
fun ProfileModal(
    user: UserProfile,
    isAdminModeEnabled: Boolean = true,
    onToggleAdminMode: (Boolean) -> Unit = {},
    onUploadProfilePhoto: (Uri) -> Unit = {},
    isShieldActive: Boolean,
    onToggleShield: () -> Unit,
    is18PlusActive: Boolean,
    onToggle18Plus: () -> Unit,
    isDesktopMode: Boolean,
    onToggleDesktopMode: () -> Unit,
    onOpenDownloads: () -> Unit,
    onWebsiteFixClick: () -> Unit,
    onToggleRotation: () -> Unit,
    onBrowseAll: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onUpdateName: (String) -> Unit = {},
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isAdmin = AdminConstants.isAdminEmail(user.email)
    var show3DotMenu by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newDisplayNameInput by remember(user.displayName) { mutableStateOf(user.displayName) }

    // Native Photo Picker for Profile Picture Upload
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadProfilePhoto(uri)
            Toast.makeText(context, "Uploading profile picture...", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler { onDismiss() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { onDismiss() }
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                if (isAdmin) Color(0xFFFFD700) else NeonCyan.copy(alpha = 0.8f),
                                NeonViolet.copy(alpha = 0.5f),
                                Color.White.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = if (isAdmin) Color(0xFFFFD700) else NeonCyan),
                shape = RoundedCornerShape(24.dp),
                color = ObsidianBlack
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // TOP BAR: Title & 3-Dot Options Menu Button & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AnyWatch Profile",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 3-DOT MENU ICON
                            Box {
                                val dotInteraction = remember { MutableInteractionSource() }
                                val isDotPressed by dotInteraction.collectIsPressedAsState()
                                val dotScale by animateFloatAsState(
                                    targetValue = if (isDotPressed) 0.90f else 1f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "3dot_scale"
                                )
                                IconButton(
                                    onClick = { show3DotMenu = true },
                                    modifier = Modifier
                                        .scale(dotScale)
                                        .size(36.dp)
                                        .background(ObsidianCard, CircleShape)
                                        .testTag("profile_3dot_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Settings Menu",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = show3DotMenu,
                                    onDismissRequest = { show3DotMenu = false },
                                    modifier = Modifier
                                        .background(ObsidianCard)
                                        .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                ) {
                                    // Watch History Button (Moved from main tabs)
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.History, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("Watch History", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        },
                                        onClick = {
                                            show3DotMenu = false
                                            onDismiss()
                                            onOpenHistory()
                                        },
                                        modifier = Modifier.testTag("menu_history_button")
                                    )

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                                    // 18+ Mature Catalog Toggle
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("18+ Mature Content", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(
                                                        if (is18PlusActive) "Mature Catalog: Unlocked" else "Safe Browsing: Active",
                                                        color = TextSecondary,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                Switch(
                                                    checked = is18PlusActive,
                                                    onCheckedChange = { onToggle18Plus() },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = ObsidianBlack,
                                                        checkedTrackColor = AlertRed
                                                    )
                                                )
                                            }
                                        },
                                        onClick = { onToggle18Plus() },
                                        modifier = Modifier.testTag("menu_18plus_toggle")
                                    )

                                    // Desktop Mode Toggle
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Desktop User-Agent", color = Color.White, fontSize = 13.sp)
                                                    Text(
                                                        if (isDesktopMode) "PC Desktop View" else "Mobile View",
                                                        color = TextSecondary,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                Switch(
                                                    checked = isDesktopMode,
                                                    onCheckedChange = { onToggleDesktopMode() },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = ObsidianBlack,
                                                        checkedTrackColor = NeonCyan
                                                    )
                                                )
                                            }
                                        },
                                        onClick = { onToggleDesktopMode() }
                                    )

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                                    // Downloads Manager
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Download, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("Download Manager", color = Color.White, fontSize = 13.sp)
                                            }
                                        },
                                        onClick = {
                                            show3DotMenu = false
                                            onDismiss()
                                            onOpenDownloads()
                                        }
                                    )

                                    // Rotate Screen
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("Rotate Screen", color = Color.White, fontSize = 13.sp)
                                            }
                                        },
                                        onClick = {
                                            show3DotMenu = false
                                            onToggleRotation()
                                        }
                                    )

                                    // Join AnyWatch Telegram
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("📢", fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text("Join Telegram", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("t.me/AnyWatchOfficial", color = TextSecondary, fontSize = 10.sp)
                                                }
                                            }
                                        },
                                        onClick = {
                                            show3DotMenu = false
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/AnyWatchOfficial")))
                                            } catch (_: Exception) {}
                                        }
                                    )

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                                    // Sign Out
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AlertRed, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("Sign Out", color = AlertRed, fontSize = 13.sp)
                                            }
                                        },
                                        onClick = {
                                            show3DotMenu = false
                                            onSignOut()
                                            onDismiss()
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ObsidianCard, CircleShape)
                                    .testTag("close_profile_modal_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // USER IDENTITY HERO CARD WITH PROFILE PICTURE UPLOAD AFFORDANCE
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ObsidianCard.copy(alpha = 0.9f),
                        border = BorderStroke(
                            1.2.dp,
                            if (isAdmin) Color(0xFFFFD700).copy(alpha = 0.7f) else NeonCyan.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User Profile Picture (Clickable to upload photo)
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        if (isAdmin) Brush.sweepGradient(listOf(Color(0xFFFFD700), Color(0xFFFF0000), Color(0xFFFFD700)))
                                        else Brush.sweepGradient(listOf(NeonCyan, NeonViolet, NeonPink, NeonCyan)),
                                        CircleShape
                                    )
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("profile_avatar_upload_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!user.photoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = user.photoUrl,
                                        contentDescription = "Profile Picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.3f), NeonViolet.copy(alpha = 0.3f)))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.displayName.take(2).uppercase(),
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Camera Badge Overlay indicating uploadability
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(ObsidianBlack)
                                        .border(1.dp, NeonCyan, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "Change photo",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val displayName = if (isAdmin) AdminConstants.ADMIN_NAME else user.displayName
                                    Text(
                                        text = displayName,
                                        color = if (isAdmin) Color(0xFFFF0000) else Color.White,
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (isAdmin) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFF0000).copy(alpha = 0.2f),
                                            border = BorderStroke(0.8.dp, Color(0xFFFF0000))
                                        ) {
                                            Text(
                                                text = "ADMIN",
                                                color = Color(0xFFFF0000),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 0.6.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Display Name Edit Button (Locked for Admin)
                                    if (!isAdmin) {
                                        IconButton(
                                            onClick = {
                                                newDisplayNameInput = user.displayName
                                                showEditNameDialog = true
                                            },
                                            modifier = Modifier
                                                .size(26.dp)
                                                .testTag("edit_display_name_pencil_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Display Name",
                                                tint = NeonCyan,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Admin Name Locked",
                                            tint = Color(0xFFFF0000).copy(alpha = 0.8f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = user.email,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                Row(
                                    modifier = Modifier.padding(top = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isAdmin) Color(0xFFFFD700) else NeonCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isAdmin) "Root Administrator" else "Firebase Authenticated",
                                        color = if (isAdmin) Color(0xFFFFD700) else NeonCyan,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // MODULE 1: ADMIN MODE SECTION (Visible ONLY if user is rm9208946@gmail.com)
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E1205),
                            border = BorderStroke(
                                1.4.dp,
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFD700), Color(0xFFFF0000), Color(0xFFFFD700))
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_mode_section_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFD700).copy(alpha = 0.18f))
                                            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = "Admin Crown",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "ADMIN MODE",
                                                color = Color(0xFFFFD700),
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 0.6.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFFF0000).copy(alpha = 0.25f),
                                                border = BorderStroke(0.8.dp, Color(0xFFFF0000))
                                            ) {
                                                Text(
                                                    text = "SPECIAL",
                                                    color = Color(0xFFFF0000),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isAdminModeEnabled) "Active • Delete & hide messages, block users" else "Paused • Standard user experience",
                                            color = if (isAdminModeEnabled) Color(0xFFFFD700).copy(alpha = 0.9f) else TextSecondary,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }

                                Switch(
                                    checked = isAdminModeEnabled,
                                    onCheckedChange = { onToggleAdminMode(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFFFD700),
                                        checkedTrackColor = Color(0xFFB8860B),
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = ObsidianBlack
                                    ),
                                    modifier = Modifier.testTag("admin_mode_toggle_switch")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // UNIFIED ANYWATCH SHIELD TOGGLE CARD
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianCard.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isShieldActive) NeonCyan.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (isShieldActive) NeonCyan else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "AnyWatch Shield 3.0",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isShieldActive) "Active • Zero popups & auto-redirects" else "Paused",
                                        color = if (isShieldActive) NeonCyan else TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Switch(
                                checked = isShieldActive,
                                onCheckedChange = { onToggleShield() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ObsidianBlack,
                                    checkedTrackColor = NeonCyan
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Custom Display Name Edit Dialog with Anti-Impersonation Protection
    if (showEditNameDialog && !isAdmin) {
        val isImpersonatingAdmin = remember(newDisplayNameInput) {
            AdminConstants.isReservedAdminName(newDisplayNameInput)
        }

        Dialog(onDismissRequest = { showEditNameDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianBlack,
                border = BorderStroke(1.5.dp, if (isImpersonatingAdmin) AlertRed else NeonCyan.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("edit_display_name_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Edit Display Name",
                        color = Color.White,
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This name updates your Google/Firebase account and is used in Community Chat.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = newDisplayNameInput,
                        onValueChange = { newDisplayNameInput = it },
                        singleLine = true,
                        isError = isImpersonatingAdmin,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isImpersonatingAdmin) AlertRed else NeonCyan,
                            unfocusedBorderColor = if (isImpersonatingAdmin) AlertRed else Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_name_input_field")
                    )

                    if (isImpersonatingAdmin) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Reserved name. You cannot use 'Admin', 'ADMIN.01', or 'AdminBot'.",
                            color = AlertRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditNameDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                if (newDisplayNameInput.isNotBlank() && !isImpersonatingAdmin) {
                                    onUpdateName(newDisplayNameInput.trim())
                                    showEditNameDialog = false
                                }
                            },
                            enabled = newDisplayNameInput.isNotBlank() && !isImpersonatingAdmin,
                            modifier = Modifier.weight(1f).testTag("save_display_name_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isImpersonatingAdmin) Color.Gray else NeonCyan,
                                contentColor = ObsidianBlack
                            )
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
