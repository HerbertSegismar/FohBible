package com.fountofhopedotorg.fohbible.creator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.fountofhopedotorg.fohbible.data.BezierNodeData
import com.fountofhopedotorg.fohbible.data.CanvasNote
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ShapeSelectionCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    shapePreview: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        shapePreview()
    }
}

@Composable
fun HexagonShape(modifier: Modifier = Modifier, color: Color = getRandomColor().copy(0.4f), drawStyle: DrawStyle = Fill) {
    Canvas(modifier = modifier) {
        val path = Path()
        val sides = 6
        val angleOffset = -Math.PI / 2
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = minOf(centerX, centerY)

        for (i in 0 until sides) {
            val angle = angleOffset + 2.0 * Math.PI * i / sides
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path = path, color = color, style = drawStyle)
    }
}

@Composable
fun OctagonShape(modifier: Modifier = Modifier, color: Color = getRandomColor().copy(0.4f), drawStyle: DrawStyle = Fill) {
    Canvas(modifier = modifier) {
        val path = Path()
        val sides = 8
        val angleOffset = -Math.PI / 2
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = minOf(centerX, centerY)

        for (i in 0 until sides) {
            val angle = angleOffset + 2.0 * Math.PI * i / sides
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path = path, color = color, style = drawStyle)
    }
}

@Composable
fun StarShape(modifier: Modifier = Modifier, color: Color = getRandomColor().copy(0.4f),  drawStyle: DrawStyle = Fill) {
    Canvas(modifier = modifier) {
        val path = Path()
        val points = 5
        val outerRadius = minOf(size.width, size.height) / 2f
        val innerRadius = outerRadius * 0.4f
        val angleOffset = -Math.PI / 2
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        for (i in 0 until points * 2) {
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val angle = angleOffset + Math.PI * i / points
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path = path, color = color, style = drawStyle)
    }
}

@Composable
fun DiamondShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill
) {
    Canvas(modifier = modifier) {
        val path = Path()
        val w = size.width
        val h = size.height

        path.moveTo(w / 2f, 0f)
        path.quadraticTo(w * 0.68f, h * 0.32f, w, h / 2f)
        path.quadraticTo(w * 0.68f, h * 0.68f, w / 2f, h)
        path.quadraticTo(w * 0.32f, h * 0.68f, 0f, h / 2f)
        path.quadraticTo(w * 0.32f, h * 0.32f, w / 2f, 0f)

        path.close()
        drawPath(path = path, color = color, style = drawStyle)
    }
}

@Composable
fun MoonShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val moonPath = Path().apply {
            addOval(Rect(0f, 0f, w, h))
        }

        val cutterPath = Path().apply {
            addOval(Rect(w * 0.35f, -h * 0.05f, w * 1.35f, h * 1.05f))
        }
        val crescentPath = Path.combine(
            operation = PathOperation.Difference,
            path1 = moonPath,
            path2 = cutterPath
        )

        drawPath(path = crescentPath, color = color, style = drawStyle)
    }
}

@Composable
fun CrossShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill
) {
    Canvas(modifier = modifier) {
        val path = Path()
        val w = size.width
        val h = size.height

        // Top wing (pointed)
        path.moveTo(w * 0.38f, h * 0.05f)
        path.lineTo(w * 0.50f, 0f) // Top point
        path.lineTo(w * 0.62f, h * 0.05f)

        // Inner corner top-right
        path.lineTo(w * 0.60f, h * 0.21f)
        path.quadraticTo(w * 0.60f, h * 0.28f, w * 0.68f, h * 0.28f)

        // Right wing (pointed)
        path.lineTo(w * 0.9f, h * 0.24f)
        path.lineTo(w * 0.98f, h * 0.35f) // Right point
        path.lineTo(w * 0.9f, h * 0.46f)

        // Inner corner bottom-right
        path.lineTo(w * 0.68f, h * 0.42f)
        path.quadraticTo(w * 0.60f, h * 0.42f, w * 0.60f, h * 0.52f)

        // Bottom wing (pointed)
        path.lineTo(w * 0.62f, h * 0.95f)
        path.lineTo(w * 0.50f, h) // Bottom point
        path.lineTo(w * 0.38f, h * 0.95f)

        // Inner corner bottom-left
        path.lineTo(w * 0.40f, h * 0.52f)
        path.quadraticTo(w * 0.40f, h * 0.42f, w * 0.32f, h * 0.42f)

        // Left wing (pointed)
        path.lineTo(w * 0.1f, h * 0.46f)
        path.lineTo(w * 0.02f, h * 0.35f) // Left point
        path.lineTo(w * 0.1f, h * 0.24f)

        // Inner corner top-left
        path.lineTo(w * 0.32f, h * 0.28f)
        path.quadraticTo(w * 0.40f, h * 0.28f, w * 0.40f, h * 0.21f)

        path.close()
        drawPath(path = path, color = color, style = drawStyle)
    }
}

@Composable
fun GearShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    teethCount: Int = 8,
    drawStyle: DrawStyle = Fill
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val rOuter = minOf(w, h) / 2f
        val rInner = rOuter * 0.7f
        val rHole = rOuter * 0.25f
        val gearPath = Path().apply {
            val step = 2.0 * Math.PI / teethCount
            val offsetAngle = -Math.PI / 2

            for (i in 0 until teethCount) {
                val a1 = offsetAngle + step * (i + 0.1)
                val a2 = offsetAngle + step * (i + 0.3)
                val a3 = offsetAngle + step * (i + 0.7)
                val a4 = offsetAngle + step * (i + 0.9)

                val p1x = (cx + rInner * cos(a1)).toFloat()
                val p1y = (cy + rInner * sin(a1)).toFloat()
                val p2x = (cx + rOuter * cos(a2)).toFloat()
                val p2y = (cy + rOuter * sin(a2)).toFloat()
                val p3x = (cx + rOuter * cos(a3)).toFloat()
                val p3y = (cy + rOuter * sin(a3)).toFloat()
                val p4x = (cx + rInner * cos(a4)).toFloat()
                val p4y = (cy + rInner * sin(a4)).toFloat()

                if (i == 0) moveTo(p1x, p1y) else lineTo(p1x, p1y)
                lineTo(p2x, p2y)
                lineTo(p3x, p3y)
                lineTo(p4x, p4y)
            }
            close()
        }

        val holePath = Path().apply {
            addOval(Rect(cx - rHole, cy - rHole, cx + rHole, cy + rHole))
        }

        val finalPath = Path.combine(
            operation = PathOperation.Difference,
            path1 = gearPath,
            path2 = holePath
        )

        drawPath(path = finalPath, color = color, style = drawStyle)
    }
}

@Composable
fun DavidStarShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    curveFactor: Float = 0.85f,
    drawStyle: DrawStyle = Fill
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val rOuter = minOf(w, h) / 2f
        val rInner = rOuter / sqrt(3f)
        val path = Path()
        val vertices = List(12) { i ->
            val angleDeg = -90f + (i * 30f)
            val angleRad = (angleDeg * PI / 180f).toFloat()
            val radius = if (i % 2 == 0) rOuter else rInner

            Offset(
                x = cx + radius * cos(angleRad),
                y = cy + radius * sin(angleRad)
            )
        }

        path.moveTo(vertices[0].x, vertices[0].y)

        for (i in 0 until 12) {
            val startPoint = vertices[i]
            val endPoint = vertices[(i + 1) % 12]
            val midX = (startPoint.x + endPoint.x) / 2f
            val midY = (startPoint.y + endPoint.y) / 2f
            val controlX = cx + (midX - cx) * curveFactor
            val controlY = cy + (midY - cy) * curveFactor

            path.quadraticTo(
                x1 = controlX, y1 = controlY,
                x2 = endPoint.x, y2 = endPoint.y
            )
        }

        path.close()
        drawPath(path = path, color = color, style = drawStyle)
    }
}

@Composable
fun HeartShape(modifier: Modifier = Modifier, color: Color = getRandomColor().copy(0.4f), drawStyle: DrawStyle = Fill) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.32f)

            cubicTo(
                w * 0.32f, h * 0.08f,
                w * 0.06f, h * 0.16f,
                w * 0.06f, h * 0.44f
            )

            cubicTo(
                w * 0.06f, h * 0.64f,
                w * 0.36f, h * 0.80f,
                w * 0.5f, h * 0.96f
            )

            cubicTo(
                w * 0.64f, h * 0.80f,
                w * 0.94f, h * 0.64f,
                w * 0.94f, h * 0.44f
            )

            cubicTo(
                w * 0.94f, h * 0.16f,
                w * 0.68f, h * 0.08f,
                w * 0.5f, h * 0.32f
            )

            close()
        }

        drawPath(path = path, color = color, style = drawStyle)
    }
}

@Composable
fun ArrowRightShape(modifier: Modifier = Modifier, color: Color = getRandomColor().copy(0.4f), drawStyle: DrawStyle = Fill) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path()
        val headWidth = w * 0.4f
        path.moveTo(0f, h * 0.3f)
        path.lineTo(w - headWidth, h * 0.3f)
        path.lineTo(w - headWidth, 0f)
        path.lineTo(w, h / 2f)
        path.lineTo(w - headWidth, h)
        path.lineTo(w - headWidth, h * 0.7f)
        path.lineTo(0f, h * 0.7f)
        path.close()
        drawPath(path = path, color = color, style = drawStyle)
    }
}

@Composable
fun SquareShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill
) {
    Canvas(modifier = modifier) {
        drawRect(color = color, style = drawStyle)
    }
}

@Composable
fun CircleShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill
) {
    Canvas(modifier = modifier) {
        drawCircle(color = color, style = drawStyle)
    }
}

@Composable
fun TriangleShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill
) {
    Canvas(modifier = modifier) {
        val trianglePath = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path = trianglePath, color = color, style = drawStyle)
    }
}

@Composable
fun LineShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    strokeWidth: Float = 8f,
) {
    Canvas(modifier = modifier) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun ThornCrownShape(
    modifier: Modifier = Modifier,
    thornColor: Color = getRandomColor().copy(0.4f),
    seed: Long = 42
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val crownPaths = generateThornCrownPaths(seed, size)

            onDrawBehind {
                val strokeWidthScale = size.minDimension / 938f
                drawPath(
                    path = crownPaths.vinePath,
                    color = thornColor,
                    style = Stroke(
                        width = 8f * strokeWidthScale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                drawPath(
                    path = crownPaths.thornsPath,
                    color = thornColor,
                    style = Fill
                )
            }
        }
    )
}

@Composable
fun PolygonShape(
    points: List<Offset>,
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.8f),
    drawStyle: DrawStyle = Fill
) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        val path = Path().apply {
            moveTo(points.first().x * size.width, points.first().y * size.height)
            points.drop(1).forEach { point ->
                lineTo(point.x * size.width, point.y * size.height)
            }
            close()
        }

        drawPath(path = path, color = color, style = drawStyle)
    }
}

@Composable
fun BezierPolygonShape(
    nodes: List<BezierNodeData>,
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.8f),
    closed: Boolean = true,
    drawStyle: DrawStyle = if (closed) Fill else Stroke(width = 4f)
) {
    Canvas(modifier = modifier) {
        if (nodes.isEmpty()) return@Canvas
        val path = Path().apply {
            val first = nodes[0]
            moveTo(first.anchor.x * size.width, first.anchor.y * size.height)
            for (i in 1 until nodes.size) {
                val prev = nodes[i - 1]
                val curr = nodes[i]
                cubicTo(
                    prev.handleOut.x * size.width, prev.handleOut.y * size.height,
                    curr.handleIn.x * size.width, curr.handleIn.y * size.height,
                    curr.anchor.x * size.width, curr.anchor.y * size.height
                )
            }
            if (closed) {
                val last = nodes.last()
                cubicTo(
                    last.handleOut.x * size.width, last.handleOut.y * size.height,
                    first.handleIn.x * size.width, first.handleIn.y * size.height,
                    first.anchor.x * size.width, first.anchor.y * size.height
                )
                close()
            }
        }
        if (closed) {
            drawPath(path, color = color)
        } else {
            drawPath(path, color = color, style = drawStyle)
        }
    }
}

@Composable
fun CanvasSvgItem(
    note: CanvasNote,
    isSelected: Boolean,
    isLocked: Boolean,
    onSelect: () -> Unit,
    onUpdatePosition: (Offset, Float, Float, Float) -> Unit,
    onScaleChanged: (Float, Float) -> Unit,
    onColorPickerRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    proportionalEditing: Boolean,
    onProportionalToggle: () -> Unit
) {
    val density = LocalDensity.current.density
    val latestProportional by rememberUpdatedState(proportionalEditing)
    var offset by remember(note.offset) { mutableStateOf(note.offset) }
    var baseSize by remember { mutableStateOf(IntSize.Zero) }
    val currentRotation by rememberUpdatedState(note.rotation)
    val currentScaleX by rememberUpdatedState(note.scaleX)
    val currentScaleY by rememberUpdatedState(note.scaleY)

    val currentWidth by rememberUpdatedState(note.width)
    val currentHeight by rememberUpdatedState(note.height)

    val handleSize = 24.dp
    val handleRadiusPx = with(LocalDensity.current) { handleSize.toPx() / 2f }

    val isCustomPolygon = note.content.startsWith("Shape:CustomPolygon:")
    val isCustomLine = note.content.startsWith("Shape:CustomLine:")
    val isAnyCustomBezier = isCustomPolygon || isCustomLine

    val parsedData = remember(note.content) {
        if (isAnyCustomBezier) {
            val prefix = if (isCustomPolygon) "Shape:CustomPolygon:" else "Shape:CustomLine:"
            val serializedPoints = note.content.removePrefix(prefix)
            val rawNodes = serializedPoints.split(";").mapNotNull { nodeStr ->
                val parts = nodeStr.split(":")
                if (parts.size == 3) {
                    val a = parts[0].split(",")
                    val hi = parts[1].split(",")
                    val ho = parts[2].split(",")
                    if (a.size == 2 && hi.size == 2 && ho.size == 2) {
                        BezierNodeData(
                            anchor = Offset(a[0].toFloatOrNull() ?: 0f, a[1].toFloatOrNull() ?: 0f),
                            handleIn = Offset(hi[0].toFloatOrNull() ?: 0f, hi[1].toFloatOrNull() ?: 0f),
                            handleOut = Offset(ho[0].toFloatOrNull() ?: 0f, ho[1].toFloatOrNull() ?: 0f)
                        )
                    } else null
                } else {
                    val coords = parts[0].split(",")
                    if (coords.size == 2) {
                        val pt = Offset(coords[0].toFloatOrNull() ?: 0f, coords[1].toFloatOrNull() ?: 0f)
                        BezierNodeData(pt, pt, pt)
                    } else null
                }
            }

            if (rawNodes.size >= (if (isCustomLine) 2 else 3)) {
                val allPoints = rawNodes.flatMap { listOf(it.anchor, it.handleIn, it.handleOut) }
                val minX = allPoints.minOf { it.x }
                val maxX = allPoints.maxOf { it.x }
                val minY = allPoints.minOf { it.y }
                val maxY = allPoints.maxOf { it.y }
                val polyWidth = maxX - minX
                val polyHeight = maxY - minY

                val normalizedNodes = rawNodes.map { node ->
                    BezierNodeData(
                        anchor = Offset(
                            x = if (polyWidth > 0) (node.anchor.x - minX) / polyWidth else 0.5f,
                            y = if (polyHeight > 0) (node.anchor.y - minY) / polyHeight else 0.5f
                        ),
                        handleIn = Offset(
                            x = if (polyWidth > 0) (node.handleIn.x - minX) / polyWidth else 0.5f,
                            y = if (polyHeight > 0) (node.handleIn.y - minY) / polyHeight else 0.5f
                        ),
                        handleOut = Offset(
                            x = if (polyWidth > 0) (node.handleOut.x - minX) / polyWidth else 0.5f,
                            y = if (polyHeight > 0) (node.handleOut.y - minY) / polyHeight else 0.5f
                        )
                    )
                }
                Triple(normalizedNodes, polyWidth, polyHeight)
            } else null
        } else null
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (offset.x * density).roundToInt(),
                    (offset.y * density).roundToInt()
                )
            }
            .wrapContentSize(unbounded = true)
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    this.scaleX = currentScaleX
                    this.scaleY = currentScaleY
                    rotationZ = currentRotation
                }
                .onSizeChanged { baseSize = it }
                .then(
                    if (!isLocked) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, rot ->
                                    val angleRad = currentRotation * (Math.PI / 180.0)
                                    val localPanX = pan.x * currentScaleX
                                    val localPanY = pan.y * currentScaleY

                                    val screenPanX = localPanX * cos(angleRad) - localPanY * sin(angleRad)
                                    val screenPanY = localPanX * sin(angleRad) + localPanY * cos(angleRad)
                                    offset += Offset(
                                        screenPanX.toFloat() / density,
                                        screenPanY.toFloat() / density
                                    )

                                    val newScaleX = (currentScaleX * zoom).coerceIn(0.1f, 10f)
                                    val newScaleY = (currentScaleY * zoom).coerceIn(0.1f, 10f)
                                    val newRotation = currentRotation + rot

                                    onScaleChanged(newScaleX, newScaleY)
                                    onUpdatePosition(offset, currentWidth, currentHeight, newRotation)
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    onSelect()
                                }
                            }
                    } else {
                        Modifier
                    }
                )
                .alpha(if (isLocked) 0.2f else 1f)
        ) {
            Box(
                modifier = Modifier
                    .width(currentWidth.dp)
                    .height(currentHeight.dp)
                    .padding(handleSize / 2),
                contentAlignment = Alignment.Center
            ) {
                if (note.shadowColor != null && note.shadowColor.alpha > 0f) {
                    val shadowModifier = Modifier
                        .fillMaxSize()
                        .offset(x = note.shadowOffsetX.dp, y = note.shadowOffsetY.dp)
                        .blur(radius = 2.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)

                    when {
                        note.content == "Shape: Square" -> SquareShape(shadowModifier, note.shadowColor)
                        note.content == "Shape: Circle" -> CircleShape(shadowModifier, note.shadowColor)
                        note.content == "Shape: Triangle" -> TriangleShape(shadowModifier, note.shadowColor)
                        note.content == "Shape: Line" -> LineShape(shadowModifier, note.shadowColor)
                        note.content == "Shape: Pentagon" -> {
                            val pentagonPoints = listOf(
                                Offset(0.5f, 0f), Offset(1f, 0.4f),
                                Offset(0.8f, 1f), Offset(0.2f, 1f), Offset(0f, 0.4f)
                            )
                            PolygonShape(pentagonPoints, shadowModifier, note.shadowColor)
                        }
                        note.content == "Shape: Hexagon" -> HexagonShape(shadowModifier, note.shadowColor)
                        note.content == "Shape: Star" -> StarShape(shadowModifier, note.shadowColor)
                        note.content == "Shape: Diamond" -> DiamondShape(shadowModifier, note.shadowColor)
                        note.content == "Shape: Heart" -> HeartShape(shadowModifier, note.shadowColor)
                        note.content == "Shape: ArrowRight" -> ArrowRightShape(shadowModifier, note.shadowColor)
                        note.content == "Shape: Octagon" -> OctagonShape(shadowModifier, note.shadowColor)
                        note.content == "Shape: Cross" -> CrossShape(shadowModifier, note.shadowColor)
                        note.content == "Shape: ThornCrown" -> ThornCrownShape(shadowModifier, note.shadowColor)
                        note.content == "Shape: Moon" -> MoonShape(shadowModifier, note.shadowColor)
                        note.content == "Shape: DavidStar" -> DavidStarShape(shadowModifier, note.shadowColor)
                        note.content == "Shape: Gear" -> GearShape(shadowModifier, note.shadowColor)
                        parsedData != null -> {
                            BezierPolygonShape(
                                nodes = parsedData.first,
                                modifier = shadowModifier,
                                color = note.shadowColor,
                                closed = !isCustomLine
                            )
                        }
                    }
                }
                val mainModifier = Modifier.fillMaxSize()
                when {
                    note.content == "Shape: Square" -> SquareShape(mainModifier, note.backgroundColor)
                    note.content == "Shape: Circle" -> CircleShape(mainModifier, note.backgroundColor)
                    note.content == "Shape: Triangle" -> TriangleShape(mainModifier, note.backgroundColor)
                    note.content == "Shape: Line" -> LineShape(mainModifier, note.backgroundColor)
                    note.content == "Shape: Pentagon" -> {
                        val pentagonPoints = listOf(
                            Offset(0.5f, 0f), Offset(1f, 0.4f),
                            Offset(0.8f, 1f), Offset(0.2f, 1f), Offset(0f, 0.4f)
                        )
                        PolygonShape(pentagonPoints, mainModifier, note.backgroundColor)
                    }
                    note.content == "Shape: Hexagon" -> HexagonShape(mainModifier, note.backgroundColor)
                    note.content == "Shape: Star" -> StarShape(mainModifier, note.backgroundColor)
                    note.content == "Shape: Diamond" -> DiamondShape(mainModifier, note.backgroundColor)
                    note.content == "Shape: Heart" -> HeartShape(mainModifier, note.backgroundColor)
                    note.content == "Shape: ArrowRight" -> ArrowRightShape(mainModifier, note.backgroundColor)
                    note.content == "Shape: Octagon" -> OctagonShape(mainModifier, note.backgroundColor)
                    note.content == "Shape: Cross" -> CrossShape(mainModifier, note.backgroundColor)
                    note.content == "Shape: ThornCrown" -> ThornCrownShape(mainModifier, note.backgroundColor)
                    note.content == "Shape: Moon" -> MoonShape(mainModifier, note.backgroundColor)
                    note.content == "Shape: DavidStar" -> DavidStarShape(mainModifier, note.backgroundColor)
                    note.content == "Shape: Gear" -> GearShape(mainModifier, note.backgroundColor)
                    parsedData != null -> {
                        BezierPolygonShape(
                            nodes = parsedData.first,
                            modifier = mainModifier,
                            color = note.backgroundColor,
                            closed = !isCustomLine
                        )
                    }
                }

                if (note.borderThickness > 0f && note.borderColor != null) {
                    val borderColor = note.borderColor
                    val strokeWidthPx = with(LocalDensity.current) { note.borderThickness.dp.toPx() }
                    val strokeStyle = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val borderModifier = Modifier.fillMaxSize()

                    when {
                        note.content == "Shape: Square" -> SquareShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        note.content == "Shape: Circle" -> CircleShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        note.content == "Shape: Triangle" -> TriangleShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        note.content == "Shape: Line" -> LineShape(borderModifier, borderColor, strokeWidth = strokeWidthPx)
                        note.content == "Shape: Pentagon" -> {
                            val pentagonPoints = listOf(
                                Offset(0.5f, 0f), Offset(1f, 0.4f),
                                Offset(0.8f, 1f), Offset(0.2f, 1f), Offset(0f, 0.4f)
                            )
                            PolygonShape(pentagonPoints, borderModifier, borderColor, drawStyle = strokeStyle)
                        }
                        note.content == "Shape: Hexagon" -> HexagonShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        note.content == "Shape: Star" -> StarShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        note.content == "Shape: Diamond" -> DiamondShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        note.content == "Shape: Heart" -> HeartShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        note.content == "Shape: ArrowRight" -> ArrowRightShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        note.content == "Shape: Octagon" -> OctagonShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        note.content == "Shape: Cross" -> CrossShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        note.content == "Shape: ThornCrown" -> {}
                        note.content == "Shape: Moon" -> MoonShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        note.content == "Shape: DavidStar" -> DavidStarShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        note.content == "Shape: Gear" -> GearShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        parsedData != null -> {
                            BezierPolygonShape(
                                nodes = parsedData.first,
                                modifier = borderModifier,
                                color = borderColor,
                                closed = !isCustomLine,
                                drawStyle = strokeStyle
                            )
                        }
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(0.5.dp, MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        if (isSelected && baseSize != IntSize.Zero && !isLocked) {
            val cx = baseSize.width / 2f
            val cy = baseSize.height / 2f

            fun getTargetHandleOffset(localX: Float, localY: Float): IntOffset {
                val dx = localX - cx
                val dy = localY - cy
                val scaledDx = dx * currentScaleX
                val scaledDy = dy * currentScaleY
                val rad = currentRotation * (Math.PI / 180.0)
                val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
                val ry = scaledDx * sin(rad) + scaledDy * cos(rad)
                return IntOffset(
                    (cx + rx - handleRadiusPx).roundToInt(),
                    (cy + ry - handleRadiusPx).roundToInt()
                )
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(handleRadiusPx, handleRadiusPx) }
                    .size(handleSize)
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onDeleteRequested() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(handleRadiusPx, baseSize.height - handleRadiusPx) }
                    .size(handleSize)
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onColorPickerRequested() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Palette, "Change Color", tint = Color.Blue, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(baseSize.width - handleRadiusPx, handleRadiusPx) }
                    .size(handleSize)
                    .pointerInput(baseSize) {
                        var startRotation = 0f
                        var accumulatedAngle = 0f
                        var currentVector = Offset.Zero
                        detectDragGestures(
                            onDragStart = {
                                startRotation = currentRotation
                                accumulatedAngle = 0f
                                val rad = startRotation * (Math.PI / 180.0)
                                val dx = (baseSize.width - handleRadiusPx) - cx
                                val dy = handleRadiusPx - cy
                                val scaledDx = dx * currentScaleX
                                val scaledDy = dy * currentScaleY
                                val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
                                val ry = scaledDx * sin(rad) + scaledDy * cos(rad)
                                currentVector = Offset(rx.toFloat(), ry.toFloat())
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val previousAngle = atan2(currentVector.y, currentVector.x)
                                currentVector += dragAmount
                                val newAngle = atan2(currentVector.y, currentVector.x)
                                var deltaAngle = Math.toDegrees((newAngle - previousAngle).toDouble()).toFloat()
                                if (deltaAngle > 180f) deltaAngle -= 360f
                                else if (deltaAngle < -180f) deltaAngle += 360f
                                accumulatedAngle += deltaAngle
                                val newRotation = startRotation + accumulatedAngle
                                onUpdatePosition(offset, currentWidth, currentHeight, newRotation)
                            }
                        )
                    }
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Refresh, "Rotate", tint = Color.DarkGray, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(baseSize.width - handleRadiusPx, baseSize.height - handleRadiusPx) }
                    .size(handleSize)
                    .pointerInput(baseSize) {
                        var startScaleX = 1f
                        var startScaleY = 1f
                        var fixedRotation = 0f
                        var currentVector = Offset.Zero
                        detectDragGestures(
                            onDragStart = {
                                startScaleX = currentScaleX
                                startScaleY = currentScaleY
                                fixedRotation = currentRotation
                                val rad = fixedRotation * (Math.PI / 180.0)
                                val dx = (baseSize.width - handleRadiusPx) - cx
                                val dy = (baseSize.height - handleRadiusPx) - cy
                                val scaledDx = dx * startScaleX
                                val scaledDy = dy * startScaleY
                                val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
                                val ry = scaledDx * sin(rad) + scaledDy * cos(rad)
                                currentVector = Offset(rx.toFloat(), ry.toFloat())
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentVector += dragAmount
                                val rad = fixedRotation * (Math.PI / 180.0)
                                val unrotatedRad = -rad
                                val newScaledDx = currentVector.x * cos(unrotatedRad) - currentVector.y * sin(unrotatedRad)
                                val newScaledDy = currentVector.x * sin(unrotatedRad) + currentVector.y * cos(unrotatedRad)
                                val dx = (baseSize.width - handleRadiusPx) - cx
                                val dy = (baseSize.height - handleRadiusPx) - cy
                                if (latestProportional) {
                                    val originalDist = sqrt(dx * dx + dy * dy)
                                    val newDist = sqrt(newScaledDx * newScaledDx + newScaledDy * newScaledDy)
                                    val uniformScale = if (originalDist > 0f) (newDist / originalDist).toFloat().coerceIn(0.1f, 10f) else 1f
                                    onScaleChanged(uniformScale, uniformScale)
                                } else {
                                    val newScaleX = if (dx != 0f) (newScaledDx / dx).toFloat().coerceIn(0.1f, 10f) else startScaleX
                                    val newScaleY = if (dy != 0f) (newScaledDy / dy).toFloat().coerceIn(0.1f, 10f) else startScaleY
                                    onScaleChanged(newScaleX, newScaleY)
                                }
                            }
                        )
                    }
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(10.dp).background(Color.DarkGray, CircleShape))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(baseSize.width / 2f, handleRadiusPx) }
                    .size(handleSize)
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onProportionalToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (proportionalEditing) Icons.Default.Link else Icons.Default.LinkOff,
                    contentDescription = if (proportionalEditing) "Disable proportional" else "Enable proportional",
                    tint = if (proportionalEditing) Color(0xFF1976D2) else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun CanvasTextItem(
    note: CanvasNote,
    isSelected: Boolean,
    isLocked: Boolean,
    onSelect: () -> Unit,
    onUpdatePosition: (Offset, Float, Float, Float) -> Unit,
    onScaleChanged: (Float, Float) -> Unit,
    onColorPickerRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    proportionalEditing: Boolean,
    onProportionalToggle: () -> Unit
) {
    val density = LocalDensity.current.density
    var offset by remember(note.offset) { mutableStateOf(note.offset) }
    var baseSize by remember { mutableStateOf(IntSize.Zero) }
    val currentRotation by rememberUpdatedState(note.rotation)
    val currentScaleX by rememberUpdatedState(note.scaleX)
    val currentScaleY by rememberUpdatedState(note.scaleY)
    val currentWidth by rememberUpdatedState(note.width)
    val currentHeight by rememberUpdatedState(note.height)

    val handleSize = 24.dp
    val handleRadiusPx = with(LocalDensity.current) { handleSize.toPx() / 2f }

    val latestProportional by rememberUpdatedState(proportionalEditing)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (offset.x * density).roundToInt(),
                    (offset.y * density).roundToInt()
                )
            }
            .wrapContentSize(unbounded = true)
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    this.scaleX = currentScaleX
                    this.scaleY = currentScaleY
                    rotationZ = currentRotation
                }
                .onSizeChanged { baseSize = it }
                .then(
                    if (!isLocked) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, rot ->
                                    val angleRad = currentRotation * (Math.PI / 180.0)
                                    val localPanX = pan.x * currentScaleX
                                    val localPanY = pan.y * currentScaleY

                                    val screenPanX = localPanX * cos(angleRad) - localPanY * sin(angleRad)
                                    val screenPanY = localPanX * sin(angleRad) + localPanY * cos(angleRad)
                                    offset += Offset(
                                        screenPanX.toFloat() / density,
                                        screenPanY.toFloat() / density
                                    )

                                    val newScaleX = (currentScaleX * zoom).coerceIn(0.1f, 10f)
                                    val newScaleY = (currentScaleY * zoom).coerceIn(0.1f, 10f)
                                    val newRotation = currentRotation + rot

                                    onScaleChanged(newScaleX, newScaleY)
                                    onUpdatePosition(offset, currentWidth, currentHeight, newRotation)
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures { onSelect() }
                            }
                    } else {
                        Modifier
                    }
                )
                .alpha(if (isLocked) 0.2f else 1f)
        ) {
            Box(
                modifier = Modifier
                    .padding(handleSize / 2),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(0.5.dp, MaterialTheme.colorScheme.primary)
                    )
                }
                Box(
                    modifier = Modifier.onSizeChanged { size ->
                        val newWidth = size.width / density
                        val newHeight = size.height / density
                        if ((newWidth - currentWidth).absoluteValue > 1f ||
                            (newHeight - currentHeight).absoluteValue > 1f
                        ) {
                            onUpdatePosition(offset, newWidth, newHeight, currentRotation)
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    val textShadow = if (note.shadowColor != null && note.shadowColor.alpha > 0f) {
                        Shadow(
                            color = note.shadowColor,
                            offset = Offset(note.shadowOffsetX * density, note.shadowOffsetY * density),
                            blurRadius = 2f * density
                        )
                    } else null

                    if (note.borderThickness > 0f && note.borderColor != null) {
                        Text(
                            text = note.content,
                            color = note.borderColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                shadow = textShadow,
                                drawStyle = Stroke(
                                    width = note.borderThickness * density,
                                    join = StrokeJoin.Round
                                )
                            ),
                            modifier = Modifier
                                .padding(8.dp)
                                .widthIn(max = 250.dp)
                        )
                    }

                    Text(
                        text = note.content,
                        color = note.textColor ?: Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            shadow = if (note.borderThickness > 0f && note.borderColor != null) null else textShadow
                        ),
                        modifier = Modifier
                            .padding(8.dp)
                            .widthIn(max = 250.dp)
                    )
                }
            }
        }
        if (isSelected && baseSize != IntSize.Zero && !isLocked) {
            val cx = baseSize.width / 2f
            val cy = baseSize.height / 2f

            fun getTargetHandleOffset(localX: Float, localY: Float): IntOffset {
                val dx = localX - cx
                val dy = localY - cy
                val scaledDx = dx * currentScaleX
                val scaledDy = dy * currentScaleY
                val rad = currentRotation * (Math.PI / 180.0)
                val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
                val ry = scaledDx * sin(rad) + scaledDy * cos(rad)
                return IntOffset(
                    (cx + rx - handleRadiusPx).roundToInt(),
                    (cy + ry - handleRadiusPx).roundToInt()
                )
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(handleRadiusPx, handleRadiusPx) }
                    .size(handleSize)
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onDeleteRequested() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(handleRadiusPx, baseSize.height - handleRadiusPx) }
                    .size(handleSize)
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onColorPickerRequested() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Palette, "Change Color", tint = Color.Blue, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(baseSize.width - handleRadiusPx, handleRadiusPx) }
                    .size(handleSize)
                    .pointerInput(baseSize) {
                        var startRotation = 0f
                        var accumulatedAngle = 0f
                        var currentVector = Offset.Zero
                        detectDragGestures(
                            onDragStart = {
                                startRotation = currentRotation
                                accumulatedAngle = 0f
                                val rad = startRotation * (Math.PI / 180.0)
                                val dx = (baseSize.width - handleRadiusPx) - cx
                                val dy = handleRadiusPx - cy
                                val scaledDx = dx * currentScaleX
                                val scaledDy = dy * currentScaleY
                                val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
                                val ry = scaledDx * sin(rad) + scaledDy * cos(rad)
                                currentVector = Offset(rx.toFloat(), ry.toFloat())
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val previousAngle = atan2(currentVector.y, currentVector.x)
                                currentVector += dragAmount
                                val newAngle = atan2(currentVector.y, currentVector.x)
                                var deltaAngle = Math.toDegrees((newAngle - previousAngle).toDouble()).toFloat()
                                if (deltaAngle > 180f) deltaAngle -= 360f
                                else if (deltaAngle < -180f) deltaAngle += 360f
                                accumulatedAngle += deltaAngle
                                val newRotation = startRotation + accumulatedAngle
                                onUpdatePosition(offset, currentWidth, currentHeight, newRotation)
                            }
                        )
                    }
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Refresh, "Rotate", tint = Color.DarkGray, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(baseSize.width - handleRadiusPx, baseSize.height - handleRadiusPx) }
                    .size(handleSize)
                    .pointerInput(baseSize) {
                        var startScaleX = 1f
                        var startScaleY = 1f
                        var fixedRotation = 0f
                        var currentVector = Offset.Zero
                        detectDragGestures(
                            onDragStart = {
                                startScaleX = currentScaleX
                                startScaleY = currentScaleY
                                fixedRotation = currentRotation
                                val rad = fixedRotation * (Math.PI / 180.0)
                                val dx = (baseSize.width - handleRadiusPx) - cx
                                val dy = (baseSize.height - handleRadiusPx) - cy
                                val scaledDx = dx * startScaleX
                                val scaledDy = dy * startScaleY
                                val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
                                val ry = scaledDx * sin(rad) + scaledDy * cos(rad)
                                currentVector = Offset(rx.toFloat(), ry.toFloat())
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentVector += dragAmount
                                val rad = fixedRotation * (Math.PI / 180.0)
                                val unrotatedRad = -rad
                                val newScaledDx = currentVector.x * cos(unrotatedRad) - currentVector.y * sin(unrotatedRad)
                                val newScaledDy = currentVector.x * sin(unrotatedRad) + currentVector.y * cos(unrotatedRad)
                                val dx = (baseSize.width - handleRadiusPx) - cx
                                val dy = (baseSize.height - handleRadiusPx) - cy
                                if (latestProportional) {
                                    val originalDist = sqrt(dx * dx + dy * dy)
                                    val newDist = sqrt(newScaledDx * newScaledDx + newScaledDy * newScaledDy)
                                    val uniformScale = if (originalDist > 0f) (newDist / originalDist).toFloat().coerceIn(0.1f, 10f) else 1f
                                    onScaleChanged(uniformScale, uniformScale)
                                } else {
                                    val newScaleX = if (dx != 0f) (newScaledDx / dx).toFloat().coerceIn(0.1f, 10f) else startScaleX
                                    val newScaleY = if (dy != 0f) (newScaledDy / dy).toFloat().coerceIn(0.1f, 10f) else startScaleY
                                    onScaleChanged(newScaleX, newScaleY)
                                }
                            }
                        )
                    }
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(10.dp).background(Color.DarkGray, CircleShape))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(baseSize.width / 2f, handleRadiusPx) }
                    .size(handleSize)
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onProportionalToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (proportionalEditing) Icons.Default.Link else Icons.Default.LinkOff,
                    contentDescription = if (proportionalEditing) "Disable proportional" else "Enable proportional",
                    tint = if (proportionalEditing) Color(0xFF1976D2) else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun CanvasImageItem(
    note: CanvasNote,
    isSelected: Boolean,
    isLocked: Boolean,
    onSelect: () -> Unit,
    onUpdatePosition: (Offset, Float, Float, Float) -> Unit,
    onScaleChanged: (Float, Float) -> Unit,
    onColorPickerRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    proportionalEditing: Boolean,
    onProportionalToggle: () -> Unit
) {
    var offset by remember(note.offset) { mutableStateOf(note.offset) }
    var rotation by remember(note.id, note.rotation) { mutableFloatStateOf(note.rotation) }
    val scaleX by rememberUpdatedState(note.scaleX)
    val scaleY by rememberUpdatedState(note.scaleY)

    var baseSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current.density

    val handleSize = 24.dp
    val handleRadiusPx = with(LocalDensity.current) { handleSize.toPx() / 2f }

    val latestProportional by rememberUpdatedState(proportionalEditing)

    val uriString = note.content.removePrefix("Image: ")
    val uri = uriString.toUri()

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (offset.x * density).roundToInt(),
                    (offset.y * density).roundToInt()
                )
            }
            .wrapContentSize(unbounded = true)
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                    rotationZ = rotation
                }
                .onSizeChanged { baseSize = it }
                .then(
                    if (!isLocked) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, rot ->
                                    val angleRad = rotation * (Math.PI / 180.0)
                                    val localPanX = pan.x * scaleX
                                    val localPanY = pan.y * scaleY

                                    val screenPanX = localPanX * cos(angleRad) - localPanY * sin(angleRad)
                                    val screenPanY = localPanX * sin(angleRad) + localPanY * cos(angleRad)
                                    offset += Offset(
                                        screenPanX.toFloat() / density,
                                        screenPanY.toFloat() / density
                                    )

                                    val newScaleX = (scaleX * zoom).coerceIn(0.1f, 10f)
                                    val newScaleY = (scaleY * zoom).coerceIn(0.1f, 10f)
                                    rotation += rot

                                    onScaleChanged(newScaleX, newScaleY)
                                    onUpdatePosition(offset, note.width, note.height, rotation)
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures { onSelect() }
                            }
                    } else {
                        Modifier
                    }
                )
                .alpha(if (isLocked) 0.2f else 1f)
        ) {
            Box(
                modifier = Modifier
                    .width(note.width.dp)
                    .height(note.height.dp)
                    .padding(handleSize / 2),
                contentAlignment = Alignment.Center
            ) {

                if (note.shadowColor != null && note.shadowColor.alpha > 0f) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Image Shadow",
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = note.shadowOffsetX.dp, y = note.shadowOffsetY.dp)
                            .blur(radius = 2.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(note.shadowColor)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(0.5.dp, MaterialTheme.colorScheme.primary)
                    )
                }

                AsyncImage(
                    model = uri,
                    contentDescription = "Canvas Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(note.backgroundColor)
                )
            }
        }

        if (isSelected && baseSize != IntSize.Zero && !isLocked) {
            val cx = baseSize.width / 2f
            val cy = baseSize.height / 2f

            fun getTargetHandleOffset(localX: Float, localY: Float): IntOffset {
                val dx = localX - cx
                val dy = localY - cy
                val scaledDx = dx * scaleX
                val scaledDy = dy * scaleY
                val rad = rotation * (Math.PI / 180.0)
                val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
                val ry = scaledDx * sin(rad) + scaledDy * cos(rad)
                return IntOffset(
                    (cx + rx - handleRadiusPx).roundToInt(),
                    (cy + ry - handleRadiusPx).roundToInt()
                )
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(handleRadiusPx, handleRadiusPx) }
                    .size(handleSize)
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onDeleteRequested() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(handleRadiusPx, baseSize.height - handleRadiusPx) }
                    .size(handleSize)
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onColorPickerRequested() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Palette, "Change Color", tint = Color.Blue, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(baseSize.width - handleRadiusPx, handleRadiusPx) }
                    .size(handleSize)
                    .pointerInput(baseSize) {
                        var startRotation = 0f
                        var accumulatedAngle = 0f
                        var currentVector = Offset.Zero

                        detectDragGestures(
                            onDragStart = {
                                startRotation = rotation
                                accumulatedAngle = 0f
                                val rad = startRotation * (Math.PI / 180.0)
                                val dx = (baseSize.width - handleRadiusPx) - cx
                                val dy = handleRadiusPx - cy
                                val scaledDx = dx * scaleX
                                val scaledDy = dy * scaleY
                                val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
                                val ry = scaledDx * sin(rad) + scaledDy * cos(rad)
                                currentVector = Offset(rx.toFloat(), ry.toFloat())
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val previousAngle = atan2(currentVector.y, currentVector.x)
                                currentVector += dragAmount
                                val newAngle = atan2(currentVector.y, currentVector.x)
                                var deltaAngle = Math.toDegrees((newAngle - previousAngle).toDouble()).toFloat()
                                if (deltaAngle > 180f) deltaAngle -= 360f
                                else if (deltaAngle < -180f) deltaAngle += 360f
                                accumulatedAngle += deltaAngle
                                rotation = startRotation + accumulatedAngle
                                onUpdatePosition(offset, note.width, note.height, rotation)
                            }
                        )
                    }
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Refresh, "Rotate", tint = Color.DarkGray, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(baseSize.width - handleRadiusPx, baseSize.height - handleRadiusPx) }
                    .size(handleSize)
                    .pointerInput(baseSize) {
                        var startScaleX = 1f
                        var startScaleY = 1f
                        var fixedRotation = 0f
                        var currentVector = Offset.Zero

                        detectDragGestures(
                            onDragStart = {
                                startScaleX = scaleX
                                startScaleY = scaleY
                                fixedRotation = rotation

                                val rad = fixedRotation * (Math.PI / 180.0)
                                val dx = (baseSize.width - handleRadiusPx) - cx
                                val dy = (baseSize.height - handleRadiusPx) - cy
                                val scaledDx = dx * startScaleX
                                val scaledDy = dy * startScaleY
                                val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
                                val ry = scaledDx * sin(rad) + scaledDy * cos(rad)
                                currentVector = Offset(rx.toFloat(), ry.toFloat())
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentVector += dragAmount

                                val rad = fixedRotation * (Math.PI / 180.0)
                                val unrotatedRad = -rad
                                val newScaledDx = currentVector.x * cos(unrotatedRad) - currentVector.y * sin(unrotatedRad)
                                val newScaledDy = currentVector.x * sin(unrotatedRad) + currentVector.y * cos(unrotatedRad)

                                val dx = (baseSize.width - handleRadiusPx) - cx
                                val dy = (baseSize.height - handleRadiusPx) - cy

                                if (latestProportional) {
                                    val originalDist = sqrt(dx * dx + dy * dy)
                                    val newDist = sqrt(newScaledDx * newScaledDx + newScaledDy * newScaledDy)
                                    val uniformScale = if (originalDist > 0f) (newDist / originalDist).toFloat().coerceIn(0.1f, 10f) else 1f
                                    onScaleChanged(uniformScale, uniformScale)
                                } else {
                                    val newScaleX = if (dx != 0f) (newScaledDx / dx).toFloat().coerceIn(0.1f, 10f) else startScaleX
                                    val newScaleY = if (dy != 0f) (newScaledDy / dy).toFloat().coerceIn(0.1f, 10f) else startScaleY
                                    onScaleChanged(newScaleX, newScaleY)
                                }
                            }
                        )
                    }
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(10.dp).background(Color.DarkGray, CircleShape))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(baseSize.width / 2f, handleRadiusPx) }
                    .size(handleSize)
                    .background(Color.White, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onProportionalToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (proportionalEditing) Icons.Default.Link else Icons.Default.LinkOff,
                    contentDescription = if (proportionalEditing) "Disable proportional" else "Enable proportional",
                    tint = if (proportionalEditing) Color(0xFF1976D2) else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun CustomPathPreview(
    pointsData: String,
    isClosed: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val segments = pointsData.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (segments.size >= 2) {
            val path = Path()
            var firstX = 0f
            var firstY = 0f
            var lastX = 0f
            var lastY = 0f
            var firstHandles = emptyList<String>()

            for (i in segments.indices) {
                val segment = segments[i]
                val parts = segment.split(":")

                val mainCoords = parts[0].split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (mainCoords.size < 2) continue

                val px = (mainCoords[0].toFloatOrNull() ?: 0f) * w
                val py = (mainCoords[1].toFloatOrNull() ?: 0f) * h

                if (i == 0) {
                    firstX = px
                    firstY = py
                    firstHandles = if (parts.size > 1) {
                        parts[1].split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    } else {
                        emptyList()
                    }
                    path.moveTo(px, py)
                } else {
                    val handles = if (parts.size > 1) {
                        parts[1].split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    } else {
                        emptyList()
                    }

                    when (handles.size) {
                        4 -> {
                            val cp1x = (handles[0].toFloatOrNull() ?: 0f) * w
                            val cp1y = (handles[1].toFloatOrNull() ?: 0f) * h
                            val cp2x = (handles[2].toFloatOrNull() ?: 0f) * w
                            val cp2y = (handles[3].toFloatOrNull() ?: 0f) * h
                            path.cubicTo(cp1x, cp1y, cp2x, cp2y, px, py)
                        }
                        2 -> {
                            val cpx = (handles[0].toFloatOrNull() ?: 0f) * w
                            val cpy = (handles[1].toFloatOrNull() ?: 0f) * h
                            path.quadraticTo(cpx, cpy, px, py)
                        }
                        else -> {
                            path.lineTo(px, py)
                        }
                    }
                }
                lastX = px
                lastY = py
            }

            if (isClosed) {
                val isAlreadyAtStart = abs(lastX - firstX) < 0.5f && abs(lastY - firstY) < 0.5f

                if (!isAlreadyAtStart) {
                    when (firstHandles.size) {
                        4 -> {
                            val cp1x = (firstHandles[0].toFloatOrNull() ?: 0f) * w
                            val cp1y = (firstHandles[1].toFloatOrNull() ?: 0f) * h
                            val cp2x = (firstHandles[2].toFloatOrNull() ?: 0f) * w
                            val cp2y = (firstHandles[3].toFloatOrNull() ?: 0f) * h
                            path.cubicTo(cp1x, cp1y, cp2x, cp2y, firstX, firstY)
                        }
                        2 -> {
                            val cpx = (firstHandles[0].toFloatOrNull() ?: 0f) * w
                            val cpy = (firstHandles[1].toFloatOrNull() ?: 0f) * h
                            path.quadraticTo(cpx, cpy, firstX, firstY)
                        }
                        else -> {
                        }
                    }
                }

                path.close()

                drawPath(path = path, color = color, style = Fill)
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.8f),
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else {
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 0.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}