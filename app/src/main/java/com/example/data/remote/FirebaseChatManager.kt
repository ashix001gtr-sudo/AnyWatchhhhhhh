package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.AdminConstants
import com.example.data.model.ChatMessage
import com.example.data.model.ReplySnapshot
import com.example.data.model.UserProfile
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Coroutine await extension for Google Play Services / Firebase Tasks
 */
suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            cont.resume(result, onCancellation = null)
        }
        addOnFailureListener { exception ->
            cont.resumeWith(Result.failure(exception))
        }
        addOnCanceledListener {
            cont.cancel()
        }
    }
}

/**
 * Firebase Realtime Backend Integration for AnyWatch PRO
 * Manages Firebase Authentication (Google Sign-In) and
 * Firestore Realtime Sync ('messages' & 'blocked_users' collections).
 */
object FirebaseChatManager {
    private const val TAG = "FirebaseChatManager"

    const val COLLECTION_MESSAGES = "messages"
    const val COLLECTION_BLOCKED_USERS = "blocked_users"

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    var isInitialized: Boolean = true
        private set

    fun init(context: Context) {
        // Firebase is initialized automatically via FirebaseInitProvider / google-services.json
        Log.d(TAG, "FirebaseChatManager ready (Project: anywatch-stream-app)")
    }

    /**
     * Retrieve the currently logged in Firebase User converted to UserProfile
     */
    fun getCurrentUser(): UserProfile? {
        val user = auth.currentUser ?: return null
        val isAdmin = AdminConstants.isAdminEmail(user.email)
        val displayName = if (isAdmin) {
            AdminConstants.ADMIN_NAME
        } else {
            user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "Streamer"
        }
        val photoUrl = user.photoUrl?.toString()

        return UserProfile(
            uid = user.uid,
            displayName = displayName,
            email = user.email ?: "",
            photoUrl = photoUrl,
            isVerified = user.isEmailVerified,
            joinedAt = user.metadata?.creationTimestamp ?: System.currentTimeMillis()
        )
    }

    /**
     * Sign in to Firebase with Google AuthCredential
     */
    suspend fun signInWithCredential(credential: AuthCredential): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                    ?: return@withContext Result.failure(IllegalStateException("FirebaseUser is null after sign in"))

                val isAdmin = AdminConstants.isAdminEmail(firebaseUser.email)
                val displayName = if (isAdmin) {
                    AdminConstants.ADMIN_NAME
                } else {
                    firebaseUser.displayName?.takeIf { it.isNotBlank() }
                        ?: firebaseUser.email?.substringBefore("@")
                        ?: "Streamer"
                }

                val profile = UserProfile(
                    uid = firebaseUser.uid,
                    displayName = displayName,
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    isVerified = firebaseUser.isEmailVerified,
                    joinedAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
                )
                Log.d(TAG, "Firebase Google Sign-In successful for user ${profile.displayName} (${profile.email})")
                Result.success(profile)
            } catch (e: Throwable) {
                Log.e(TAG, "Firebase signInWithCredential failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Sign in to Firebase using Google ID Token string
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<UserProfile> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return signInWithCredential(credential)
    }

    /**
     * Update user display name in Firebase Auth
     */
    suspend fun updateUserName(newName: String): UserProfile? {
        return withContext(Dispatchers.IO) {
            try {
                if (AdminConstants.isReservedAdminName(newName)) {
                    Log.w(TAG, "Anti-impersonation: Rejected admin username attempt: $newName")
                    return@withContext null
                }
                val user = auth.currentUser ?: return@withContext null
                val changeRequest = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName.trim())
                    .build()
                user.updateProfile(changeRequest).await()
                getCurrentUser()
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to update profile name: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Sign out current Firebase user
     */
    fun signOut() {
        try {
            auth.signOut()
            Log.d(TAG, "Firebase signed out successfully")
        } catch (e: Throwable) {
            Log.e(TAG, "Firebase signOut error", e)
        }
    }

    /**
     * Listen in real-time to Firestore 'messages' collection using addSnapshotListener.
     * Uses exact column keys: 'senderId', 'senderName', 'messageText', 'timestamp'
     */
    fun observeMessages(onMessages: (List<ChatMessage>) -> Unit): ListenerRegistration? {
        return try {
            firestore.collection(COLLECTION_MESSAGES)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Messages snapshotListener error: ${error.message}, falling back to unindexed query")
                        // Fallback listener without orderBy in case index is not immediately available
                        firestore.collection(COLLECTION_MESSAGES)
                            .addSnapshotListener { fbSnapshot, fbError ->
                                if (fbSnapshot != null) {
                                    val messages = parseMessagesSnapshot(fbSnapshot)
                                    onMessages(messages.sortedBy { it.timestamp })
                                } else if (fbError != null) {
                                    Log.e(TAG, "Fallback snapshot error: ${fbError.message}")
                                }
                            }
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val messages = parseMessagesSnapshot(snapshot)
                        onMessages(messages)
                    }
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register messages snapshot listener: ${e.message}", e)
            null
        }
    }

    private fun parseMessagesSnapshot(snapshot: com.google.firebase.firestore.QuerySnapshot): List<ChatMessage> {
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000L)
        return snapshot.documents.mapNotNull { doc ->
            try {
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                if (timestamp < sevenDaysAgo) return@mapNotNull null

                // Exact column keys specified: 'senderId', 'senderName', 'messageText', 'timestamp'
                val senderId = doc.getString("senderId")
                    ?: doc.getString("senderID")
                    ?: doc.getString("senderUid")
                    ?: ""
                val senderName = doc.getString("senderName") ?: "Streamer"
                val messageText = doc.getString("messageText")
                    ?: doc.getString("messagetext")
                    ?: doc.getString("text")
                    ?: ""

                val senderEmail = doc.getString("senderEmail")
                val senderPhotoUrl = doc.getString("senderPhotoUrl")
                val imageUrl = doc.getString("imageUrl")
                val isHidden = doc.getBoolean("isHidden") ?: false

                val isAdmin = AdminConstants.isAdminEmail(senderEmail) ||
                        senderName == AdminConstants.ADMIN_NAME ||
                        (doc.getBoolean("isAdmin") == true)

                val replyTo = doc.getString("replyToId")?.let { replyId ->
                    ReplySnapshot(
                        messageId = replyId,
                        senderName = doc.getString("replyToSender") ?: "User",
                        textSnippet = doc.getString("replyToSnippet") ?: ""
                    )
                }

                ChatMessage(
                    id = doc.id,
                    channelId = doc.getString("channelId") ?: "general",
                    senderUid = senderId,
                    senderName = if (isAdmin) AdminConstants.ADMIN_NAME else senderName,
                    senderEmail = senderEmail,
                    senderPhotoUrl = senderPhotoUrl?.takeIf { it.isNotBlank() },
                    text = if (isHidden) "This message was hidden by Admin." else messageText,
                    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
                    timestamp = timestamp,
                    replyTo = replyTo,
                    isHidden = isHidden,
                    isAdmin = isAdmin
                )
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to parse message doc ${doc.id}: ${e.message}")
                null
            }
        }
    }

    /**
     * Send a community message to Firestore 'messages' collection
     * Must write exact column keys: 'senderId', 'senderName', 'messageText', 'timestamp'
     */
    suspend fun sendMessage(message: ChatMessage): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val data = hashMapOf<String, Any>(
                    "senderId" to message.senderUid,
                    "senderName" to message.senderName,
                    "messageText" to message.text,
                    "timestamp" to message.timestamp,
                    "channelId" to message.channelId,
                    "isAdmin" to message.isAdmin,
                    "isHidden" to message.isHidden
                )
                message.senderEmail?.let { data["senderEmail"] = it }
                message.senderPhotoUrl?.let { data["senderPhotoUrl"] = it }
                message.imageUrl?.let { data["imageUrl"] = it }

                if (message.replyTo != null) {
                    data["replyToId"] = message.replyTo.messageId
                    data["replyToSender"] = message.replyTo.senderName
                    data["replyToSnippet"] = message.replyTo.textSnippet
                }

                firestore.collection(COLLECTION_MESSAGES)
                    .document(message.id)
                    .set(data)
                    .await()

                Log.d(TAG, "Successfully sent message ${message.id} to Firestore collection '$COLLECTION_MESSAGES'")
                true
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to send message to Firestore: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Delete message from Firestore 'messages' collection
     */
    suspend fun deleteMessage(messageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection(COLLECTION_MESSAGES)
                    .document(messageId)
                    .delete()
                    .await()
                Log.d(TAG, "Successfully deleted message $messageId from Firestore")
                true
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to delete message $messageId: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Hide message in Firestore
     */
    suspend fun hideMessage(messageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection(COLLECTION_MESSAGES)
                    .document(messageId)
                    .update(
                        mapOf(
                            "messageText" to "This message was hidden by Admin.",
                            "isHidden" to true
                        )
                    )
                    .await()
                Log.d(TAG, "Successfully hid message $messageId")
                true
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to hide message $messageId: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Block a user in Firestore 'blocked_users' collection
     */
    suspend fun blockUser(targetUid: String, adminUid: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val data = hashMapOf<String, Any>(
                    "userId" to targetUid,
                    "blockedBy" to adminUid,
                    "timestamp" to System.currentTimeMillis()
                )
                firestore.collection(COLLECTION_BLOCKED_USERS)
                    .document(targetUid)
                    .set(data)
                    .await()
                Log.d(TAG, "Blocked user $targetUid in Firestore")
                true
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to block user $targetUid: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Unblock a user in Firestore 'blocked_users' collection
     */
    suspend fun unblockUser(targetUid: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection(COLLECTION_BLOCKED_USERS)
                    .document(targetUid)
                    .delete()
                    .await()
                Log.d(TAG, "Unblocked user $targetUid in Firestore")
                true
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to unblock user $targetUid: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Listen in real-time to 'blocked_users' collection
     */
    fun observeBlockedUsers(onBlocked: (Set<String>) -> Unit): ListenerRegistration? {
        return try {
            firestore.collection(COLLECTION_BLOCKED_USERS)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null) {
                        val set = snapshot.documents.mapNotNull { it.getString("userId") ?: it.id }.toSet()
                        onBlocked(set)
                    } else if (error != null) {
                        Log.w(TAG, "observeBlockedUsers error: ${error.message}")
                    }
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Error registering blocked users listener: ${e.message}", e)
            null
        }
    }
}
