package com.fountofhopedotorg.fohbible.gfx_creator

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@Composable
fun ReorderHandle(
    originalIndex: Int,
    isUpEnabled: Boolean,
    isDownEnabled: Boolean,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val maxDragRangePx = with(density) { 20.dp.toPx() }
    val triggerThresholdPx = maxDragRangePx * 0.8f
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember(originalIndex) { Animatable(0f) }

    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .width(20.dp)
            .height(30.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(primaryColor.copy(alpha = 0.2f), CircleShape)
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .size(17.dp)
                .background(primaryColor.copy(alpha = 0.7f), CircleShape)
                .pointerInput(originalIndex) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val targetValue = offsetY.value + dragAmount.y
                                val clamped = targetValue.coerceIn(-maxDragRangePx, maxDragRangePx)
                                offsetY.snapTo(clamped)
                            }
                        },
                        onDragEnd = {
                            if (offsetY.value <= -triggerThresholdPx && isUpEnabled) {
                                onReorder(originalIndex, originalIndex - 1)
                            } else if (offsetY.value >= triggerThresholdPx && isDownEnabled) {
                                onReorder(originalIndex, originalIndex + 1)
                            }
                            coroutineScope.launch {
                                offsetY.animateTo(
                                    0f,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetY.animateTo(0f, spring())
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}