package com.fountofhopedotorg.fohbible.app_composables

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun FallingSnow(number: Int = 70) {
    var showSnow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500.milliseconds)
        showSnow = true
    }

    if (!showSnow) return

    val snowflakes = remember(number) {
        List(number) { generateSnowflakeData() }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val containerHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

        if (containerWidthPx <= 0 || containerHeightPx <= 0) return@BoxWithConstraints

        snowflakes.forEach { data ->
            val infiniteTransition = rememberInfiniteTransition(label = "snow")

            val progress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(data.durationMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ), label = "y"
            )

            val wobble by infiniteTransition.animateFloat(
                initialValue = -1f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(data.wobbleDuration, easing = SineWaveEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "x"
            )

            Box(
                modifier = Modifier
                    .size(data.sizeDp)
                    .zIndex(data.zIndex)
                    .graphicsLayer {
                        val xPos = (data.leftPercent * containerWidthPx) + (wobble * data.wobbleMaxPx)
                        val yPos = (progress * (containerHeightPx + 100f)) - 50f

                        translationX = xPos
                        translationY = yPos
                        alpha = data.opacity

                        rotationZ = progress * data.rotationSpeed
                    }
                    .blur(data.blurDp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color.White.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
        }
    }
}

val SineWaveEasing = Easing { fraction ->
    sin(fraction * PI.toFloat() * 0.5f)
}

private data class SnowflakeData(
    val sizeDp: Dp,
    val durationMillis: Int,
    val opacity: Float,
    val blurDp: Dp,
    val leftPercent: Float,
    val zIndex: Float,
    val wobbleMaxPx: Float,
    val wobbleDuration: Int,
    val rotationSpeed: Float
)

private fun generateSnowflakeData(): SnowflakeData {
    val layer = Random.nextInt(0, 3)

    return when (layer) {
        0 -> SnowflakeData(
            sizeDp = Random.nextInt(6, 11).dp,
            durationMillis = Random.nextInt(3000, 5001),
            opacity = Random.nextFloat() * 0.9f + 0.4f,
            blurDp = Random.nextInt(2, 5).dp,
            leftPercent = Random.nextFloat(),
            zIndex = 10f,
            wobbleMaxPx = Random.nextInt(40, 81).toFloat(),
            wobbleDuration = Random.nextInt(2000, 4001),
            rotationSpeed = Random.nextInt(90, 181).toFloat()
        )
        1 -> SnowflakeData(
            sizeDp = Random.nextInt(3, 6).dp,
            durationMillis = Random.nextInt(6000, 9001),
            opacity = Random.nextFloat() * 0.9f  + 0.6f,
            blurDp = 1.dp,
            leftPercent = Random.nextFloat(),
            zIndex = 5f,
            wobbleMaxPx = Random.nextInt(20, 41).toFloat(),
            wobbleDuration = Random.nextInt(3000, 5001),
            rotationSpeed = Random.nextInt(45, 91).toFloat()
        )
        else -> SnowflakeData(
            sizeDp = Random.nextInt(1, 3).dp,
            durationMillis = Random.nextInt(10000, 15001),
            opacity = Random.nextFloat() + 0.3f,
            blurDp = 0.dp,
            leftPercent = Random.nextFloat(),
            zIndex = 1f,
            wobbleMaxPx = Random.nextInt(10, 21).toFloat(),
            wobbleDuration = Random.nextInt(5000, 7001),
            rotationSpeed = 30f
        )
    }
}