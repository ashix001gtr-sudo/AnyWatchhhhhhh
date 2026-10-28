package com.example.data.model

import java.io.File

data class QuickSite(
    val id: String,
    val name: String,
    val url: String,
    val category: String, // "Anime", "Manga", "Movies", "Novel"
    val badge: String,
    val isAdult: Boolean = false,
    val color: String? = null,
    val isCustom: Boolean = false
)

object DefaultStreamingSites {
    val movies: List<QuickSite> = listOf(
        QuickSite("mov_pv", "PVRPlay", "https://ghar-baithe-free-movies-dekho-bina-rukawat-pvrplay-hd.pvrplay.fun/", "Movies", "PV"),
        QuickSite("mov_mv", "Movy", "https://www.movy.sx/", "Movies", "MV"),
        QuickSite("mov_at", "Atlantic", "https://atlantic.st/", "Movies", "AT"),
        QuickSite("mov_cj", "CineJoy", "https://cinejoy.pk/", "Movies", "CJ"),
        QuickSite("mov_7m", "7Movies", "https://7movies.ac", "Movies", "7M"),
        QuickSite("mov_mt", "Meow tv", "https://meowtv.ru/", "Movies", "MT"),
        QuickSite("mov_sn", "Skyflixer", "https://skyflixer.fun/browse", "Movies", "SN"),
        QuickSite("mov_ns", "Netout", "https://netout.pages.dev/home", "Movies", "NS")
    )

    val anime: List<QuickSite> = listOf(
        // The 15 New Sources
        QuickSite("ani_xa", "XAnime", "https://xanime.me/", "Anime", "XA"),
        QuickSite("ani_az", "AniZone", "https://anizone.to/", "Anime", "AZ"),
        QuickSite("ani_gt", "Guts", "https://guts.to/home", "Anime", "GT"),
        QuickSite("ani_an", "Anichi", "https://anichi.to/home", "Anime", "AN"),
        QuickSite("ani_ap", "AnimeParadise", "https://www.animeparadise.moe/", "Anime", "AP"),
        QuickSite("ani_9i", "9ime", "https://9ime.live/home", "Anime", "9I"),
        QuickSite("ani_kz", "Kazora", "https://kazora.cc/", "Anime", "KZ"),
        QuickSite("ani_bs", "BarelyStarted", "https://barelystarted.miruro.tv/", "Anime", "BS"),
        QuickSite("ani_gk", "Gotaku", "https://gotaku.to/", "Anime", "GK"),
        QuickSite("ani_av", "Anv", "https://anv.to/", "Anime", "AV"),
        QuickSite("ani_ao", "AnimeOnsen", "https://www.animeonsen.xyz/", "Anime", "AO"),
        QuickSite("ani_ka", "Kaa", "https://kaa.lt/", "Anime", "KA"),
        QuickSite("ani_as", "AniSnatch", "https://anisnatch.to/home", "Anime", "AS"),
        QuickSite("ani_se", "Senshi", "https://senshi.to/", "Anime", "SE"),
        QuickSite("ani_zx", "Zanix", "https://zanix.tr/home", "Anime", "ZX"),
        // Established Anime Sources
        QuickSite("ani_hi", "HiAnime", "https://ani.pm/", "Anime", "HI"),
        QuickSite("ani_ra", "RareAnimes", "https://www.rareanimes.mov/", "Anime", "RA"),
        QuickSite("ani_ba", "BlakiteAnime", "https://blakiteanime.buzz", "Anime", "BA"),
        QuickSite("ani_sl", "AnimeSalt", "https://animesalt.ro/", "Anime", "SL"),
        QuickSite("ani_tm", "ToonStream", "https://toonstream.us", "Anime", "TM"),
        QuickSite("ani_ye", "Yenime", "https://yenime.net", "Anime", "YE"),
        QuickSite("ani_re", "ReAnime", "https://reanime.to", "Anime", "RE")
    )

    val manga: List<QuickSite> = listOf(
        QuickSite("manga_mf", "MangaFire", "https://mangafire.to/", "Manga", "MF"),
        QuickSite("manga_mg", "MangaGo", "https://www.mangago.zone/", "Manga", "MG"),
        QuickSite("manga_ae", "Atsu", "https://atsu.moe/", "Manga", "AE"),
        QuickSite("manga_mb", "MangaBuddy", "https://mangabuddy1.co.uk/home", "Manga", "MB"),
        QuickSite("manga_cs", "ComicK", "https://comick.io", "Manga", "CS"),
        QuickSite("manga_ml", "MangaLivre", "https://mangalivre.net", "Manga", "ML"),
        QuickSite("manga_wl", "WeebCentral", "https://weebcentral.com", "Manga", "WL")
    )

    val novel: List<QuickSite> = listOf(
        QuickSite("nov_ka", "Kartoons", "https://kartoons.to/home", "Novel", "KA"),
        QuickSite("nov_tx", "Toonix", "https://toonix.bond/", "Novel", "TX"),
        QuickSite("nov_nf", "NovelFire", "https://novelfire.net/", "Novel", "NF"),
        QuickSite("nov_nx", "NovelDex", "https://noveldex.io", "Novel", "NX"),
        QuickSite("nov_ne", "NovelArchive", "https://novelarchive.cc", "Novel", "NE"),
        QuickSite("nov_ny", "NovelBuddy", "https://novelbuddy.me/home", "Novel", "NY")
    )

    val all: List<QuickSite> = movies + anime + manga + novel
}

data class HistoryItem(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isBookmark: Boolean = false
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadTask(
    val id: String,
    val title: String,
    val url: String,
    val fileName: String,
    val file: File? = null,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val errorMessage: String? = null,
    val mimeType: String? = "video/mp4",
    val referer: String? = null,
    val userAgent: String? = null,
    val cookie: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedSpeed: String
        get() = when {
            speedBytesPerSec > 1024 * 1024 -> String.format("%.1f MB/s", speedBytesPerSec / (1024.0 * 1024.0))
            speedBytesPerSec > 1024 -> String.format("%.0f KB/s", speedBytesPerSec / 1024.0)
            speedBytesPerSec > 0 -> "$speedBytesPerSec B/s"
            else -> "0 KB/s"
        }

    val formattedEta: String
        get() = when {
            etaSeconds <= 0 -> "--"
            etaSeconds < 60 -> "${etaSeconds}s left"
            etaSeconds < 3600 -> "${etaSeconds / 60}m ${etaSeconds % 60}s left"
            else -> "${etaSeconds / 3600}h ${(etaSeconds % 3600) / 60}m left"
        }

    val formattedProgressSize: String
        get() {
            val downMb = downloadedBytes / (1024.0 * 1024.0)
            return if (totalBytes > 0) {
                val totalMb = totalBytes / (1024.0 * 1024.0)
                String.format("%.1f / %.1f MB", downMb, totalMb)
            } else {
                String.format("%.1f MB", downMb)
            }
        }
}
