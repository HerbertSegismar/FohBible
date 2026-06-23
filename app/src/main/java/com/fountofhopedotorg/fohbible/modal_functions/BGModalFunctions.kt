package com.fountofhopedotorg.fohbible.modal_functions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.Blob
import com.fountofhopedotorg.fohbible.data.ColorTheme
import com.fountofhopedotorg.fohbible.data.Droplet
import com.fountofhopedotorg.fohbible.data.RealisticSplash
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun ColorOptionItem(theme: ColorTheme, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = theme.primaryColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(12.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    theme.primaryColor,
                                    theme.secondaryColor
                                )
                            )
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(theme.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Primary & Secondary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun ColorSplashCanvas() {
    val splashes = remember {
        List(3) { generateRealisticSplash() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            splashes.forEach { splash ->
                val center = Offset(
                    splash.position.x * size.width,
                    splash.position.y * size.height
                )

                splash.mainBlobs.forEach { blob ->
                    drawCircle(
                        color = splash.color.copy(alpha = blob.alpha),
                        center = center + (blob.offset * splash.scale),
                        radius = blob.radius * splash.scale
                    )
                }

                splash.droplets.forEach { droplet ->
                    val dropletCenter = center + (droplet.offset * splash.scale)
                    val dropletColor = splash.color.copy(alpha = droplet.alpha)

                    drawCircle(
                        color = dropletColor,
                        center = dropletCenter,
                        radius = droplet.radius * splash.scale
                    )

                    if (droplet.hasTail && droplet.tailLength > 0) {
                        val tailDirection = droplet.offset.normalized()
                        val tailStart = dropletCenter - tailDirection * (droplet.tailLength * splash.scale)
                        drawLine(
                            color = dropletColor.copy(alpha = droplet.alpha * 0.4f),
                            start = tailStart,
                            end = dropletCenter,
                            strokeWidth = (droplet.radius * 0.6f) * splash.scale
                        )
                    }
                }
            }
        }
    }
}

private fun generateRealisticSplash(): RealisticSplash {
    val baseColor = Color(
        Random.nextInt(100, 256),
        Random.nextInt(100, 256),
        Random.nextInt(100, 256)
    )
    val position = Offset(Random.nextFloat(), Random.nextFloat())
    val scale = Random.nextFloat() * 0.6f + 0.4f

    val mainBlobs = mutableListOf<Blob>()
    val mainBlobCount = Random.nextInt(4, 8)
    repeat(mainBlobCount) {
        val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
        val distance = Random.nextFloat() * 25f
        val offset = Offset(cos(angle) * distance, sin(angle) * distance)
        val radius = Random.nextFloat() * 35f + 20f
        val alpha = Random.nextFloat() * 0.5f + 0.3f
        mainBlobs.add(Blob(offset, radius, alpha))
    }

    val droplets = mutableListOf<Droplet>()
    val dropletCount = Random.nextInt(25, 45)
    val biasAngle = Random.nextFloat() * 2 * Math.PI.toFloat()
    val maxDistance = 220f

    repeat(dropletCount) {
        val angleBias = Random.nextFloat().let { r ->
            if (r < 0.7f) biasAngle + (gaussianRandom().toFloat() * 0.8f)
            else Random.nextFloat() * 2 * Math.PI.toFloat()
        }
        val angle = angleBias.normalizedAngle()

        val distance = if (Random.nextFloat() < 0.85f) {
            Random.nextFloat() * 130f + 20f
        } else {
            Random.nextFloat() * 100f + 150f
        }

        val offset = Offset(cos(angle) * distance, sin(angle) * distance)
        val distanceFactor = (1f - (distance / maxDistance).coerceIn(0f, 1f))
        val radius = (Random.nextFloat() * 20f + 3f) * (distanceFactor * 0.8f + 0.2f)

        val alpha = if (distance > 120f) {
            Random.nextFloat() * 0.4f + 0.1f
        } else {
            Random.nextFloat() * 0.6f + 0.3f
        }

        val hasTail = distance > 40f && radius > 5f && Random.nextFloat() < 0.4f
        val tailLength = if (hasTail) Random.nextFloat() * 30f + 10f else 0f

        droplets.add(Droplet(offset, radius, alpha, hasTail, tailLength))
    }

    val secondaryCount = Random.nextInt(8, 16)
    repeat(secondaryCount) {
        val parentDroplet = droplets.randomOrNull() ?: return@repeat
        val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
        val distance = Random.nextFloat() * 30f + 5f
        val offset = parentDroplet.offset + Offset(cos(angle) * distance, sin(angle) * distance)
        val radius = Random.nextFloat() * 6f + 1f
        val alpha = Random.nextFloat() * 0.5f + 0.2f
        droplets.add(Droplet(offset, radius, alpha, false, 0f))
    }

    return RealisticSplash(baseColor, position, scale, mainBlobs, droplets)
}

// Helper functions
private fun Float.normalizedAngle(): Float {
    var angle = this
    while (angle < 0f) angle += 2 * Math.PI.toFloat()
    while (angle >= 2 * Math.PI.toFloat()) angle -= 2 * Math.PI.toFloat()
    return angle
}

private fun Offset.normalized(): Offset {
    val length = kotlin.math.hypot(x.toDouble(), y.toDouble()).toFloat()
    return if (length > 0f) Offset(x / length, y / length) else Offset.Zero
}

private fun gaussianRandom(mean: Double = 0.0, stdDev: Double = 1.0): Double {
    var u: Double
    var v: Double
    var s: Double
    do {
        u = Random.nextDouble() * 2 - 1
        v = Random.nextDouble() * 2 - 1
        s = u * u + v * v
    } while (s >= 1 || s == 0.0)
    val multiplier = sqrt(-2 * ln(s) / s)
    return mean + stdDev * u * multiplier
}

@Composable
fun SelectableBox(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else
                    Color.Transparent
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}