package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.example.data.model.AdminConstants
import com.example.data.model.ChatChannel
import com.example.data.model.ChatMessage
import com.example.data.model.ReplySnapshot
import com.example.data.model.UserProfile
import com.example.data.remote.FirebaseChatManager
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Community Chat Repository backed by Firebase Firestore Realtime Sync
 */
class CommunityChatRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("anywatch_firebase_chat", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    val channels: List<ChatChannel> = listOf(
        ChatChannel("general", "General Hub", "Global community chat for all streamers", "🌐")
    )

    private val _activeChannel = MutableStateFlow(channels.first())
    val activeChannel: StateFlow<ChatChannel> = _activeChannel.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _onlineUsersCount = MutableStateFlow(142)
    val onlineUsersCount: StateFlow<Int> = _onlineUsersCount.asStateFlow()

    private val _blockedUsers = MutableStateFlow<Set<String>>(loadBlockedUsers())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers.asStateFlow()

    private var messagesListener: ListenerRegistration? = null
    private var blockedUsersListener: ListenerRegistration? = null

    init {
        loadLocalMessages()
        startRealtimeSync()
        startOnlineUsersHeartbeat()
    }

    fun startRealtimeSync() {
        cleanupRealtimeSubscription()

        // 1. Listen in real-time to Firestore messages collection
        try {
            messagesListener = FirebaseChatManager.observeMessages { remoteMessages ->
                if (remoteMessages.isNotEmpty()) {
                    val currentMap = _messages.value.associateBy { it.id }.toMutableMap()
                    remoteMessages.forEach { remote ->
                        currentMap[remote.id] = remote
                    }
                    _messages.value = currentMap.values.sortedBy { it.timestamp }
                    saveLocalMessages()
                }
            }
        } catch (e: Throwable) {
            Log.w("ChatRepo", "Error attaching messagesListener: ${e.message}")
        }

        // 2. Listen in real-time to blocked users in Firestore
        try {
            blockedUsersListener = FirebaseChatManager.observeBlockedUsers { remoteBlocked ->
                val merged = _blockedUsers.value.toMutableSet().apply { addAll(remoteBlocked) }
                _blockedUsers.value = merged
                saveBlockedUsers(merged)
            }
        } catch (e: Throwable) {
            Log.w("ChatRepo", "Error attaching blockedUsersListener: ${e.message}")
        }
    }

    fun cleanupRealtimeSubscription() {
        try {
            messagesListener?.remove()
            messagesListener = null
            blockedUsersListener?.remove()
            blockedUsersListener = null
        } catch (e: Throwable) {
            Log.w("ChatRepo", "Error during realtime cleanup: ${e.message}")
        }
    }

    private fun startOnlineUsersHeartbeat() {
        scope.launch {
            while (isActive) {
                delay(30_000)
                val jitter = (-3..4).random()
                _onlineUsersCount.value = (142 + jitter).coerceIn(120, 200)
            }
        }
    }

    fun setActiveChannel(channel: ChatChannel) {
        _activeChannel.value = channel
    }

    fun getChannelMessages(channelId: String): List<ChatMessage> {
        return _messages.value.filter { it.channelId == channelId }
    }

    fun isUserBlocked(uid: String): Boolean {
        return _blockedUsers.value.contains(uid)
    }

    fun blockUser(targetUid: String, adminUid: String) {
        val current = _blockedUsers.value.toMutableSet()
        current.add(targetUid)
        _blockedUsers.value = current
        saveBlockedUsers(current)
        scope.launch {
            FirebaseChatManager.blockUser(targetUid, adminUid)
        }
    }

    fun unblockUser(targetUid: String) {
        val current = _blockedUsers.value.toMutableSet()
        current.remove(targetUid)
        _blockedUsers.value = current
        saveBlockedUsers(current)
        scope.launch {
            FirebaseChatManager.unblockUser(targetUid)
        }
    }

    private fun loadBlockedUsers(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_USERS, emptySet())?.toSet() ?: emptySet()
    }

    private fun saveBlockedUsers(users: Set<String>) {
        prefs.edit().putStringSet(KEY_BLOCKED_USERS, users).apply()
    }

    fun getSentImageCount(userUid: String): Int {
        return prefs.getInt(KEY_IMAGE_COUNT_PREFIX + userUid, 0)
    }

    fun canSendImage(userUid: String, isAdmin: Boolean = false): Boolean {
        if (isAdmin) return true
        return getSentImageCount(userUid) < 5
    }

    fun incrementSentImageCount(userUid: String) {
        val next = getSentImageCount(userUid) + 1
        prefs.edit().putInt(KEY_IMAGE_COUNT_PREFIX + userUid, next).apply()
    }

    fun deleteMessage(messageId: String, senderUid: String, isAdmin: Boolean = false): Boolean {
        val current = _messages.value
        val target = current.find { it.id == messageId } ?: return false
        if (!isAdmin && target.senderUid != senderUid) return false

        _messages.value = current.filterNot { it.id == messageId }
        saveLocalMessages()
        scope.launch {
            FirebaseChatManager.deleteMessage(messageId)
        }
        return true
    }

    fun hideMessage(messageId: String, adminUid: String) {
        val current = _messages.value
        val target = current.find { it.id == messageId } ?: return
        val hiddenMsg = target.copy(
            text = "This message was hidden by Admin.",
            isHidden = true
        )
        _messages.value = current.map { if (it.id == messageId) hiddenMsg else it }
        saveLocalMessages()
        scope.launch {
            FirebaseChatManager.hideMessage(messageId)
        }
    }

    private fun hasUserSentMessageBefore(userUid: String): Boolean {
        return prefs.getBoolean(KEY_SENT_FIRST_MSG_PREFIX + userUid, false)
    }

    private fun markUserSentFirstMessage(userUid: String) {
        prefs.edit().putBoolean(KEY_SENT_FIRST_MSG_PREFIX + userUid, true).apply()
    }

    fun sendMessage(
        sender: UserProfile,
        channelId: String,
        text: String,
        imageUri: Uri? = null,
        replyTo: ReplySnapshot? = null,
        isAdmin: Boolean = false
    ) {
        if (isUserBlocked(sender.uid)) return
        if (text.isBlank() && imageUri == null) return

        if (imageUri != null && !isAdmin) {
            incrementSentImageCount(sender.uid)
        }

        val isFirstMessage = !hasUserSentMessageBefore(sender.uid) && !AdminConstants.isAdminEmail(sender.email)
        if (isFirstMessage) {
            markUserSentFirstMessage(sender.uid)
        }

        val messageId = UUID.randomUUID().toString()
        val effectiveName = if (isAdmin || AdminConstants.isAdminEmail(sender.email)) {
            AdminConstants.ADMIN_NAME
        } else {
            sender.displayName
        }

        val newMessage = ChatMessage(
            id = messageId,
            channelId = channelId,
            senderUid = sender.uid,
            senderName = effectiveName,
            senderEmail = sender.email,
            senderPhotoUrl = sender.photoUrl,
            text = text.trim(),
            imageUrl = imageUri?.toString(),
            timestamp = System.currentTimeMillis(),
            replyTo = replyTo,
            isHidden = false,
            isAdmin = isAdmin || AdminConstants.isAdminEmail(sender.email)
        )

        val updated = _messages.value.toMutableList().apply { add(newMessage) }
        _messages.value = updated
        saveLocalMessages()

        scope.launch {
            FirebaseChatManager.sendMessage(newMessage)

            if (isFirstMessage) {
                delay(600)
                val botWelcome = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    channelId = channelId,
                    senderUid = "adminbot_system",
                    senderName = "AdminBot",
                    senderEmail = "adminbot@anywatch.pro",
                    text = "Welcome to AnyWatch Community, ${sender.displayName}! Please follow the rules.",
                    timestamp = System.currentTimeMillis()
                )
                val withBot = _messages.value.toMutableList().apply { add(botWelcome) }
                _messages.value = withBot
                saveLocalMessages()
                FirebaseChatManager.sendMessage(botWelcome)
            }
        }
    }

    private fun loadLocalMessages() {
        val raw = prefs.getString(KEY_MESSAGES, null)
        if (raw.isNullOrBlank()) {
            _messages.value = emptyList()
            return
        }
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000L)
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                if (timestamp < sevenDaysAgo) continue

                val replyObj = obj.optJSONObject("replyTo")
                val reply = if (replyObj != null) {
                    ReplySnapshot(
                        messageId = replyObj.optString("messageId"),
                        senderName = replyObj.optString("senderName"),
                        textSnippet = replyObj.optString("textSnippet")
                    )
                } else null

                val isHidden = obj.optBoolean("isHidden", false)
                val rawText = obj.optString("text")
                val displayText = if (isHidden) "This message was hidden by Admin." else rawText
                val senderEmail = obj.optString("senderEmail").takeIf { it.isNotBlank() }
                val isAdmin = obj.optBoolean("isAdmin", false) || AdminConstants.isAdminEmail(senderEmail)

                list.add(
                    ChatMessage(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        channelId = obj.optString("channelId", "general"),
                        senderUid = obj.optString("senderUid"),
                        senderName = if (isAdmin) AdminConstants.ADMIN_NAME else obj.optString("senderName"),
                        senderEmail = senderEmail,
                        senderPhotoUrl = obj.optString("senderPhotoUrl").takeIf { it.isNotBlank() },
                        text = displayText,
                        imageUrl = obj.optString("imageUrl").takeIf { it.isNotBlank() },
                        timestamp = timestamp,
                        replyTo = reply,
                        isHidden = isHidden,
                        isAdmin = isAdmin
                    )
                )
            }
            _messages.value = list
        } catch (_: Exception) {
            _messages.value = emptyList()
        }
    }

    private fun saveLocalMessages() {
        try {
            val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000L)
            val array = JSONArray()
            _messages.value.filter { it.timestamp >= sevenDaysAgo }.takeLast(200).forEach { msg ->
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("channelId", msg.channelId)
                    put("senderUid", msg.senderUid)
                    put("senderName", msg.senderName)
                    put("senderEmail", msg.senderEmail ?: "")
                    put("senderPhotoUrl", msg.senderPhotoUrl ?: "")
                    put("text", msg.text)
                    put("imageUrl", msg.imageUrl ?: "")
                    put("timestamp", msg.timestamp)
                    put("isHidden", msg.isHidden)
                    put("isAdmin", msg.isAdmin)
                    if (msg.replyTo != null) {
                        val reply = JSONObject().apply {
                            put("messageId", msg.replyTo.messageId)
                            put("senderName", msg.replyTo.senderName)
                            put("textSnippet", msg.replyTo.textSnippet)
                        }
                        put("replyTo", reply)
                    }
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_MESSAGES, array.toString()).apply()
        } catch (_: Exception) {}
    }

    companion object {
        private const val KEY_MESSAGES = "firebase_community_messages"
        private const val KEY_IMAGE_COUNT_PREFIX = "sent_image_count_"
        private const val KEY_SENT_FIRST_MSG_PREFIX = "sent_first_msg_"
        private const val KEY_BLOCKED_USERS = "chat_blocked_users"

        fun formatTimestamp(timestamp: Long): String {
            val diff = System.currentTimeMillis() - timestamp
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "${diff / 3600_000}h ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}
