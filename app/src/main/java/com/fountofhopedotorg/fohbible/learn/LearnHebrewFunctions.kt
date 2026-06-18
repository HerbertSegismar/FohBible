package com.fountofhopedotorg.fohbible.learn

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Offset

private const val EM_HEIGHT = 110f
private const val BASELINE_PATH_Y = 64f
private const val BASELINE_FRACTION = 0.85f

private fun DrawScope.calculateOffsets(
    bounds: Rect,
    scale: Float
): Pair<Float, Float> {
    val baselineYCanvas = size.height * BASELINE_FRACTION
    val leftOffset = (size.width - bounds.width * scale) / 2f - bounds.left * scale
    val topOffset = baselineYCanvas - BASELINE_PATH_Y * scale
    return leftOffset to topOffset
}

fun DrawScope.drawAleph(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val alephPath = Path().apply {
        moveTo(8.629f, 0.5f)
        lineTo(1.091f, 8.925f)
        lineTo(16.758f, 21.34f)
        cubicTo(6.831f, 28.68f, 2.888f, 40.063f, 4.786f, 56.074f)
        lineTo(0.5f, 61.691f)
        lineTo(16.906f, 61.691f)
        lineTo(22.375f, 50.31f)
        lineTo(14.393f, 50.31f)
        cubicTo(9.516f, 43.934f, 14.732f, 25.276f, 20.453f, 24.592f)
        lineTo(56.37f, 62.726f)
        lineTo(62.134f, 53.118f)
        lineTo(46.319f, 36.416f)
        cubicTo(57.048f, 23.561f, 60.622f, 10.988f, 55.187f, 0.5f)
        lineTo(45.284f, 9.96f)
        cubicTo(48.603f, 20.203f, 48.025f, 27.848f, 43.067f, 32.574f)
        lineTo(8.629f, 0.5f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(alephPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = alephPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = alephPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawBet(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val bethPath = Path().apply {
        moveTo(11.96f, 0.5f)
        lineTo(1.023f, 14.689f)
        lineTo(31.914f, 14.689f)
        cubicTo(34.174f, 15.042f, 35.741f, 15.474f, 35.984f, 18.652f)
        lineTo(35.98f, 52.469f)
        lineTo(8.708f, 52.527f)
        lineTo(0.5f, 62.437f)
        lineTo(44.92f, 62.43f)
        lineTo(53.352f, 52.614f)
        lineTo(44.92f, 52.617f)
        lineTo(44.934f, 16.467f)
        cubicTo(44.787f, 10.609f, 41.998f, 6.554f, 36.805f, 5.54f)
        lineTo(12.094f, 5.608f)
        lineTo(11.96f, 0.5f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(bethPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = bethPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = bethPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawGimel(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val gimelPath = Path().apply {
        moveTo(5.135f, 8.729f)
        lineTo(12.324f, 0.5f)
        cubicTo(34.201f, 13.382f, 30.365f, 20.834f, 32.379f, 56.121f)
        lineTo(26.892f, 62.364f)
        cubicTo(25.557f, 58.797f, 25.074f, 53.413f, 25.0f, 46.567f)
        cubicTo(19.983f, 54.072f, 11.571f, 59.053f, 0.5f, 60.756f)
        cubicTo(3.068f, 57.552f, 4.87f, 53.87f, 5.419f, 49.405f)
        cubicTo(21.311f, 46.304f, 25.645f, 39.421f, 22.958f, 26.373f)
        cubicTo(21.332f, 18.479f, 15.14f, 12.429f, 5.135f, 8.729f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(gimelPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = gimelPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = gimelPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawDalet(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val daletPath = Path().apply {
        moveTo(12.442f, 6.709f)
        lineTo(12.506f, 1.506f)
        lineTo(0.5f, 16.598f)
        lineTo(31.301f, 16.644f)
        lineTo(31.419f, 64.769f)
        lineTo(39.459f, 57.202f)
        lineTo(39.338f, 16.537f)
        lineTo(45.542f, 16.543f)
        cubicTo(46.256f, 11.616f, 47.934f, 8.735f, 50.101f, 6.715f)
        lineTo(39.407f, 6.72f)
        lineTo(39.35f, 0.5f)
        lineTo(33.665f, 6.723f)
        lineTo(12.442f, 6.709f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(daletPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = daletPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = daletPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawHe(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val rightSidePath = Path().apply {
        moveTo(11.844f, 0.5f)
        cubicTo(8.796f, 5.463f, 5.001f, 10.426f, 0.5f, 15.389f)
        lineTo(40.063f, 15.593f)
        cubicTo(42.578f, 16.088f, 44.379f, 17.429f, 44.309f, 20.993f)
        lineTo(44.32f, 64.404f)
        lineTo(53.023f, 56.363f)
        lineTo(53.106f, 18.007f)
        cubicTo(52.618f, 11.559f, 50.332f, 7.615f, 44.354f, 6.172f)
        lineTo(11.874f, 6.138f)
        lineTo(11.844f, 0.5f)
        close()
    }

    val leftLegPath = Path().apply {
        moveTo(4.874f, 33.472f)
        cubicTo(8.069f, 31.495f, 11.095f, 28.924f, 13.967f, 25.817f)
        lineTo(13.755f, 56.14f)
        lineTo(4.855f, 64.37f)
        lineTo(4.874f, 33.472f)
        close()
    }

    val rightMeasure = PathMeasure().apply { setPath(rightSidePath, false) }
    val leftMeasure = PathMeasure().apply { setPath(leftLegPath, false) }

    val totalLength = rightMeasure.length + leftMeasure.length
    val targetLength = totalLength * strokeProgress

    val animatedPath = Path()
    if (targetLength <= rightMeasure.length) {
        rightMeasure.getSegment(0f, targetLength, animatedPath, true)
    } else {
        rightMeasure.getSegment(0f, rightMeasure.length, animatedPath, true)
        val remainingLength = targetLength - rightMeasure.length
        leftMeasure.getSegment(0f, remainingLength, animatedPath, true)
    }

    val fullHePath = Path().apply {
        addPath(rightSidePath)
        addPath(leftLegPath)
    }

    val scale = size.height / EM_HEIGHT
    val bounds = fullHePath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = fullHePath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawVav(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val vavPath = Path().apply {
        moveTo(0.500f, 9.676f)
        lineTo(8.257f, 0.500f)
        lineTo(20.932f, 12.325f)
        cubicTo(23.380f, 14.708f, 24.784f, 17.787f, 24.811f, 21.784f)
        lineTo(22.975f, 56.954f)
        cubicTo(19.722f, 59.801f, 17.015f, 61.272f, 14.367f, 64.522f)
        lineTo(15.919f, 28.784f)
        cubicTo(16.401f, 24.833f, 15.411f, 22.014f, 13.270f, 20.081f)
        lineTo(0.500f, 9.676f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(vavPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = vavPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = vavPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawZayin(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val zayinPath = Path().apply {
        moveTo(0.500f, 6.404f)
        lineTo(6.175f, 1.409f)
        cubicTo(8.066f, 0.159f, 9.859f, 0.187f, 11.548f, 1.560f)
        lineTo(29.256f, 15.636f)
        cubicTo(26.963f, 17.630f, 25.571f, 20.226f, 25.094f, 23.431f)
        lineTo(15.938f, 16.922f)
        cubicTo(12.360f, 24.204f, 16.109f, 35.950f, 22.824f, 49.690f)
        lineTo(16.467f, 61.344f)
        cubicTo(9.047f, 40.178f, 4.244f, 21.523f, 12.002f, 15.333f)
        cubicTo(9.129f, 11.671f, 4.806f, 9.043f, 0.500f, 6.404f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(zayinPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = zayinPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = zayinPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawChet(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val chetPath = Path().apply {
        moveTo(0.500f, 6.402f)
        cubicTo(3.984f, 4.697f, 8.216f, 2.608f, 11.094f, 0.500f)
        lineTo(11.170f, 6.024f)
        lineTo(36.673f, 5.948f)
        cubicTo(47.174f, 8.699f, 52.331f, 15.305f, 54.154f, 24.716f)
        lineTo(54.305f, 57.256f)
        cubicTo(51.508f, 58.863f, 48.727f, 61.098f, 46.737f, 63.916f)
        lineTo(46.662f, 29.332f)
        cubicTo(45.650f, 22.131f, 41.898f, 17.916f, 35.235f, 15.559f)
        lineTo(11.170f, 15.559f)
        lineTo(11.170f, 58.770f)
        cubicTo(8.521f, 60.568f, 6.327f, 62.370f, 3.678f, 64.975f)
        lineTo(3.754f, 16.240f)
        cubicTo(3.805f, 11.825f, 2.669f, 8.597f, 0.500f, 6.402f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(chetPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = chetPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = chetPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawAyin(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val ayinPath = Path().apply {
        moveTo(1.635f, 10.716f)
        lineTo(10.149f, 0.878f)
        lineTo(27.932f, 47.608f)
        cubicTo(39.521f, 42.624f, 41.159f, 30.015f, 33.324f, 10.149f)
        lineTo(44.203f, 0.500f)
        cubicTo(50.446f, 47.157f, 41.362f, 48.746f, 1.380f, 70.114f)
        cubicTo(1.108f, 70.260f, 0.794f, 70.309f, 0.500f, 70.405f)
        lineTo(4.094f, 57.730f)
        lineTo(21.878f, 50.730f)
        cubicTo(16.574f, 35.468f, 9.967f, 21.942f, 1.635f, 10.716f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(ayinPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = ayinPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = ayinPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawKaf(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val kafPath = Path().apply {
        moveTo(3.098f, 15.621f)
        lineTo(15.606f, 0.500f)
        lineTo(15.791f, 6.657f)
        lineTo(33.345f, 6.676f)
        cubicTo(56.621f, 12.737f, 51.971f, 48.133f, 34.206f, 63.243f)
        lineTo(0.500f, 62.964f)
        lineTo(7.717f, 54.090f)
        lineTo(35.426f, 54.015f)
        cubicTo(42.899f, 44.867f, 47.007f, 21.406f, 29.080f, 15.621f)
        lineTo(3.098f, 15.621f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(kafPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = kafPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = kafPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawLamed(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val lamedPath = Path().apply {
        moveTo(10.149f, 22.846f)
        lineTo(10.244f, 1.941f)
        cubicTo(9.551f, 0.803f, 8.571f, 0.240f, 7.122f, 0.616f)
        cubicTo(5.230f, 3.405f, 3.338f, 5.553f, 1.446f, 7.333f)
        cubicTo(0.864f, 9.729f, 1.679f, 12.125f, 2.865f, 14.522f)
        lineTo(2.865f, 31.832f)
        lineTo(30.865f, 32.116f)
        cubicTo(34.254f, 33.396f, 35.326f, 35.299f, 35.661f, 38.032f)
        cubicTo(36.129f, 41.856f, 36.155f, 45.165f, 35.048f, 48.075f)
        cubicTo(31.981f, 56.142f, 24.805f, 65.662f, 9.014f, 67.968f)
        lineTo(0.500f, 81.116f)
        cubicTo(24.416f, 76.289f, 39.178f, 63.393f, 42.806f, 49.438f)
        cubicTo(44.259f, 43.852f, 44.880f, 34.875f, 43.549f, 29.180f)
        cubicTo(42.787f, 25.918f, 39.183f, 23.750f, 34.933f, 22.751f)
        lineTo(10.149f, 22.846f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(lamedPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = lamedPath.getBounds()
    var (leftOffset, topOffset) = calculateOffsets(bounds, scale)
    val descenderDepth = (bounds.bottom - BASELINE_PATH_Y).coerceAtLeast(0f)
    topOffset -= descenderDepth * scale

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = lamedPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawMem(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val memPath = Path().apply {
        moveTo(1.541f, 10.527f)
        lineTo(8.730f, 0.500f)
        cubicTo(13.243f, 7.043f, 15.411f, 15.241f, 15.635f, 24.811f)
        cubicTo(30.367f, -11.083f, 60.560f, -0.529f, 57.540f, 45.527f)
        cubicTo(54.369f, 53.254f, 51.158f, 58.895f, 49.500f, 61.324f)
        lineTo(17.811f, 61.324f)
        lineTo(24.149f, 51.676f)
        lineTo(50.919f, 51.676f)
        cubicTo(52.978f, -0.892f, 19.850f, -0.094f, 9.770f, 56.406f)
        lineTo(0.500f, 63.406f)
        cubicTo(7.681f, 42.210f, 14.219f, 25.154f, 1.541f, 10.527f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(memPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = memPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = memPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawNun(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val nunPath = Path().apply {
        moveTo(6.270f, 10.149f)
        lineTo(12.608f, 0.500f)
        lineTo(26.324f, 11.851f)
        cubicTo(28.015f, 13.712f, 28.701f, 15.572f, 28.878f, 17.432f)
        lineTo(28.783f, 49.784f)
        cubicTo(25.963f, 53.041f, 23.438f, 56.742f, 21.121f, 60.757f)
        lineTo(0.500f, 60.851f)
        lineTo(5.324f, 51.108f)
        lineTo(21.594f, 51.108f)
        lineTo(21.500f, 23.995f)
        cubicTo(19.043f, 19.241f, 13.519f, 14.650f, 6.270f, 10.149f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(nunPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = nunPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = nunPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawPeh(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val pehPath = Path().apply {
        moveTo(16.699f, 37.392f)
        cubicTo(0.679f, 28.899f, 2.765f, 16.561f, 22.375f, 0.500f)
        cubicTo(36.556f, 4.796f, 49.824f, 19.587f, 49.824f, 19.587f)
        lineTo(49.808f, 47.679f)
        lineTo(42.831f, 62.223f)
        lineTo(0.500f, 62.105f)
        lineTo(7.477f, 52.173f)
        lineTo(41.176f, 52.646f)
        cubicTo(42.155f, 42.732f, 41.075f, 25.469f, 41.075f, 25.469f)
        cubicTo(42.479f, 24.591f, 23.341f, 8.518f, 19.301f, 10.551f)
        cubicTo(12.738f, 13.854f, 12.486f, 25.520f, 22.257f, 29.588f)
        lineTo(16.699f, 37.392f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(pehPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = pehPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = pehPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawQof(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val qofTopPath = Path().apply {
        moveTo(0.500f, 16.817f)
        lineTo(14.098f, 0.500f)
        lineTo(14.098f, 8.067f)
        lineTo(45.314f, 8.185f)
        cubicTo(52.329f, 8.737f, 56.648f, 12.712f, 58.320f, 16.935f)
        cubicTo(60.272f, 21.862f, 59.055f, 27.063f, 58.439f, 32.070f)
        cubicTo(56.586f, 47.127f, 34.719f, 60.146f, 20.246f, 66.124f)
        lineTo(24.503f, 55.010f)
        cubicTo(37.765f, 51.104f, 48.494f, 43.153f, 49.452f, 31.361f)
        lineTo(49.452f, 23.912f)
        cubicTo(49.533f, 20.007f, 45.511f, 17.369f, 42.121f, 16.935f)
        lineTo(0.500f, 16.817f)
        close()
    }

    val qofStemPath = Path().apply {
        moveTo(13.979f, 24.030f)
        cubicTo(12.071f, 27.980f, 8.714f, 31.943f, 5.229f, 35.263f)
        lineTo(5.466f, 85.871f)
        cubicTo(7.536f, 86.174f, 10.333f, 83.282f, 13.979f, 76.648f)
        lineTo(13.979f, 24.030f)
        close()
    }

    val qofFull = Path().apply {
        addPath(qofTopPath)
        addPath(qofStemPath)
    }

    val measureTop = PathMeasure().apply { setPath(qofTopPath, false) }
    val measureStem = PathMeasure().apply { setPath(qofStemPath, false) }

    val lenTop = measureTop.length
    val lenStem = measureStem.length
    val totalLength = lenTop + lenStem
    val currentDistance = strokeProgress * totalLength

    val animatedPath = Path()
    if (currentDistance <= lenTop) {
        measureTop.getSegment(0f, currentDistance, animatedPath, true)
    } else {
        measureTop.getSegment(0f, lenTop, animatedPath, true)
        measureStem.getSegment(0f, currentDistance - lenTop, animatedPath, true)
    }

    val scale = size.height / EM_HEIGHT
    val bounds = qofFull.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = qofFull, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawResh(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val reshPath = Path().apply {
        moveTo(0.500f, 16.463f)
        lineTo(13.270f, 0.500f)
        lineTo(13.388f, 6.648f)
        lineTo(30.297f, 6.648f)
        cubicTo(38.309f, 8.827f, 43.057f, 13.378f, 44.132f, 20.601f)
        lineTo(44.132f, 56.310f)
        lineTo(35.263f, 66.243f)
        lineTo(35.382f, 23.794f)
        cubicTo(35.131f, 20.293f, 32.795f, 17.835f, 28.760f, 16.226f)
        lineTo(0.500f, 16.463f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(reshPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = reshPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = reshPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawSamech(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val samechOuter = Path().apply {
        moveTo(0.500f, 14.406f)
        lineTo(11.094f, 0.500f)
        lineTo(11.284f, 6.649f)
        lineTo(44.297f, 6.744f)
        cubicTo(52.794f, 7.823f, 58.250f, 11.970f, 59.432f, 20.365f)
        lineTo(59.432f, 45.433f)
        cubicTo(58.009f, 54.805f, 54.259f, 59.794f, 48.554f, 63.122f)
        lineTo(13.554f, 62.933f)
        cubicTo(8.497f, 62.997f, 5.241f, 60.094f, 4.473f, 53.095f)
        lineTo(4.378f, 18.189f)
        cubicTo(4.035f, 16.114f, 2.609f, 14.967f, 0.500f, 14.406f)
        close()
    }

    val samechInner = Path().apply {
        moveTo(11.284f, 16.298f)
        lineTo(11.094f, 53.189f)
        lineTo(48.648f, 53.189f)
        cubicTo(49.945f, 52.709f, 51.073f, 51.809f, 51.865f, 50.068f)
        lineTo(51.770f, 27.649f)
        cubicTo(52.110f, 22.720f, 48.593f, 17.001f, 42.878f, 16.108f)
        lineTo(11.284f, 16.298f)
        close()
    }

    val samechFull = Path().apply {
        fillType = PathFillType.EvenOdd
        addPath(samechOuter)
        addPath(samechInner)
    }

    val measureOuter = PathMeasure().apply { setPath(samechOuter, false) }
    val measureInner = PathMeasure().apply { setPath(samechInner, false) }

    val lenOuter = measureOuter.length
    val lenInner = measureInner.length
    val totalLength = lenOuter + lenInner
    val currentDistance = strokeProgress * totalLength

    val animatedPath = Path()
    if (currentDistance <= lenOuter) {
        measureOuter.getSegment(0f, currentDistance, animatedPath, true)
    } else {
        measureOuter.getSegment(0f, lenOuter, animatedPath, true)
        measureInner.getSegment(0f, currentDistance - lenOuter, animatedPath, true)
    }

    val scale = size.height / EM_HEIGHT
    val bounds = samechFull.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = samechFull, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawShin(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val shinPath = Path().apply {
        moveTo(0.500f, 10.550f)
        lineTo(8.659f, 0.973f)
        cubicTo(13.150f, 10.580f, 15.756f, 21.872f, 17.409f, 35.263f)
        cubicTo(28.396f, 33.751f, 30.543f, 24.320f, 26.395f, 9.250f)
        lineTo(35.855f, 0.736f)
        cubicTo(40.021f, 19.349f, 35.749f, 33.678f, 19.419f, 41.885f)
        lineTo(20.720f, 52.290f)
        cubicTo(25.292f, 56.970f, 28.913f, 53.849f, 34.436f, 51.699f)
        cubicTo(51.090f, 45.214f, 55.935f, 31.894f, 48.625f, 11.851f)
        lineTo(59.267f, 0.500f)
        cubicTo(66.619f, 28.021f, 53.812f, 57.403f, 26.636f, 62.578f)
        cubicTo(22.198f, 63.423f, 17.368f, 64.575f, 14.216f, 57.256f)
        cubicTo(10.076f, 38.668f, 5.587f, 22.516f, 0.500f, 10.550f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(shinPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = shinPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = shinPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawTav(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val tavPath = Path().apply {
        moveTo(3.692f, 16.462f)
        lineTo(19.655f, 0.500f)
        lineTo(19.773f, 6.885f)
        lineTo(46.434f, 7.073f)
        cubicTo(56.670f, 8.278f, 60.849f, 14.816f, 60.364f, 25.165f)
        cubicTo(60.364f, 25.165f, 60.091f, 42.885f, 60.212f, 56.074f)
        lineTo(51.935f, 65.297f)
        lineTo(51.859f, 22.166f)
        cubicTo(51.772f, 17.735f, 48.767f, 16.708f, 44.892f, 16.697f)
        lineTo(24.739f, 16.935f)
        cubicTo(27.874f, 42.402f, 27.201f, 57.606f, 23.084f, 63.523f)
        lineTo(0.499f, 63.760f)
        cubicTo(3.908f, 60.193f, 6.550f, 56.882f, 8.067f, 53.945f)
        lineTo(19.064f, 53.945f)
        cubicTo(19.657f, 43.740f, 18.707f, 31.713f, 15.516f, 16.462f)
        lineTo(3.692f, 16.462f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(tavPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = tavPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = tavPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawTet(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val tetPath = Path().apply {
        moveTo(25.904f, 16.088f)
        lineTo(30.523f, 4.541f)
        cubicTo(77.349f, -8.538f, 68.858f, 69.101f, 14.645f, 60.256f)
        cubicTo(12.082f, 47.711f, 7.991f, 22.753f, 0.500f, 10.603f)
        lineTo(9.161f, 0.500f)
        cubicTo(17.051f, 19.456f, 16.281f, 42.359f, 21.285f, 51.019f)
        cubicTo(57.910f, 61.389f, 67.801f, -2.269f, 25.904f, 16.088f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(tetPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = tetPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = tetPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawTsadeh(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val tsadehPath = Path().apply {
        moveTo(4.520f, 8.304f)
        lineTo(11.733f, 0.736f)
        cubicTo(19.222f, 15.565f, 26.710f, 23.322f, 34.199f, 28.996f)
        cubicTo(38.760f, 23.054f, 41.243f, 16.733f, 35.382f, 8.895f)
        lineTo(43.895f, 0.500f)
        cubicTo(50.897f, 12.680f, 47.811f, 22.767f, 38.101f, 31.479f)
        lineTo(47.206f, 43.067f)
        cubicTo(50.702f, 47.147f, 46.183f, 58.818f, 40.230f, 59.740f)
        cubicTo(32.209f, 60.981f, 0.500f, 60.449f, 0.500f, 60.449f)
        lineTo(7.594f, 50.280f)
        cubicTo(7.594f, 50.280f, 31.046f, 50.483f, 40.821f, 50.398f)
        cubicTo(46.298f, 50.351f, 15.883f, 27.684f, 4.520f, 8.304f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(tsadehPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = tsadehPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = tsadehPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawYod(progress: Float, isDarkMode: Boolean = false) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val yodPath = Path().apply {
        moveTo(0.500f, 10.027f)
        lineTo(9.449f, 0.500f)
        cubicTo(39.345f, 15.486f, 26.541f, 38.068f, 9.449f, 44.668f)
        cubicTo(23.259f, 23.756f, 22.199f, 19.584f, 0.500f, 10.027f)
        close()
    }

    val pathMeasure = PathMeasure().apply { setPath(yodPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

    val scale = size.height / EM_HEIGHT
    val bounds = yodPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)

    withTransform({
        translate(left = leftOffset, top = topOffset)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        if (fillProgress > 0f) {
            drawPath(path = yodPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}