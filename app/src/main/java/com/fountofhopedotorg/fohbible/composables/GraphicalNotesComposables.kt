package com.fountofhopedotorg.fohbible.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fountofhopedotorg.fohbible.data.CanvasNote
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


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
fun SquareShape(modifier: Modifier = Modifier, color: Color = randomColor().copy(0.4f),) {
    Canvas(modifier = modifier) {
        drawRect(color = color)
    }
}

@Composable
fun CircleShape(modifier: Modifier = Modifier, color: Color = randomColor().copy(0.4f),) {
    Canvas(modifier = modifier) {
        drawCircle(color = color)
    }
}

@Composable
fun TriangleShape(modifier: Modifier = Modifier, color: Color = randomColor().copy(0.4f),) {
    Canvas(modifier = modifier) {
        val trianglePath = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path = trianglePath, color = color)
    }
}

@Composable
fun PolygonShape(
    points: List<Offset>,
    modifier: Modifier = Modifier,
    color: Color = randomColor().copy(0.8f),
) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        val path = Path().apply {
            moveTo(points[0].x * size.width, points[0].y * size.height)
            for (i in 1 until points.size) {
                lineTo(points[i].x * size.width, points[i].y * size.height)
            }
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
fun CanvasSvgItem(
    note: CanvasNote,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdatePosition: (Offset, Float, Float) -> Unit,
    onColorPickerRequested: () -> Unit,
    onDeleteRequested: () -> Unit
) {
    var offset by remember { mutableStateOf(Offset(50f, 50f)) }
    var scaleX by remember { mutableFloatStateOf(1f) }
    var scaleY by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }

    var baseSize by remember { mutableStateOf(IntSize.Zero) }

    val handleSize = 24.dp
    val handleRadiusPx = with(LocalDensity.current) { handleSize.toPx() / 2f }
    val isCustomPolygon = note.content.startsWith("Shape: CustomPolygon:")

    val parsedData = remember(note.content) {
        if (isCustomPolygon) {
            val serializedPoints = note.content.removePrefix("Shape: CustomPolygon:")
            val rawPoints = serializedPoints.split(";").mapNotNull { pointStr ->
                val coords = pointStr.split(",")
                if (coords.size == 2) {
                    val x = coords[0].toFloatOrNull() ?: 0f
                    val y = coords[1].toFloatOrNull() ?: 0f
                    Offset(x, y)
                } else null
            }

            if (rawPoints.size >= 3) {
                val minX = rawPoints.minOf { it.x }
                val maxX = rawPoints.maxOf { it.x }
                val minY = rawPoints.minOf { it.y }
                val maxY = rawPoints.maxOf { it.y }

                val polyWidth = maxX - minX
                val polyHeight = maxY - minY

                val normalizedPoints = rawPoints.map { pt ->
                    Offset(
                        x = if (polyWidth > 0) (pt.x - minX) / polyWidth else 0.5f,
                        y = if (polyHeight > 0) (pt.y - minY) / polyHeight else 0.5f
                    )
                }
                Triple(normalizedPoints, polyWidth, polyHeight)
            } else null
        } else null
    }

    val baseWidthDp = if (parsedData != null) (parsedData.second * 200f).dp else 200.dp
    val baseHeightDp = if (parsedData != null) (parsedData.third * 200f).dp else 200.dp

    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
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
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rot ->
                        val angleRad = rotation * (Math.PI / 180.0)
                        val localPanX = pan.x * scaleX
                        val localPanY = pan.y * scaleY

                        val screenPanX = localPanX * cos(angleRad) - localPanY * sin(angleRad)
                        val screenPanY = localPanX * sin(angleRad) + localPanY * cos(angleRad)
                        offset += Offset(screenPanX.toFloat(), screenPanY.toFloat())

                        scaleX = (scaleX * zoom).coerceIn(0.2f, 5f)
                        scaleY = (scaleY * zoom).coerceIn(0.2f, 5f)
                        rotation += rot
                        onUpdatePosition(offset, 0f, 0f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        onSelect()
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .width(baseWidthDp)
                    .height(baseHeightDp)
                    .padding(handleSize / 2),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(1.dp, Color.Gray.copy(alpha = 0.5f))
                    )
                }

                when {
                    note.content == "Shape: Square" -> SquareShape(modifier = Modifier.fillMaxSize(), color = note.backgroundColor)
                    note.content == "Shape: Circle" -> CircleShape(modifier = Modifier.fillMaxSize(), color = note.backgroundColor)
                    note.content == "Shape: Triangle" -> TriangleShape(modifier = Modifier.fillMaxSize(), color = note.backgroundColor)

                    note.content == "Shape: Pentagon" -> {
                        val pentagonPoints = listOf(
                            Offset(0.5f, 0f),
                            Offset(1f, 0.4f),
                            Offset(0.8f, 1f),
                            Offset(0.2f, 1f),
                            Offset(0f, 0.4f)
                        )
                        PolygonShape(
                            points = pentagonPoints,
                            modifier = Modifier.fillMaxSize(),
                            color = note.backgroundColor
                        )
                    }

                    parsedData != null -> {
                        PolygonShape(
                            points = parsedData.first,
                            modifier = Modifier.fillMaxSize(),
                            color = note.backgroundColor
                        )
                    }
                }
            }
        }
        if (isSelected && baseSize != IntSize.Zero) {
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
                    .offset { getTargetHandleOffset(0f, 0f) }
                    .size(handleSize)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                    .clickable { onDeleteRequested() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
            }
            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(0f, baseSize.height.toFloat()) }
                    .size(handleSize)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                    .clickable { onColorPickerRequested() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Palette, contentDescription = "Change Color", tint = Color.Blue, modifier = Modifier.size(14.dp))
            }
            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(baseSize.width.toFloat(), 0f) }
                    .size(handleSize)
                    .pointerInput(baseSize) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()

                            val rad = rotation * (Math.PI / 180.0)
                            val dx = baseSize.width.toFloat() - cx
                            val dy = 0f - cy

                            val scaledDx = dx * scaleX
                            val scaledDy = dy * scaleY
                            val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
                            val ry = scaledDx * sin(rad) + scaledDy * cos(rad)

                            val currentAngle = atan2(ry, rx)
                            val newAngle = atan2(ry + dragAmount.y, rx + dragAmount.x)

                            var deltaAngle = Math.toDegrees(newAngle - currentAngle).toFloat()
                            if (deltaAngle > 180f) deltaAngle -= 360f
                            else if (deltaAngle < -180f) deltaAngle += 360f

                            rotation += deltaAngle
                        }
                    }
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Rotate", tint = Color.DarkGray, modifier = Modifier.size(14.dp))
            }
            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(baseSize.width.toFloat(), baseSize.height.toFloat()) }
                    .size(handleSize)
                    .pointerInput(baseSize) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()

                            val rad = rotation * (Math.PI / 180.0)
                            val dx = baseSize.width.toFloat() - cx
                            val dy = baseSize.height.toFloat() - cy

                            val scaledDx = dx * scaleX
                            val scaledDy = dy * scaleY
                            val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
                            val ry = scaledDx * sin(rad) + scaledDy * cos(rad)

                            val newRx = rx + dragAmount.x
                            val newRy = ry + dragAmount.y

                            val unrotatedRad = -rad
                            val newScaledDx = newRx * cos(unrotatedRad) - newRy * sin(unrotatedRad)
                            val newScaledDy = newRx * sin(unrotatedRad) + newRy * cos(unrotatedRad)

                            if (dx != 0f) scaleX = (newScaledDx / dx).toFloat().coerceIn(0.2f, 5f)
                            if (dy != 0f) scaleY = (newScaledDy / dy).toFloat().coerceIn(0.2f, 5f)
                        }
                    }
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(10.dp).background(Color.DarkGray, CircleShape))
            }
        }
    }
}

@Composable
fun CanvasTextItem(
    note: CanvasNote,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdatePosition: (Offset, Float, Float) -> Unit,
    onColorPickerRequested: () -> Unit,
    onDeleteRequested: () -> Unit
) {
    var offset by remember { mutableStateOf(Offset(50f, 50f)) }
    var scaleX by remember { mutableFloatStateOf(1f) }
    var scaleY by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }

    var baseSize by remember { mutableStateOf(IntSize.Zero) }

    val handleSize = 24.dp
    val handleRadiusPx = with(LocalDensity.current) { handleSize.toPx() / 2f }

    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
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
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rot ->
                        val angleRad = rotation * (Math.PI / 180.0)
                        val localPanX = pan.x * scaleX
                        val localPanY = pan.y * scaleY

                        val screenPanX = localPanX * cos(angleRad) - localPanY * sin(angleRad)
                        val screenPanY = localPanX * sin(angleRad) + localPanY * cos(angleRad)
                        offset += Offset(screenPanX.toFloat(), screenPanY.toFloat())

                        scaleX = (scaleX * zoom).coerceIn(0.2f, 5f)
                        scaleY = (scaleY * zoom).coerceIn(0.2f, 5f)
                        rotation += rot
                        onUpdatePosition(offset, 0f, 0f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        onSelect()
                    }
                }
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
                            .border(1.dp, Color.Gray.copy(alpha = 0.5f))
                    )
                }

                Text(
                    text = note.content,
                    color = note.backgroundColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(8.dp)
                        .widthIn(max = 250.dp)
                )
            }
        }

        if (isSelected && baseSize != IntSize.Zero) {
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
                    .offset { getTargetHandleOffset(0f, 0f) }
                    .size(handleSize)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                    .clickable { onDeleteRequested() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(0f, baseSize.height.toFloat()) }
                    .size(handleSize)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                    .clickable { onColorPickerRequested() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Palette, contentDescription = "Change Color", tint = Color.Blue, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(baseSize.width.toFloat(), 0f) }
                    .size(handleSize)
                    .pointerInput(baseSize) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()

                            val rad = rotation * (Math.PI / 180.0)
                            val dx = baseSize.width.toFloat() - cx
                            val dy = 0f - cy

                            val scaledDx = dx * scaleX
                            val scaledDy = dy * scaleY
                            val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
                            val ry = scaledDx * sin(rad) + scaledDy * cos(rad)

                            val currentAngle = atan2(ry, rx)
                            val newAngle = atan2(ry + dragAmount.y, rx + dragAmount.x)

                            var deltaAngle = Math.toDegrees(newAngle - currentAngle).toFloat()
                            if (deltaAngle > 180f) deltaAngle -= 360f
                            else if (deltaAngle < -180f) deltaAngle += 360f

                            rotation += deltaAngle
                        }
                    }
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Rotate", tint = Color.DarkGray, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier = Modifier
                    .offset { getTargetHandleOffset(baseSize.width.toFloat(), baseSize.height.toFloat()) }
                    .size(handleSize)
                    .pointerInput(baseSize) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()

                            val rad = rotation * (Math.PI / 180.0)
                            val dx = baseSize.width.toFloat() - cx
                            val dy = baseSize.height.toFloat() - cy

                            val scaledDx = dx * scaleX
                            val scaledDy = dy * scaleY
                            val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
                            val ry = scaledDx * sin(rad) + scaledDy * cos(rad)

                            val newRx = rx + dragAmount.x
                            val newRy = ry + dragAmount.y

                            val unrotatedRad = -rad
                            val newScaledDx = newRx * cos(unrotatedRad) - newRy * sin(unrotatedRad)
                            val newScaledDy = newRx * sin(unrotatedRad) + newRy * cos(unrotatedRad)

                            if (dx != 0f) scaleX = (newScaledDx / dx).toFloat().coerceIn(0.2f, 5f)
                            if (dy != 0f) scaleY = (newScaledDy / dy).toFloat().coerceIn(0.2f, 5f)
                        }
                    }
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(10.dp).background(Color.DarkGray, CircleShape))
            }
        }
    }
}