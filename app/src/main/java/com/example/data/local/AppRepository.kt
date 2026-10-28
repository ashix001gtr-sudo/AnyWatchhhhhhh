package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AdminConstants
import com.example.data.model.HistoryItem
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class AppRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("anywatch_prefs", Context.MODE_PRIVATE)

    // User Authentication Gatekeeper State
    private val _currentUser = MutableStateFlow<UserProfile?>(loadUserProfile())
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<HistoryItem>>(emptyList())
    val bookmarks: StateFlow<List<HistoryItem>> = _bookmarks.asStateFlow()

    // 18+ Content Toggle (Simple, Premium)
    private val _is18PlusEnabled = MutableStateFlow(prefs.getBoolean(KEY_18_PLUS, false))
    val is18PlusEnabled: StateFlow<Boolean> = _is18PlusEnabled.asStateFlow()

    // AnyWatch Brave-Style Shield
    private val _anywatchShieldsEnabled = MutableStateFlow(prefs.getBoolean(KEY_ANYWATCH_SHIELDS, true))
    val anywatchShieldsEnabled: StateFlow<Boolean> = _anywatchShieldsEnabled.asStateFlow()

    // Admin Mode State (Toggleable for rm9208946@gmail.com)
    private val _isAdminModeEnabled = MutableStateFlow(prefs.getBoolean(KEY_ADMIN_MODE, true))
    val isAdminModeEnabled: StateFlow<Boolean> = _isAdminModeEnabled.asStateFlow()

    // Blocked Ads & Trackers Dynamic Counter
    private val _blockedAdsCount = MutableStateFlow(prefs.getInt(KEY_BLOCKED_COUNT, 38))
    val blockedAdsCount: StateFlow<Int> = _blockedAdsCount.asStateFlow()

    // Per-Site Whitelist (Set of domains where shield is bypassed)
    private val _whitelistedDomains = MutableStateFlow<Set<String>>(loadWhitelistedDomains())
    val whitelistedDomains: StateFlow<Set<String>> = _whitelistedDomains.asStateFlow()

    private val _desktopMode = MutableStateFlow(prefs.getBoolean(KEY_DESKTOP_MODE, false))
    val desktopMode: StateFlow<Boolean> = _desktopMode.asStateFlow()

    private val _unblockerEnabled = MutableStateFlow(prefs.getBoolean(KEY_UNBLOCKER, false))
    val unblockerEnabled: StateFlow<Boolean> = _unblockerEnabled.asStateFlow()

    private val _hindiDubbedPreference = MutableStateFlow(prefs.getBoolean(KEY_HINDI_DUBBED, false))
    val hindiDubbedPreference: StateFlow<Boolean> = _hindiDubbedPreference.asStateFlow()

    private val _lastVisitedUrl = MutableStateFlow(prefs.getString(KEY_LAST_URL, "https://ani.pm/") ?: "https://ani.pm/")
    val lastVisitedUrl: StateFlow<String> = _lastVisitedUrl.asStateFlow()

    init {
        loadHistory()
        loadBookmarks()
    }

    fun incrementBlockedAdsCount() {
        val next = _blockedAdsCount.value + 1
        _blockedAdsCount.value = next
        prefs.edit().putInt(KEY_BLOCKED_COUNT, next).apply()
    }

    fun incrementBlockedCount() = incrementBlockedAdsCount()

    fun resetBlockedCount() {
        _blockedAdsCount.value = 0
        prefs.edit().putInt(KEY_BLOCKED_COUNT, 0).apply()
    }

    fun setAnywatchShieldsEnabled(enabled: Boolean) {
        _anywatchShieldsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ANYWATCH_SHIELDS, enabled).apply()
    }

    fun setAdminModeEnabled(enabled: Boolean) {
        _isAdminModeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ADMIN_MODE, enabled).apply()
    }

    fun set18PlusEnabled(enabled: Boolean) {
        _is18PlusEnabled.value = enabled
        prefs.edit().putBoolean(KEY_18_PLUS, enabled).apply()
    }

    fun isDomainWhitelisted(domain: String): Boolean {
        if (domain.isBlank()) return false
        val cleanDomain = domain.lowercase().removePrefix("www.")
        val base = com.example.webview.ShieldDomainManager.extractBaseDomain(cleanDomain)
        return _whitelistedDomains.value.any { whitelisted ->
            val cleanW = whitelisted.lowercase().removePrefix("www.")
            cleanDomain == cleanW || cleanDomain.endsWith(".$cleanW") || cleanW.endsWith(".$cleanDomain") ||
                (base.isNotBlank() && (base == cleanW || base == com.example.webview.ShieldDomainManager.extractBaseDomain(cleanW)))
        }
    }

    fun setDomainWhitelisted(domain: String, whitelisted: Boolean) {
        if (domain.isBlank()) return
        val cleanDomain = domain.lowercase().removePrefix("www.")
        val base = com.example.webview.ShieldDomainManager.extractBaseDomain(cleanDomain)
        val current = _whitelistedDomains.value.toMutableSet()
        if (whitelisted) {
            current.add(cleanDomain)
            if (base.isNotBlank()) current.add(base)
        } else {
            current.remove(cleanDomain)
            if (base.isNotBlank()) current.remove(base)
        }
        _whitelistedDomains.value = current
        saveWhitelistedDomains(current)
    }

    private fun loadWhitelistedDomains(): Set<String> {
        return prefs.getStringSet(KEY_WHITELIST_DOMAINS, emptySet())?.toSet() ?: emptySet()
    }

    private fun saveWhitelistedDomains(set: Set<String>) {
        prefs.edit().putStringSet(KEY_WHITELIST_DOMAINS, set).apply()
    }

    fun setDesktopMode(enabled: Boolean) {
        _desktopMode.value = enabled
        prefs.edit().putBoolean(KEY_DESKTOP_MODE, enabled).apply()
    }

    fun setUnblockerEnabled(enabled: Boolean) {
        _unblockerEnabled.value = enabled
        prefs.edit().putBoolean(KEY_UNBLOCKER, enabled).apply()
    }

    fun setHindiDubbedPreference(enabled: Boolean) {
        _hindiDubbedPreference.value = enabled
        prefs.edit().putBoolean(KEY_HINDI_DUBBED, enabled).apply()
    }

    fun setLastVisitedUrl(url: String) {
        _lastVisitedUrl.value = url
        prefs.edit().putString(KEY_LAST_URL, url).apply()
    }

    fun addHistory(title: String, url: String) {
        if (url.isBlank() || url == "about:blank") return
        val current = _history.value.toMutableList()
        current.removeAll { it.url.equals(url, ignoreCase = true) }
        current.add(0, HistoryItem(title = title.ifBlank { url }, url = url, timestamp = System.currentTimeMillis()))
        if (current.size > 100) {
            current.subList(100, current.size).clear()
        }
        _history.value = current
        saveHistory()
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun toggleBookmark(title: String, url: String): Boolean {
        val current = _bookmarks.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.url.equals(url, ignoreCase = true) }
        val isNowBookmarked: Boolean
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
            isNowBookmarked = false
        } else {
            current.add(0, HistoryItem(title = title.ifBlank { url }, url = url, timestamp = System.currentTimeMillis(), isBookmark = true))
            isNowBookmarked = true
        }
        _bookmarks.value = current
        saveBookmarks()
        return isNowBookmarked
    }

    fun removeBookmark(url: String) {
        val current = _bookmarks.value.toMutableList()
        current.removeAll { it.url.equals(url, ignoreCase = true) }
        _bookmarks.value = current
        saveBookmarks()
    }

    fun isBookmarked(url: String): Boolean {
        return _bookmarks.value.any { it.url.equals(url, ignoreCase = true) }
    }

    private fun saveHistory() {
        val array = JSONArray()
        _history.value.take(100).forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("url", it.url)
            obj.put("timestamp", it.timestamp)
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    private fun loadHistory() {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<HistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HistoryItem(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        title = obj.optString("title"),
                        url = obj.optString("url"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            _history.value = list
        } catch (_: Exception) {}
    }

    private fun saveBookmarks() {
        val array = JSONArray()
        _bookmarks.value.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("url", it.url)
            obj.put("timestamp", it.timestamp)
            array.put(obj)
        }
        prefs.edit().putString(KEY_BOOKMARKS, array.toString()).apply()
    }

    private fun loadBookmarks() {
        val raw = prefs.getString(KEY_BOOKMARKS, null) ?: return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<HistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HistoryItem(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        title = obj.optString("title"),
                        url = obj.optString("url"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isBookmark = true
                    )
                )
            }
            _bookmarks.value = list
        } catch (_: Exception) {}
    }

    fun saveUserProfile(profile: UserProfile) {
        val sanitizedProfile = if (AdminConstants.isAdminEmail(profile.email)) {
            profile.copy(displayName = AdminConstants.ADMIN_NAME)
        } else {
            profile
        }
        _currentUser.value = sanitizedProfile
        try {
            val obj = JSONObject().apply {
                put("uid", sanitizedProfile.uid)
                put("displayName", sanitizedProfile.displayName)
                put("email", sanitizedProfile.email)
                put("photoUrl", sanitizedProfile.photoUrl ?: "")
                put("isVerified", sanitizedProfile.isVerified)
                put("joinedAt", sanitizedProfile.joinedAt)
            }
            prefs.edit().putString(KEY_USER_PROFILE, obj.toString()).apply()
        } catch (_: Exception) {}
    }

    fun updateDisplayName(newName: String): Boolean {
        val current = _currentUser.value ?: return false
        if (AdminConstants.isAdminEmail(current.email)) {
            return false // Admin name is locked to ADMIN.01
        }
        if (AdminConstants.isReservedAdminName(newName)) {
            return false // Anti-impersonation protection
        }
        val updated = current.copy(displayName = newName.trim())
        saveUserProfile(updated)
        return true
    }

    fun updateProfilePhoto(newPhotoUrl: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(photoUrl = newPhotoUrl)
        saveUserProfile(updated)
    }

    fun signOut() {
        _currentUser.value = null
        prefs.edit().remove(KEY_USER_PROFILE).apply()
    }

    private fun loadUserProfile(): UserProfile? {
        val raw = prefs.getString(KEY_USER_PROFILE, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            val email = obj.getString("email")
            val rawName = obj.getString("displayName")
            val displayName = if (AdminConstants.isAdminEmail(email)) {
                AdminConstants.ADMIN_NAME
            } else {
                rawName
            }
            UserProfile(
                uid = obj.getString("uid"),
                displayName = displayName,
                email = email,
                photoUrl = obj.optString("photoUrl").takeIf { it.isNotBlank() },
                isVerified = obj.optBoolean("isVerified", true),
                joinedAt = obj.optLong("joinedAt", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val KEY_USER_PROFILE = "user_profile_json"
        private const val KEY_HISTORY = "history_json"
        private const val KEY_BOOKMARKS = "bookmarks_json"
        private const val KEY_18_PLUS = "setting_18_plus"
        private const val KEY_DESKTOP_MODE = "setting_desktop_mode"
        private const val KEY_UNBLOCKER = "setting_unblocker"
        private const val KEY_HINDI_DUBBED = "setting_hindi_dubbed"
        private const val KEY_LAST_URL = "last_visited_url"
        private const val KEY_ANYWATCH_SHIELDS = "setting_anywatch_shields"
        private const val KEY_BLOCKED_COUNT = "setting_blocked_ads_count"
        private const val KEY_WHITELIST_DOMAINS = "setting_whitelisted_domains"
        private const val KEY_ADMIN_MODE = "setting_admin_mode"
    }
}
