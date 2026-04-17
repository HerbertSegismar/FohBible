package com.fountofhopedotorg.fohbible.orbs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlin.random.Random

class Orb {
    var x = 0f; var y = 0f
    var dx = 0f; var dy = 0f
    var baseRadius = 0f
    var hue = 0f; var hueSpeed = 0f
    var pulsePhase = 0f; var pulseSpeed = 0f
    var shapePhase = 0f; var shapeSpeed = 0f  // kept for potential subtle future use, but not applied to radius
    var alpha = 0f; var targetMaxAlpha = 0f; var alphaSpeed = 0f
    var state = 0
    var holdTimer = 0f

    fun spawn(width: Float, height: Float) {
        baseRadius = Random.nextFloat() * 150f + 200f
        x = Random.nextFloat() * width
        y = Random.nextFloat() * height

        dx = (Random.nextFloat() - 0.5f) * 60f
        dy = (Random.nextFloat() - 0.5f) * 60f

        hue = Random.nextFloat() * 360f
        hueSpeed = (Random.nextFloat() - 0.5f) * 10f
        pulsePhase = Random.nextFloat() * (2 * PI.toFloat())
        pulseSpeed = Random.nextFloat() * 1.5f + 0.5f
        shapePhase = Random.nextFloat() * (2 * PI.toFloat())
        shapeSpeed = Random.nextFloat() * 1.5f + 0.5f

        alpha = 0f
        targetMaxAlpha = Random.nextFloat() * 0.15f + 0.05f
        alphaSpeed = Random.nextFloat() * 0.05f + 0.02f
        holdTimer = Random.nextFloat() * 6f + 4f
        state = 1
    }

    fun update(dt: Float, width: Float, height: Float) {
        if (state == 0) {
            if (Random.nextFloat() < 0.01f) spawn(width, height)
            return
        }

        x += dx * dt
        y += dy * dt

        // Soft bounce
        if (x < 0 && dx < 0) dx *= -0.95f
        if (x > width && dx > 0) dx *= -0.95f
        if (y < 0 && dy < 0) dy *= -0.95f
        if (y > height && dy > 0) dy *= -0.95f

        hue = (hue + hueSpeed * dt) % 360f
        if (hue < 0) hue += 360f

        pulsePhase += pulseSpeed * dt
        shapePhase += shapeSpeed * dt

        when (state) {
            1 -> {
                alpha += alphaSpeed * dt
                if (alpha >= targetMaxAlpha) {
                    alpha = targetMaxAlpha
                    state = 2
                }
            }
            2 -> {
                holdTimer -= dt
                if (holdTimer <= 0f) state = 3
            }
            3 -> {
                alpha -= alphaSpeed * dt
                if (alpha <= 0f) {
                    alpha = 0f
                    state = 0
                }
            }
        }
    }
}

/**
 * Apply gentle repulsion and color blending when orbs are close.
 */
private fun applyInteractions(orbs: List<Orb>, dt: Float) {
    val interactionRadius = 350f
    val repulsionStrength = 80f
    val hueBlendFactor = 0.03f

    for (i in orbs.indices) {
        if (orbs[i].state == 0) continue
        for (j in i + 1 until orbs.size) {
            if (orbs[j].state == 0) continue
            val a = orbs[i]
            val b = orbs[j]

            val dxPos = b.x - a.x
            val dyPos = b.y - a.y
            val dist = sqrt(dxPos * dxPos + dyPos * dyPos)

            if (dist < interactionRadius && dist > 0.1f) {
                val force = (1f - dist / interactionRadius) * repulsionStrength * dt
                val fx = (dxPos / dist) * force
                val fy = (dyPos / dist) * force

                a.dx -= fx
                a.dy -= fy
                b.dx += fx
                b.dy += fy

                // Hue blending
                val hueDiff = b.hue - a.hue
                val normalizedDiff = when {
                    hueDiff > 180f -> hueDiff - 360f
                    hueDiff < -180f -> hueDiff + 360f
                    else -> hueDiff
                }
                val shift = normalizedDiff * hueBlendFactor * dt
                a.hue = (a.hue + shift) % 360f
                b.hue = (b.hue - shift) % 360f
                if (a.hue < 0) a.hue += 360f
                if (b.hue < 0) b.hue += 360f
            }
        }
    }
}

@Composable
fun FloatingOrbsBackground(
    modifier: Modifier = Modifier,
    orbCount: Int = 6
) {
    val orbs = remember { List(orbCount) { Orb() } }

    val time by produceState(initialValue = 0L) {
        while (true) {
            withFrameMillis { value = it }
        }
    }

    var lastFrameTime by remember { mutableLongStateOf(0L) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .blur(radius = 12.dp)  // softens edges globally
    ) {
        val w = size.width
        val h = size.height
        if (w == 0f || h == 0f) return@Canvas

        val currentTime = time
        if (lastFrameTime == 0L) lastFrameTime = currentTime
        val dt = (currentTime - lastFrameTime) / 1000f
        lastFrameTime = currentTime

        val safeDt = if (dt > 0.05f) 0.016f else dt

        // Update individual behaviors
        orbs.forEach { orb ->
            orb.update(safeDt, w, h)
        }

        // Apply mutual interactions
        applyInteractions(orbs, safeDt)

        // Draw each orb as a smooth circle
        orbs.forEach { orb ->
            if (orb.state != 0) {
                // Reduced ovality: single radius with subtle 15% pulse
                val pulseFactor = 1f + 0.15f * sin(orb.pulsePhase)
                val radius = orb.baseRadius * pulseFactor
                val gradientRadius = radius * 1.15f  // fade extends beyond visible edge

                val center = Offset(orb.x, orb.y)
                val orbColor = Color.hsv(
                    hue = orb.hue,
                    saturation = 0.8f,
                    value = 1f,
                    alpha = orb.alpha
                )
                val transparent = orbColor.copy(alpha = 0f)

                val brush = Brush.radialGradient(
                    colors = listOf(orbColor, transparent),
                    center = center,
                    radius = gradientRadius.coerceAtLeast(1f)
                )

                drawCircle(
                    brush = brush,
                    center = center,
                    radius = radius
                )
            }
        }
    }
}