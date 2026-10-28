package com.example.webview

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, asynchronous domain manager for the Brave-grade AnyWatch Shield.
 * Domain lists are loaded asynchronously in a background IO thread to prevent
 * startup main-thread blocking, ANRs, or splash screen memory leaks.
 */
object ShieldDomainManager {
    private const val TAG = "ShieldDomainManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    // Thread-safe fast lookup sets
    private val blockedDomainsSet = ConcurrentHashMap.newKeySet<String>()
    private val adResourceHostsSet = ConcurrentHashMap.newKeySet<String>()
    private val primaryStreamingDomainsSet = ConcurrentHashMap.newKeySet<String>()
    private val videoCdnKeywordsSet = ConcurrentHashMap.newKeySet<String>()

    init {
        loadDomainListsAsync()
    }

    /**
     * Initializes and loads the ad-blocker domain lists asynchronously in background IO thread.
     */
    fun loadDomainListsAsync() {
        scope.launch {
            try {
                // Primary Whitelisted Streaming & Reading Domains
                val primaryDomains = listOf(
                    // Movies
                    "pvrplay.fun", "atlantic.st", "cinejoy.pk", "7movies.ac", "movy.sx",
                    "meowtv.ru", "skyflixer.fun", "netout.pages.dev", "netout.to",
                    // Anime (Including all 15 new sources)
                    "xanime.me", "anizone.to", "guts.to", "anichi.to",
                    "animeparadise.moe", "9ime.live", "kazora.cc", "barelystarted.miruro.tv",
                    "miruro.tv", "gotaku.to", "anv.to", "animeonsen.xyz",
                    "kaa.lt", "anisnatch.to", "senshi.to", "zanix.tr",
                    "ani.pm", "rareanimes.mov", "blakiteanime.buzz", "animesalt.ro",
                    "toonstream.us", "yenime.net", "reanime.to",
                    // Manga
                    "mangaplaza.com", "mangafire.to", "mangago.zone", "atsu.moe",
                    "mangabuddy1.co.uk", "comick.io", "mangalivre.net", "weebcentral.com",
                    // Novels
                    "kartoons.to", "toonix.bond", "novelfire.net", "noveldex.io", "novelarchive.cc", "novelbuddy.me",
                    // External community
                    "t.me"
                )
                primaryStreamingDomainsSet.addAll(primaryDomains)

                // Video CDN & Streaming Player Hosts (CRITICAL: NEVER BLOCKED)
                val videoCdns = listOf(
                    "megacloud", "rabbitstream", "dokicloud", "vidcloud", "vidhide",
                    "mixdrop", "filemoon", "streamtape", "streamwish", "dood",
                    "doodstream", "luluvdo", "mp4upload", "playertv", "vidsrc",
                    "animecdn", "mycloud", "vizcloud", "gogocdn", "kwik",
                    "jwplayer", "streamruby", "streamhub", "voe", "cloudemb",
                    "vidmoly", "streamsb", "sbembed", "sbplay", "streamlare",
                    "autoembed", "2embed", "superembed", "videoview",
                    "upstream", "chillx", "asiancdn", "vidplay", "vidspeeds",
                    "closeload", "waaw", "streamembed", "vcdn", "hydrax",
                    "streamz", "playhydra", "allvids", "multimovies"
                )
                videoCdnKeywordsSet.addAll(videoCdns)

                // Known Spam Ad Networks, Rogue Redirectors, Popunders
                val blockedDomains = listOf(
                    "bit.ly", "ouo.io", "ouo.press", "adf.ly", "tinyurl.com", "shorturl.at",
                    "popcash.net", "adkeeper.co.uk", "adsterra.com", "exdynsrv.com", "syndication.exdynsrv.com",
                    "onclickprediction.com", "trafficjunky.net", "propellerads.com", "yllix.com",
                    "revenuehits.com", "directrev.com", "betting", "casino", "1xbet", "adult-tracker",
                    "adclick.g.", "adnxs.com", "taboola.com", "outbrain.com", "mgid.com", "monetag.com",
                    "hilltopads.com", "richpush.co", "clickadu.com", "adxpansion.com", "juicyads.com",
                    "trafficstars.com", "popads.net", "bidvertiser.com", "infolinks.com", "zeroredirect",
                    "ad-maven.com", "coinhive.com", "cryptoloot.pro", "shorte.st", "bc.vc", "shrinkme.io",
                    "track.com", "doubleclick.net", "adform.net", "cpmstar.com", "exoclick.com",
                    "rtbmarket.com", "popmyads.com", "popunder", "rotator", "dating", "smartlink",
                    "clicktrack", "trafficforce", "adcolony", "applovin", "vungle"
                )
                blockedDomainsSet.addAll(blockedDomains)

                // Network Level Ad Resource Hosts
                val adResources = listOf(
                    "doubleclick.net", "googlesyndication.com", "googleadservices.com",
                    "adservice.google", "popads.net", "popcash.net", "adsterra.com",
                    "exoclick.com", "exdynsrv.com", "propellerads.com", "juicyads.com",
                    "trafficjunky.com", "trafficstars.com", "cpmstar.com", "adxpansion.com",
                    "hilltopads.net", "hilltopads.com", "monetag.com", "clickadu.com", "yllix.com",
                    "ad-maven.com", "onclickprediction.com", "ptest2.com", "adnxs.com",
                    "mgid.com", "taboola.com", "outbrain.com", "criteo.com", "adroll.com",
                    "scorecardresearch.com", "zedo.com", "admob.com", "inmobi.com",
                    "serving-sys.com", "openx.net", "rubiconproject.com", "pubmatic.com",
                    "casalemedia.com", "smartadserver.com", "amazon-adsystem.com",
                    "adcolony.com", "applovin.com", "unityads.unity3d.com", "vungle.com",
                    "richpush.co", "zeroredirect", "coinhive.com", "cryptoloot.pro",
                    "shorte.st", "bc.vc", "shrinkme.io", "adform.net", "rtbmarket.com",
                    "popmyads.com", "trafficforce.com", "adkeeper.co.uk", "ouo.io", "ouo.press"
                )
                adResourceHostsSet.addAll(adResources)

                _isReady.value = true
                Log.d(TAG, "ShieldDomainManager initialized: ${blockedDomainsSet.size} blocked domains ready.")
            } catch (e: Throwable) {
                Log.e(TAG, "Error initializing domain lists asynchronously", e)
                _isReady.value = true
            }
        }
    }

    /**
     * Extracts the base domain from a hostname (e.g. "pvrplay.fun" from "sub.pvrplay.fun").
     */
    fun extractBaseDomain(host: String): String {
        val clean = host.lowercase().removePrefix("www.").trim()
        if (clean.isBlank()) return ""
        val parts = clean.split(".")
        if (parts.size <= 2) return clean
        val lastTwo = "${parts[parts.size - 2]}.${parts.last()}"
        val twoPartTLDs = setOf("co.uk", "com.pk", "org.pk", "net.pk", "co.in", "com.br", "pages.dev")
        return if (twoPartTLDs.contains(lastTwo) && parts.size >= 3) {
            "${parts[parts.size - 3]}.$lastTwo"
        } else {
            lastTwo
        }
    }

    /**
     * Checks if host1 and host2 belong to the same base domain or if one is a subdomain of the other.
     * Essential for preventing 503 errors on main site scripts.
     */
    fun isSameOrSubdomain(host1: String, host2: String): Boolean {
        if (host1.isBlank() || host2.isBlank()) return false
        val h1 = host1.lowercase().removePrefix("www.").trim()
        val h2 = host2.lowercase().removePrefix("www.").trim()
        if (h1 == h2) return true
        if (h1.endsWith(".$h2") || h2.endsWith(".$h1")) return true
        val b1 = extractBaseDomain(h1)
        val b2 = extractBaseDomain(h2)
        return b1.isNotBlank() && b1 == b2
    }

    /**
     * Checks if a request corresponds to a valid video stream, playlist, segment, or video host.
     * CRITICAL RULE: Video playback requests MUST NEVER BE BLOCKED.
     */
    fun isMediaOrStreamRequest(uri: Uri): Boolean {
        return try {
            val url = uri.toString().lowercase()
            val path = uri.path?.lowercase() ?: ""
            val host = uri.host?.lowercase() ?: ""

            // 1. Media streaming extensions, playlists, chunks, keys, and segments
            if (url.contains(".m3u8") || url.contains(".mp4") || url.contains(".ts") ||
                url.contains(".webm") || url.contains(".mpd") || url.contains(".mkv") ||
                url.contains(".m4s") || url.contains(".m4a") || url.contains(".aac") ||
                url.contains(".flv") || url.contains(".key") ||
                path.endsWith(".m3u8") || path.endsWith(".mp4") || path.endsWith(".ts") ||
                path.endsWith(".webm") || path.endsWith(".mpd") || path.endsWith(".m4s") ||
                path.endsWith(".key") || path.contains("/hls/") || path.contains("/dash/") ||
                path.contains("/stream/") || path.contains("playlist") || path.contains("chunklist") ||
                url.contains("video/") || url.contains("media/") || path.contains("/seg-") ||
                path.contains("segment-") || path.contains("frag-")
            ) {
                return true
            }

            // 2. Video host domains (like *.stream, *.sb, *.buzz, *.xyz, *.fun, or video host CDN suffixes)
            if (host.endsWith(".stream") || host.endsWith(".sb") || host.endsWith(".buzz") ||
                host.endsWith(".xyz") || host.contains("stream") || host.contains("player") ||
                host.contains("video") || host.contains("embed") || host.contains("media") ||
                host.contains("pvrplay") || (host.endsWith(".fun") && path.contains("video"))
            ) {
                return true
            }

            // 3. Known streaming video CDNs
            for (keyword in videoCdnKeywordsSet) {
                if (host.contains(keyword)) return true
            }

            // 4. Video player core libraries and scripts (JWPlayer, VideoJS, HLS.js, Dash.js, Plyr, Clappr, Shaka, Artplayer)
            if (path.contains("player.js") || path.contains("video.js") || path.contains("hls.") ||
                path.contains("dash.") || path.contains("plyr") || path.contains("jwplayer") ||
                path.contains("clappr") || path.contains("shaka") || path.contains("artplayer")
            ) {
                return true
            }

            false
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Checks if a host is an allowed primary site
     */
    fun isAllowedPrimaryDomain(host: String): Boolean {
        if (host.isBlank()) return false
        val cleanHost = host.lowercase().removePrefix("www.")
        return primaryStreamingDomainsSet.any { cleanHost == it || cleanHost.endsWith(".$it") }
    }

    /**
     * Checks if a domain/url is a known ad or spam tracking host
     */
    fun isBlockedDomain(url: String, host: String): Boolean {
        if (host.isBlank()) return false
        val cleanHost = host.lowercase().removePrefix("www.")
        val lowerUrl = url.lowercase()

        for (domain in blockedDomainsSet) {
            if (cleanHost == domain || cleanHost.endsWith(".$domain") || cleanHost.contains(domain) || lowerUrl.contains(domain)) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if a network resource is a third-party ad/tracker resource
     */
    fun isThirdPartyAdDomain(host: String, uri: Uri): Boolean {
        return try {
            val cleanHost = host.lowercase().removePrefix("www.")
            val path = uri.path?.lowercase() ?: ""
            val query = uri.query?.lowercase() ?: ""

            // Check against blocked hosts
            for (adHost in adResourceHostsSet) {
                if (cleanHost == adHost || cleanHost.endsWith(".$adHost") || cleanHost.contains(adHost)) return true
            }
            for (blocked in blockedDomainsSet) {
                if (cleanHost == blocked || cleanHost.endsWith(".$blocked") || cleanHost.contains(blocked)) return true
            }

            // Check ad paths
            if (path.contains("/ad/") || path.contains("/ads/") ||
                path.contains("/banner/") || path.contains("/popunder") ||
                path.contains("/ads.js") || path.contains("prebid") ||
                path.contains("adsystem") || path.contains("/adservice") ||
                path.contains("taboola") || path.contains("outbrain")
            ) {
                return true
            }

            // Check ad query params
            if (query.contains("ad_unit") || query.contains("click_id") || query.contains("popunder")) {
                return true
            }

            false
        } catch (_: Throwable) {
            false
        }
    }
}
