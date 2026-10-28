package com.example.data.model

import java.net.URLEncoder

/**
 * Represents a streaming or reading source with its search URL template.
 */
data class SearchSource(
    val id: String,
    val name: String,
    val category: String, // "Anime", "Movies", "Manga", "Novel"
    val badge: String,
    val searchUrlPattern: String,
    val homeUrl: String
) {
    /**
     * Constructs the full direct search URL for this source given a user search query.
     */
    fun buildSearchUrl(query: String): String {
        val trimmed = query.trim()
        val encoded = try {
            URLEncoder.encode(trimmed, "UTF-8")
        } catch (_: Throwable) {
            trimmed.replace(" ", "+")
        }
        return if (searchUrlPattern.contains("%s")) {
            searchUrlPattern.replace("%s", encoded)
        } else {
            searchUrlPattern + encoded
        }
    }
}

/**
 * Global registry mapping every supported streaming and reading site to its specific search endpoint.
 */
object GlobalSearchRegistry {
    val searchSources: List<SearchSource> = listOf(
        // ANIME SOURCES
        SearchSource("ani_hi", "HiAnime", "Anime", "HI", "https://ani.pm/search?keyword=", "https://ani.pm/"),
        SearchSource("ani_xa", "XAnime", "Anime", "XA", "https://xanime.me/search?q=", "https://xanime.me/"),
        SearchSource("ani_az", "AniZone", "Anime", "AZ", "https://anizone.to/search?q=", "https://anizone.to/"),
        SearchSource("ani_gt", "Guts", "Anime", "GT", "https://guts.to/search?keyword=", "https://guts.to/home"),
        SearchSource("ani_9i", "9ime", "Anime", "9I", "https://9ime.live/search?keyword=", "https://9ime.live/home"),
        SearchSource("ani_kz", "Kazora", "Anime", "KZ", "https://kazora.cc/search?keyword=", "https://kazora.cc/"),
        SearchSource("ani_as", "AniSnatch", "Anime", "AS", "https://anisnatch.to/search?keyword=", "https://anisnatch.to/home"),
        SearchSource("ani_se", "Senshi", "Anime", "SE", "https://senshi.to/search?keyword=", "https://senshi.to/"),
        SearchSource("ani_sl", "AnimeSalt", "Anime", "SL", "https://animesalt.ro/search?q=", "https://animesalt.ro/"),
        SearchSource("ani_tm", "ToonStream", "Anime", "TM", "https://toonstream.us/?s=", "https://toonstream.us"),
        SearchSource("ani_ra", "RareAnimes", "Anime", "RA", "https://www.rareanimes.mov/?s=", "https://www.rareanimes.mov/"),
        SearchSource("ani_ba", "BlakiteAnime", "Anime", "BA", "https://blakiteanime.buzz/?s=", "https://blakiteanime.buzz"),
        SearchSource("ani_ap", "AnimeParadise", "Anime", "AP", "https://www.animeparadise.moe/search?q=", "https://www.animeparadise.moe/"),
        SearchSource("ani_ao", "AnimeOnsen", "Anime", "AO", "https://www.animeonsen.xyz/search?q=", "https://www.animeonsen.xyz/"),
        SearchSource("ani_ka", "Kaa", "Anime", "KA", "https://kaa.lt/search?keyword=", "https://kaa.lt/"),
        SearchSource("ani_zx", "Zanix", "Anime", "ZX", "https://zanix.tr/search?keyword=", "https://zanix.tr/home"),

        // MOVIES & SHOWS
        SearchSource("mov_pv", "PVRPlay", "Movies", "PV", "https://ghar-baithe-free-movies-dekho-bina-rukawat-pvrplay-hd.pvrplay.fun/search?q=", "https://ghar-baithe-free-movies-dekho-bina-rukawat-pvrplay-hd.pvrplay.fun/"),
        SearchSource("mov_mv", "Movy", "Movies", "MV", "https://www.movy.sx/search?q=", "https://www.movy.sx/"),
        SearchSource("mov_at", "Atlantic", "Movies", "AT", "https://atlantic.st/search?q=", "https://atlantic.st/"),
        SearchSource("mov_cj", "CineJoy", "Movies", "CJ", "https://cinejoy.pk/search?q=", "https://cinejoy.pk/"),
        SearchSource("mov_7m", "7Movies", "Movies", "7M", "https://7movies.ac/search?q=", "https://7movies.ac"),
        SearchSource("mov_sn", "Skyflixer", "Movies", "SN", "https://skyflixer.fun/browse?search=", "https://skyflixer.fun/browse"),
        SearchSource("mov_mt", "Meow tv", "Movies", "MT", "https://meowtv.ru/search?q=", "https://meowtv.ru/"),
        SearchSource("mov_ns", "Netout", "Movies", "NS", "https://netout.pages.dev/search?q=", "https://netout.pages.dev/home"),

        // MANGA & COMICS
        SearchSource("manga_mf", "MangaFire", "Manga", "MF", "https://mangafire.to/filter?keyword=", "https://mangafire.to/"),
        SearchSource("manga_mg", "MangaGo", "Manga", "MG", "https://www.mangago.zone/r/l_search/?name=", "https://www.mangago.zone/"),
        SearchSource("manga_cs", "ComicK", "Manga", "CS", "https://comick.io/search?q=", "https://comick.io"),
        SearchSource("manga_mb", "MangaBuddy", "Manga", "MB", "https://mangabuddy1.co.uk/search?q=", "https://mangabuddy1.co.uk/home"),
        SearchSource("manga_wl", "WeebCentral", "Manga", "WL", "https://weebcentral.com/search?keyword=", "https://weebcentral.com"),
        SearchSource("manga_ae", "Atsu", "Manga", "AE", "https://atsu.moe/search?q=", "https://atsu.moe/"),
        SearchSource("manga_ml", "MangaLivre", "Manga", "ML", "https://mangalivre.net/search?q=", "https://mangalivre.net"),

        // NOVELS & LIGHT NOVELS (Including new Kartoons KA & Toonix TX)
        SearchSource("nov_ka", "Kartoons", "Novel", "KA", "https://kartoons.to/search?q=", "https://kartoons.to/home"),
        SearchSource("nov_tx", "Toonix", "Novel", "TX", "https://toonix.bond/search?q=", "https://toonix.bond/"),
        SearchSource("nov_nf", "NovelFire", "Novel", "NF", "https://novelfire.net/search?keyword=", "https://novelfire.net/"),
        SearchSource("nov_nx", "NovelDex", "Novel", "NX", "https://noveldex.io/search?q=", "https://noveldex.io"),
        SearchSource("nov_ne", "NovelArchive", "Novel", "NE", "https://novelarchive.cc/search?q=", "https://novelarchive.cc"),
        SearchSource("nov_ny", "NovelBuddy", "Novel", "NY", "https://novelbuddy.me/search?keyword=", "https://novelbuddy.me/home")
    )
}
