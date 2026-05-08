package com.fountofhopedotorg.fohbible.composables

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import kotlin.math.*
import kotlin.random.Random

@Composable
fun FloatingOrbs(number: Int = 3) {
    var showOrbs by remember { mutableStateOf(false) }
    var fadeIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000)
        showOrbs = true
        kotlinx.coroutines.delay(50)
        fadeIn = true
    }

    if (!showOrbs) return

    val orbs = remember(number) {
        List(number) { index -> generateOrbData(index) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current

        if (!maxWidth.isFinite || !maxHeight.isFinite || maxWidth == 0.dp || maxHeight == 0.dp)
            return@BoxWithConstraints

        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        val globalAlpha by animateFloatAsState(
            targetValue = if (fadeIn) 1f else 0f,
            animationSpec = tween(durationMillis = 800)
        )

        orbs.forEach { orb ->
            val initialX = containerWidthPx * orb.leftPercent / 100f
            val initialY = containerHeightPx * orb.topPercent / 100f

            val infiniteTransition = rememberInfiniteTransition()
            val animatedOffset = when (orb.animationType) {
                AnimationType.VERTICAL -> {
                    val amplitude = containerHeightPx * 0.4f
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = (2 * PI).toFloat(),
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = (orb.durationSec * 1000).toInt()),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                    Offset(0f, amplitude * sin(angle.toDouble()).toFloat())
                }
                AnimationType.HORIZONTAL -> {
                    val amplitude = containerWidthPx * 0.4f
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = (2 * PI).toFloat(),
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = (orb.durationSec * 1000).toInt()),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                    Offset(amplitude * sin(angle.toDouble()).toFloat(), 0f)
                }
                AnimationType.CIRCULAR -> {
                    val radius = minOf(containerWidthPx, containerHeightPx) * 0.3f
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = (2 * PI).toFloat(),
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = (orb.durationSec * 1000).toInt()),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                    Offset(
                        radius * cos(angle.toDouble()).toFloat(),
                        radius * sin(angle.toDouble()).toFloat() * (if (orb.reverseDirection) -1f else 1f)
                    )
                }
            }

            val finalX = initialX + animatedOffset.x
            val finalY = initialY + animatedOffset.y
            if (finalX.isNaN() || finalY.isNaN()) return@forEach

            Box(
                modifier = Modifier
                    .offset { IntOffset(finalX.roundToInt(), finalY.roundToInt()) }
                    .size(orb.sizeDp)
                    .blur(orb.blurDp)                     // blur kept moderate
                    .clip(CircleShape)
                    .shadow(
                        elevation = orb.sizeDp / 8,       // softer shadow
                        shape = CircleShape,
                        clip = false,
                        spotColor = orb.shadowColor,
                        ambientColor = orb.shadowColor
                    )
                    .graphicsLayer {
                        alpha = globalAlpha * orb.finalAlpha
                    }
            )
        }
    }
}

private enum class AnimationType { VERTICAL, HORIZONTAL, CIRCULAR }

private data class OrbData(
    val leftPercent: Float,
    val topPercent: Float,
    val sizeDp: androidx.compose.ui.unit.Dp,
    val blurDp: androidx.compose.ui.unit.Dp,
    val shadowColor: Color,
    val finalAlpha: Float,
    val animationType: AnimationType,
    val durationSec: Float,
    val reverseDirection: Boolean
)

private fun generateOrbData(index: Int): OrbData {
    val leftPercent = Random.nextFloat() * 100f
    val topPercent = Random.nextFloat() * 100f

    // Brighter, slightly smaller orbs – 80‑100 dp, blur reduced to 4‑8 dp
    val sizeDp = (Random.nextInt(80, 101)).dp
    val blurDp = (Random.nextInt(4, 9)).dp

    // More vivid colors with stronger alpha
    val colors = listOf(
        Color(0xFF96BEFF),   // bright blue
        Color(0xFFFFB598),   // warm peach
        Color(0xFF6DF1C1)    // mint green
    )
    val shadowColor = colors[index % 3].copy(alpha = 0.4f)
    val orbAlpha = Random.nextFloat() * 0.3f + 0.5f   // 0.5‑0.8 (more visible)

    val animationStyle = index % 5
    val animationType = when (animationStyle) {
        0 -> AnimationType.VERTICAL
        1 -> AnimationType.CIRCULAR
        2 -> AnimationType.CIRCULAR
        3 -> AnimationType.HORIZONTAL
        else -> AnimationType.CIRCULAR
    }
    val durationSec = when (animationStyle) {
        0 -> 25f + Random.nextFloat() * 10f
        1 -> 15f + Random.nextFloat() * 10f
        2 -> 30f + Random.nextFloat() * 15f
        3 -> 30f + Random.nextFloat() * 15f
        else -> 15f + Random.nextFloat() * 10f
    }
    val reverseDirection = animationStyle == 2

    return OrbData(
        leftPercent = leftPercent,
        topPercent = topPercent,
        sizeDp = sizeDp,
        blurDp = blurDp,
        shadowColor = shadowColor,
        finalAlpha = orbAlpha,
        animationType = animationType,
        durationSec = durationSec,
        reverseDirection = reverseDirection
    )
}