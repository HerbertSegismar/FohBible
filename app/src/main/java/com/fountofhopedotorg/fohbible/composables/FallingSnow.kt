package com.fountofhopedotorg.fohbible.composables

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun FallingSnow(number: Int = 80) {
    var showSnow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000)
        showSnow = true
    }

    if (!showSnow) return

    val snowflakes = remember(number) {
        List(number) { generateSnowflakeData() }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current

        // Guard against zero or unbounded container
        if (!maxWidth.isFinite || !maxHeight.isFinite ||
            maxWidth == 0.dp || maxHeight == 0.dp) return@BoxWithConstraints

        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        snowflakes.forEach { data ->
            val infiniteTransition = rememberInfiniteTransition()
            val progress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (data.speedSec * 1000).toInt(),
                        delayMillis = (data.delaySec * 1000).toInt()
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )

            val sizePx = with(density) { data.sizeDp.toPx() }
            val startY = -sizePx
            val endY = containerHeightPx + sizePx
            val yOffset = startY + (endY - startY) * progress
            val xOffset = containerWidthPx * data.leftPercent / 100f

            if (xOffset.isNaN() || yOffset.isNaN()) return@forEach

            Box(
                modifier = Modifier
                    .offset { IntOffset(xOffset.roundToInt(), yOffset.roundToInt()) }
                    .size(data.sizeDp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .alpha(data.opacity)
                    .blur(radius = data.blurDp)
                    .zIndex(data.zIndex)
            )
        }
    }
}

// ---------- SnowflakeData and generator (unchanged) ----------
private data class SnowflakeData(
    val sizeDp: Dp,
    val speedSec: Float,
    val opacity: Float,
    val blurDp: Dp,
    val leftPercent: Float,
    val delaySec: Float,
    val zIndex: Float
)

private fun generateSnowflakeData(): SnowflakeData {
    val layer = Random.nextInt(4)
    val (sizeDp, speedSec, opacity, blurDp) = when (layer) {
        0 -> { val size = Random.nextFloat() * 5f + 3f; val speed = Random.nextFloat() * 3f + 2f; val opac = Random.nextFloat() * 0.9f + 0.1f; val blur = 1f; listOf(size, speed, opac, blur) }
        1 -> { val size = Random.nextFloat() * 4f + 2f; val speed = Random.nextFloat() * 5f + 4f; val opac = Random.nextFloat() * 0.7f + 0.3f; val blur = 0.5f; listOf(size, speed, opac, blur) }
        2 -> { val size = Random.nextFloat() * 3f + 1f; val speed = Random.nextFloat() * 7f + 6f; val opac = Random.nextFloat() * 0.5f + 0.2f; val blur = 0.3f; listOf(size, speed, opac, blur) }
        else -> { val size = Random.nextFloat() * 2f + 0.5f; val speed = Random.nextFloat() * 10f + 8f; val opac = Random.nextFloat() * 0.3f + 0.1f; val blur = 0.2f; listOf(size, speed, opac, blur) }
    }
    return SnowflakeData(
        sizeDp = sizeDp.dp, speedSec = speedSec, opacity = opacity,
        blurDp = blurDp.dp, leftPercent = Random.nextFloat() * 100f,
        delaySec = Random.nextFloat() * 5f, zIndex = 30f - layer * 5f
    )
}