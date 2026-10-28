package com.example.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.example.download.SmartDownloadManager
import java.io.ByteArrayInputStream

class WebAppInterface(
    private val onVideoDetected: (String, String?) -> Unit
) {
    @JavascriptInterface
    fun onMediaFound(videoUrl: String, title: String?) {
        try {
            if (videoUrl.isNotBlank() && !videoUrl.startsWith("blob:") && !videoUrl.startsWith("data:")) {
                onVideoDetected(videoUrl, title)
            }
        } catch (e: Throwable) {
            Log.e("WebAppInterface", "Error in onMediaFound", e)
        }
    }
}

class PopupShieldClient(
    private val context: Context,
    private val downloadManager: SmartDownloadManager,
    private val onPageStarted: (String) -> Unit,
    private val onPageFinished: (String, String?) -> Unit,
    private val onProgress: (Int) -> Unit,
    private val onError: (String, String) -> Unit, // description, failingUrl
    private val isProxyEnabled: () -> Boolean,
    private val isShieldEnabled: () -> Boolean,
    private val isDomainWhitelisted: (String) -> Boolean = { false },
    private val onAdBlocked: () -> Unit = {}
) {
    companion object {
        private const val TAG = "PopupShieldClient"

        const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 AnyWatch/2.0"
        const val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 AnyWatch/2.0"

        // BULLETPROOF POPUP KILLER & MUTATION OBSERVER DOM SANITIZER (100% VIDEO SAFE)
        const val JS_POPUP_SHIELD = """
            (function() {
                try {
                    // POPUP & REDIRECT KILLER: Inject window.open = function() { return null; };
                    try {
                        window.open = function() { return null; };
                        Object.defineProperty(window, 'open', {
                            value: function() { return null; },
                            writable: true,
                            configurable: true
                        });
                    } catch(_) {
                        window.open = function() { return null; };
                    }

                    // Suppress rogue alert/confirm/prompt hijacking & onbeforeunload spam
                    try {
                        window.onbeforeunload = null;
                        window.alert = function() {};
                        window.confirm = function() { return true; };
                        window.prompt = function() { return null; };
                    } catch(_) {}

                    // Smart Clickjacking Interception
                    var nativeAddEventListener = EventTarget.prototype.addEventListener;
                    EventTarget.prototype.addEventListener = function(type, listener, options) {
                        if (type === 'click' || type === 'mousedown' || type === 'mouseup' || type === 'touchstart') {
                            var fnStr = listener ? listener.toString().toLowerCase() : '';
                            if ((fnStr.indexOf('window.open') !== -1 || fnStr.indexOf('popunder') !== -1) &&
                                (fnStr.indexOf('video') === -1 && fnStr.indexOf('player') === -1)) {
                                console.log('AnyWatch Shield prevented malicious clickjack event listener');
                                return;
                            }
                        }
                        return nativeAddEventListener.call(this, type, listener, options);
                    };

                    // Brave-Grade CSS Ad & Popup Sanitizer (Guaranteed 100% Video Safe)
                    var styleId = 'anywatch-shield-style-rules';
                    if (!document.getElementById(styleId)) {
                        var style = document.createElement('style');
                        style.id = styleId;
                        style.type = 'text/css';
                        style.innerHTML = `
                            iframe[id*="ad" i]:not([id*="player" i]):not([id*="video" i]),
                            iframe[src*="ad" i]:not([src*="player" i]):not([src*="video" i]),
                            iframe[src*="banner" i], iframe[src*="pop" i],
                            iframe[src*="syndication" i], iframe[src*="exoclick" i], iframe[src*="doubleclick" i],
                            iframe[src*="adsterra" i], iframe[src*="monetag" i], iframe[src*="clickadu" i],
                            div[class*="banner" i]:not(.vjs-control-bar):not(.player-container):not([class*="player"]):not([class*="video"]),
                            div[class*="ad-" i]:not([class*="player"]):not([class*="video"]):not([class*="episode"]):not([class*="load"]),
                            div[class*="-ad" i]:not([class*="player"]):not([class*="video"]):not([class*="episode"]):not([class*="load"]),
                            div[id*="ad-" i]:not([id*="player"]):not([id*="video"]):not([id*="episode"]),
                            div[id*="-ad" i]:not([id*="player"]):not([id*="video"]):not([id*="episode"]),
                            div[class*="popup" i]:not([class*="player"]):not([class*="episode"]):not([class*="modal"]),
                            div[class*="popunder" i], div[id*="popunder" i],
                            div[class*="sponsored" i], div[id*="adbox" i], div[class*="adbox" i],
                            div[class*="advertisement" i], ins.adsbygoogle, .ad-container, .ads-holder,
                            .floating-ad, #float-ads, a[href*="betting" i], a[href*="casino" i],
                            a[href*="1xbet" i], a[href*="ouo.io" i],
                            div[style*="z-index: 2147483647"]:not(video):not(.plyr):not(.vjs-tech):not([class*="player"]):not([class*="video"]),
                            div[style*="z-index: 9999999"]:not(video):not(.plyr):not(.vjs-tech):not([class*="player"]):not([class*="video"]) {
                                display: none !important;
                                visibility: hidden !important;
                                opacity: 0 !important;
                                pointer-events: none !important;
                                height: 0 !important;
                                width: 0 !important;
                                overflow: hidden !important;
                            }
                        `;
                        (document.head || document.documentElement).appendChild(style);
                    }

                    // Video Safety Validator: NEVER delete <video> tags or their parent containers!
                    function isVideoRelated(el) {
                        if (!el) return false;
                        if (el.tagName === 'VIDEO' || el.tagName === 'AUDIO' || el.tagName === 'SOURCE') return true;
                        if (el.querySelector && (el.querySelector('video') || el.querySelector('audio') || el.querySelector('iframe[src*="stream"]') || el.querySelector('iframe[src*="embed"]') || el.querySelector('iframe[src*="player"]'))) return true;
                        if (el.closest && (el.closest('.vjs-tech') || el.closest('.plyr') || el.closest('[class*="player" i]') || el.closest('[id*="player" i]') || el.closest('[class*="video" i]') || el.closest('[id*="video" i]') || el.closest('.jwplayer'))) return true;
                        var cls = (el.className && typeof el.className === 'string') ? el.className.toLowerCase() : '';
                        var id = (el.id && typeof el.id === 'string') ? el.id.toLowerCase() : '';
                        if (cls.indexOf('player') !== -1 || cls.indexOf('video') !== -1 || cls.indexOf('control') !== -1 || cls.indexOf('play') !== -1 || cls.indexOf('episode') !== -1 || cls.indexOf('media') !== -1) return true;
                        if (id.indexOf('player') !== -1 || id.indexOf('video') !== -1 || id.indexOf('control') !== -1 || id.indexOf('play') !== -1 || id.indexOf('episode') !== -1 || id.indexOf('media') !== -1) return true;
                        if (el.getAttribute && (el.getAttribute('data-number') !== null || el.getAttribute('data-episode') !== null)) return true;
                        return false;
                    }

                    // Dynamic DOM Cleaner & Clickjacking Neutralizer
                    function cleanDomAds() {
                        try {
                            var adSelectors = [
                                'div[id*="ad" i]:not([id*="player"]):not([id*="video"]):not([id*="load"])',
                                'div[class*="ad-" i]:not([class*="player"]):not([class*="video"])',
                                'div[class*="-ad" i]:not([class*="player"]):not([class*="video"])',
                                'iframe[src*="ads" i]', 'iframe[src*="doubleclick" i]', 'iframe[src*="syndication" i]',
                                'iframe[src*="exoclick" i]', 'iframe[src*="adsterra" i]', 'iframe[src*="ad-maven" i]',
                                'iframe[src*="monetag" i]', 'iframe[src*="clickadu" i]', 'ins.adsbygoogle',
                                '.floating-ad', '#float-ads', '.ad-container', 'div[class*="popunder" i]',
                                'div[id*="popunder" i]', 'div[class*="advertisement" i]', 'div[class*="banner" i]:not([class*="player"])',
                                '.ad-overlay', '.popup-banner', '.banner-ad', '[class*="ad-overlay" i]', '[class*="popup-banner" i]',
                                'div[class*="overlay" i]:not([class*="player"]):not([class*="video"]):not([class*="control"]):not([class*="play"])'
                            ];

                            for (var s = 0; s < adSelectors.length; s++) {
                                var nodes = document.querySelectorAll(adSelectors[s]);
                                for (var n = 0; n < nodes.length; n++) {
                                    var el = nodes[n];
                                    if (isVideoRelated(el)) continue;
                                    el.style.display = 'none';
                                    el.remove();
                                }
                            }

                            // Neutralize deceptive invisible overlays (high z-index full-screen clickjack traps)
                            var suspectedOverlays = document.querySelectorAll('div, a, section, span');
                            var winW = window.innerWidth;
                            var winH = window.innerHeight;
                            for (var i = 0; i < suspectedOverlays.length; i++) {
                                var o = suspectedOverlays[i];
                                if (isVideoRelated(o)) continue;
                                var style = window.getComputedStyle(o);
                                var pos = style.position;
                                var zIndex = parseInt(style.zIndex, 10);
                                if ((pos === 'fixed' || pos === 'absolute') && zIndex >= 800) {
                                    var rect = o.getBoundingClientRect();
                                    var isFullScreen = (rect.width >= winW * 0.7 && rect.height >= winH * 0.7);
                                    var isTransparent = (parseFloat(style.opacity) < 0.1 || style.visibility === 'hidden' ||
                                                         style.backgroundColor === 'transparent' || style.backgroundColor === 'rgba(0, 0, 0, 0)');
                                    if (isFullScreen && isTransparent) {
                                        console.log('AnyWatch Shield neutralized transparent clickjack overlay');
                                        o.style.pointerEvents = 'none';
                                        o.remove();
                                    }
                                }
                            }
                        } catch(e) {
                            console.error('Error cleaning DOM ads:', e);
                        }
                    }

                    // Video Source Detector for 1-Tap Download
                    function scanVideos() {
                        try {
                            var vids = document.querySelectorAll('video');
                            for (var v = 0; v < vids.length; v++) {
                                var vid = vids[v];
                                var src = vid.currentSrc || vid.src;
                                if (!src) {
                                    var srcs = vid.querySelectorAll('source');
                                    for (var s = 0; s < srcs.length; s++) {
                                        if (srcs[s].src) {
                                            src = srcs[s].src;
                                            break;
                                        }
                                    }
                                }
                                if (src && window.AnyWatchBridge) {
                                    window.AnyWatchBridge.onMediaFound(src, document.title);
                                }
                            }
                        } catch(_) {}
                    }

                    cleanDomAds();
                    scanVideos();

                    // High-efficiency MutationObserver
                    var scheduled = false;
                    var obs = new MutationObserver(function() {
                        if (!scheduled) {
                            scheduled = true;
                            requestAnimationFrame(function() {
                                cleanDomAds();
                                scanVideos();
                                scheduled = false;
                            });
                        }
                    });
                    if (document.body) {
                        obs.observe(document.body, { childList: true, subtree: true });
                    }
                    document.addEventListener('DOMContentLoaded', function() {
                        cleanDomAds();
                        scanVideos();
                    });
                    document.addEventListener('play', scanVideos, true);
                } catch(e) {
                    console.error('AnyWatch Shield initialization error:', e);
                }
            })();
        """
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var currentLoadedUrl: String = ""

    private fun notifyAdBlocked() {
        mainHandler.post {
            try {
                onAdBlocked()
            } catch (e: Throwable) {
                Log.e(TAG, "Error in onAdBlocked", e)
            }
        }
    }

    fun configureWebView(
        webView: WebView,
        customViewContainer: FrameLayout?,
        onVideoFound: (String, String?) -> Unit,
        onCustomViewToggle: (Boolean) -> Unit,
        isDesktopMode: Boolean = false
    ) {
        try {
            val settings = webView.settings
            configureSettings(settings, isDesktopMode)

            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(webView, true)
            } catch (e: Throwable) {
                Log.e(TAG, "CookieManager initialization warning", e)
            }

            webView.addJavascriptInterface(
                WebAppInterface(onVideoFound),
                "AnyWatchBridge"
            )

            // Smart Download Listener with Header Extraction
            webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                try {
                    val cookie = CookieManager.getInstance().getCookie(url)
                    val referer = currentLoadedUrl
                    downloadManager.startDownload(
                        url = url,
                        userAgent = userAgent.ifBlank { settings.userAgentString },
                        contentDisposition = contentDisposition,
                        mimeType = mimetype,
                        contentLength = contentLength,
                        cookie = cookie,
                        referer = referer,
                        suggestedTitle = webView.title
                    )
                } catch (e: Throwable) {
                    Log.e(TAG, "Download trigger error", e)
                }
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    try {
                        url?.let {
                            currentLoadedUrl = it
                            onPageStarted(it)
                        }
                        val currentHost = Uri.parse(url ?: "").host ?: ""
                        if (isShieldEnabled() && !isDomainWhitelisted(currentHost)) {
                            view?.evaluateJavascript(JS_POPUP_SHIELD, null)
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error in onPageStarted", e)
                    }
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    url?.let { currentLoadedUrl = it }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    try {
                        url?.let {
                            currentLoadedUrl = it
                            onPageFinished(it, view?.title)
                        }
                        val currentHost = Uri.parse(url ?: "").host ?: ""
                        if (isShieldEnabled() && !isDomainWhitelisted(currentHost)) {
                            view?.evaluateJavascript(JS_POPUP_SHIELD, null)
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error in onPageFinished", e)
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    try {
                        if (request?.isForMainFrame == true) {
                            val desc = error?.description?.toString() ?: "Failed to connect to website"
                            val failingUrl = request.url?.toString() ?: currentLoadedUrl
                            Log.w(TAG, "Main frame error: $desc at $failingUrl")
                            onError(desc, failingUrl)
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error in onReceivedError", e)
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    try {
                        val statusCode = errorResponse?.statusCode ?: 200
                        val host = request?.url?.host?.lowercase() ?: ""
                        // Never trigger error boundary for Cloudflare challenges, Turnstile or DDoS checks (frequently 503 while serving HTML)
                        if (host.contains("cloudflare") || host.contains("turnstile") || host.contains("challenges.cloudflare.com")) {
                            return
                        }
                        // Only trigger error for real server failures, never for 503 challenges that the website is solving
                        if (request?.isForMainFrame == true && statusCode >= 500 && statusCode != 503) {
                            val reason = errorResponse?.reasonPhrase ?: "HTTP Error"
                            val failingUrl = request.url?.toString() ?: currentLoadedUrl
                            Log.w(TAG, "HTTP error: $statusCode $reason at $failingUrl")
                            onError("$reason ($statusCode)", failingUrl)
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error in onReceivedHttpError", e)
                    }
                }

                // FIX WHITE SCREEN AUTO-REDIRECTS: Strict navigation control
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    try {
                        val uri = request?.url ?: return false
                        val url = uri.toString()
                        val host = uri.host?.lowercase() ?: ""
                        val currentHost = try { Uri.parse(currentLoadedUrl).host?.lowercase() } catch (_: Throwable) { null } ?: ""

                        // 1. External application intents (Telegram, WhatsApp, Mail)
                        if (url.startsWith("intent:") || url.startsWith("tg:") || url.startsWith("whatsapp:") || url.startsWith("mailto:")) {
                            try {
                                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                if (intent != null) {
                                    intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                    intent.component = null
                                    context.startActivity(intent)
                                    return true
                                }
                            } catch (_: Throwable) {
                                return true
                            }
                        }

                        // 2. Cloudflare & Captcha verification infrastructure
                        if (host.contains("cloudflare") || host.contains("turnstile") ||
                            host.contains("recaptcha") || host.contains("hcaptcha") ||
                            host.contains("challenges.cloudflare.com")) {
                            return false // ALLOW
                        }

                        // 3. CRITICAL: Video Streams & CDNs must NEVER be blocked
                        if (ShieldDomainManager.isMediaOrStreamRequest(uri)) {
                            return false // ALLOW
                        }

                        // 4. If site is whitelisted, permit normal navigation
                        if (currentHost.isNotBlank() && isDomainWhitelisted(currentHost)) {
                            return false // ALLOW
                        }

                        // 5. Shield Aggressive Navigation Filtering
                        if (isShieldEnabled()) {
                            // Block known malicious ad / spam networks
                            if (ShieldDomainManager.isBlockedDomain(url, host)) {
                                Log.d(TAG, "AnyWatch Shield blocked ad redirect: $url")
                                notifyAdBlocked()
                                return true // BLOCK
                            }

                            // Allow if navigation stays on the same website (base domain or subdomain)
                            val isCurrentSite = currentHost.isNotBlank() && ShieldDomainManager.isSameOrSubdomain(host, currentHost)
                            // Allow if navigation is to an allowed primary streaming catalog domain
                            val isPrimarySite = ShieldDomainManager.isAllowedPrimaryDomain(host)

                            if (isCurrentSite || isPrimarySite) {
                                return false // ALLOW
                            }

                            // If not the current site, not primary site, and not video:
                            // It is a rogue off-site auto-redirect/popup causing white screens!
                            Log.d(TAG, "AnyWatch Shield blocked rogue off-site redirect to: $url (currentHost=$currentHost)")
                            notifyAdBlocked()
                            return true // BLOCK
                        }

                        return false
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error in shouldOverrideUrlLoading", e)
                        return false
                    }
                }

                // FIX 503 SHIELD ERROR: Strict Network Interception that NEVER blocks the main domain
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    try {
                        if (request == null) return null
                        val uri = request.url ?: return null

                        // 1. NEVER block main-frame requests under any circumstances! (Fixes 503 errors)
                        if (request.isForMainFrame) {
                            return null
                        }

                        // 2. VIDEO WHITELIST (Crucial): NEVER block URLs containing .m3u8, .mp4, .ts, .webm,
                        // or requests to known video hosting CDNs. Videos must play perfectly.
                        val rawUrl = uri.toString().lowercase()
                        if (rawUrl.contains(".m3u8") || rawUrl.contains(".mp4") || rawUrl.contains(".ts") ||
                            rawUrl.contains(".webm") || rawUrl.contains(".mpd") || rawUrl.contains(".m4s") ||
                            rawUrl.contains(".key") || ShieldDomainManager.isMediaOrStreamRequest(uri)) {
                            return null // ALLOW VIDEO STREAM IMMEDIATELY
                        }

                        // Safely extract active host without calling view.getUrl() on worker thread
                        val pageHost = try { Uri.parse(currentLoadedUrl).host?.lowercase() } catch (_: Throwable) { null } ?: ""
                        val refererHost = request.requestHeaders?.get("Referer")?.let {
                            try { Uri.parse(it).host?.lowercase() } catch (_: Throwable) { null }
                        } ?: ""
                        val activeHost = if (pageHost.isNotBlank()) pageHost else refererHost
                        val requestHost = uri.host?.lowercase() ?: ""

                        // 3. Per-site whitelist check
                        if (activeHost.isNotBlank() && isDomainWhitelisted(activeHost)) {
                            return null
                        }

                        if (!isShieldEnabled()) {
                            return null
                        }

                        // 4. FIX 503 ERROR: NEVER block the main domain (the website the user is visiting) or its essential scripts!
                        // Only block third-party ad domains!
                        if (activeHost.isNotBlank() && ShieldDomainManager.isSameOrSubdomain(requestHost, activeHost)) {
                            return null // Always allow first-party scripts, styles, and APIs
                        }

                        // 5. Cloudflare, Turnstile, and Captcha infrastructure must never be blocked
                        if (requestHost.contains("cloudflare") || requestHost.contains("turnstile") ||
                            requestHost.contains("recaptcha") || requestHost.contains("hcaptcha") ||
                            requestHost.contains("challenges.cloudflare.com")) {
                            return null
                        }

                        // 6. Only block verified third-party ad/tracking resources
                        if (ShieldDomainManager.isThirdPartyAdDomain(requestHost, uri)) {
                            Log.d(TAG, "AnyWatch Shield dropped third-party ad resource: $uri")
                            notifyAdBlocked()
                            return WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                ByteArrayInputStream(ByteArray(0))
                            )
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error in shouldInterceptRequest", e)
                    }
                    return null
                }
            }

            webView.webChromeClient = object : WebChromeClient() {
                private var customView: View? = null
                private var customViewCallback: CustomViewCallback? = null

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    try {
                        onProgress(newProgress)
                    } catch (_: Throwable) {}
                }

                // Popup Killer: reject all popup window creation
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    Log.d(TAG, "AnyWatch Shield suppressed onCreateWindow popup")
                    return false
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    try {
                        if (customView != null) {
                            onHideCustomView()
                            return
                        }
                        customView = view
                        customViewCallback = callback

                        (view?.parent as? ViewGroup)?.removeView(view)

                        customViewContainer?.addView(
                            customView,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                        customViewContainer?.visibility = View.VISIBLE
                        onCustomViewToggle(true)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error showing custom full-screen view", e)
                    }
                }

                override fun onHideCustomView() {
                    try {
                        if (customView == null) return
                        customViewContainer?.removeView(customView)
                        customView = null
                        customViewContainer?.visibility = View.GONE
                        customViewCallback?.onCustomViewHidden()
                        customViewCallback = null
                        onCustomViewToggle(false)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error hiding custom full-screen view", e)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal error configuring WebView", e)
            throw e
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureSettings(settings: WebSettings, isDesktop: Boolean = false) {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.setSupportMultipleWindows(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        if (isDesktop) {
            settings.userAgentString = DESKTOP_USER_AGENT
        } else {
            settings.userAgentString = MOBILE_USER_AGENT
        }
    }
}
