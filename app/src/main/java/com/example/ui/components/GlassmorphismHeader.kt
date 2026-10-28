package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Warning
import com.example.data.model.UserProfile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.DefaultStreamingSites
import com.example.data.model.QuickSite
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.PurpleToPinkBrush
import com.example.ui.theme.CyanToBlueBrush
import com.example.ui.theme.NeonSunsetBrush
import com.example.ui.theme.CyberpunkHeaderGlow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GlassmorphismHeader(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    currentUrl: String,
    onQuickSiteSelected: (QuickSite) -> Unit,
    onWebsiteFixClick: () -> Unit,
    onOpenDownloads: () -> Unit,
    onRefresh: () -> Unit,
    onToggleRotation: () -> Unit,
    onBrowseAll: () -> Unit,
    isShieldActive: Boolean = true,
    onOpenShields: () -> Unit = {},
    onToggleShield: () -> Unit = {},
    is18PlusActive: Boolean = false,
    onToggle18Plus: () -> Unit = {},
    isDesktopMode: Boolean = false,
    onToggleDesktopMode: () -> Unit = {},
    currentUser: UserProfile? = null,
    onOpenProfile: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories = listOf("Anime", "Manga", "Novel", "Movies")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xF0120E26), Color(0xEB0A0818))))
            .testTag("glassmorphic_header")
    ) {
        // TOP APP BAR AREA: Sleek Metallic AW PRO Logo / User Avatar + Action Icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val profileInteraction = remember { MutableInteractionSource() }
            val isProfilePressed by profileInteraction.collectIsPressedAsState()
            val profileScale by animateFloatAsState(
                targetValue = if (isProfilePressed) 0.94f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "profile_scale"
            )

            // MODULE 4: Top-left header icon displays user's uploaded profile picture when logged in
            Box(modifier = Modifier.scale(profileScale)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(interactionSource = profileInteraction, indication = null) { onOpenProfile() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("profile_aw_menu_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(8.dp, CircleShape, ambientColor = NeonCyan, spotColor = NeonViolet)
                            .clip(CircleShape)
                            .border(
                                width = 1.8.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFFFFFFFF),
                                        NeonCyan,
                                        NeonViolet,
                                        NeonPink,
                                        Color(0xFFFF9900),
                                        Color(0xFFFFFFFF)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .background(ObsidianBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentUser?.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = currentUser?.photoUrl,
                                contentDescription = "User Profile Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_app_logo),
                                contentDescription = "AnyWatch Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "AnyWatch",
                                color = Color.White,
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.4.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Metallic Neon "PRO" Pill
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NeonCyan.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.8f))
                            ) {
                                Text(
                                    "PRO",
                                    color = NeonCyan,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.6.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            "HYBRID STREAM ENGINE",
                            color = NeonCyan.copy(alpha = 0.85f),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.1.sp
                        )
                    }
                }
            }

            // RIGHT ACTIONS: COMMUNITY CHAT ICON + ANYWATCH SHIELD ICON
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // REAL-TIME COMMUNITY CHAT ICON BUTTON
                val chatInteraction = remember { MutableInteractionSource() }
                val isChatPressed by chatInteraction.collectIsPressedAsState()
                val chatScale by animateFloatAsState(
                    targetValue = if (isChatPressed) 0.90f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "chat_scale"
                )

                Box(
                    modifier = Modifier
                        .scale(chatScale)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ObsidianCard)
                        .border(1.2.dp, NeonCyan.copy(alpha = 0.6f), CircleShape)
                        .clickable(interactionSource = chatInteraction, indication = null) { onOpenChat() }
                        .testTag("header_community_chat_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = "Community Chat",
                        tint = NeonCyan,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // GLOBAL STREAM SEARCH BUTTON
                val searchInteraction = remember { MutableInteractionSource() }
                val isSearchPressed by searchInteraction.collectIsPressedAsState()
                val searchScale by animateFloatAsState(
                    targetValue = if (isSearchPressed) 0.90f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "search_scale"
                )

                Box(
                    modifier = Modifier
                        .scale(searchScale)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ObsidianCard)
                        .border(1.2.dp, NeonCyan.copy(alpha = 0.6f), CircleShape)
                        .clickable(interactionSource = searchInteraction, indication = null) { onOpenSearch() }
                        .testTag("header_search_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Global Search",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // ANYWATCH POPUP & AD SHIELD BUTTON
                val shieldInteraction = remember { MutableInteractionSource() }
                val isShieldPressed by shieldInteraction.collectIsPressedAsState()
                val shieldScale by animateFloatAsState(
                    targetValue = if (isShieldPressed) 0.90f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "shield_scale"
                )

                Box(
                    modifier = Modifier
                        .scale(shieldScale)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isShieldActive) NeonCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            1.2.dp,
                            if (isShieldActive) NeonCyan.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.12f),
                            CircleShape
                        )
                        .clickable(interactionSource = shieldInteraction, indication = null) { onOpenShields() }
                        .testTag("header_shield_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "AnyWatch Shield",
                        tint = if (isShieldActive) NeonCyan else TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // ROW 1: STREAM CATEGORY TABS (History removed as per previous update)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                val tabInteraction = remember { MutableInteractionSource() }
                val isTabPressed by tabInteraction.collectIsPressedAsState()
                val tabScale by animateFloatAsState(
                    targetValue = if (isTabPressed) 0.92f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "tab_scale_$category"
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color.Transparent else Color(0x301E1838),
                    border = if (isSelected) {
                        BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFFF007F))))
                    } else {
                        BorderStroke(1.dp, Color(0x35A855F7))
                    },
                    modifier = Modifier
                        .scale(tabScale)
                        .shadow(
                            elevation = if (isSelected) 8.dp else 0.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = if (isSelected) Color(0xFFFF007F) else Color.Transparent,
                            spotColor = if (isSelected) Color(0xFF00F5D4) else Color.Transparent
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .then(
                            if (isSelected) {
                                when (category) {
                                    "Anime" -> Modifier.background(CyanToBlueBrush)
                                    "Manga" -> Modifier.background(PurpleToPinkBrush)
                                    "Novel" -> Modifier.background(NeonSunsetBrush)
                                    else -> Modifier.background(PurpleToPinkBrush)
                                }
                            } else Modifier
                        )
                        .clickable(interactionSource = tabInteraction, indication = null) { onCategorySelected(category) }
                        .testTag("category_tab_$category")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }

        // ROW 2: VIBRANT CYBERPUNK WEBSITE PILLS WITH NEON GLOW & SMOOTH ANIMATIONS
        AnimatedContent(
            targetState = selectedCategory,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                 slideInHorizontally(
                     animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
                 ) { width -> width / 4 })
                .togetherWith(
                    fadeOut(animationSpec = tween(150)) +
                    slideOutHorizontally(animationSpec = tween(150)) { width -> -width / 4 }
                )
            },
            label = "row2_animated_tab_transition"
        ) { currentCat ->
            val list = remember(currentCat) {
                when (currentCat) {
                    "Anime" -> DefaultStreamingSites.anime
                    "Manga" -> DefaultStreamingSites.manga
                    "Novel" -> DefaultStreamingSites.novel
                    "Movies" -> DefaultStreamingSites.movies
                    else -> emptyList()
                }
            }

            if (list.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    list.forEach { site ->
                        val cleanSiteKey = site.url.removePrefix("https://").removePrefix("http://").take(12)
                        val isCurrentSite = currentUrl.contains(cleanSiteKey, ignoreCase = true)
                        val itemInteraction = remember { MutableInteractionSource() }
                        val isItemPressed by itemInteraction.collectIsPressedAsState()
                        val itemScale by animateFloatAsState(
                            targetValue = if (isItemPressed) 0.92f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "site_scale_${site.id}"
                        )

                        // Vibrant Cyberpunk Glowing Website Pill
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCurrentSite) Color(0x3500F5D4) else Color(0xDD15122B),
                            border = BorderStroke(
                                if (isCurrentSite) 1.5.dp else 1.dp,
                                if (isCurrentSite)
                                    Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFFF007F)))
                                else
                                    Brush.horizontalGradient(listOf(Color(0x3500F5D4), Color(0x35A855F7)))
                            ),
                            modifier = Modifier
                                .scale(itemScale)
                                .shadow(
                                    elevation = if (isCurrentSite) 10.dp else 2.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    ambientColor = if (isCurrentSite) Color(0xFF00F5D4) else Color.Transparent,
                                    spotColor = if (isCurrentSite) Color(0xFFFF007F) else Color.Transparent
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(interactionSource = itemInteraction, indication = null) { onQuickSiteSelected(site) }
                                .testTag("quick_site_${site.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Monogram Container with gradient fill when active
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .shadow(if (isCurrentSite) 6.dp else 2.dp, RoundedCornerShape(8.dp), ambientColor = Color(0xFF00F5D4))
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isCurrentSite)
                                                Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xFF00BBF9)))
                                            else
                                                Brush.linearGradient(listOf(Color(0xFF1E1A38), Color(0xFF141228)))
                                        )
                                        .border(
                                            0.9.dp,
                                            if (isCurrentSite) Color.White.copy(alpha = 0.8f) else Color(0x40A855F7),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = site.badge,
                                        color = if (isCurrentSite) Color(0xFF090812) else NeonCyan,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.4.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = site.name,
                                    color = if (isCurrentSite) Color.White else TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isCurrentSite) FontWeight.Black else FontWeight.Medium
                                )

                                if (isCurrentSite) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .shadow(5.dp, CircleShape, ambientColor = Color(0xFF00F5D4), spotColor = Color(0xFFFF007F))
                                            .clip(CircleShape)
                                            .background(Brush.radialGradient(listOf(Color.White, Color(0xFF00F5D4))))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // SUBTLE GLOWING CYBERPUNK BOTTOM BORDER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(CyberpunkHeaderGlow)
        )
    }
}
