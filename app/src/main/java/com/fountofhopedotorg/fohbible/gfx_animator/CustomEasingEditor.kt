package com.fountofhopedotorg.fohbible.gfx_animator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.fountofhopedotorg.fohbible.data.EasingPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private enum class HandleType { IN, OUT }

@Composable
fun CustomEasingEditor(
    points: List<EasingPoint>,
    onPointsChanged: (List<EasingPoint>) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPoints by rememberUpdatedState(newValue = points)

    var draggedPointIndex by remember { mutableStateOf<Int?>(null) }
    var draggedHandle by remember { mutableStateOf<Pair<Int, HandleType>?>(null) }

    val density = LocalDensity.current
    val anchorHitRadiusPx = remember(density) { with(density) { 24.dp.toPx() } }
    val handleHitRadiusPx = remember(density) { with(density) { 20.dp.toPx() } }

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val pathColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.secondary
    val handleColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
    val handleLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val normX = (offset.x / size.width).coerceIn(0f, 1f)
                        val normY = 1f - (offset.y / size.height).coerceIn(0f, 1f)

                        val newPoints = currentPoints.toMutableList()
                        newPoints.add(EasingPoint(x = normX, y = normY, isSmooth = false))
                        newPoints.sortBy { it.x }
                        onPointsChanged(newPoints)
                    },
                    onLongPress = { offset ->
                        val clickedIndex = currentPoints.indexOfFirst { pt ->
                            val px = Offset(pt.x * size.width, (1f - pt.y) * size.height)
                            (px - offset).getDistance() < anchorHitRadiusPx
                        }
                        if (clickedIndex != -1) {
                            val newPoints = currentPoints.toMutableList()
                            val p = newPoints[clickedIndex]
                            val targetSmooth = !p.isSmooth

                            newPoints[clickedIndex] = if (targetSmooth) {
                                val prevX = newPoints.getOrNull(clickedIndex - 1)?.x ?: 0f
                                val nextX = newPoints.getOrNull(clickedIndex + 1)?.x ?: 1f
                                val dsOut = (nextX - p.x) * 0.33f
                                val dsIn = (prevX - p.x) * 0.33f
                                p.copy(
                                    isSmooth = true,
                                    handleOut = Offset(dsOut, 0f),
                                    handleIn = Offset(dsIn, 0f)
                                )
                            } else {
                                p.copy(isSmooth = false, handleIn = Offset.Zero, handleOut = Offset.Zero)
                            }
                            onPointsChanged(newPoints)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        for ((i, pt) in currentPoints.withIndex()) {
                            if (!pt.isSmooth) continue

                            if (i < currentPoints.size - 1) {
                                val hx = (pt.x + pt.handleOut.x) * size.width
                                val hy = (1f - (pt.y + pt.handleOut.y)) * size.height
                                if ((Offset(hx, hy) - offset).getDistance() < handleHitRadiusPx) {
                                    draggedHandle = i to HandleType.OUT
                                    return@detectDragGestures
                                }
                            }
                            if (i > 0) {
                                val hx = (pt.x + pt.handleIn.x) * size.width
                                val hy = (1f - (pt.y + pt.handleIn.y)) * size.height
                                if ((Offset(hx, hy) - offset).getDistance() < handleHitRadiusPx) {
                                    draggedHandle = i to HandleType.IN
                                    return@detectDragGestures
                                }
                            }
                        }

                        val hitIndex = currentPoints.indexOfFirst { pt ->
                            val px = Offset(pt.x * size.width, (1f - pt.y) * size.height)
                            (px - offset).getDistance() < anchorHitRadiusPx
                        }
                        if (hitIndex != -1) {
                            draggedPointIndex = hitIndex
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val normDx = dragAmount.x / w
                        val normDy = -dragAmount.y / h

                        draggedHandle?.let { (idx, type) ->
                            val newPoints = currentPoints.toMutableList()
                            val pt = newPoints[idx]

                            newPoints[idx] = if (type == HandleType.OUT) {
                                val updatedOut = Offset(pt.handleOut.x + normDx, pt.handleOut.y + normDy)
                                val lenIn = pt.handleIn.getDistance()
                                val angleOut = atan2(updatedOut.y, updatedOut.x)
                                val updatedIn = if (lenIn > 0f) {
                                    Offset(cos(angleOut + Math.PI).toFloat(), sin(angleOut + Math.PI).toFloat()) * lenIn
                                } else -updatedOut * 0.5f
                                pt.copy(handleOut = updatedOut, handleIn = updatedIn)
                            } else {
                                val updatedIn = Offset(pt.handleIn.x + normDx, pt.handleIn.y + normDy)
                                val lenOut = pt.handleOut.getDistance()
                                val angleIn = atan2(updatedIn.y, updatedIn.x)
                                val updatedOut = if (lenOut > 0f) {
                                    Offset(cos(angleIn + Math.PI).toFloat(), sin(angleIn + Math.PI).toFloat()) * lenOut
                                } else -updatedIn * 0.5f
                                pt.copy(handleIn = updatedIn, handleOut = updatedOut)
                            }
                            onPointsChanged(newPoints)
                            return@detectDragGestures
                        }

                        draggedPointIndex?.let { idx ->
                            val newPoints = currentPoints.toMutableList()
                            val pt = newPoints[idx]

                            if (idx == 0 || idx == currentPoints.size - 1) {
                                val targetY = (pt.y + normDy).coerceIn(0f, 1f)
                                newPoints[idx] = pt.copy(y = targetY)
                            } else {
                                val prevX = newPoints[idx - 1].x
                                val nextX = newPoints[idx + 1].x
                                val targetX = (pt.x + normDx).coerceIn(prevX + 0.005f, nextX - 0.005f)
                                val targetY = (pt.y + normDy).coerceIn(0f, 1f)
                                newPoints[idx] = pt.copy(x = targetX, y = targetY)
                            }
                            onPointsChanged(newPoints)
                        }
                    },
                    onDragEnd = {
                        draggedPointIndex = null
                        draggedHandle = null
                    },
                    onDragCancel = {
                        draggedPointIndex = null
                        draggedHandle = null
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height

        val horizontalLines = 4
        val verticalLines = 4
        for (i in 1 until horizontalLines) {
            val y = h * (i.toFloat() / horizontalLines)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
        }
        for (i in 1 until verticalLines) {
            val x = w * (i.toFloat() / verticalLines)
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx())
        }

        if (points.isNotEmpty()) {
            val elementPath = Path().apply {
                val start = points.first()
                moveTo(start.x * w, (1f - start.y) * h)

                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]

                    val cp1x = (prev.x + prev.handleOut.x) * w
                    val cp1y = (1f - (prev.y + prev.handleOut.y)) * h
                    val cp2x = (curr.x + curr.handleIn.x) * w
                    val cp2y = (1f - (curr.y + curr.handleIn.y)) * h

                    val endX = curr.x * w
                    val endY = (1f - curr.y) * h

                    cubicTo(cp1x, cp1y, cp2x, cp2y, endX, endY)
                }
            }
            drawPath(elementPath, pathColor, style = Stroke(width = 3.dp.toPx()))
        }

        points.forEachIndexed { i, pt ->
            if (!pt.isSmooth) return@forEachIndexed
            val anchorX = pt.x * w
            val anchorY = (1f - pt.y) * h

            if (i < points.size - 1) {
                val hx = (pt.x + pt.handleOut.x) * w
                val hy = (1f - (pt.y + pt.handleOut.y)) * h
                drawLine(handleLineColor, Offset(anchorX, anchorY), Offset(hx, hy), strokeWidth = 1.dp.toPx())
                drawCircle(handleColor, radius = 5.dp.toPx(), center = Offset(hx, hy))
            }

            if (i > 0) {
                val hx = (pt.x + pt.handleIn.x) * w
                val hy = (1f - (pt.y + pt.handleIn.y)) * h
                drawLine(handleLineColor, Offset(anchorX, anchorY), Offset(hx, hy), strokeWidth = 1.dp.toPx())
                drawCircle(handleColor, radius = 5.dp.toPx(), center = Offset(hx, hy))
            }
        }

        points.forEach { pt ->
            val px = pt.x * w
            val py = (1f - pt.y) * h
            if (pt.isSmooth) {
                drawCircle(color = pointColor, radius = 6.dp.toPx(), center = Offset(px, py))
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(px, py))
            } else {
                drawCircle(color = pointColor, radius = 5.dp.toPx(), center = Offset(px, py))
            }
        }
    }
}