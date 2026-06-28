package com.fountofhopedotorg.fohbible.color_wheel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class GradientButton { START, END }

@Composable
fun GradientPickerSection(
    startColor: Color,
    endColor: Color,
    startOffset: Offset,
    endOffset: Offset,
    isSolidColor: Boolean,
    onStartOffsetChange: (Offset) -> Unit,
    onEndOffsetChange: (Offset) -> Unit,
    onButtonClick: (GradientButton) -> Unit,
    activeButton: GradientButton?,
    modifier: Modifier = Modifier
) {
    var boxSizePx by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    val activeSizeDp = 32.dp
    val inactiveSizeDp = 24.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onSizeChanged { boxSizePx = it }
    ) {
        if (boxSizePx != IntSize.Zero) {
            Canvas(modifier = Modifier.matchParentSize()) {
                if (isSolidColor) {
                    drawRect(color = startColor)
                } else {
                    val startPx = Offset(startOffset.x * boxSizePx.width, startOffset.y * boxSizePx.height)
                    val endPx = Offset(endOffset.x * boxSizePx.width, endOffset.y * boxSizePx.height)
                    drawRect(
                        brush = Brush.linearGradient(
                            start = startPx,
                            end = endPx,
                            colors = listOf(startColor, endColor)
                        )
                    )
                }
            }
        }

        if (boxSizePx != IntSize.Zero) {
            val isStartActive = activeButton == GradientButton.START
            val startSizeDp = if (isStartActive) activeSizeDp else inactiveSizeDp
            val startRadiusPx = with(density) { (startSizeDp / 2).toPx() }

            val startPos = Offset(
                startOffset.x * boxSizePx.width - startRadiusPx,
                startOffset.y * boxSizePx.height - startRadiusPx
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(startPos.x.roundToInt(), startPos.y.roundToInt()) }
                    .size(startSizeDp)
                    .clip(CircleShape)
                    .background(if (isSolidColor) startColor else endColor)
                    .border(
                        width = 2.dp,
                        color = if (isStartActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .draggableGradientButton(
                        offset = startOffset,
                        boxSizePx = boxSizePx,
                        dragThresholdPx = with(density) { 10.dp.toPx() },
                        onOffsetChange = onStartOffsetChange,
                        onClick = { onButtonClick(GradientButton.START) }
                    )
            )

            if (!isSolidColor) {
                val isEndActive = activeButton == GradientButton.END
                val endSizeDp = if (isEndActive) activeSizeDp else inactiveSizeDp
                val endRadiusPx = with(density) { (endSizeDp / 2).toPx() }

                val endPos = Offset(
                    endOffset.x * boxSizePx.width - endRadiusPx,
                    endOffset.y * boxSizePx.height - endRadiusPx
                )

                Box(
                    modifier = Modifier
                        .offset { IntOffset(endPos.x.roundToInt(), endPos.y.roundToInt()) }
                        .size(endSizeDp)
                        .clip(CircleShape)
                        .background(startColor)
                        .border(
                            width = 2.dp,
                            color = if (isEndActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .draggableGradientButton(
                            offset = endOffset,
                            boxSizePx = boxSizePx,
                            dragThresholdPx = with(density) { 10.dp.toPx() },
                            onOffsetChange = onEndOffsetChange,
                            onClick = { onButtonClick(GradientButton.END) }
                        )
                )
            }
        }
    }
}

fun Modifier.draggableGradientButton(
    offset: Offset,
    boxSizePx: IntSize,
    dragThresholdPx: Float,
    onOffsetChange: (Offset) -> Unit,
    onClick: () -> Unit
): Modifier = composed {
    val currentOffset by rememberUpdatedState(offset)
    val currentBoxSize by rememberUpdatedState(boxSizePx)
    val currentOnOffsetChange by rememberUpdatedState(onOffsetChange)
    val currentOnClick by rememberUpdatedState(onClick)

    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()

            var dragDistance = 0f
            var isDragging = false

            var currentNormX = currentOffset.x
            var currentNormY = currentOffset.y

            drag(down.id) { change ->
                val delta = change.positionChange()
                dragDistance += delta.getDistance()

                if (dragDistance > dragThresholdPx && !isDragging) {
                    isDragging = true
                }

                if (isDragging) {
                    change.consume()

                    val width = currentBoxSize.width.toFloat()
                    val height = currentBoxSize.height.toFloat()

                    if (width > 0f && height > 0f) {
                        currentNormX = (currentNormX + delta.x / width).coerceIn(0f, 1f)
                        currentNormY = (currentNormY + delta.y / height).coerceIn(0f, 1f)

                        currentOnOffsetChange(Offset(currentNormX, currentNormY))
                    }
                }
            }

            if (!isDragging) {
                currentOnClick()
            }
        }
    }
}