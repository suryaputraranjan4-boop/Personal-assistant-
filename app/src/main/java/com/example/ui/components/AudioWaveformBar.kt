package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AudioWaveformBar(
    amplitude: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 12,
    height: Dp = 36.dp,
    barWidth: Dp = 4.dp,
    activeColorStart: Color = Color(0xFF6C5CE7),
    activeColorEnd: Color = Color(0xFFFF4081)
) {
    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val gradient = Brush.verticalGradient(
            colors = listOf(activeColorStart, activeColorEnd)
        )

        for (i in 0 until barCount) {
            // Distribute varying harmonic weights for animated equalizer feel
            val offsetWeight = (sin(i.toDouble() * 0.7) * 0.35 + 0.65).toFloat()
            val targetHeightFraction = (amplitude * offsetWeight).coerceIn(0.12f, 1.0f)

            val animatedHeight by animateFloatAsState(
                targetValue = targetHeightFraction,
                animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
                label = "bar_anim_$i"
            )

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(animatedHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(gradient)
            )
        }
    }
}
