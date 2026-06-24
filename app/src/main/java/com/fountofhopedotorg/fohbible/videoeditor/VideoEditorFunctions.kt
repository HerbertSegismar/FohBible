package com.fountofhopedotorg.fohbible.videoeditor

import com.fountofhopedotorg.fohbible.data.CanvasKeyframe


fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

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