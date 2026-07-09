package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.fountofhopedotorg.fohbible.data.CanvasKeyframe
import com.fountofhopedotorg.fohbible.data.EasingPoint
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

fun ease(
    t: Float,
    type: TweenType,
    customPoints: List<EasingPoint> = emptyList()
): Float = when (type) {
    TweenType.LINEAR -> t
    TweenType.EASE_IN -> t * t
    TweenType.EASE_OUT -> t * (2 - t)
    TweenType.EASE_IN_OUT -> if (t < 0.5f) 2 * t * t else -1 + (4 - 2 * t) * t
    TweenType.CUSTOM -> evaluateCustomEasing(customPoints, t)
}

fun evaluateCustomEasing(points: List<EasingPoint>, x: Float): Float {
    if (points.isEmpty()) return x
    val sorted = points.sortedBy { it.x }
    if (x <= sorted.first().x) return sorted.first().y
    if (x >= sorted.last().x) return sorted.last().y

    for (i in 1 until sorted.size) {
        val prev = sorted[i - 1]
        val curr = sorted[i]
        if (x >= prev.x && x <= curr.x) {
            val segmentX0 = prev.x
            val segmentX1 = curr.x
            val dx = segmentX1 - segmentX0
            if (dx == 0f) return prev.y

            val cp1x = prev.x + prev.handleOut.x
            val cp1y = prev.y + prev.handleOut.y
            val cp2x = curr.x + curr.handleIn.x
            val cp2y = curr.y + curr.handleIn.y

            val t = findCubicTForX(prev.x, cp1x, cp2x, curr.x, x, prev.x, curr.x)

            return evaluateCubicBezier(prev.y, cp1y, cp2y, curr.y, t).coerceIn(0f, 1f)
        }
    }
    return x
}

private fun findCubicTForX(
    p0: Float, p1: Float, p2: Float, p3: Float,
    targetX: Float, xMin: Float, xMax: Float
): Float {
    var t = ((targetX - xMin) / (xMax - xMin)).coerceIn(0f, 1f)
    repeat(8) {
        val x = cubicBezierValue(p0, p1, p2, p3, t)
        val d = cubicBezierDerivative(p0, p1, p2, p3, t)
        if (d == 0f) return@repeat
        t -= (x - targetX) / d
        t = t.coerceIn(0f, 1f)
    }
    return t
}

private fun cubicBezierValue(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
    val u = 1 - t
    return u*u*u * p0 + 3 * u*u * t * p1 + 3 * u * t*t * p2 + t*t*t * p3
}

private fun cubicBezierDerivative(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
    val u = 1 - t
    return 3*u*u * (p1 - p0) + 6*u*t * (p2 - p1) + 3*t*t * (p3 - p2)
}

private fun evaluateCubicBezier(y0: Float, y1: Float, y2: Float, y3: Float, t: Float) =
    cubicBezierValue(y0, y1, y2, y3, t)