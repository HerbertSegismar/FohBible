package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.fountofhopedotorg.fohbible.data.CanvasKeyframe
import com.fountofhopedotorg.fohbible.data.GradientConfig
import com.fountofhopedotorg.fohbible.data.TweenType
import java.util.Locale
import kotlin.math.roundToInt


fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}

fun findSurroundingKeyframes(
    keyframes: List<CanvasKeyframe>,
    currentMs: Long
): Pair<CanvasKeyframe?, CanvasKeyframe?> {
    if (keyframes.isEmpty()) return null to null
    if (currentMs <= keyframes.first().timestampMs) return keyframes.first() to keyframes.first()
    if (currentMs >= keyframes.last().timestampMs) return keyframes.last() to keyframes.last()
    for (i in 0 until keyframes.size - 1) {
        if (currentMs in keyframes[i].timestampMs..keyframes[i + 1].timestampMs) {
            return keyframes[i] to keyframes[i + 1]
        }
    }
    return keyframes.last() to keyframes.last()
}

fun lerpGradient(a: GradientConfig, b: GradientConfig, fraction: Float): GradientConfig {
    return GradientConfig(
        startColor = lerpColor(a.startColor, b.startColor, fraction),
        endColor = lerpColor(a.endColor, b.endColor, fraction),
        startOffset = Offset(
            lerp(a.startOffset.x, b.startOffset.x, fraction),
            lerp(a.startOffset.y, b.startOffset.y, fraction)
        ),
        endOffset = Offset(
            lerp(a.endOffset.x, b.endOffset.x, fraction),
            lerp(a.endOffset.y, b.endOffset.y, fraction)
        )
    )
}

fun formatPosition(value: Float): String = value.roundToInt().toString()
fun formatScale(value: Float): String = String.format(Locale.US, "%.2f", value)
fun formatRotation(value: Float): String = String.format(Locale.US, "%.1f", value)

fun ease(t: Float, type: TweenType): Float = when (type) {
    TweenType.LINEAR -> t
    TweenType.EASE_IN -> t * t
    TweenType.EASE_OUT -> t * (2 - t)
    TweenType.EASE_IN_OUT -> if (t < 0.5f) 2 * t * t else -1 + (4 - 2 * t) * t
}