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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun SimpleMeteors(number: Int = 5) {
    var showMeteors by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200)
        showMeteors = true
    }

    if (!showMeteors) return

    val meteors = remember(number) {
        List(number) { generateMeteorData() }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current

        if (!maxWidth.isFinite || !maxHeight.isFinite || maxWidth == 0.dp || maxHeight == 0.dp)
            return@BoxWithConstraints

        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        meteors.forEach { meteor ->
            val meteorSizePx = with(density) { meteor.sizeDp.toPx() }

            val infiniteTransition = rememberInfiniteTransition()
            val progress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (meteor.animationDurationSec * 1000).toInt(),
                        delayMillis = (meteor.animationDelaySec * 1000).toInt()
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )

            val startX = screenWidthPx * meteor.startLeftPercent / 100f
            val startY = -meteorSizePx
            val endX = startX + screenWidthPx * meteor.horizontalShiftPercent / 100f
            val endY = screenHeightPx + meteorSizePx

            val currentX = startX + (endX - startX) * progress
            val currentY = startY + (endY - startY) * progress

            if (currentX.isNaN() || currentY.isNaN()) return@forEach

            // Meteor body + trail container
            Box(
                modifier = Modifier
                    .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                    .rotate(meteor.rotationDeg)
                    .size(meteor.sizeDp)
                    .graphicsLayer { alpha = meteor.opacity }
            ) {
                // Trail – longer and more opaque
                Box(
                    modifier = Modifier
                        .offset(x = -(meteor.sizeDp * 3), y = -meteor.sizeDp / 2)
                        .size(width = meteor.sizeDp * 3, height = 2.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFF5DEB3).copy(alpha = 0.9f), // wheat gold
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Meteor head
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            clip = false,
                            spotColor = Color.White.copy(alpha = 0.5f),
                            ambientColor = Color.White.copy(alpha = 0.5f)
                        )
                        .background(Color.White.copy(alpha = 0.95f))
                )
            }
        }
    }
}

private data class MeteorData(
    val startLeftPercent: Float,
    val horizontalShiftPercent: Float,
    val sizeDp: Dp,
    val opacity: Float,
    val rotationDeg: Float,
    val animationDurationSec: Float,
    val animationDelaySec: Float
)

private fun generateMeteorData(): MeteorData {
    val startLeft = Random.nextFloat() * 100f
    val shift = -(Random.nextFloat() * 80f)          // move left-upwards
    val sizeDp = (Random.nextFloat() * 6f + 3f).dp   // 3‑9 dp (much larger)
    val opacity = Random.nextFloat() * 0.4f + 0.6f   // 0.6‑1.0
    val rotation = Random.nextFloat() * 30f + 210f   // typical meteor angle
    val duration = Random.nextFloat() * 8f + 8f
    val delay = Random.nextFloat() * 2f

    return MeteorData(
        startLeftPercent = startLeft,
        horizontalShiftPercent = shift,
        sizeDp = sizeDp,
        opacity = opacity,
        rotationDeg = rotation,
        animationDurationSec = duration,
        animationDelaySec = delay
    )
}