package com.fountofhopedotorg.fohbible.gfx_creator

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.OpenWith
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size as Size2
import com.fountofhopedotorg.fohbible.data.BezierNodeData
import com.fountofhopedotorg.fohbible.data.CanvasElement
import com.fountofhopedotorg.fohbible.data.GradientConfig
import com.fountofhopedotorg.fohbible.gfx_animator.PivotHandle
import com.fountofhopedotorg.fohbible.utils.getFontFamily
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

fun GradientConfig.toBrush(size: Size): Brush = Brush.linearGradient(
    start = Offset(startOffset.x * size.width, startOffset.y * size.height),
    end = Offset(endOffset.x * size.width, endOffset.y * size.height),
    colors = listOf(startColor, endColor)
)

fun Modifier.requiredSizePx(width: Float, height: Float) = this.layout { measurable, _ ->
    val w = width.roundToInt().coerceAtLeast(0)
    val h = height.roundToInt().coerceAtLeast(0)
    val placeable = measurable.measure(Constraints.fixed(w, h))
    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
}

fun Modifier.paddingPx(all: Float) = this.layout { measurable, constraints ->
    val p = all.roundToInt()
    val horizontal = p * 2
    val vertical = p * 2
    val newConstraints = constraints.offset(-horizontal, -vertical)
    val placeable = measurable.measure(newConstraints)
    val w = placeable.width + horizontal
    val h = placeable.height + vertical
    layout(w, h) { placeable.placeRelative(p, p) }
}

@Composable
fun ShapeCanvas(
    modifier: Modifier = Modifier,
    defaultColor: Color,
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig?,
    pathBuilder: (Size) -> Path
) {
    Canvas(modifier = modifier) {
        val path = pathBuilder(size)
        val brush = gradientConfig?.toBrush(size) ?: SolidColor(defaultColor)
        drawPath(path = path, brush = brush, style = drawStyle)
    }
}

fun regularPolygonPath(sides: Int, size: Size): Path {
    val path = Path()
    val angleOffset = -PI / 2
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = minOf(centerX, centerY)
    for (i in 0 until sides) {
        val angle = angleOffset + 2.0 * PI * i / sides
        val x = centerX + radius * cos(angle).toFloat()
        val y = centerY + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

@Composable
fun PentagonShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) { regularPolygonPath(5, it) }

@Composable
fun HexagonShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) { regularPolygonPath(6, it) }

@Composable
fun OctagonShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) { regularPolygonPath(8, it) }

@Composable
fun StarShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) { size ->
    val path = Path()
    val points = 5
    val outerR = minOf(size.width, size.height) / 2f
    val innerR = outerR * 0.4f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val offset = -PI / 2
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerR else innerR
        val angle = offset + PI * i / points
        val x = cx + r * cos(angle).toFloat()
        val y = cy + r * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    path
}

@Composable
fun DiamondShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) { size ->
    Path().apply {
        val w = size.width
        val h = size.height
        moveTo(w / 2f, 0f)
        quadraticTo(w * 0.68f, h * 0.32f, w, h / 2f)
        quadraticTo(w * 0.68f, h * 0.68f, w / 2f, h)
        quadraticTo(w * 0.32f, h * 0.68f, 0f, h / 2f)
        quadraticTo(w * 0.32f, h * 0.32f, w / 2f, 0f)
        close()
    }
}

@Composable
fun MoonShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) { size ->
    val moon = Path().apply { addOval(Rect(0f, 0f, size.width, size.height)) }
    val cutter = Path().apply {
        addOval(Rect(size.width * 0.35f, -size.height * 0.05f, size.width * 1.35f, size.height * 1.05f))
    }
    Path.combine(PathOperation.Difference, moon, cutter)
}

@Composable
fun CrossShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) { size ->
    Path().apply {
        val w = size.width
        val h = size.height
        moveTo(w * 0.38f, h * 0.05f)
        lineTo(w * 0.50f, 0f)
        lineTo(w * 0.62f, h * 0.05f)
        lineTo(w * 0.60f, h * 0.21f)
        quadraticTo(w * 0.60f, h * 0.28f, w * 0.68f, h * 0.28f)
        lineTo(w * 0.9f, h * 0.24f)
        lineTo(w * 0.98f, h * 0.35f)
        lineTo(w * 0.9f, h * 0.46f)
        lineTo(w * 0.68f, h * 0.42f)
        quadraticTo(w * 0.60f, h * 0.42f, w * 0.60f, h * 0.52f)
        lineTo(w * 0.62f, h * 0.95f)
        lineTo(w * 0.50f, h)
        lineTo(w * 0.38f, h * 0.95f)
        lineTo(w * 0.40f, h * 0.52f)
        quadraticTo(w * 0.40f, h * 0.42f, w * 0.32f, h * 0.42f)
        lineTo(w * 0.1f, h * 0.46f)
        lineTo(w * 0.02f, h * 0.35f)
        lineTo(w * 0.1f, h * 0.24f)
        lineTo(w * 0.32f, h * 0.28f)
        quadraticTo(w * 0.40f, h * 0.28f, w * 0.40f, h * 0.21f)
        close()
    }
}

@Composable
fun HeartShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) { size ->
    Path().apply {
        moveTo(size.width * 0.5f, size.height * 0.32f)
        cubicTo(
            size.width * 0.32f, size.height * 0.08f,
            size.width * 0.06f, size.height * 0.16f,
            size.width * 0.06f, size.height * 0.44f
        )
        cubicTo(
            size.width * 0.06f, size.height * 0.64f,
            size.width * 0.36f, size.height * 0.80f,
            size.width * 0.5f, size.height * 0.96f
        )
        cubicTo(
            size.width * 0.64f, size.height * 0.80f,
            size.width * 0.94f, size.height * 0.64f,
            size.width * 0.94f, size.height * 0.44f
        )
        cubicTo(
            size.width * 0.94f, size.height * 0.16f,
            size.width * 0.68f, size.height * 0.08f,
            size.width * 0.5f, size.height * 0.32f
        )
        close()
    }
}

@Composable
fun ArrowRightShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) { size ->
    Path().apply {
        val headWidth = size.width * 0.4f
        moveTo(0f, size.height * 0.3f)
        lineTo(size.width - headWidth, size.height * 0.3f)
        lineTo(size.width - headWidth, 0f)
        lineTo(size.width, size.height / 2f)
        lineTo(size.width - headWidth, size.height)
        lineTo(size.width - headWidth, size.height * 0.7f)
        lineTo(0f, size.height * 0.7f)
        close()
    }
}

@Composable
fun SquareShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) {
    Path().apply { addRect(Rect(0f, 0f, it.width, it.height)) }
}

@Composable
fun CircleShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) {
    Path().apply { addOval(Rect(0f, 0f, it.width, it.height)) }
}

@Composable
fun TriangleShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) { size ->
    Path().apply {
        moveTo(size.width / 2f, 0f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
}

@Composable
fun LineShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    strokeWidth: Float = 8f,
    gradientConfig: GradientConfig? = null
) {
    Canvas(modifier = modifier) {
        val brush = gradientConfig?.toBrush(size) ?: SolidColor(color)
        drawLine(
            brush = brush,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun GearShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    teethCount: Int = 8,
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) { size ->
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val rOuter = minOf(w, h) / 2f
    val rInner = rOuter * 0.7f
    val rHole = rOuter * 0.25f
    val gear = Path().apply {
        val step = 2.0 * PI / teethCount
        val offsetAngle = -PI / 2
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
    val hole = Path().apply { addOval(Rect(cx - rHole, cy - rHole, cx + rHole, cy + rHole)) }
    Path.combine(PathOperation.Difference, gear, hole)
}

@Composable
fun DavidStarShape(
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.4f),
    curveFactor: Float = 0.85f,
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) { size ->
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val rOuter = minOf(w, h) / 2f
    val rInner = rOuter / sqrt(3f)
    val vertices = List(12) { i ->
        val angleDeg = -90f + (i * 30f)
        val angleRad = (angleDeg * PI / 180f).toFloat()
        val radius = if (i % 2 == 0) rOuter else rInner
        Offset(cx + radius * cos(angleRad), cy + radius * sin(angleRad))
    }
    Path().apply {
        moveTo(vertices[0].x, vertices[0].y)
        for (i in 0 until 12) {
            val start = vertices[i]
            val end = vertices[(i + 1) % 12]
            val midX = (start.x + end.x) / 2f
            val midY = (start.y + end.y) / 2f
            val ctrlX = cx + (midX - cx) * curveFactor
            val ctrlY = cy + (midY - cy) * curveFactor
            quadraticTo(ctrlX, ctrlY, end.x, end.y)
        }
        close()
    }
}

@Composable
fun ThornCrownShape(
    modifier: Modifier = Modifier,
    thornColor: Color = getRandomColor().copy(0.4f),
    seed: Long = 42,
    gradientConfig: GradientConfig? = null
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
                val thornBrush = gradientConfig?.toBrush(size) ?: SolidColor(thornColor)
                drawPath(path = crownPaths.thornsPath, brush = thornBrush, style = Fill)
            }
        }
    )
}

@Composable
fun ShapeSelectionCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    shapePreview: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .paddingPx(12f),
        contentAlignment = Alignment.Center
    ) {
        shapePreview()
    }
}

@Composable
fun CanvasItemSelectionHandles(
    baseSize: IntSize,
    currentScaleX: Float,
    currentScaleY: Float,
    currentRotation: Float,
    currentWidth: Float,
    currentHeight: Float,
    proportionalEditing: Boolean,
    pivotX: Float,
    pivotY: Float,
    onUpdatePosition: (Offset, Float, Float, Float) -> Unit,
    onScaleChanged: (Float, Float) -> Unit,
    onDeleteRequested: () -> Unit,
    onColorPickerRequested: () -> Unit,
    onProportionalToggle: () -> Unit,
    density: Float
) {
    val handleSizePx = 64f
    val handleRadiusPx = handleSizePx / 2f
    val latestProportional by rememberUpdatedState(proportionalEditing)

    val latestScaleX by rememberUpdatedState(currentScaleX)
    val latestScaleY by rememberUpdatedState(currentScaleY)
    val latestRotation by rememberUpdatedState(currentRotation)
    val latestPivotX by rememberUpdatedState(pivotX)
    val latestPivotY by rememberUpdatedState(pivotY)

    // Offset helper that uses pivot as origin
    fun offsetAt(localX: Float, localY: Float): IntOffset {
        val pivotLocalX = latestPivotX * baseSize.width
        val pivotLocalY = latestPivotY * baseSize.height
        val dx = localX - pivotLocalX
        val dy = localY - pivotLocalY
        val scaledDx = dx * currentScaleX
        val scaledDy = dy * currentScaleY
        val rad = currentRotation * (PI / 180.0)
        val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
        val ry = scaledDx * sin(rad) + scaledDy * cos(rad)
        return IntOffset(
            (pivotLocalX + rx - handleRadiusPx).roundToInt(),
            (pivotLocalY + ry - handleRadiusPx).roundToInt()
        )
    }

    // Delete handle (top-left)
    Box(
        modifier = Modifier
            .offset { offsetAt(0f, 0f) }
            .requiredSizePx(handleSizePx, handleSizePx)
            .background(Color.White, CircleShape)
            .border(width = (1f / density).dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape)
            .clickable { onDeleteRequested() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Close, "Delete", tint = Color.Red, modifier = Modifier.requiredSizePx(handleSizePx, handleSizePx))
    }

    // Color picker handle (bottom-left)
    Box(
        modifier = Modifier
            .offset { offsetAt(0f, baseSize.height.toFloat()) }
            .requiredSizePx(handleSizePx, handleSizePx)
            .background(Color.White, CircleShape)
            .border(width = (1f / density).dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape)
            .clickable { onColorPickerRequested() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Palette, "Change Color", tint = Color.Blue, modifier = Modifier.requiredSizePx(handleSizePx, handleSizePx))
    }

    // Rotate handle (top-right)
    Box(
        modifier = Modifier
            .offset { offsetAt(baseSize.width.toFloat(), 0f) }
            .requiredSizePx(handleSizePx, handleSizePx)
            .pointerInput(baseSize) {
                var startRotation = 0f
                var accumulatedAngle = 0f
                var currentVector = Offset.Zero
                detectDragGestures(
                    onDragStart = {
                        startRotation = currentRotation
                        accumulatedAngle = 0f
                        val pivotLocX = latestPivotX * baseSize.width
                        val pivotLocY = latestPivotY * baseSize.height
                        val dx = baseSize.width.toFloat() - pivotLocX
                        val dy = 0f - pivotLocY
                        val rad = startRotation * (PI / 180.0)
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
                        onUpdatePosition(Offset.Zero, currentWidth, currentHeight, newRotation)
                    }
                )
            }
            .background(Color.White, CircleShape)
            .border(width = (1f / density).dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Refresh, "Rotate", tint = Color.DarkGray, modifier = Modifier.requiredSizePx(handleSizePx, handleSizePx))
    }

    // Scale handle (bottom-right)
    Box(
        modifier = Modifier
            .offset { offsetAt(baseSize.width.toFloat(), baseSize.height.toFloat()) }
            .requiredSizePx(handleSizePx, handleSizePx)
            .pointerInput(baseSize) {
                var startScaleX = 1f
                var startScaleY = 1f
                var fixedRotation = 0f
                var currentVector = Offset.Zero
                detectDragGestures(
                    onDragStart = {
                        startScaleX = latestScaleX
                        startScaleY = latestScaleY
                        fixedRotation = latestRotation
                        val pivotLocX = latestPivotX * baseSize.width
                        val pivotLocY = latestPivotY * baseSize.height
                        val dx = baseSize.width.toFloat() - pivotLocX
                        val dy = baseSize.height.toFloat() - pivotLocY
                        val rad = fixedRotation * (PI / 180.0)
                        val scaledDx = dx * startScaleX
                        val scaledDy = dy * startScaleY
                        val rx = scaledDx * cos(rad) - scaledDy * sin(rad)
                        val ry = scaledDx * sin(rad) + scaledDy * cos(rad)
                        currentVector = Offset(rx.toFloat(), ry.toFloat())
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentVector += dragAmount
                        val rad = fixedRotation * (PI / 180.0)
                        val unrotatedRad = -rad
                        val newScaledDx = currentVector.x * cos(unrotatedRad) - currentVector.y * sin(unrotatedRad)
                        val newScaledDy = currentVector.x * sin(unrotatedRad) + currentVector.y * cos(unrotatedRad)

                        val pivotLocX = latestPivotX * baseSize.width
                        val pivotLocY = latestPivotY * baseSize.height
                        val dx = baseSize.width.toFloat() - pivotLocX
                        val dy = baseSize.height.toFloat() - pivotLocY

                        if (latestProportional) {
                            val originalDist = sqrt(dx * dx + dy * dy)
                            val newDist = sqrt(newScaledDx * newScaledDx + newScaledDy * newScaledDy)
                            val uniformScale = if (originalDist > 0f) (newDist / originalDist).toFloat().coerceIn(0.1f, 25f) else 1f
                            onScaleChanged(uniformScale, uniformScale)
                        } else {
                            val newScaleX = if (dx != 0f) (newScaledDx / dx).toFloat().coerceIn(0.1f, 25f) else startScaleX
                            val newScaleY = if (dy != 0f) (newScaledDy / dy).toFloat().coerceIn(0.1f, 25f) else startScaleY
                            onScaleChanged(newScaleX, newScaleY)
                        }
                    }
                )
            }
            .background(Color.White, CircleShape)
            .border(width = (1f / density).dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.OpenWith,
            contentDescription = "Scale",
            tint = Color.DarkGray,
            modifier = Modifier.requiredSizePx(width = 60f, height = 60f)
        )
    }

    // Proportional toggle handle (top-center)
    Box(
        modifier = Modifier
            .offset { offsetAt(baseSize.width.toFloat() / 2f, 0f) }
            .requiredSizePx(handleSizePx, handleSizePx)
            .background(Color.White, CircleShape)
            .border(width = (1f / density).dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape)
            .clickable { onProportionalToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (proportionalEditing) Icons.Default.Link else Icons.Default.LinkOff,
            contentDescription = if (proportionalEditing) "Disable proportional" else "Enable proportional",
            tint = if (proportionalEditing) Color(0xFF1976D2) else Color.Gray,
            modifier = Modifier.requiredSizePx(handleSizePx, handleSizePx)
        )
    }
}

@Composable
fun CanvasSvgItem(
    element: CanvasElement,
    isSelected: Boolean,
    isLocked: Boolean,
    onSelect: () -> Unit,
    onUpdatePosition: (Offset, Float, Float, Float) -> Unit,
    onScaleChanged: (Float, Float) -> Unit,
    onColorPickerRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    proportionalEditing: Boolean,
    onProportionalToggle: () -> Unit,
    gradientConfig: GradientConfig? = null,
    onStartPivotPlacement: () -> Unit = {},
    onPlacePivotLocal: (Float, Float) -> Unit = { _, _ -> },
    isPivotPlacementActive: Boolean = false,
    isActivePivotTarget: Boolean = false
) {
    val density = LocalDensity.current.density
    var offset by remember(element.offset) { mutableStateOf(element.offset) }
    var baseSize by remember { mutableStateOf(IntSize.Zero) }
    val currentRotation by rememberUpdatedState(element.rotation)
    val currentScaleX by rememberUpdatedState(element.scaleX)
    val currentScaleY by rememberUpdatedState(element.scaleY)
    val currentWidth by rememberUpdatedState(element.width)
    val currentHeight by rememberUpdatedState(element.height)

    val isCustomPolygon = element.content.startsWith("Shape:CustomPolygon:")
    val isCustomLine = element.content.startsWith("Shape:CustomLine:")
    val isAnyCustomBezier = isCustomPolygon || isCustomLine

    val parsedData = remember(element.content) {
        if (isAnyCustomBezier) {
            val prefix = if (isCustomPolygon) "Shape:CustomPolygon:" else "Shape:CustomLine:"
            val serializedPoints = element.content.removePrefix(prefix)
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
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .wrapContentSize(unbounded = true)
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    this.scaleX = currentScaleX
                    this.scaleY = currentScaleY
                    rotationZ = currentRotation
                    transformOrigin = TransformOrigin(element.pivotX, element.pivotY)
                }
                .onSizeChanged { baseSize = it }
                .then(
                    // Disable all interactions while placing a pivot
                    if (!isLocked && !isPivotPlacementActive) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { onSelect() }
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, rot ->
                                    val angleRad = currentRotation * (PI / 180.0)
                                    val localPanX = pan.x * currentScaleX
                                    val localPanY = pan.y * currentScaleY

                                    val screenPanX = localPanX * cos(angleRad) - localPanY * sin(angleRad)
                                    val screenPanY = localPanX * sin(angleRad) + localPanY * cos(angleRad)
                                    offset += Offset(screenPanX.toFloat(), screenPanY.toFloat())

                                    val newScaleX = (currentScaleX * zoom).coerceIn(0.1f, 25f)
                                    val newScaleY = (currentScaleY * zoom).coerceIn(0.1f, 25f)
                                    val newRotation = currentRotation + rot

                                    onScaleChanged(newScaleX, newScaleY)
                                    onUpdatePosition(offset, currentWidth, currentHeight, newRotation)
                                }
                            }
                    } else Modifier
                )
                .alpha(if (isLocked) 0.2f else 1f)
        ) {
            Box(
                modifier = Modifier
                    .requiredSizePx(currentWidth, currentHeight),
                contentAlignment = Alignment.Center
            ) {
                // Shadow layer
                if (element.shadowColor != null && element.shadowColor.alpha > 0f) {
                    val shadowModifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(element.shadowOffsetX.roundToInt(), element.shadowOffsetY.roundToInt()) }
                        .blur(radius = 2.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)

                    when {
                        element.content == "Shape: Square" -> SquareShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: Circle" -> CircleShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: Triangle" -> TriangleShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: Line" -> LineShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: Pentagon" -> PentagonShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: Hexagon" -> HexagonShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: Star" -> StarShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: Diamond" -> DiamondShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: Heart" -> HeartShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: ArrowRight" -> ArrowRightShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: Octagon" -> OctagonShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: Cross" -> CrossShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: ThornCrown" -> ThornCrownShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: Moon" -> MoonShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: DavidStar" -> DavidStarShape(shadowModifier, element.shadowColor)
                        element.content == "Shape: Gear" -> GearShape(shadowModifier, element.shadowColor)
                        parsedData != null -> {
                            BezierPolygonShape(
                                nodes = parsedData.first,
                                modifier = shadowModifier,
                                color = element.shadowColor,
                                closed = !isCustomLine
                            )
                        }
                    }
                }

                // Main shape
                val mainModifier = Modifier.fillMaxSize()
                when {
                    element.content == "Shape: Square" -> SquareShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: Circle" -> CircleShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: Triangle" -> TriangleShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: Line" -> LineShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: Pentagon" -> PentagonShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: Hexagon" -> HexagonShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: Star" -> StarShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: Diamond" -> DiamondShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: Heart" -> HeartShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: ArrowRight" -> ArrowRightShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: Octagon" -> OctagonShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: Cross" -> CrossShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: ThornCrown" -> ThornCrownShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: Moon" -> MoonShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: DavidStar" -> DavidStarShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    element.content == "Shape: Gear" -> GearShape(mainModifier, element.backgroundColor, gradientConfig = gradientConfig)
                    parsedData != null -> {
                        BezierPolygonShape(
                            nodes = parsedData.first,
                            modifier = mainModifier,
                            color = element.backgroundColor,
                            closed = !isCustomLine
                        )
                    }
                }

                // Border
                if (element.borderThickness > 0f && element.borderColor != null) {
                    val borderColor = element.borderColor
                    val strokeWidthPx = element.borderThickness
                    val strokeStyle = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val borderModifier = Modifier.fillMaxSize()

                    when {
                        element.content == "Shape: Square" -> SquareShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        element.content == "Shape: Circle" -> CircleShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        element.content == "Shape: Triangle" -> TriangleShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        element.content == "Shape: Line" -> LineShape(borderModifier, borderColor, strokeWidth = strokeWidthPx)
                        element.content == "Shape: Pentagon" -> PentagonShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        element.content == "Shape: Hexagon" -> HexagonShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        element.content == "Shape: Star" -> StarShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        element.content == "Shape: Diamond" -> DiamondShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        element.content == "Shape: Heart" -> HeartShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        element.content == "Shape: ArrowRight" -> ArrowRightShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        element.content == "Shape: Octagon" -> OctagonShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        element.content == "Shape: Cross" -> CrossShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        element.content == "Shape: ThornCrown" -> {} // no separate border layer for thorn crown
                        element.content == "Shape: Moon" -> MoonShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        element.content == "Shape: DavidStar" -> DavidStarShape(borderModifier, borderColor, drawStyle = strokeStyle)
                        element.content == "Shape: Gear" -> GearShape(borderModifier, borderColor, drawStyle = strokeStyle)
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

                // Selection highlight
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(width = (0.5f / density).dp, color = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        // Handles and PivotHandle (hidden during pivot placement except the pivot indicator)
        if (isSelected && baseSize != IntSize.Zero && !isLocked) {
            if (!isPivotPlacementActive) {
                CanvasItemSelectionHandles(
                    baseSize = baseSize,
                    currentScaleX = currentScaleX,
                    currentScaleY = currentScaleY,
                    currentRotation = currentRotation,
                    currentWidth = currentWidth,
                    currentHeight = currentHeight,
                    proportionalEditing = proportionalEditing,
                    pivotX = element.pivotX,
                    pivotY = element.pivotY,
                    onUpdatePosition = { _, w, h, rot -> onUpdatePosition(offset, w, h, rot) },
                    onScaleChanged = onScaleChanged,
                    onDeleteRequested = onDeleteRequested,
                    onColorPickerRequested = onColorPickerRequested,
                    onProportionalToggle = onProportionalToggle,
                    density = density
                )
            }
            PivotHandle(
                baseSize = baseSize,
                pivotX = element.pivotX,
                pivotY = element.pivotY,
                density = density,
                isActive = isActivePivotTarget,
                onStartPivotPlacement = onStartPivotPlacement,
                onPlacePivotLocal = onPlacePivotLocal,
                currentRotation = currentRotation,
                currentScaleX = currentScaleX,
                currentScaleY = currentScaleY,
                elementWidth = currentWidth,
                elementHeight = currentHeight
            )
        }
    }
}

@Composable
fun CanvasTextItem(
    element: CanvasElement,
    gradientConfig: GradientConfig? = null,
    isSelected: Boolean,
    isLocked: Boolean,
    onSelect: () -> Unit,
    onUpdatePosition: (Offset, Float, Float, Float) -> Unit,
    onScaleChanged: (Float, Float) -> Unit,
    onColorPickerRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    proportionalEditing: Boolean,
    onProportionalToggle: () -> Unit,
    onStartPivotPlacement: () -> Unit = {},
    onPlacePivotLocal: (Float, Float) -> Unit = { _, _ -> },
    isPivotPlacementActive: Boolean = false,
    isActivePivotTarget: Boolean = false
) {
    val fontFamily = getFontFamily(element.fontFamily ?: "system")
    val textAlignment = when (element.textAlign) {
        "Left"   -> TextAlign.Left
        "Right"  -> TextAlign.Right
        "Center" -> TextAlign.Center
        else     -> TextAlign.Center
    }
    val density = LocalDensity.current.density
    var offset by remember(element.offset) { mutableStateOf(element.offset) }
    var baseSize by remember { mutableStateOf(IntSize.Zero) }
    val currentRotation by rememberUpdatedState(element.rotation)
    val currentScaleX by rememberUpdatedState(element.scaleX)
    val currentScaleY by rememberUpdatedState(element.scaleY)
    val currentWidth by rememberUpdatedState(element.width)
    val currentHeight by rememberUpdatedState(element.height)

    val lockedFontSize = (60f / density).sp
    val lockedPadding = (24f / density).dp
    val lockedMaxWidth = (750f / density).dp

    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .wrapContentSize(unbounded = true)
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    this.scaleX = currentScaleX
                    this.scaleY = currentScaleY
                    rotationZ = currentRotation
                    transformOrigin = TransformOrigin(element.pivotX, element.pivotY)
                }
                .onSizeChanged { baseSize = it }
                .then(
                    if (!isLocked && !isPivotPlacementActive) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { onSelect() }
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, rot ->
                                    val angleRad = currentRotation * (PI / 180.0)
                                    val localPanX = pan.x * currentScaleX
                                    val localPanY = pan.y * currentScaleY

                                    val screenPanX = localPanX * cos(angleRad) - localPanY * sin(angleRad)
                                    val screenPanY = localPanX * sin(angleRad) + localPanY * cos(angleRad)
                                    offset += Offset(screenPanX.toFloat(), screenPanY.toFloat())

                                    val newScaleX = (currentScaleX * zoom).coerceIn(0.1f, 25f)
                                    val newScaleY = (currentScaleY * zoom).coerceIn(0.1f, 25f)
                                    val newRotation = currentRotation + rot

                                    onScaleChanged(newScaleX, newScaleY)
                                    onUpdatePosition(offset, currentWidth, currentHeight, newRotation)
                                }
                            }
                    } else Modifier
                )
                .alpha(if (isLocked) 0.2f else 1f)
        ) {
            Box(
                modifier = Modifier,
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(width = (0.5f / density).dp, color = MaterialTheme.colorScheme.primary)
                    )
                }
                Box(
                    modifier = Modifier.onSizeChanged { size ->
                        val newW = size.width.toFloat()
                        val newH = size.height.toFloat()
                        val oldW = currentWidth
                        val oldH = currentHeight
                        if ((newW - oldW).absoluteValue > 1f || (newH - oldH).absoluteValue > 1f) {
                            val px = element.pivotX
                            val py = element.pivotY
                            val deltaOffsetX = px * (oldW - newW)
                            val deltaOffsetY = py * (oldH - newH)
                            val newOffset = Offset(offset.x + deltaOffsetX, offset.y + deltaOffsetY)
                            offset = newOffset
                            onUpdatePosition(newOffset, newW, newH, currentRotation)
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    val textShadow = if (element.shadowColor != null && element.shadowColor.alpha > 0f) {
                        Shadow(
                            color = element.shadowColor,
                            offset = Offset(element.shadowOffsetX, element.shadowOffsetY),
                            blurRadius = 6f
                        )
                    } else null

                    val borderColor = element.borderColor
                    val hasBorder = element.borderThickness > 0f && borderColor != null && borderColor.alpha > 0f

                    val commonModifier = Modifier
                        .padding(lockedPadding)
                        .widthIn(max = lockedMaxWidth)

                    val baseTextStyle = TextStyle(
                        fontSize = lockedFontSize,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Medium,
                        textAlign = textAlignment
                    )

                    val needsDedicatedShadow = textShadow != null && (hasBorder || gradientConfig != null)
                    if (needsDedicatedShadow) {
                        Text(
                            text = element.content,
                            color = element.textColor ?: Color.Black,
                            style = baseTextStyle.copy(shadow = textShadow),
                            modifier = commonModifier
                        )
                    }

                    if (hasBorder) {
                        Text(
                            text = element.content,
                            color = borderColor,
                            style = baseTextStyle.copy(
                                drawStyle = Stroke(
                                    width = element.borderThickness,
                                    join = StrokeJoin.Round
                                )
                            ),
                            modifier = commonModifier
                        )
                    }

                    if (gradientConfig != null) {
                        Box(
                            modifier = Modifier
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawRect(
                                        brush = Brush.linearGradient(
                                            colors = listOf(gradientConfig.startColor, gradientConfig.endColor),
                                            start = Offset(
                                                gradientConfig.startOffset.x * size.width,
                                                gradientConfig.startOffset.y * size.height
                                            ),
                                            end = Offset(
                                                gradientConfig.endOffset.x * size.width,
                                                gradientConfig.endOffset.y * size.height
                                            )
                                        )
                                    )
                                    val paint = Paint().apply {
                                        xfermode = PorterDuffXfermode(
                                            PorterDuff.Mode.DST_IN
                                        )
                                    }
                                    drawContext.canvas.nativeCanvas.saveLayer(
                                        RectF(0f, 0f, size.width, size.height),
                                        paint
                                    )
                                    drawContent()
                                    drawContext.canvas.nativeCanvas.restore()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = element.content,
                                color = Color.Black,
                                style = baseTextStyle,
                                modifier = commonModifier
                            )
                        }
                    } else {
                        Text(
                            text = element.content,
                            color = element.textColor ?: Color.Black,
                            style = baseTextStyle.copy(
                                shadow = if (needsDedicatedShadow) null else textShadow
                            ),
                            modifier = commonModifier
                        )
                    }
                }
            }
        }

        if (isSelected && baseSize != IntSize.Zero && !isLocked) {
            if (!isPivotPlacementActive) {
                CanvasItemSelectionHandles(
                    baseSize = baseSize,
                    currentScaleX = currentScaleX,
                    currentScaleY = currentScaleY,
                    currentRotation = currentRotation,
                    currentWidth = currentWidth,
                    currentHeight = currentHeight,
                    proportionalEditing = proportionalEditing,
                    pivotX = element.pivotX,
                    pivotY = element.pivotY,
                    onUpdatePosition = { _, w, h, rot -> onUpdatePosition(offset, w, h, rot) },
                    onScaleChanged = onScaleChanged,
                    onDeleteRequested = onDeleteRequested,
                    onColorPickerRequested = onColorPickerRequested,
                    onProportionalToggle = onProportionalToggle,
                    density = density
                )
            }
            PivotHandle(
                baseSize = baseSize,
                pivotX = element.pivotX,
                pivotY = element.pivotY,
                density = density,
                isActive = isActivePivotTarget,
                onStartPivotPlacement = onStartPivotPlacement,
                onPlacePivotLocal = onPlacePivotLocal,
                currentRotation = currentRotation,
                currentScaleX = currentScaleX,
                currentScaleY = currentScaleY,
                elementWidth = currentWidth,
                elementHeight = currentHeight
            )
        }
    }
}

@Composable
fun CanvasImageItem(
    element: CanvasElement,
    gradientConfig: GradientConfig? = null,
    isSelected: Boolean,
    isLocked: Boolean,
    onSelect: () -> Unit,
    onUpdatePosition: (Offset, Float, Float, Float) -> Unit,
    onScaleChanged: (Float, Float) -> Unit,
    onColorPickerRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    proportionalEditing: Boolean,
    onProportionalToggle: () -> Unit,
    onStartPivotPlacement: () -> Unit = {},
    onPlacePivotLocal: (Float, Float) -> Unit = { _, _ -> },
    isPivotPlacementActive: Boolean = false,
    isActivePivotTarget: Boolean = false
) {
    val density = LocalDensity.current.density
    val context = LocalContext.current

    var offset by remember(element.offset) { mutableStateOf(element.offset) }
    var rotation by remember(element.id, element.rotation) { mutableFloatStateOf(element.rotation) }
    val scaleX by rememberUpdatedState(element.scaleX)
    val scaleY by rememberUpdatedState(element.scaleY)

    var baseSize by remember { mutableStateOf(IntSize.Zero) }

    val uriString = element.content.removePrefix("Image: ")
    val uri = uriString.toUri()

    val fullResImageRequest = remember(uri, context) {
        ImageRequest.Builder(context)
            .data(uri)
            .size(Size2.ORIGINAL)
            .build()
    }

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
                    transformOrigin = TransformOrigin(element.pivotX, element.pivotY)
                }
                .onSizeChanged { baseSize = it }
                .then(
                    // Disable interactions while placing a pivot
                    if (!isLocked && !isPivotPlacementActive) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { onSelect() }
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, rot ->
                                    val angleRad = rotation * (PI / 180.0)
                                    val localPanX = pan.x * scaleX
                                    val localPanY = pan.y * scaleY

                                    val screenPanX = localPanX * cos(angleRad) - localPanY * sin(angleRad)
                                    val screenPanY = localPanX * sin(angleRad) + localPanY * cos(angleRad)
                                    offset += Offset(screenPanX.toFloat(), screenPanY.toFloat())

                                    val newScaleX = (scaleX * zoom).coerceIn(0.1f, 25f)
                                    val newScaleY = (scaleY * zoom).coerceIn(0.1f, 25f)
                                    rotation += rot

                                    onScaleChanged(newScaleX, newScaleY)
                                    onUpdatePosition(offset, element.width, element.height, rotation)
                                }
                            }
                    } else Modifier
                )
                .alpha(if (isLocked) 0.2f else 1f)
        ) {
            Box(
                modifier = Modifier
                    .requiredSizePx(element.width, element.height),
                contentAlignment = Alignment.Center
            ) {
                if (element.shadowColor != null && element.shadowColor.alpha > 0f) {
                    AsyncImage(
                        model = fullResImageRequest,
                        contentDescription = "Image Shadow",
                        modifier = Modifier
                            .matchParentSize()
                            .offset { IntOffset(element.shadowOffsetX.roundToInt(), element.shadowOffsetY.roundToInt()) }
                            .blur(radius = 2.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(element.shadowColor)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(width = (0.5f / density).dp, color = MaterialTheme.colorScheme.primary)
                    )
                }

                AsyncImage(
                    model = fullResImageRequest,
                    contentDescription = "Canvas Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                if (gradientConfig != null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBehind {
                                drawRect(brush = gradientConfig.toBrush(size))
                            }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(element.backgroundColor)
                    )
                }
            }
        }

        // Handles and PivotHandle (hidden during pivot placement except the pivot indicator)
        if (isSelected && baseSize != IntSize.Zero && !isLocked) {
            if (!isPivotPlacementActive) {
                CanvasItemSelectionHandles(
                    baseSize = baseSize,
                    currentScaleX = scaleX,
                    currentScaleY = scaleY,
                    currentRotation = rotation,
                    currentWidth = element.width,
                    currentHeight = element.height,
                    proportionalEditing = proportionalEditing,
                    pivotX = element.pivotX,
                    pivotY = element.pivotY,
                    onUpdatePosition = { _, w, h, rot -> onUpdatePosition(offset, w, h, rot) },
                    onScaleChanged = onScaleChanged,
                    onDeleteRequested = onDeleteRequested,
                    onColorPickerRequested = onColorPickerRequested,
                    onProportionalToggle = onProportionalToggle,
                    density = density
                )
            }
            PivotHandle(
                baseSize = baseSize,
                pivotX = element.pivotX,
                pivotY = element.pivotY,
                density = density,
                isActive = isActivePivotTarget,
                onStartPivotPlacement = onStartPivotPlacement,
                onPlacePivotLocal = onPlacePivotLocal,
                currentRotation = rotation,          // variable name in image item is “rotation”
                currentScaleX = scaleX,
                currentScaleY = scaleY,
                elementWidth = element.width,
                elementHeight = element.height
            )
        }
    }
}

@Composable
fun PolygonShape(
    points: List<Offset>,
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.8f),
    drawStyle: DrawStyle = Fill,
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) { size ->
    Path().apply {
        if (points.isEmpty()) return@ShapeCanvas Path()
        moveTo(points.first().x * size.width, points.first().y * size.height)
        points.drop(1).forEach { point ->
            lineTo(point.x * size.width, point.y * size.height)
        }
        close()
    }
}

@Composable
fun BezierPolygonShape(
    nodes: List<BezierNodeData>,
    modifier: Modifier = Modifier,
    color: Color = getRandomColor().copy(0.8f),
    closed: Boolean = true,
    drawStyle: DrawStyle = if (closed) Fill else Stroke(width = 4f),
    gradientConfig: GradientConfig? = null
) = ShapeCanvas(modifier, color, drawStyle, gradientConfig) { size ->
    Path().apply {
        if (nodes.isEmpty()) return@ShapeCanvas Path()
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
}

@Composable
fun CustomPathPreview(
    pointsData: String,
    isClosed: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    gradientConfig: GradientConfig? = null
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
                        else -> {}
                    }
                }

                path.close()

                val brush = gradientConfig?.toBrush(size) ?: SolidColor(color)
                drawPath(path = path, brush = brush, style = Fill)
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.8f),
                    style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else {
                val brush = gradientConfig?.toBrush(size) ?: SolidColor(color)
                drawPath(
                    path = path,
                    brush = brush,
                    style = Stroke(width = 0.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}