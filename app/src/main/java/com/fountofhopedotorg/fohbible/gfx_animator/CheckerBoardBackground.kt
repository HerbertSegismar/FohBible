package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun CheckerboardBackground(
    modifier: Modifier = Modifier,
    tileSizeDp: Dp = 20.dp,
    color1: Color = MaterialTheme.colorScheme.primary.copy(0.11f),
    color2: Color = MaterialTheme.colorScheme.primary.copy(0.33f)
) {
    Canvas(modifier = modifier) {
        val targetTilePx = tileSizeDp.toPx()

        val tilesX = (size.width / targetTilePx).roundToInt().coerceAtLeast(1)
        val tilesY = (size.height / targetTilePx).roundToInt().coerceAtLeast(1)

        val actualTileWidth = size.width / tilesX
        val actualTileHeight = size.height / tilesY

        for (row in 0 until tilesY) {
            for (col in 0 until tilesX) {
                val color = if ((row + col) % 2 == 0) color1 else color2
                drawRect(
                    color = color,
                    topLeft = Offset(col * actualTileWidth, row * actualTileHeight),
                    size = Size(actualTileWidth, actualTileHeight)
                )
            }
        }
    }
}