package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CheckerboardBackground(
    modifier: Modifier = Modifier,
    tileSizeDp: Dp = 20.dp,
    color1: Color = Color(0xFFCCCCCC),
    color2: Color = Color(0xFF666666)
) {
    Canvas(modifier = modifier) {
        val tilePx = tileSizeDp.toPx()
        val tilesX = (size.width / tilePx).toInt() + 1
        val tilesY = (size.height / tilePx).toInt() + 1
        for (row in 0 until tilesY) {
            for (col in 0 until tilesX) {
                val color = if ((row + col) % 2 == 0) color1 else color2
                drawRect(
                    color = color,
                    topLeft = Offset(col * tilePx, row * tilePx),
                    size = Size(tilePx, tilePx)
                )
            }
        }
    }
}