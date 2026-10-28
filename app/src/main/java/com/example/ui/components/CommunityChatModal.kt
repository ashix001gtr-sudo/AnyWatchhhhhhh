package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.local.CommunityChatRepository
import com.example.data.model.AdminConstants
import com.example.data.model.ChatMessage
import com.example.data.model.ReplySnapshot
import com.example.data.model.UserProfile
import com.example.ui.theme.AlertRed
import com.example.ui.theme.UnblockPink
import com.example.ui.theme.AdminGlowBrush
import com.example.ui.theme.CyanToBlueBrush
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Real-Time Community Chat Modal / Screen
 * Features:
 * - Single "General Hub" clean UI
 * - Live dynamic online streamer counter (e.g. 142 Online)
 * - Strict Admin moderation (Delete any message, Hide message, Block user)
 * - Anti-impersonation and hardcoded ADMIN.01 in Red with Admin badge
 * - Mini-profile view showing Google photo, name, and Firebase email on avatar click
 * - 5-photo limit for normal users, bypassed (Infinity) for Admin
 * - AdminBot first message welcome
 */
@Composable
fun CommunityChatModal(
    chatRepository: CommunityChatRepository,
    currentUser: UserProfile,
    isAdminModeEnabled: Boolean = true,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val isUserAdmin = AdminConstants.isAdminEmail(currentUser.email)
    val isEffectiveAdmin = isUserAdmin && isAdminModeEnabled

    val allMessages by chatRepository.messages.collectAsState()
    val activeChannel by chatRepository.activeChannel.collectAsState()
    val channelMessages = remember(allMessages, activeChannel.id) {
        chatRepository.getChannelMessages(activeChannel.id)
    }

    val onlineUsersCount by chatRepository.onlineUsersCount.collectAsState()
    val blockedUsers by chatRepository.blockedUsers.collectAsState()
    val isBlocked = blockedUsers.contains(currentUser.uid)

    var textInput by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var activeReplyTo by remember { mutableStateOf<ReplySnapshot?>(null) }

    // Dialog state for long-press message actions
    var messageForAction by remember { mutableStateOf<ChatMessage?>(null) }

    // Dialog state for avatar click mini-profile
    var miniProfileMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val listState = rememberLazyListState()

    // CLEANUP: Lifecycle management for Firebase Firestore Realtime subscription
    DisposableEffect(Unit) {
        chatRepository.startRealtimeSync()
        onDispose {
            chatRepository.cleanupRealtimeSubscription()
        }
    }

    LaunchedEffect(channelMessages.size) {
        if (channelMessages.isNotEmpty()) {
            listState.animateScrollToItem(channelMessages.size - 1)
        }
    }

    // Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val canSend = chatRepository.canSendImage(currentUser.uid, isEffectiveAdmin)
            if (canSend) {
                selectedImageUri = uri
            } else {
                Toast.makeText(context, "Limit reached: You can only send 5 photos.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("community_chat_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR: CLEAN GENERAL HUB + REAL-TIME ONLINE STREAMER COUNT + CLOSE BUTTON
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ObsidianCard.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.2f), NeonViolet.copy(alpha = 0.2f))))
                                .border(1.2.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🌐", fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "General Hub",
                                    color = Color.White,
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.3.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = NeonCyan.copy(alpha = 0.15f),
                                    border = BorderStroke(0.8.dp, NeonCyan.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "COMMUNITY",
                                        color = NeonCyan,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            // Real-time online count next to pulsing green dot
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E676))
                                        .shadow(4.dp, CircleShape, ambientColor = Color(0xFF00E676))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "$onlineUsersCount Online",
                                    color = Color(0xFF00E676),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isEffectiveAdmin) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ADMIN ACTIVE",
                                        color = Color(0xFFFF5252),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(ObsidianBlack, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                            .testTag("chat_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Chat",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // CHAT MESSAGES LIST
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (channelMessages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Welcome to General Hub!",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Be the first to say hi to online streamers 👋",
                                color = TextSecondary,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }

                items(channelMessages, key = { it.id }) { message ->
                    ChatMessageItem(
                        message = message,
                        currentUserId = currentUser.uid,
                        onAvatarClick = { miniProfileMessage = message },
                        onLongClick = { messageForAction = message },
                        onReplyClick = {
                            activeReplyTo = ReplySnapshot(
                                messageId = message.id,
                                senderName = message.senderName,
                                textSnippet = if (message.isHidden) "This message was hidden by Admin." else message.text.take(60)
                            )
                        }
                    )
                }
            }

            // ACTIVE REPLY PREVIEW BANNER
            AnimatedVisibility(
                visible = activeReplyTo != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                if (activeReplyTo != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        color = ObsidianCard,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Replying to ${activeReplyTo?.senderName}",
                                        color = NeonCyan,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = activeReplyTo?.textSnippet ?: "",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { activeReplyTo = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel reply", tint = TextSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // ATTACHED IMAGE PREVIEW
            AnimatedVisibility(
                visible = selectedImageUri != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (selectedImageUri != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Attached photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { selectedImageUri = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = AlertRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // BOTTOM CHAT INPUT BAR (Disabled if user is blocked)
            if (isBlocked) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = AlertRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.2.dp, AlertRed.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = AlertRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "You are blocked by Admin.",
                            color = AlertRed,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ObsidianCard.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Image attachment button (Checked against 5-photo limit or Admin bypass)
                        val canSendImage = chatRepository.canSendImage(currentUser.uid, isEffectiveAdmin)
                        val sentImageCount = chatRepository.getSentImageCount(currentUser.uid)

                        IconButton(
                            onClick = {
                                if (canSendImage) {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } else {
                                    Toast.makeText(context, "Limit reached: You can only send 5 photos.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (canSendImage) ObsidianBlack else Color.White.copy(alpha = 0.04f))
                                .border(1.dp, if (canSendImage) NeonCyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f), CircleShape)
                                .testTag("chat_photo_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = if (canSendImage) "Add Photo" else "Photo limit reached (5)",
                                tint = if (canSendImage) NeonCyan else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Text Field
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = {
                                Text(
                                    text = if (isEffectiveAdmin) "Chat as ADMIN.01..." else "Message in General Hub...",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isEffectiveAdmin) Color(0xFFFF5252) else NeonCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_text_input_field")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Send Button
                        val canSend = textInput.isNotBlank() || selectedImageUri != null
                        IconButton(
                            onClick = {
                                if (canSend) {
                                    chatRepository.sendMessage(
                                        sender = currentUser,
                                        channelId = activeChannel.id,
                                        text = textInput,
                                        imageUri = selectedImageUri,
                                        replyTo = activeReplyTo,
                                        isAdmin = isEffectiveAdmin
                                    )
                                    textInput = ""
                                    selectedImageUri = null
                                    activeReplyTo = null
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (canSend) (if (isEffectiveAdmin) Color(0xFFFF5252) else NeonCyan) else Color.White.copy(alpha = 0.05f))
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (canSend) ObsidianBlack else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // MODULE 2.1 & 2.4: LONG PRESS ACTION MENU (Delete Message & Hide Message)
        if (messageForAction != null) {
            val msg = messageForAction!!
            val isAuthor = msg.senderUid == currentUser.uid
            val canDelete = isAuthor || isEffectiveAdmin
            val canHide = isEffectiveAdmin && !msg.isHidden

            Dialog(onDismissRequest = { messageForAction = null }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianBlack,
                    border = BorderStroke(1.2.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("chat_long_press_action_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Message Options",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "From: ${msg.senderName}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reply Option
                        Button(
                            onClick = {
                                activeReplyTo = ReplySnapshot(
                                    messageId = msg.id,
                                    senderName = msg.senderName,
                                    textSnippet = if (msg.isHidden) "This message was hidden by Admin." else msg.text.take(60)
                                )
                                messageForAction = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianCard),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reply to Message", color = Color.White)
                            }
                        }

                        // ADMIN ONLY: Hide Message Option (if contains bad words)
                        if (canHide) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    chatRepository.hideMessage(msg.id, currentUser.uid)
                                    Toast.makeText(context, "Message hidden by Admin", Toast.LENGTH_SHORT).show()
                                    messageForAction = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF332000)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFB300)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_hide_message_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Hide Message (Admin)", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Author OR Admin: Delete Message Option
                        if (canDelete) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    chatRepository.deleteMessage(msg.id, currentUser.uid, isEffectiveAdmin)
                                    Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                                    messageForAction = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AlertRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("delete_message_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEffectiveAdmin && !isAuthor) "Delete Message (Admin)" else "Delete Message",
                                        color = AlertRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { messageForAction = null },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text("Dismiss", color = TextSecondary)
                        }
                    }
                }
            }
        }

        // MODULE 2.2 & 2.3: USER MINI-PROFILE MODAL (Avatar click -> View Email & Block User)
        if (miniProfileMessage != null) {
            val msg = miniProfileMessage!!
            val isTargetAdmin = AdminConstants.isAdminEmail(msg.senderEmail) || msg.senderName == AdminConstants.ADMIN_NAME
            val isTargetBlocked = blockedUsers.contains(msg.senderUid)
            val isViewerAdmin = AdminConstants.isAdminEmail(currentUser.email)

            Dialog(onDismissRequest = { miniProfileMessage = null }) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ObsidianBlack,
                    border = BorderStroke(
                        1.5.dp,
                        if (isTargetAdmin) Color(0xFFFFD700) else NeonCyan.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("user_mini_profile_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    if (isTargetAdmin) Color(0xFFFF0000) else NeonCyan,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!msg.senderPhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = msg.senderPhotoUrl,
                                    contentDescription = "User Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(ObsidianCard),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = msg.senderName.take(2).uppercase(),
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // User Name
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = msg.senderName,
                                color = if (isTargetAdmin) Color(0xFFFF0000) else Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (isTargetAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFF0000).copy(alpha = 0.2f),
                                    border = BorderStroke(0.8.dp, Color(0xFFFF0000))
                                ) {
                                    Text("ADMIN", color = Color(0xFFFF0000), fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // FIX EMAIL PRIVACY: Strictly visible ONLY to Admin (rm9208946@gmail.com)
                        if (isViewerAdmin) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ObsidianCard,
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Google Account Email (Admin Only)", color = TextSecondary, fontSize = 10.sp)
                                        Text(
                                            text = msg.senderEmail?.ifBlank { "No email recorded" } ?: "No email recorded",
                                            color = Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ObsidianCard.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Email Hidden for Privacy",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // ADMIN MODERATION ACTION: BLOCK / UNBLOCK USER BUTTON
                        if (isEffectiveAdmin && !isTargetAdmin && msg.senderUid != currentUser.uid) {
                            Spacer(modifier = Modifier.height(14.dp))
                            if (isTargetBlocked) {
                                Button(
                                    onClick = {
                                        chatRepository.unblockUser(msg.senderUid)
                                        Toast.makeText(context, "${msg.senderName} has been unblocked by Admin.", Toast.LENGTH_SHORT).show()
                                        miniProfileMessage = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = UnblockPink),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_unblock_user_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Unblock User",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        chatRepository.blockUser(msg.senderUid, currentUser.uid)
                                        Toast.makeText(context, "${msg.senderName} has been blocked by Admin.", Toast.LENGTH_SHORT).show()
                                        miniProfileMessage = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_block_user_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Block, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Block User (Admin)",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { miniProfileMessage = null },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single Chat Bubble Item with long-press support and Admin / AdminBot badges
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    currentUserId: String,
    onAvatarClick: () -> Unit,
    onLongClick: () -> Unit,
    onReplyClick: () -> Unit
) {
    val isMe = message.senderUid == currentUserId
    val isAdmin = message.isAdmin || AdminConstants.isAdminEmail(message.senderEmail) || message.senderName == AdminConstants.ADMIN_NAME
    val isBot = message.senderName == "AdminBot"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* normal click */ },
                onLongClick = onLongClick
            )
            .padding(vertical = 3.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            // User Avatar (Clickable to view mini-profile)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(
                        1.2.dp,
                        when {
                            isAdmin -> Color(0xFFFF0000)
                            isBot -> Color(0xFFFFD700)
                            else -> NeonCyan.copy(alpha = 0.6f)
                        },
                        CircleShape
                    )
                    .clickable { onAvatarClick() }
                    .testTag("avatar_${message.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (isBot) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2A1C00)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = "Bot", tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                    }
                } else if (!message.senderPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = message.senderPhotoUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = message.senderName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
        }

        // Chat Bubble
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            // Header Row: Sender Name + Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Text(
                    text = if (isAdmin) AdminConstants.ADMIN_NAME else message.senderName,
                    color = when {
                        isAdmin -> Color(0xFFFF0000)
                        isBot -> Color(0xFFFFD700)
                        isMe -> NeonCyan
                        else -> Color.White.copy(alpha = 0.9f)
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )

                if (isAdmin) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color(0xFFFF0000).copy(alpha = 0.2f),
                        border = BorderStroke(0.6.dp, Color(0xFFFF0000))
                    ) {
                        Text(
                            text = "ADMIN",
                            color = Color(0xFFFF0000),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                        )
                    }
                } else if (isBot) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        border = BorderStroke(0.6.dp, Color(0xFFFFD700))
                    ) {
                        Text(
                            text = "BOT",
                            color = Color(0xFFFFD700),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = CommunityChatRepository.formatTimestamp(message.timestamp),
                    color = TextSecondary,
                    fontSize = 9.5.sp
                )
            }

            // Quoted Reply Preview
            if (message.replyTo != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.4f),
                    border = BorderStroke(0.8.dp, NeonCyan.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            text = message.replyTo.senderName,
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = message.replyTo.textSnippet,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            val bubbleShape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            )

            // Message Bubble Card - High-End Cyberpunk Glassmorphism
            Surface(
                shape = bubbleShape,
                color = when {
                    message.isHidden -> ObsidianCard.copy(alpha = 0.5f)
                    isAdmin -> Color(0x353D0A14)
                    isBot -> Color(0xFF241C0C)
                    isMe -> Color(0x3500F5D4)
                    else -> ObsidianCard.copy(alpha = 0.95f)
                },
                border = when {
                    message.isHidden -> BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    isAdmin -> BorderStroke(1.5.dp, AdminGlowBrush)
                    isBot -> BorderStroke(1.2.dp, Color(0xFFFFD700).copy(alpha = 0.7f))
                    isMe -> BorderStroke(1.2.dp, CyanToBlueBrush)
                    else -> BorderStroke(1.dp, Color(0x40A855F7))
                },
                modifier = Modifier.shadow(
                    elevation = if (isAdmin) 8.dp else if (isMe) 5.dp else 2.dp,
                    shape = bubbleShape,
                    ambientColor = if (isAdmin) Color(0xFFFF0055) else if (isMe) NeonCyan else Color.Transparent,
                    spotColor = if (isAdmin) Color(0xFFFF3366) else if (isMe) NeonCyan else Color.Transparent
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Attached Image if available
                    if (!message.imageUrl.isNullOrBlank() && !message.isHidden) {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Message image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .padding(bottom = 6.dp)
                        )
                    }

                    if (message.isHidden) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "This message was hidden by Admin.",
                                color = TextSecondary,
                                fontStyle = FontStyle.Italic,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Text(
                            text = message.text,
                            color = Color.White,
                            fontSize = 13.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
