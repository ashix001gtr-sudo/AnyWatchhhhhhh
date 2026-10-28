package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TelegramBlue

@Composable
fun TelegramOfficialLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    url: String = "https://t.me/Ashix019"
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "telegram_scale")

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
            .testTag("telegram_official_button"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val radius = this.size.minDimension / 2f
            // Blue circle background
            drawCircle(
                color = TelegramBlue,
                radius = radius,
                center = center
            )

            // Official Telegram paper plane path
            val w = this.size.width
            val h = this.size.height

            val planePath = Path().apply {
                moveTo(w * 0.24f, h * 0.50f)
                lineTo(w * 0.74f, h * 0.28f)
                lineTo(w * 0.62f, h * 0.76f)
                lineTo(w * 0.47f, h * 0.62f)
                lineTo(w * 0.39f, h * 0.68f)
                lineTo(w * 0.40f, h * 0.56f)
                lineTo(w * 0.68f, h * 0.35f)
                lineTo(w * 0.33f, h * 0.54f)
                close()
            }

            drawPath(
                path = planePath,
                color = Color.White,
                style = Fill
            )
        }
    }
}
