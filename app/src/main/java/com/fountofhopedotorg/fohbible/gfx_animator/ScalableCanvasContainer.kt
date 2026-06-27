package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import kotlin.math.min

@Composable
fun ScalableCanvasContainer(
    canvasWidthDp: Dp,
    canvasHeightDp: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val maxWidthDp = maxWidth
        val maxHeightDp = maxHeight
        val scale = min(maxWidthDp / canvasWidthDp, maxHeightDp / canvasHeightDp)
        val offsetXDp = (maxWidthDp - canvasWidthDp * scale) / 2
        val offsetYDp = (maxHeightDp - canvasHeightDp * scale) / 2

        Box(
            modifier = Modifier
                .size(canvasWidthDp, canvasHeightDp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetXDp.toPx()
                    translationY = offsetYDp.toPx()
                },
            content = content
        )
    }
}