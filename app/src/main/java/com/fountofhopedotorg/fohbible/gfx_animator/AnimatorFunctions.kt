package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.ui.graphics.Color
import com.fountofhopedotorg.fohbible.data.CanvasKeyframe


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