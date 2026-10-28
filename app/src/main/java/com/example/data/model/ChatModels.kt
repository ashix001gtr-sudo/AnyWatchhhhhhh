package com.example.data.model

import java.util.UUID

object AdminConstants {
    const val ADMIN_EMAIL = "rm9208946@gmail.com"
    const val ADMIN_NAME = "ADMIN.01"

    fun isAdminEmail(email: String?): Boolean {
        return email?.trim()?.equals(ADMIN_EMAIL, ignoreCase = true) == true
    }

    fun isReservedAdminName(name: String): Boolean {
        val clean = name.trim().lowercase()
        return clean == "admin" || clean == "admin.01" || clean == "adminbot" ||
                clean.startsWith("admin") || clean.contains("adminbot")
    }
}

/**
 * Authenticated User Profile obtained from Google Sign-In / Firebase
 */
data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val isVerified: Boolean = true,
    val joinedAt: Long = System.currentTimeMillis()
)

/**
 * Quoted snippet when replying to an existing community message
 */
data class ReplySnapshot(
    val messageId: String,
    val senderName: String,
    val textSnippet: String
)

/**
 * Real-time community chat message
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val channelId: String = "general",
    val senderUid: String,
    val senderName: String,
    val senderEmail: String? = null,
    val senderPhotoUrl: String? = null,
    val text: String,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val replyTo: ReplySnapshot? = null,
    val isHidden: Boolean = false,
    val isAdmin: Boolean = false
)

/**
 * Community discussion channels
 */
data class ChatChannel(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String
)
