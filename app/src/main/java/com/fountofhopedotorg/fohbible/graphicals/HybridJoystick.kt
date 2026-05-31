package com.fountofhopedotorg.fohbible.graphicals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun HybridJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    thumbSize: Dp = 60.dp,
    nudgeAmount: Float,
    enabled: Boolean = true,
    onNudgeAmountClick: () -> Unit,
    onDirectionClick: (dx: Float, dy: Float) -> Unit
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    val thumbSizePx = with(density) { thumbSize.toPx() }

    val maxDisplacementPx = (sizePx - thumbSizePx) / 2f
    val allowedDisplacementPx = maxDisplacementPx * 1.3f

    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val currentOffset by rememberUpdatedState(thumbOffset)
    val currentNudgeAmount by rememberUpdatedState(nudgeAmount)
    val currentOnDirectionClick by rememberUpdatedState(onDirectionClick)

    LaunchedEffect(isDragging, enabled) {
        if (!enabled) {
            isDragging = false
            thumbOffset = Offset.Zero
        }

        while (isDragging && enabled) {
            if (currentOffset != Offset.Zero) {
                val normalizedX = currentOffset.x / maxDisplacementPx
                val normalizedY = currentOffset.y / maxDisplacementPx
                val frameSpeedFactor = 0.05f

                currentOnDirectionClick(
                    normalizedX * currentNudgeAmount * frameSpeedFactor,
                    normalizedY * currentNudgeAmount * frameSpeedFactor
                )
            }
            delay(16)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
            .pointerInput(enabled) {
                if (enabled) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = thumbOffset + dragAmount
                            val currentDisplacement = newOffset.getDistance()
                            thumbOffset = if (currentDisplacement <= allowedDisplacementPx) {
                                newOffset
                            } else {
                                newOffset.times(allowedDisplacementPx / currentDisplacement)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            thumbOffset = Offset.Zero
                        },
                        onDragCancel = {
                            isDragging = false
                            thumbOffset = Offset.Zero
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { onDirectionClick(0f, -nudgeAmount) },
            modifier = Modifier.align(Alignment.TopCenter).size(36.dp),
            enabled = enabled
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Up",
                tint = MaterialTheme.colorScheme.primary.copy(if (enabled) 0.5f else 0.2f)
            )
        }
        IconButton(
            onClick = { onDirectionClick(0f, nudgeAmount) },
            modifier = Modifier.align(Alignment.BottomCenter).size(36.dp),
            enabled = enabled
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Down",
                tint = MaterialTheme.colorScheme.primary.copy(if (enabled) 0.5f else 0.2f)
            )
        }
        IconButton(
            onClick = { onDirectionClick(-nudgeAmount, 0f) },
            modifier = Modifier.align(Alignment.CenterStart).size(36.dp),
            enabled = enabled
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Left",
                tint = MaterialTheme.colorScheme.primary.copy(if (enabled) 0.5f else 0.2f)
            )
        }
        IconButton(
            onClick = { onDirectionClick(nudgeAmount, 0f) },
            modifier = Modifier.align(Alignment.CenterEnd).size(36.dp),
            enabled = enabled
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Right",
                tint = MaterialTheme.colorScheme.primary.copy(if (enabled) 0.5f else 0.2f)
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.x.roundToInt(), thumbOffset.y.roundToInt()) }
                .size(thumbSize)
                .background(
                    MaterialTheme.colorScheme.primary.copy(if (enabled) 1f else 0.5f),
                    CircleShape
                )
                .clickable(enabled = enabled) { onNudgeAmountClick() },
            contentAlignment = Alignment.Center
        ) {
            val amountText = when (nudgeAmount) {
                0.01f -> "1%"
                0.05f -> "5%"
                0.1f -> "10%"
                else -> "${(nudgeAmount * 100).toInt()}%"
            }
            Text(
                text = amountText,
                color = Color.White.copy(if (enabled) 1f else 0.5f),
                style = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )
        }
    }
}