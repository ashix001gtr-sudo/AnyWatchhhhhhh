package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.data.local.AppRepository
import com.example.data.local.CommunityChatRepository
import com.example.data.model.UserProfile
import com.example.download.SmartDownloadManager
import com.example.ui.components.AnyWatchShieldsModal
import com.example.ui.components.CommunityChatModal
import com.example.ui.components.DirectoryModal
import com.example.ui.components.DownloadManagerModal
import com.example.ui.components.FixWebsiteModal
import com.example.ui.components.FloatingDownloadWidget
import com.example.ui.components.GlassmorphismHeader
import com.example.ui.components.GlobalSearchModal
import com.example.ui.components.GoogleLoginScreen
import com.example.ui.components.HistoryBookmarksModal
import com.example.ui.components.OfflineGuardOverlay
import com.example.ui.components.ProfileModal
import com.example.data.remote.FirebaseChatManager
import com.example.ui.components.SplashScreen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AnyWatchTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.webview.PopupShieldClient
import com.example.webview.ShieldDomainManager
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var appRepository: AppRepository
    private lateinit var downloadManager: SmartDownloadManager
    private lateinit var chatRepository: CommunityChatRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            FirebaseChatManager.init(applicationContext)
            appRepository = AppRepository(applicationContext)
            downloadManager = SmartDownloadManager(applicationContext)
            chatRepository = CommunityChatRepository(applicationContext)
            // Trigger asynchronous ad-blocker domain list pre-warming in background
            ShieldDomainManager.loadDomainListsAsync()
        } catch (e: Throwable) {
            Log.e("MainActivity", "Error during onCreate initialization", e)
        }

        setContent {
            AnyWatchTheme {
                AnyWatchApp(
                    activity = this,
                    repository = appRepository,
                    downloadManager = downloadManager,
                    chatRepository = chatRepository
                )
            }
        }
    }
}

/**
 * Open downloaded video using Android System Default Player
 */
fun playDownloadedVideo(context: Context, file: File?) {
    if (file == null || !file.exists()) return
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(file), "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AnyWatchApp(
    activity: ComponentActivity,
    repository: AppRepository,
    downloadManager: SmartDownloadManager,
    chatRepository: CommunityChatRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isBooting by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("Anime") }
    var currentUrl by remember { mutableStateOf(repository.lastVisitedUrl.value) }
    var webProgress by remember { mutableIntStateOf(0) }
    var isWebLoading by remember { mutableStateOf(false) }

    // Error Boundary States
    var webPageError by remember { mutableStateOf<Pair<String, String>?>(null) } // description, url
    var webViewFatalInitError by remember { mutableStateOf<String?>(null) }

    // Active Modals
    var showProfileModal by remember { mutableStateOf(false) }
    var showDownloadsModal by remember { mutableStateOf(false) }
    var showFixModal by remember { mutableStateOf(false) }
    var showHistoryModal by remember { mutableStateOf(false) }
    var showDirectoryModal by remember { mutableStateOf(false) }
    var showShieldsModal by remember { mutableStateOf(false) }
    var showChatModal by remember { mutableStateOf(false) }
    var showSearchModal by remember { mutableStateOf(false) }

    // Current User Profile State
    val currentUser by repository.currentUser.collectAsState()

    // Detected stream pill for 1-tap download
    var detectedMediaUrl by remember { mutableStateOf<String?>(null) }
    var detectedMediaTitle by remember { mutableStateOf<String?>(null) }

    // Offline Guard State
    var isOffline by remember { mutableStateOf(false) }

    // Screen Orientation toggle state
    var isPermanentRotate by remember { mutableStateOf(false) }

    // WebView reference
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var customViewContainer by remember { mutableStateOf<FrameLayout?>(null) }
    var isHtml5Fullscreen by remember { mutableStateOf(false) }

    // App state flows
    val anywatchShieldsEnabled by repository.anywatchShieldsEnabled.collectAsState()
    val isAdminModeEnabled by repository.isAdminModeEnabled.collectAsState()
    val is18Plus by repository.is18PlusEnabled.collectAsState()
    val isDesktopMode by repository.desktopMode.collectAsState()
    val isHindiDubbed by repository.hindiDubbedPreference.collectAsState()

    // Safe Network connectivity listener
    DisposableEffect(Unit) {
        var networkCallback: ConnectivityManager.NetworkCallback? = null
        val connectivityManager = try {
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        } catch (_: Throwable) {
            null
        }

        try {
            if (connectivityManager != null) {
                networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        scope.launch {
                            if (isOffline) {
                                isOffline = false
                                webPageError = null
                                webViewInstance?.reload()
                            }
                        }
                    }

                    override fun onLost(network: Network) {
                        scope.launch {
                            isOffline = true
                        }
                    }
                }

                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()

                connectivityManager.registerNetworkCallback(request, networkCallback)

                val activeNet = connectivityManager.activeNetwork
                val caps = connectivityManager.getNetworkCapabilities(activeNet)
                isOffline = caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        } catch (e: Throwable) {
            Log.w("AnyWatchApp", "Network monitoring warning", e)
        }

        onDispose {
            try {
                if (connectivityManager != null && networkCallback != null) {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                }
            } catch (_: Throwable) {}
        }
    }

    // Back button handling
    BackHandler {
        when {
            showProfileModal -> showProfileModal = false
            showChatModal -> showChatModal = false
            showShieldsModal -> showShieldsModal = false
            showDownloadsModal -> showDownloadsModal = false
            showFixModal -> showFixModal = false
            showHistoryModal -> showHistoryModal = false
            showDirectoryModal -> showDirectoryModal = false
            webPageError != null -> {
                webPageError = null
                currentUrl = "https://ani.pm/"
                repository.setLastVisitedUrl(currentUrl)
                webViewInstance?.loadUrl(currentUrl)
            }
            webViewInstance?.canGoBack() == true -> {
                webPageError = null
                webViewInstance?.goBack()
            }
            else -> {
                activity.finish()
            }
        }
    }

    // Apply Desktop Mode User Agent dynamically
    LaunchedEffect(isDesktopMode, webViewInstance) {
        try {
            webViewInstance?.settings?.let { settings ->
                if (isDesktopMode) {
                    settings.userAgentString = PopupShieldClient.DESKTOP_USER_AGENT
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                } else {
                    settings.userAgentString = PopupShieldClient.MOBILE_USER_AGENT
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                }
            }
        } catch (_: Throwable) {}
    }

    // Boot Splash Screen (safely unmounts and mounts main container)
    if (isBooting) {
        SplashScreen(onBootFinished = { isBooting = false })
        return
    }

    // MANDATORY GOOGLE LOGIN SYSTEM (GATEKEEPER)
    // No Bypass: The user CANNOT access the WebView, Movies, Anime, etc. without logging in.
    if (currentUser == null) {
        GoogleLoginScreen(
            activity = activity,
            onLoginSuccess = { profile ->
                repository.saveUserProfile(profile)
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .background(ObsidianBlack)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Fullscreen Video: Completely hide Header and Tabs when HTML5 video is fullscreen
                    if (!isHtml5Fullscreen) {
                        GlassmorphismHeader(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { cat ->
                                selectedCategory = cat
                                if (cat == "History") {
                                    showHistoryModal = true
                                } else {
                                    val targetUrl = when (cat) {
                                        "Anime" -> "https://ani.pm/"
                                        "Manga" -> "https://mangafire.to/"
                                        "Novel" -> "https://novelfire.net/"
                                        "Movies" -> "https://meowtv.ru/"
                                        else -> currentUrl
                                    }
                                    webPageError = null
                                    currentUrl = targetUrl
                                    repository.setLastVisitedUrl(targetUrl)
                                    webViewInstance?.loadUrl(targetUrl)
                                }
                            },
                            currentUrl = currentUrl,
                            onQuickSiteSelected = { site ->
                                webPageError = null
                                currentUrl = site.url
                                repository.setLastVisitedUrl(site.url)
                                webViewInstance?.loadUrl(site.url)
                            },
                            onWebsiteFixClick = { showFixModal = true },
                            onOpenDownloads = { showDownloadsModal = true },
                            onRefresh = {
                                webPageError = null
                                webViewInstance?.reload()
                            },
                            onToggleRotation = {
                                isPermanentRotate = !isPermanentRotate
                                activity.requestedOrientation = if (isPermanentRotate) {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }
                            },
                            onBrowseAll = { showDirectoryModal = true },
                            isShieldActive = anywatchShieldsEnabled,
                            onOpenShields = { showShieldsModal = true },
                            onToggleShield = {
                                repository.setAnywatchShieldsEnabled(!anywatchShieldsEnabled)
                                webViewInstance?.reload()
                            },
                            is18PlusActive = is18Plus,
                            onToggle18Plus = {
                                repository.set18PlusEnabled(!is18Plus)
                            },
                            isDesktopMode = isDesktopMode,
                            onToggleDesktopMode = {
                                val nextMode = !isDesktopMode
                                repository.setDesktopMode(nextMode)
                                webViewInstance?.let { wv ->
                                    wv.settings.let { s ->
                                        if (nextMode) {
                                            s.userAgentString = PopupShieldClient.DESKTOP_USER_AGENT
                                            s.useWideViewPort = true
                                            s.loadWithOverviewMode = true
                                        } else {
                                            s.userAgentString = PopupShieldClient.MOBILE_USER_AGENT
                                            s.useWideViewPort = true
                                            s.loadWithOverviewMode = true
                                        }
                                    }
                                    webPageError = null
                                    wv.reload()
                                }
                            },
                            currentUser = currentUser,
                            onOpenProfile = { showProfileModal = true },
                        onOpenSearch = { showSearchModal = true },
                            onOpenChat = { showChatModal = true },
                            onSignOut = {
                                scope.launch {
                                    FirebaseChatManager.signOut()
                                    repository.signOut()
                                }
                            }
                        )

                        // Web Loading Progress Bar
                        if (isWebLoading && webProgress in 1..99) {
                            LinearProgressIndicator(
                                progress = { webProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp),
                                color = NeonCyan,
                                trackColor = Color.Transparent
                            )
                        }
                    }

                    // WebView Engine & Video Container (Native HTML5 playback inside WebView)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black)
                    ) {
                        if (webViewFatalInitError != null) {
                            // Fatal WebView Provider Error Screen
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ObsidianBlack)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = ObsidianCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("System WebView Unavailable", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            webViewFatalInitError ?: "Please ensure Android System WebView is updated.",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            AndroidView(
                                factory = { ctx ->
                                    try {
                                        val rootLayout = FrameLayout(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        }

                                        val webView = WebView(ctx).apply {
                                            layoutParams = FrameLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        }

                                        val fullScreenContainer = FrameLayout(ctx).apply {
                                            layoutParams = FrameLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            visibility = android.view.View.GONE
                                        }

                                        val shieldClient = PopupShieldClient(
                                            context = ctx,
                                            downloadManager = downloadManager,
                                            onPageStarted = { url ->
                                                currentUrl = url
                                                isWebLoading = true
                                                webPageError = null
                                            },
                                            onPageFinished = { url, title ->
                                                currentUrl = url
                                                isWebLoading = false
                                                repository.setLastVisitedUrl(url)
                                                repository.addHistory(title ?: "AnyWatch Page", url)
                                            },
                                            onProgress = { progress ->
                                                webProgress = progress
                                                isWebLoading = progress < 100
                                            },
                                            onError = { description, failingUrl ->
                                                isWebLoading = false
                                                webPageError = Pair(description, failingUrl)
                                            },
                                            isProxyEnabled = { repository.unblockerEnabled.value },
                                            isShieldEnabled = { repository.anywatchShieldsEnabled.value },
                                            isDomainWhitelisted = { domain -> repository.isDomainWhitelisted(domain) },
                                            onAdBlocked = { repository.incrementBlockedCount() }
                                        )

                                        shieldClient.configureWebView(
                                            webView = webView,
                                            customViewContainer = fullScreenContainer,
                                            onVideoFound = { videoSrc, videoTitle ->
                                                detectedMediaUrl = videoSrc
                                                detectedMediaTitle = videoTitle
                                            },
                                            onCustomViewToggle = { isFull ->
                                                isHtml5Fullscreen = isFull
                                            },
                                            isDesktopMode = isDesktopMode
                                        )

                                        rootLayout.addView(webView)
                                        rootLayout.addView(fullScreenContainer)

                                        webViewInstance = webView
                                        customViewContainer = fullScreenContainer

                                        webView.loadUrl(currentUrl)
                                        rootLayout
                                    } catch (e: Throwable) {
                                        Log.e("AnyWatchApp", "Fatal WebView instantiation error", e)
                                        webViewFatalInitError = e.localizedMessage ?: "Failed to start WebView engine"
                                        FrameLayout(ctx)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // ERROR BOUNDARY: Custom sleek screen when website fails to load instead of crashing
                        if (webPageError != null) {
                            val (errorReason, failingUrl) = webPageError!!
                            val failingHost = remember(failingUrl) {
                                try {
                                    Uri.parse(failingUrl).host?.lowercase()?.removePrefix("www.") ?: ""
                                } catch (_: Throwable) {
                                    ""
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ObsidianBlack.copy(alpha = 0.94f))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = ObsidianSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        Brush.horizontalGradient(listOf(NeonViolet, NeonCyan))
                                    ),
                                    shadowElevation = 16.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("web_error_boundary")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(22.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(NeonCyan.copy(alpha = 0.15f))
                                                .border(1.5.dp, NeonCyan, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Security,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Text(
                                            "Connection Shield Intercepted",
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            errorReason,
                                            color = NeonPink,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            failingUrl.take(65) + if (failingUrl.length > 65) "..." else "",
                                            color = TextSecondary,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2
                                        )

                                        Spacer(modifier = Modifier.height(18.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    webPageError = null
                                                    webViewInstance?.reload()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, tint = ObsidianBlack, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Retry", color = ObsidianBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }

                                            if (failingHost.isNotBlank()) {
                                                Button(
                                                    onClick = {
                                                        repository.setDomainWhitelisted(failingHost, true)
                                                        webPageError = null
                                                        if (failingUrl.isNotBlank() && (failingUrl.startsWith("http://") || failingUrl.startsWith("https://"))) {
                                                            currentUrl = failingUrl
                                                            repository.setLastVisitedUrl(failingUrl)
                                                            webViewInstance?.loadUrl(failingUrl)
                                                        } else {
                                                            webViewInstance?.reload()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Whitelist & Load", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedButton(
                                            onClick = {
                                                webPageError = null
                                                val fallbackUrl = "https://ani.pm/"
                                                currentUrl = fallbackUrl
                                                repository.setLastVisitedUrl(fallbackUrl)
                                                webViewInstance?.loadUrl(fallbackUrl)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Home, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Return to Anime Home", color = TextPrimary, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Clean 1-tap download pill for detected streams (non-intrusive)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = detectedMediaUrl != null,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(12.dp)
                        ) {
                            detectedMediaUrl?.let { mediaUrl ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = ObsidianBlack.copy(alpha = 0.94f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                                    shadowElevation = 8.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Download Stream",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                detectedMediaTitle ?: "Save for offline playback",
                                                color = NeonCyan,
                                                fontSize = 10.sp,
                                                maxLines = 1
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(
                                            onClick = {
                                                val cookie = CookieManager.getInstance().getCookie(mediaUrl)
                                                downloadManager.startDownload(
                                                    url = mediaUrl,
                                                    userAgent = webViewInstance?.settings?.userAgentString,
                                                    contentDisposition = null,
                                                    mimeType = "video/mp4",
                                                    contentLength = 0L,
                                                    cookie = cookie,
                                                    referer = currentUrl,
                                                    suggestedTitle = detectedMediaTitle
                                                )
                                                detectedMediaUrl = null
                                                showDownloadsModal = true
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(NeonViolet.copy(alpha = 0.3f))
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = "Download", tint = NeonViolet, modifier = Modifier.size(16.dp))
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        IconButton(
                                            onClick = { detectedMediaUrl = null },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Download Telemetry Widget (Clean bottom, non-intrusive)
                FloatingDownloadWidget(
                    downloadManager = downloadManager,
                    onClick = { showDownloadsModal = true },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // Offline Guard Overlay
                OfflineGuardOverlay(
                    isOffline = isOffline,
                    onRetry = {
                        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                        val activeNet = connectivityManager?.activeNetwork
                        val caps = connectivityManager?.getNetworkCapabilities(activeNet)
                        if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
                            isOffline = false
                            webPageError = null
                            webViewInstance?.reload()
                        }
                    }
                )

                // Downloads Modal: When user taps "Play" on a downloaded item, open with System Default Player
                if (showDownloadsModal) {
                    DownloadManagerModal(
                        downloadManager = downloadManager,
                        onDismiss = { showDownloadsModal = false },
                        onPlayCompletedVideo = { task ->
                            playDownloadedVideo(context, task.file)
                        }
                    )
                }

                // Fix Website Modal
                if (showFixModal) {
                    FixWebsiteModal(
                        currentUrl = currentUrl,
                        initialHindiPreference = isHindiDubbed,
                        onDismiss = { showFixModal = false },
                        onApplyFix = { targetUrl, hindiPref ->
                            repository.setHindiDubbedPreference(hindiPref)
                            showFixModal = false

                            var fixedUrl = targetUrl
                            if (!fixedUrl.startsWith("http://") && !fixedUrl.startsWith("https://")) {
                                fixedUrl = "https://$fixedUrl"
                            }

                            webPageError = null
                            currentUrl = fixedUrl
                            repository.setLastVisitedUrl(fixedUrl)
                            webViewInstance?.clearCache(true)
                            webViewInstance?.loadUrl(fixedUrl)
                        }
                    )
                }

                // History & Bookmarks Modal
                if (showHistoryModal) {
                    HistoryBookmarksModal(
                        repository = repository,
                        onDismiss = { showHistoryModal = false },
                        onLaunchUrl = { url ->
                            webPageError = null
                            currentUrl = url
                            repository.setLastVisitedUrl(url)
                            webViewInstance?.loadUrl(url)
                        }
                    )
                }

                // Directory Modal (All Websites index)
                if (showDirectoryModal) {
                    DirectoryModal(
                        onDismiss = { showDirectoryModal = false },
                        onSelectSite = { site ->
                            webPageError = null
                            currentUrl = site.url
                            repository.setLastVisitedUrl(site.url)
                            webViewInstance?.loadUrl(site.url)
                        }
                    )
                }

                // Global Stream Search Modal
                if (showSearchModal) {
                    GlobalSearchModal(
                        onDismiss = { showSearchModal = false },
                        onSelectSource = { targetUrl ->
                            showSearchModal = false
                            webPageError = null
                            currentUrl = targetUrl
                            repository.setLastVisitedUrl(targetUrl)
                            webViewInstance?.loadUrl(targetUrl)
                        }
                    )
                }

                // AnyWatch Shields Modal
                if (showShieldsModal) {
                    AnyWatchShieldsModal(
                        repository = repository,
                        currentUrl = currentUrl,
                        onDismiss = { showShieldsModal = false },
                        onReloadRequested = {
                            webPageError = null
                            webViewInstance?.reload()
                        },
                        onClearDataRequested = {
                            webViewInstance?.clearCache(true)
                            webViewInstance?.clearHistory()
                            try {
                                CookieManager.getInstance().removeAllCookies(null)
                                WebStorage.getInstance().deleteAllData()
                            } catch (_: Throwable) {}
                        }
                    )
                }

                // Real-Time Community Chat Modal
                if (showChatModal && currentUser != null) {
                    CommunityChatModal(
                        chatRepository = chatRepository,
                        currentUser = currentUser!!,
                        isAdminModeEnabled = isAdminModeEnabled,
                        onDismiss = { showChatModal = false }
                    )
                }

                // Dedicated Glassmorphic Profile Modal with Real User Data & Top-Right 3-Dot Menu
                if (showProfileModal && currentUser != null) {
                    ProfileModal(
                        user = currentUser!!,
                        isAdminModeEnabled = isAdminModeEnabled,
                        onToggleAdminMode = { repository.setAdminModeEnabled(it) },
                        onUploadProfilePhoto = { photoUri: Uri ->
                            repository.updateProfilePhoto(photoUri.toString())
                        },
                        isShieldActive = anywatchShieldsEnabled,
                        onToggleShield = {
                            repository.setAnywatchShieldsEnabled(!anywatchShieldsEnabled)
                            webViewInstance?.reload()
                        },
                        is18PlusActive = is18Plus,
                        onToggle18Plus = { repository.set18PlusEnabled(!is18Plus) },
                        isDesktopMode = isDesktopMode,
                        onToggleDesktopMode = {
                            val nextMode = !isDesktopMode
                            repository.setDesktopMode(nextMode)
                            webViewInstance?.let { wv ->
                                wv.settings.let { s ->
                                    if (nextMode) {
                                        s.userAgentString = PopupShieldClient.DESKTOP_USER_AGENT
                                        s.useWideViewPort = true
                                        s.loadWithOverviewMode = true
                                    } else {
                                        s.userAgentString = PopupShieldClient.MOBILE_USER_AGENT
                                        s.useWideViewPort = true
                                        s.loadWithOverviewMode = true
                                    }
                                }
                                webPageError = null
                                wv.reload()
                            }
                        },
                        onOpenDownloads = { showDownloadsModal = true },
                        onWebsiteFixClick = { showFixModal = true },
                        onToggleRotation = {
                            isPermanentRotate = !isPermanentRotate
                            activity.requestedOrientation = if (isPermanentRotate) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        },
                        onBrowseAll = { showDirectoryModal = true },
                        onOpenHistory = { showHistoryModal = true },
                        onUpdateName = { newName ->
                            scope.launch {
                                val updatedUser = FirebaseChatManager.updateUserName(newName)
                                if (updatedUser != null) {
                                    repository.saveUserProfile(updatedUser)
                                } else {
                                    repository.updateDisplayName(newName)
                                }
                            }
                        },
                        onSignOut = {
                            scope.launch {
                                FirebaseChatManager.signOut()
                                repository.signOut()
                            }
                        },
                        onDismiss = { showProfileModal = false }
                    )
                }
            }
        }
    )
}
