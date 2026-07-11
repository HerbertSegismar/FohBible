package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun Modifier.requiredSizePx(width: Float, height: Float) = this.layout { measurable, _ ->
    val w = width.roundToInt().coerceAtLeast(0)
    val h = height.roundToInt().coerceAtLeast(0)
    val placeable = measurable.measure(Constraints.fixed(w, h))
    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
}

@Composable
fun PivotHandle(
    baseSize: IntSize,
    pivotX: Float,
    pivotY: Float,
    density: Float,
    isActive: Boolean = false,
    onStartPivotPlacement: () -> Unit,
    onPlacePivotLocal: (Float, Float) -> Unit,
    currentRotation: Float,
    currentScaleX: Float,
    currentScaleY: Float,
    elementWidth: Float,
    elementHeight: Float
) {
    val handleSizePx = 48f
    val handleRadiusPx = handleSizePx / 2f
    val pivotLocalX = pivotX * baseSize.width
    val pivotLocalY = pivotY * baseSize.height

    // Keep the latest values inside the gesture lambdas so they don’t go stale
    val latestPivotX by rememberUpdatedState(pivotX)
    val latestPivotY by rememberUpdatedState(pivotY)
    val latestRotation by rememberUpdatedState(currentRotation)
    val latestScaleX by rememberUpdatedState(currentScaleX)
    val latestScaleY by rememberUpdatedState(currentScaleY)
    val latestElementWidth by rememberUpdatedState(elementWidth)
    val latestElementHeight by rememberUpdatedState(elementHeight)

    val bgColor = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0)
    val iconColor = if (isActive) MaterialTheme.colorScheme.onPrimary else Color.Black
    val borderColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (pivotLocalX - handleRadiusPx).roundToInt(),
                    (pivotLocalY - handleRadiusPx).roundToInt()
                )
            }
            .requiredSizePx(handleSizePx, handleSizePx)
            .pointerInput(Unit) {
                detectTapGestures { onStartPivotPlacement() }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()

                    val rad = latestRotation * (PI / 180.0).toFloat()
                    val cosA = cos(rad)
                    val sinA = sin(rad)

                    // Inverse rotation of the screen‑space drag delta
                    val localDx = dragAmount.x * cosA + dragAmount.y * sinA
                    val localDy = -dragAmount.x * sinA + dragAmount.y * cosA

                    val deltaNormX = if (latestScaleX != 0f) localDx / (latestScaleX * latestElementWidth) else 0f
                    val deltaNormY = if (latestScaleY != 0f) localDy / (latestScaleY * latestElementHeight) else 0f

                    val newNormX = latestPivotX + deltaNormX
                    val newNormY = latestPivotY + deltaNormY

                    onPlacePivotLocal(newNormX, newNormY)
                }
            }
            .background(bgColor, CircleShape)
            .border(width = (1f / density).dp, color = borderColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val armLength = 12f
            drawLine(iconColor, center - Offset(armLength, 0f), center + Offset(armLength, 0f), strokeWidth = 3f)
            drawLine(iconColor, center - Offset(0f, armLength), center + Offset(0f, armLength), strokeWidth = 3f)
        }
    }
}