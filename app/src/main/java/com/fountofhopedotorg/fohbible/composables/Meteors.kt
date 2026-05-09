package com.fountofhopedotorg.fohbible.composables

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import kotlin.math.atan2
import kotlin.random.Random

@Composable
fun Meteors(number: Int = 8) {
    var showMeteors by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        showMeteors = true
    }

    if (!showMeteors) return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current

        if (!maxWidth.isFinite || !maxHeight.isFinite || maxWidth == 0.dp || maxHeight == 0.dp)
            return@BoxWithConstraints

        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        repeat(number) {
            var meteorData by remember { mutableStateOf(generateRealisticMeteorData()) }

            val infiniteTransition = rememberInfiniteTransition(label = "meteor_transition_$it")
            val progress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (meteorData.animationDurationSec * 1000).toInt(),
                        delayMillis = (meteorData.animationDelaySec * 1000).toInt(),
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "meteor_progress_$it"
            )
            LaunchedEffect(progress == 0f) {
                if (progress == 0f) {
                    meteorData = generateRealisticMeteorData()
                }
            }

            val startX = screenWidthPx * meteorData.startHorizontalPercent / 100f
            val startY = -with(density) { meteorData.trailLengthDp.toPx() }
            val endX = startX - (screenWidthPx * meteorData.horizontalShiftPercent / 100f)
            val endY = screenHeightPx + with(density) { meteorData.trailLengthDp.toPx() }

            val currentX = startX + (endX - startX) * progress
            val currentY = startY + (endY - startY) * progress

            if (currentX.isNaN() || currentY.isNaN()) return@repeat

            val rotationDeg = Math.toDegrees(
                atan2((endY - startY).toDouble(), (endX - startX).toDouble())
            ).toFloat()

            val opacityCurve = if (progress < 0.15f) {
                progress / 0.15f
            } else {
                1f - ((progress - 0.15f) / 0.85f)
            }
            val finalAlpha = meteorData.baseOpacity * opacityCurve

            val glowRadiusPx = with(density) { meteorData.glowRadiusDp.toPx() }
            val coreRadiusPx = with(density) { meteorData.coreRadiusDp.toPx() }
            val trailLengthPx = with(density) { meteorData.trailLengthDp.toPx() }

            val containerWidthPx = trailLengthPx + glowRadiusPx
            val containerHeightPx = glowRadiusPx * 2f

            Box(
                modifier = Modifier
                    .size(
                        width = with(density) { containerWidthPx.toDp() },
                        height = with(density) { containerHeightPx.toDp() }
                    )
                    .graphicsLayer {
                        translationX = currentX
                        translationY = currentY
                        rotationZ = rotationDeg

                        val pivotX = (containerWidthPx - glowRadiusPx) / containerWidthPx
                        transformOrigin = TransformOrigin(pivotX, 0.5f)
                        alpha = finalAlpha
                    }
                    .drawBehind {
                        val centerY = size.height / 2f
                        val headCenterX = size.width - glowRadiusPx

                        val tailTip = Offset(0f, centerY)
                        val headTop = Offset(headCenterX, centerY - coreRadiusPx * 0.8f)
                        val headBottom = Offset(headCenterX, centerY + coreRadiusPx * 0.8f)

                        val trailPath = Path().apply {
                            moveTo(headTop.x, headTop.y)
                            lineTo(tailTip.x, tailTip.y)
                            lineTo(headBottom.x, headBottom.y)
                            close()
                        }

                        val tailBrush = Brush.linearGradient(
                            start = Offset(headCenterX, centerY),
                            end = tailTip,
                            colors = listOf(meteorData.meteorColor.copy(alpha = 0.8f), Color.Transparent)
                        )
                        drawPath(trailPath, brush = tailBrush)

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    meteorData.meteorColor.copy(alpha = 0.6f),
                                    Color.Transparent
                                ),
                                center = Offset(headCenterX, centerY),
                                radius = glowRadiusPx
                            ),
                            radius = glowRadiusPx,
                            center = Offset(headCenterX, centerY)
                        )

                        drawCircle(
                            color = Color.White,
                            radius = coreRadiusPx,
                            center = Offset(headCenterX, centerY)
                        )
                    }
            )
        }
    }
}

private data class MeteorData(
    val startHorizontalPercent: Float,
    val horizontalShiftPercent: Float,
    val coreRadiusDp: Dp,
    val glowRadiusDp: Dp,
    val trailLengthDp: Dp,
    val baseOpacity: Float,
    val animationDurationSec: Float,
    val animationDelaySec: Float,
    val meteorColor: Color
)

private fun generateRealisticMeteorData(): MeteorData {
    val startHorizontal = Random.nextFloat() * -100f - 20f
    val shift = -300f

    val coreRadiusDp = (Random.nextFloat() * 1f + 0.5f).dp
    val glowRadiusDp = coreRadiusDp * (Random.nextFloat() * 3f + 4f)
    val trailLengthDp = (Random.nextFloat() * 150f + 80f).dp

    val baseOpacity = Random.nextFloat() * 0.4f + 0.6f

    val duration = Random.nextFloat() * 10f + 5f
    val delay = Random.nextFloat() * 6f

    val meteorColors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFE0FFFF),
        Color(0xFFB0E0E6),
        Color(0xFFFFFACD),
        Color(0xFFFFE4B5)
    )
    val meteorColor = meteorColors[Random.nextInt(meteorColors.size)]

    return MeteorData(
        startHorizontalPercent = startHorizontal,
        horizontalShiftPercent = shift,
        coreRadiusDp = coreRadiusDp,
        glowRadiusDp = glowRadiusDp,
        trailLengthDp = trailLengthDp,
        baseOpacity = baseOpacity,
        animationDurationSec = duration,
        animationDelaySec = delay,
        meteorColor = meteorColor
    )
}