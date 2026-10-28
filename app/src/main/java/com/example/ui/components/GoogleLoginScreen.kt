package com.example.ui.components

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.remote.FirebaseChatManager
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

/**
 * Firebase Google Sign-In Screen
 * Integrates Jetpack Credential Manager and Firebase Authentication.
 */
@Composable
fun GoogleLoginScreen(
    activity: ComponentActivity,
    onLoginSuccess: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // Check if user already has an active Firebase session
    LaunchedEffect(Unit) {
        val existingUser = FirebaseChatManager.getCurrentUser()
        if (existingUser != null) {
            onLoginSuccess(existingUser)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val pulseGlow1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "login_pulse_1"
    )
    val pulseGlow2 by infiniteTransition.animateFloat(
        initialValue = 1.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "login_pulse_2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("google_login_screen")
    ) {
        // Vibrant Cyberpunk Animated Ambient Glowing Orbs
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(360.dp)
                .scale(pulseGlow1)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00F5D4).copy(alpha = 0.22f),
                            Color(0xFF00BBF9).copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(380.dp)
                .scale(pulseGlow2)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF007F).copy(alpha = 0.18f),
                            Color(0xFF9D4EDD).copy(alpha = 0.09f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(320.dp)
                .scale(pulseGlow1)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.15f),
                            Color(0xFF00F5FF).copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))

                // 3D Glowing App Emblem with Specular Rim
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .shadow(16.dp, CircleShape, ambientColor = NeonCyan, spotColor = NeonViolet)
                        .clip(CircleShape)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.White,
                                    NeonCyan,
                                    NeonViolet,
                                    NeonPink,
                                    Color(0xFFFF9900),
                                    Color.White
                                )
                            ),
                            shape = CircleShape
                        )
                        .background(ObsidianBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo),
                        contentDescription = "AnyWatch Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title & Subtitle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AnyWatch",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonCyan.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, NeonCyan)
                    ) {
                        Text(
                            text = "PRO",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Sign in to unlock streaming catalogs & live chat",
                    color = TextSecondary,
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Premium Feature Highlights Glassmorphic Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianCard.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FeatureRow(
                            icon = Icons.Default.Security,
                            iconTint = NeonCyan,
                            title = "Shield 3.0 Protection",
                            description = "Full auto-redirect and popup blocking for 100% safe playback."
                        )
                        FeatureRow(
                            icon = Icons.Default.Forum,
                            iconTint = NeonViolet,
                            title = "Real-Time Community Chat",
                            description = "Discuss episodes & share screenshots live with streamers via Firestore."
                        )
                        FeatureRow(
                            icon = Icons.Default.Speed,
                            iconTint = NeonPink,
                            title = "Instant 1080p Buffering",
                            description = "Zero-lag video engines across Movies, Anime, Manga & Novels."
                        )
                        FeatureRow(
                            icon = Icons.Default.ElectricBolt,
                            iconTint = Color(0xFFFFB300),
                            title = "Zero-Lag 120Hz Engine",
                            description = "Hardware-accelerated fluid micro-interactions and gestures."
                        )
                        FeatureRow(
                            icon = Icons.Default.CloudDone,
                            iconTint = Color(0xFF10B981),
                            title = "Firebase Cloud Auth",
                            description = "Official Google Identity protection with persistent sessions."
                        )
                        FeatureRow(
                            icon = Icons.Default.PlayCircleOutline,
                            iconTint = Color(0xFF38BDF8),
                            title = "Multi-Source Streaming",
                            description = "Comprehensive high-speed catalogs including PVRPlay, Atlantic & more."
                        )
                    }
                }
            }

            // Bottom Section: Real Native Google Sign-In Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val buttonInteraction = remember { MutableInteractionSource() }
                val isButtonPressed by buttonInteraction.collectIsPressedAsState()
                val buttonScale by animateFloatAsState(
                    targetValue = if (isButtonPressed) 0.95f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "google_button_scale"
                )

                Surface(
                    modifier = Modifier
                        .scale(buttonScale)
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = NeonCyan, spotColor = Color.White)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(interactionSource = buttonInteraction, indication = null) {
                            if (!isLoading) {
                                isLoading = true
                                scope.launch {
                                    try {
                                        val credentialManager = CredentialManager.create(context)
                                        val serverClientId = try {
                                            context.getString(R.string.default_web_client_id)
                                        } catch (_: Throwable) {
                                            "655094330390-web.apps.googleusercontent.com"
                                        }

                                        val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                                            .build()

                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(googleIdOption)
                                            .build()

                                        val result = credentialManager.getCredential(
                                            context = activity,
                                            request = request
                                        )

                                        val credential = result.credential
                                        if (credential is CustomCredential &&
                                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                        ) {
                                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                            val idToken = googleIdTokenCredential.idToken

                                            val signInResult = FirebaseChatManager.signInWithGoogleIdToken(idToken)
                                            if (signInResult.isSuccess) {
                                                val user = signInResult.getOrNull() ?: FirebaseChatManager.getCurrentUser()
                                                if (user != null) {
                                                    isLoading = false
                                                    onLoginSuccess(user)
                                                    return@launch
                                                }
                                            } else {
                                                val errorMsg = signInResult.exceptionOrNull()?.localizedMessage ?: "Firebase Sign-in failed"
                                                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            // Direct fallback if custom credential is used
                                            val currentUser = FirebaseChatManager.getCurrentUser()
                                            if (currentUser != null) {
                                                isLoading = false
                                                onLoginSuccess(currentUser)
                                                return@launch
                                            }
                                        }
                                        isLoading = false
                                    } catch (e: Throwable) {
                                        isLoading = false
                                        val msg = e.localizedMessage ?: "Google Sign-In canceled or unavailable"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        .testTag("continue_with_google_button"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = ObsidianBlack,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Connecting with Google...",
                                color = ObsidianBlack,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Official Google Logo",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Continue with Google",
                                color = Color(0xFF1F1F1F),
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Firebase Cloud Secured • Google Sign-In & Firestore",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(10.dp),
            color = iconTint.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, iconTint.copy(alpha = 0.35f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
