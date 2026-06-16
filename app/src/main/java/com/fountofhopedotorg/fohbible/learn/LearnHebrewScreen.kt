package com.fountofhopedotorg.fohbible.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fountofhopedotorg.fohbible.data.HebrewLetter

@Composable
fun LearnHebrewScreen() {
    val letters = listOf(
        HebrewLetter("Aleph", "A") { progress -> drawAleph(progress) },
        HebrewLetter("Beth", "B") { progress -> drawBeth(progress) },
        HebrewLetter("Gimel", "G") { progress -> drawGimel(progress) }
    )

    var currentIndex by remember { mutableIntStateOf(0) }
    var replayTrigger by remember { mutableIntStateOf(0) }
    var showEnglish by remember { mutableStateOf(false) }
    val progressAnimatable = remember { Animatable(0f) }

    LaunchedEffect(currentIndex, replayTrigger) {
        showEnglish = false
        progressAnimatable.snapTo(0f)
        progressAnimatable.animateTo(1f, animationSpec = tween(2000))
        showEnglish = true
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val currentLetter = letters[currentIndex]

        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                currentLetter.draw(this, progressAnimatable.value)
            }
        }

        Text(currentLetter.name, style = MaterialTheme.typography.bodyLarge, fontSize = 24.sp)
        Text("English: ${currentLetter.english}", modifier = Modifier.alpha(if (showEnglish) 1f else 0f))

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Button(onClick = { replayTrigger++ }) { Text("Replay", color = Color.White) }
            Spacer(Modifier.width(16.dp))
            Button(onClick = {
                if (currentIndex < letters.size - 1) currentIndex++ else currentIndex = 0
            }) {
                Text(if (currentIndex < letters.size - 1) "Next" else "Restart", color = Color.White)
            }
        }
    }
}

fun DrawScope.drawAleph(progress: Float) {
    val strokeWidth = 80f
    val color = Color(0xFF1A237E)

    val paths = listOf(
        Path().apply {
            moveTo(0.35f * size.width, 0.2f * size.height)
            quadraticTo(0.45f * size.width, 0.5f * size.height, 0.65f * size.width, 0.8f * size.height)
        },
        Path().apply {
            moveTo(0.75f * size.width, 0.25f * size.height)
            quadraticTo(0.55f * size.width, 0.25f * size.height, 0.5f * size.width, 0.45f * size.height)
        },
        Path().apply {
            moveTo(0.5f * size.width, 0.55f * size.height)
            quadraticTo(0.35f * size.width, 0.65f * size.height, 0.25f * size.width, 0.75f * size.height)
        }
    )

    drawComplexPath(paths, progress, color, strokeWidth)
}

fun DrawScope.drawBeth(progress: Float) {
    val strokeWidth = 80f
    val color = Color(0xFF1A237E)

    val paths = listOf(
        Path().apply {
            moveTo(0.2f * size.width, 0.2f * size.height)
            quadraticTo(
                0.5f * size.width,
                0.15f * size.height,
                0.8f * size.width,
                0.2f * size.height
            )
        },
        Path().apply {
            moveTo(0.8f * size.width, 0.2f * size.height)
            lineTo(0.8f * size.width, 0.7f * size.height)
            quadraticTo(
                0.8f * size.width,
                0.85f * size.height,
                0.6f * size.width,
                0.85f * size.height
            )
            lineTo(0.2f * size.width, 0.85f * size.height)
        }
    )

    drawComplexPath(paths, progress, color, strokeWidth)
}

// Add this function to your file
fun DrawScope.drawGimel(progress: Float) {
    val strokeWidth = 80f
    val color = Color(0xFF1A237E)

    val paths = listOf(
        Path().apply {
            moveTo(0.6f * size.width, 0.2f * size.height)
            lineTo(0.6f * size.width, 0.7f * size.height)
        },
        Path().apply {
            moveTo(0.6f * size.width, 0.7f * size.height)
            quadraticTo(
                0.55f * size.width,
                0.85f * size.height,
                0.3f * size.width,
                0.8f * size.height
            )
        }
    )

    drawComplexPath(paths, progress, color, strokeWidth)
}

fun DrawScope.drawComplexPath(paths: List<Path>, totalProgress: Float, color: Color, strokeWidth: Float) {
    val segmentCount = paths.size
    val segmentProgress = 1f / segmentCount

    paths.forEachIndexed { index, path ->
        val start = index * segmentProgress
        val progressInSegment = ((totalProgress - start) / segmentProgress).coerceIn(0f, 1f)

        if (progressInSegment > 0f) {
            val measure = PathMeasure().apply { setPath(path, false) }
            val length = measure.length
            drawPath(
                path = path, color = color,
                style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(length, length), phase = length - length * progressInSegment)
                )
            )
        }
    }
}