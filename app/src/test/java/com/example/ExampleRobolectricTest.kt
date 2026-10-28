package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CommunityChatRepository
import com.example.data.model.AdminConstants
import com.example.data.model.DefaultStreamingSites
import com.example.webview.PopupShieldClient
import com.example.webview.ShieldDomainManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AnyWatch", appName)
  }

  @Test
  fun `repository toggles settings and bookmarks correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.local.AppRepository(context)

    repo.set18PlusEnabled(true)
    assertEquals(true, repo.is18PlusEnabled.value)

    repo.set18PlusEnabled(false)
    assertEquals(false, repo.is18PlusEnabled.value)

    repo.setDesktopMode(true)
    assertEquals(true, repo.desktopMode.value)

    repo.setDesktopMode(false)
    assertEquals(false, repo.desktopMode.value)

    val bookmarked = repo.toggleBookmark("HiAnime", "https://ani.pm/")
    assertEquals(true, bookmarked)
    assertEquals(true, repo.isBookmarked("https://ani.pm/"))

    repo.addHistory("RareAnimes", "https://www.rareanimes.mov/")
    assertEquals("RareAnimes", repo.history.value.first().title)
  }

  @Test
  fun `download task telemetry formatting`() {
    val task = com.example.data.model.DownloadTask(
      id = "1",
      title = "Anime Ep 1",
      url = "https://example.com/stream.mp4",
      fileName = "Anime_Ep_1.mp4",
      totalBytes = 100 * 1024 * 1024,
      downloadedBytes = 50 * 1024 * 1024,
      speedBytesPerSec = 2500000,
      etaSeconds = 20
    )

    assertEquals(0.5f, task.progress, 0.01f)
    assertEquals("2.4 MB/s", task.formattedSpeed)
    assertEquals("20s left", task.formattedEta)
  }

  @Test
  fun `repository whitelists and tracks blocked ad counter`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.local.AppRepository(context)

    repo.resetBlockedCount()
    assertEquals(0, repo.blockedAdsCount.value)

    repo.incrementBlockedCount()
    assertEquals(1, repo.blockedAdsCount.value)

    repo.setDomainWhitelisted("meowtv.ru", true)
    assertEquals(true, repo.isDomainWhitelisted("meowtv.ru"))

    repo.setDomainWhitelisted("meowtv.ru", false)
    assertEquals(false, repo.isDomainWhitelisted("meowtv.ru"))
  }

  @Test
  fun `first party protection prevents 503 shield errors`() {
    assertTrue(ShieldDomainManager.isSameOrSubdomain("ani.pm", "ani.pm"))
    assertTrue(ShieldDomainManager.isSameOrSubdomain("cdn.ani.pm", "ani.pm"))
    assertTrue(ShieldDomainManager.isSameOrSubdomain("ghar-baithe-free-movies-dekho-bina-rukawat-pvrplay-hd.pvrplay.fun", "pvrplay.fun"))
    assertTrue(ShieldDomainManager.isSameOrSubdomain("sub.cinejoy.pk", "cinejoy.pk"))
    assertFalse(ShieldDomainManager.isSameOrSubdomain("adsterra.com", "ani.pm"))
    assertFalse(ShieldDomainManager.isSameOrSubdomain("onclickprediction.com", "pvrplay.fun"))

    // Third-party ad identification
    assertTrue(ShieldDomainManager.isThirdPartyAdDomain("doubleclick.net", Uri.parse("https://doubleclick.net/pagead/ads.js")))
    assertTrue(ShieldDomainManager.isThirdPartyAdDomain("adsterra.com", Uri.parse("https://adsterra.com/tag.js")))
    assertFalse(ShieldDomainManager.isThirdPartyAdDomain("ani.pm", Uri.parse("https://ani.pm/assets/app.js")))
  }

  @Test
  fun `desktop mode and mobile user agents are configured`() {
    assertTrue(PopupShieldClient.DESKTOP_USER_AGENT.contains("Windows NT 10.0"))
    assertTrue(PopupShieldClient.DESKTOP_USER_AGENT.contains("537.36"))
    assertTrue(PopupShieldClient.MOBILE_USER_AGENT.contains("Android"))
  }

  @Test
  fun `video streams and cdn hosts are whitelisted by shield`() {
    assertTrue(ShieldDomainManager.isMediaOrStreamRequest(Uri.parse("https://megacloud.tv/stream/master.m3u8")))
    assertTrue(ShieldDomainManager.isMediaOrStreamRequest(Uri.parse("https://cdn.example.xyz/video/1080p.mp4")))
    assertTrue(ShieldDomainManager.isMediaOrStreamRequest(Uri.parse("https://server.stream/video/hls/seg-1.ts")))
    assertTrue(ShieldDomainManager.isMediaOrStreamRequest(Uri.parse("https://player.buzz/embed/hls.js")))

    // Verify MangaPlaza is removed and MangaKakalot is removed
    assertFalse(DefaultStreamingSites.manga.any { it.name == "MangaPlaza" || it.badge == "MP" })
    assertFalse(DefaultStreamingSites.manga.any { it.name.contains("MangaKakalot", ignoreCase = true) })

    // Verify new anime sources (15 additions)
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "XA" && it.url == "https://xanime.me/" })
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "AZ" && it.url == "https://anizone.to/" })
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "GT" && it.url == "https://guts.to/home" })
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "AN" && it.url == "https://anichi.to/home" })
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "AP" && it.url == "https://www.animeparadise.moe/" })
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "9I" && it.url == "https://9ime.live/home" })
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "KZ" && it.url == "https://kazora.cc/" })
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "BS" && it.url == "https://barelystarted.miruro.tv/" })
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "GK" && it.url == "https://gotaku.to/" })
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "AV" && it.url == "https://anv.to/" })
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "AO" && it.url == "https://www.animeonsen.xyz/" })
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "KA" && it.url == "https://kaa.lt/" })
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "AS" && it.url == "https://anisnatch.to/home" })
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "SE" && it.url == "https://senshi.to/" })
    assertTrue(DefaultStreamingSites.anime.any { it.badge == "ZX" && it.url == "https://zanix.tr/home" })

    // Verify PVRPlay addition
    assertTrue(DefaultStreamingSites.movies.any { it.badge == "PV" && it.name == "PVRPlay" && it.url == "https://ghar-baithe-free-movies-dekho-bina-rukawat-pvrplay-hd.pvrplay.fun/" })
    assertTrue(ShieldDomainManager.isAllowedPrimaryDomain("pvrplay.fun"))
    assertTrue(ShieldDomainManager.isAllowedPrimaryDomain("ghar-baithe-free-movies-dekho-bina-rukawat-pvrplay-hd.pvrplay.fun"))

    // Verify new movie sources (Atlantic, CineJoy, 7Movies, Movy) + preserved (Netout, MeowTV, Skyflixer)
    assertTrue(DefaultStreamingSites.movies.any { it.badge == "AT" && it.url == "https://atlantic.st/" })
    assertTrue(DefaultStreamingSites.movies.any { it.badge == "CJ" && it.url == "https://cinejoy.pk/" })
    assertTrue(DefaultStreamingSites.movies.any { it.badge == "7M" && it.url == "https://7movies.ac" })
    assertTrue(DefaultStreamingSites.movies.any { it.badge == "MV" && it.url == "https://www.movy.sx/" })
    assertTrue(DefaultStreamingSites.movies.any { it.badge == "MT" && it.url == "https://meowtv.ru/" })
    assertTrue(DefaultStreamingSites.movies.any { it.badge == "SN" && it.url == "https://skyflixer.fun/browse" })
    assertTrue(DefaultStreamingSites.movies.any { it.badge == "NS" && it.url == "https://netout.pages.dev/home" })
  }

  @Test
  fun `whitelisting base domain bypasses shield for subdomains`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.local.AppRepository(context)

    repo.setDomainWhitelisted("pvrplay.fun", true)
    assertTrue(repo.isDomainWhitelisted("pvrplay.fun"))
    assertTrue(repo.isDomainWhitelisted("ghar-baithe-free-movies-dekho-bina-rukawat-pvrplay-hd.pvrplay.fun"))
    assertTrue(repo.isDomainWhitelisted("cdn.pvrplay.fun"))

    repo.setDomainWhitelisted("pvrplay.fun", false)
  }

  @Test
  fun `mandatory google login profile persistence and signout`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.local.AppRepository(context)

    repo.signOut()
    assertEquals(null, repo.currentUser.value)

    val profile = com.example.data.model.UserProfile(
      uid = "usr_12345",
      displayName = "Ashix",
      email = "ashix001gtr@gmail.com",
      photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
    )

    repo.saveUserProfile(profile)
    val saved = repo.currentUser.value
    assertTrue(saved != null)
    assertEquals("Ashix", saved?.displayName)
    assertEquals("ashix001gtr@gmail.com", saved?.email)
    assertEquals("usr_12345", saved?.uid)

    repo.signOut()
    assertEquals(null, repo.currentUser.value)
  }

  @Test
  fun `community chat channels, sending messages, and replies`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val chatRepo = com.example.data.local.CommunityChatRepository(context)

    assertTrue(chatRepo.channels.isNotEmpty())
    val generalChannel = chatRepo.channels.first { it.id == "general" }
    assertEquals("General Hub", generalChannel.name)

    val testUser = com.example.data.model.UserProfile(
      uid = "tester_1",
      displayName = "Ashix Streamer",
      email = "ashix001gtr@gmail.com"
    )

    val initialCount = chatRepo.getChannelMessages("general").size
    chatRepo.sendMessage(
      sender = testUser,
      channelId = "general",
      text = "Testing community chat stream with 1080p!",
      replyTo = null
    )

    val updatedMessages = chatRepo.getChannelMessages("general")
    assertEquals(initialCount + 1, updatedMessages.size)
    val latest = updatedMessages.last()
    assertEquals("Ashix Streamer", latest.senderName)
    assertEquals("Testing community chat stream with 1080p!", latest.text)

    // Test delete own message
    chatRepo.deleteMessage(latest.id, testUser.uid)
    val afterDelete = chatRepo.getChannelMessages("general")
    assertEquals(initialCount, afterDelete.size)
  }

  @Test
  fun `chat image limit enforces 5 photos maximum`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val chatRepo = com.example.data.local.CommunityChatRepository(context)
    val userUid = "limit_test_user_${System.currentTimeMillis()}"

    assertTrue(chatRepo.canSendImage(userUid))
    assertEquals(0, chatRepo.getSentImageCount(userUid))

    for (i in 1..5) {
      chatRepo.incrementSentImageCount(userUid)
    }

    assertEquals(5, chatRepo.getSentImageCount(userUid))
    assertFalse(chatRepo.canSendImage(userUid))
  }

  @Test
  fun `profile custom display name update`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.local.AppRepository(context)

    val profile = com.example.data.model.UserProfile(
      uid = "usr_custom_name",
      displayName = "Old Google Name",
      email = "ashix001gtr@gmail.com"
    )
    repo.saveUserProfile(profile)
    assertEquals("Old Google Name", repo.currentUser.value?.displayName)

    repo.updateDisplayName("Custom Ashix PRO")
    assertEquals("Custom Ashix PRO", repo.currentUser.value?.displayName)
  }

  @Test
  fun `admin unblock user and email privacy verification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val chatRepo = CommunityChatRepository(context)

    val targetUid = "troublemaker_user_404"
    val adminUid = "admin_user_01"

    // Block user
    chatRepo.blockUser(targetUid, adminUid)
    assertTrue(chatRepo.isUserBlocked(targetUid))
    assertTrue(chatRepo.blockedUsers.value.contains(targetUid))

    // Unblock user
    chatRepo.unblockUser(targetUid)
    assertFalse(chatRepo.isUserBlocked(targetUid))
    assertFalse(chatRepo.blockedUsers.value.contains(targetUid))

    // Verify Admin email privacy
    assertTrue(AdminConstants.isAdminEmail("rm9208946@gmail.com"))
    assertTrue(AdminConstants.isAdminEmail("  RM9208946@GMAIL.COM  "))
    assertFalse(AdminConstants.isAdminEmail("ashix001gtr@gmail.com"))
    assertFalse(AdminConstants.isAdminEmail("other_user@gmail.com"))
  }
}
