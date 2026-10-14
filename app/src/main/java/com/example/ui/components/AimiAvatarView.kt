package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.AssistantState
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun AimiAvatarView(
    assistantState: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    // Blinking logic: periodic random blink
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2500, 4500))
            isBlinking = true
            delay(160)
            isBlinking = false
        }
    }

    // Breathing / floating subtle bounce
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_motion")
    val gentleBounce by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gentle_bounce"
    )

    // Halo ring pulse
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )

    // Smooth mouth opening scale based on playback amplitude
    val targetMouthOpen = if (assistantState == AssistantState.SPEAKING) {
        (amplitude * 1.4f).coerceIn(0.15f, 1.0f)
    } else {
        0.0f
    }
    val animatedMouthOpen by animateFloatAsState(
        targetValue = targetMouthOpen,
        animationSpec = tween(durationMillis = 60, easing = FastOutSlowInEasing),
        label = "mouth_open_anim"
    )

    val activeHeadsetGlow = when (assistantState) {
        AssistantState.LISTENING -> Color(0xFF00E5FF) // Cyber Cyan
        AssistantState.TRANSCRIBING -> Color(0xFF76FF03) // Neon Lime
        AssistantState.THINKING -> Color(0xFFFFD600) // Golden Spark
        AssistantState.SPEAKING -> Color(0xFFFF4081) // Neon Pink
        AssistantState.HANDS_ACTION -> Color(0xFF00E5FF) // Cyber Cyan Hands Action
        AssistantState.ERROR -> Color(0xFFFF5252) // Soft Coral
        AssistantState.IDLE -> Color(0xFFB388FF) // Pastel Violet
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("aimi_avatar_view"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = this.size.width
            val canvasH = this.size.height
            val centerX = canvasW / 2f
            val centerY = canvasH / 2f + gentleBounce
            val radius = canvasW * 0.40f

            // 1. Outer Audio Responsive Glowing Rings
            val ringScale = if (assistantState == AssistantState.SPEAKING || assistantState == AssistantState.LISTENING) {
                1.0f + (amplitude * 0.28f)
            } else {
                haloPulse
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeHeadsetGlow.copy(alpha = 0.35f),
                        activeHeadsetGlow.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = radius * ringScale * 1.25f
                ),
                radius = radius * ringScale * 1.22f,
                center = Offset(centerX, centerY)
            )

            // Outer decorative circular tech ring
            drawCircle(
                color = activeHeadsetGlow.copy(alpha = 0.45f),
                radius = radius * 1.08f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.5f)
            )

            // 2. Headphone Arc Behind Head
            val arcRect = Path().apply {
                moveTo(centerX - radius * 0.85f, centerY)
                cubicTo(
                    centerX - radius * 0.85f, centerY - radius * 1.05f,
                    centerX + radius * 0.85f, centerY - radius * 1.05f,
                    centerX + radius * 0.85f, centerY
                )
            }
            drawPath(
                path = arcRect,
                color = Color(0xFF2C2C54),
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )

            // 3. Cat-Ear Cyber Accents
            val leftEar = Path().apply {
                moveTo(centerX - radius * 0.55f, centerY - radius * 0.65f)
                lineTo(centerX - radius * 0.70f, centerY - radius * 1.15f)
                lineTo(centerX - radius * 0.25f, centerY - radius * 0.82f)
                close()
            }
            drawPath(leftEar, color = Color(0xFF40407A))
            // Inner ear glow
            val leftInnerEar = Path().apply {
                moveTo(centerX - radius * 0.53f, centerY - radius * 0.70f)
                lineTo(centerX - radius * 0.64f, centerY - radius * 1.05f)
                lineTo(centerX - radius * 0.32f, centerY - radius * 0.83f)
                close()
            }
            drawPath(leftInnerEar, color = activeHeadsetGlow)

            val rightEar = Path().apply {
                moveTo(centerX + radius * 0.55f, centerY - radius * 0.65f)
                lineTo(centerX + radius * 0.70f, centerY - radius * 1.15f)
                lineTo(centerX + radius * 0.25f, centerY - radius * 0.82f)
                close()
            }
            drawPath(rightEar, color = Color(0xFF40407A))
            val rightInnerEar = Path().apply {
                moveTo(centerX + radius * 0.53f, centerY - radius * 0.70f)
                lineTo(centerX + radius * 0.64f, centerY - radius * 1.05f)
                lineTo(centerX + radius * 0.32f, centerY - radius * 0.83f)
                close()
            }
            drawPath(rightInnerEar, color = activeHeadsetGlow)

            // 4. Hair Base (Soft pastel lilac/lavender anime hair)
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE8D5FF), Color(0xFFD1B3FF), Color(0xFFB388FF)),
                    startY = centerY - radius,
                    endY = centerY + radius
                ),
                radius = radius * 0.88f,
                center = Offset(centerX, centerY)
            )

            // 5. Anime Face Shape (Soft porcelain tone)
            val faceCenterY = centerY + radius * 0.08f
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF9F5), Color(0xFFFFECE6)),
                    startY = faceCenterY - radius * 0.7f,
                    endY = faceCenterY + radius * 0.7f
                ),
                radius = radius * 0.68f,
                center = Offset(centerX, faceCenterY)
            )

            // 6. Anime Bangs & Hair Strands
            val bangsPath = Path().apply {
                moveTo(centerX - radius * 0.65f, faceCenterY - radius * 0.35f)
                // Left fringe
                cubicTo(
                    centerX - radius * 0.55f, faceCenterY - radius * 0.05f,
                    centerX - radius * 0.40f, faceCenterY - radius * 0.15f,
                    centerX - radius * 0.30f, faceCenterY - radius * 0.42f
                )
                // Center ahoge / bangs
                cubicTo(
                    centerX - radius * 0.15f, faceCenterY - radius * 0.05f,
                    centerX, faceCenterY - radius * 0.18f,
                    centerX + radius * 0.10f, faceCenterY - radius * 0.45f
                )
                // Right fringe
                cubicTo(
                    centerX + radius * 0.28f, faceCenterY - radius * 0.05f,
                    centerX + radius * 0.50f, faceCenterY - radius * 0.15f,
                    centerX + radius * 0.65f, faceCenterY - radius * 0.35f
                )
                // Hair hairline arch back up
                cubicTo(
                    centerX + radius * 0.45f, faceCenterY - radius * 0.75f,
                    centerX - radius * 0.45f, faceCenterY - radius * 0.75f,
                    centerX - radius * 0.65f, faceCenterY - radius * 0.35f
                )
                close()
            }
            drawPath(
                path = bangsPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE8D5FF), Color(0xFFC49BF0)),
                    startY = faceCenterY - radius * 0.7f,
                    endY = faceCenterY
                )
            )

            // 7. Headphone Ear Cups (Futuristic cyber pads)
            val leftCupX = centerX - radius * 0.76f
            val rightCupX = centerX + radius * 0.76f
            val cupY = faceCenterY - radius * 0.05f
            val cupW = radius * 0.22f
            val cupH = radius * 0.48f

            drawRoundRect(
                color = Color(0xFF2C2C54),
                topLeft = Offset(leftCupX - cupW / 2, cupY - cupH / 2),
                size = Size(cupW, cupH),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = activeHeadsetGlow,
                topLeft = Offset(leftCupX - cupW / 4, cupY - cupH / 3),
                size = Size(cupW / 2, cupH * 0.66f),
                cornerRadius = CornerRadius(8f, 8f)
            )

            drawRoundRect(
                color = Color(0xFF2C2C54),
                topLeft = Offset(rightCupX - cupW / 2, cupY - cupH / 2),
                size = Size(cupW, cupH),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = activeHeadsetGlow,
                topLeft = Offset(rightCupX - cupW / 4, cupY - cupH / 3),
                size = Size(cupW / 2, cupH * 0.66f),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Headset Microphone Boom
            val micPath = Path().apply {
                moveTo(leftCupX, cupY + cupH * 0.3f)
                cubicTo(
                    leftCupX + radius * 0.15f, cupY + cupH * 0.75f,
                    centerX - radius * 0.35f, faceCenterY + radius * 0.45f,
                    centerX - radius * 0.18f, faceCenterY + radius * 0.38f
                )
            }
            drawPath(
                path = micPath,
                color = Color(0xFF2C2C54),
                style = Stroke(width = 4.5f, cap = StrokeCap.Round)
            )
            drawCircle(
                color = activeHeadsetGlow,
                radius = 6.5f,
                center = Offset(centerX - radius * 0.18f, faceCenterY + radius * 0.38f)
            )

            // 8. Cheeks Blush (Kawaii pink aura)
            val blushLeftX = centerX - radius * 0.38f
            val blushRightX = centerX + radius * 0.38f
            val blushY = faceCenterY + radius * 0.12f

            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF4081).copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(blushLeftX, blushY),
                    radius = radius * 0.22f
                ),
                topLeft = Offset(blushLeftX - radius * 0.18f, blushY - radius * 0.08f),
                size = Size(radius * 0.36f, radius * 0.16f)
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF4081).copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(blushRightX, blushY),
                    radius = radius * 0.22f
                ),
                topLeft = Offset(blushRightX - radius * 0.18f, blushY - radius * 0.08f),
                size = Size(radius * 0.36f, radius * 0.16f)
            )

            // 9. Expressive Anime Eyes
            val eyeLeftX = centerX - radius * 0.24f
            val eyeRightX = centerX + radius * 0.24f
            val eyeY = faceCenterY - radius * 0.05f

            if (isBlinking) {
                // Closed happy winking arc during blink
                val leftBlink = Path().apply {
                    moveTo(eyeLeftX - radius * 0.13f, eyeY)
                    cubicTo(
                        eyeLeftX - radius * 0.06f, eyeY + radius * 0.07f,
                        eyeLeftX + radius * 0.06f, eyeY + radius * 0.07f,
                        eyeLeftX + radius * 0.13f, eyeY
                    )
                }
                drawPath(leftBlink, color = Color(0xFF2C2C54), style = Stroke(width = 4.5f, cap = StrokeCap.Round))

                val rightBlink = Path().apply {
                    moveTo(eyeRightX - radius * 0.13f, eyeY)
                    cubicTo(
                        eyeRightX - radius * 0.06f, eyeY + radius * 0.07f,
                        eyeRightX + radius * 0.06f, eyeY + radius * 0.07f,
                        eyeRightX + radius * 0.13f, eyeY
                    )
                }
                drawPath(rightBlink, color = Color(0xFF2C2C54), style = Stroke(width = 4.5f, cap = StrokeCap.Round))
            } else {
                // Open sparkling eyes
                val eyeWidth = radius * 0.22f
                val eyeHeight = radius * 0.32f

                // Eyeball gradient (Deep cosmic purple to vibrant violet)
                val irisBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF240046), Color(0xFF5A189A), Color(0xFF9D4EDD)),
                    startY = eyeY - eyeHeight / 2,
                    endY = eyeY + eyeHeight / 2
                )

                // Left eye
                drawOval(
                    brush = irisBrush,
                    topLeft = Offset(eyeLeftX - eyeWidth / 2, eyeY - eyeHeight / 2),
                    size = Size(eyeWidth, eyeHeight)
                )
                // Right eye
                drawOval(
                    brush = irisBrush,
                    topLeft = Offset(eyeRightX - eyeWidth / 2, eyeY - eyeHeight / 2),
                    size = Size(eyeWidth, eyeHeight)
                )

                // Eyelashes / Upper eyelid line
                val leftEyelid = Path().apply {
                    moveTo(eyeLeftX - eyeWidth * 0.7f, eyeY - eyeHeight * 0.45f)
                    cubicTo(
                        eyeLeftX - eyeWidth * 0.2f, eyeY - eyeHeight * 0.65f,
                        eyeLeftX + eyeWidth * 0.3f, eyeY - eyeHeight * 0.60f,
                        eyeLeftX + eyeWidth * 0.7f, eyeY - eyeHeight * 0.35f
                    )
                }
                drawPath(leftEyelid, color = Color(0xFF1E1E2F), style = Stroke(width = 5f, cap = StrokeCap.Round))

                val rightEyelid = Path().apply {
                    moveTo(eyeRightX - eyeWidth * 0.7f, eyeY - eyeHeight * 0.35f)
                    cubicTo(
                        eyeRightX - eyeWidth * 0.3f, eyeY - eyeHeight * 0.60f,
                        eyeRightX + eyeWidth * 0.2f, eyeY - eyeHeight * 0.65f,
                        eyeRightX + eyeWidth * 0.7f, eyeY - eyeHeight * 0.45f
                    )
                }
                drawPath(rightEyelid, color = Color(0xFF1E1E2F), style = Stroke(width = 5f, cap = StrokeCap.Round))

                // Big sparkling eye highlights (Primary reflection)
                drawCircle(
                    color = Color.White,
                    radius = eyeWidth * 0.24f,
                    center = Offset(eyeLeftX - eyeWidth * 0.16f, eyeY - eyeHeight * 0.20f)
                )
                drawCircle(
                    color = Color.White,
                    radius = eyeWidth * 0.24f,
                    center = Offset(eyeRightX - eyeWidth * 0.16f, eyeY - eyeHeight * 0.20f)
                )

                // Small secondary sparkle highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = eyeWidth * 0.12f,
                    center = Offset(eyeLeftX + eyeWidth * 0.18f, eyeY + eyeHeight * 0.16f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = eyeWidth * 0.12f,
                    center = Offset(eyeRightX + eyeWidth * 0.18f, eyeY + eyeHeight * 0.16f)
                )
            }

            // 10. Cute Little Anime Nose
            drawCircle(
                color = Color(0xFFFFAB91),
                radius = 2.2f,
                center = Offset(centerX, faceCenterY + radius * 0.14f)
            )

            // 11. Animated Anime Mouth
            val mouthY = faceCenterY + radius * 0.28f

            if (assistantState == AssistantState.SPEAKING && animatedMouthOpen > 0.05f) {
                // Open mouth synchronized with playback amplitude
                val mouthW = radius * (0.16f + animatedMouthOpen * 0.14f)
                val mouthH = radius * (0.08f + animatedMouthOpen * 0.24f)

                // Outer mouth cavity
                drawOval(
                    color = Color(0xFFD81B60),
                    topLeft = Offset(centerX - mouthW / 2, mouthY - mouthH / 2),
                    size = Size(mouthW, mouthH),
                    style = Fill
                )
                // Cute little tongue
                drawOval(
                    color = Color(0xFFFF80AB),
                    topLeft = Offset(centerX - mouthW * 0.35f, mouthY),
                    size = Size(mouthW * 0.7f, mouthH * 0.5f)
                )
                // Upper tooth shine
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(centerX - mouthW * 0.3f, mouthY - mouthH / 2 + 1f),
                    size = Size(mouthW * 0.6f, mouthH * 0.25f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            } else {
                // Sweet contented smile
                val smilePath = Path().apply {
                    moveTo(centerX - radius * 0.10f, mouthY)
                    cubicTo(
                        centerX - radius * 0.05f, mouthY + radius * 0.06f,
                        centerX + radius * 0.05f, mouthY + radius * 0.06f,
                        centerX + radius * 0.10f, mouthY
                    )
                }
                drawPath(
                    path = smilePath,
                    color = Color(0xFFC2185B),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )
            }

            // 12. Cute Hairclip Star Accent
            val starCenterX = centerX + radius * 0.42f
            val starCenterY = faceCenterY - radius * 0.48f
            val starR = radius * 0.10f
            val starPath = Path().apply {
                moveTo(starCenterX, starCenterY - starR)
                lineTo(starCenterX + starR * 0.3f, starCenterY - starR * 0.3f)
                lineTo(starCenterX + starR, starCenterY)
                lineTo(starCenterX + starR * 0.3f, starCenterY + starR * 0.3f)
                lineTo(starCenterX, starCenterY + starR)
                lineTo(starCenterX - starR * 0.3f, starCenterY + starR * 0.3f)
                lineTo(starCenterX - starR, starCenterY)
                lineTo(starCenterX - starR * 0.3f, starCenterY - starR * 0.3f)
                close()
            }
            drawPath(starPath, color = Color(0xFFFFD54F))
        }
    }
}
