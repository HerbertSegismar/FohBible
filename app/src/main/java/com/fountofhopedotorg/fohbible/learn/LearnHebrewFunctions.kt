package com.fountofhopedotorg.fohbible.learn

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform

fun DrawScope.drawAleph(progress: Float) {
    val color = Color(0xFF1A237E)

    val baseWidth = 63f
    val baseHeight = 64f

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

    withTransform({
        scale(size.width / baseWidth, size.height / baseHeight, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        if (fillProgress > 0f) {
            drawPath(
                path = alephPath,
                color = color.copy(alpha = fillProgress),
                style = Fill
            )
        }
    }
}

fun DrawScope.drawBeth(progress: Float) {
    val color = Color(0xFF1A237E)

    val baseWidth = 54f
    val baseHeight = 64f

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

    withTransform({
        scale(size.width / baseWidth, size.height / baseHeight, pivot = Offset.Zero)
    }) {
        drawPath(
            path = animatedPath,
            color = color,
            style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        if (fillProgress > 0f) {
            drawPath(
                path = bethPath,
                color = color.copy(alpha = fillProgress),
                style = Fill
            )
        }
    }
}

fun DrawScope.drawGimel(progress: Float) {
    val color = Color(0xFF1A237E)

    val baseWidth = 33f
    val baseHeight = 64f

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

    val scale = minOf(size.width / baseWidth, size.height / baseHeight)

    val leftOffset = (size.width - (baseWidth * scale)) / 2f
    val topOffset = (size.height - (baseHeight * scale)) / 2f

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
            drawPath(
                path = gimelPath,
                color = color.copy(alpha = fillProgress),
                style = Fill
            )
        }
    }
}

fun DrawScope.drawDalet(progress: Float) {
    val color = Color(0xFF1A237E)

    val baseWidth = 51f
    val baseHeight = 64f

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

    val scale = minOf(size.width / baseWidth, size.height / baseHeight)

    val leftOffset = (size.width - (baseWidth * scale)) / 2f
    val topOffset = (size.height - (baseHeight * scale)) / 2f

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
            drawPath(
                path = daletPath,
                color = color.copy(alpha = fillProgress),
                style = Fill
            )
        }
    }
}

fun DrawScope.drawHe(progress: Float) {
    val color = Color(0xFF1A237E)

    val baseWidth = 54f
    val baseHeight = 64f

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

    val scale = minOf(size.width / baseWidth, size.height / baseHeight)
    val leftOffset = (size.width - (baseWidth * scale)) / 2f
    val topOffset = (size.height - (baseHeight * scale)) / 2f

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
            drawPath(
                path = fullHePath,
                color = color.copy(alpha = fillProgress),
                style = Fill
            )
        }
    }
}

fun DrawScope.drawVav(progress: Float) {
    val color = Color(0xFF1A237E)

    val baseWidth = 26f
    val baseHeight = 64f

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

    val scale = minOf(size.width / baseWidth, size.height / baseHeight)
    val leftOffset = (size.width - (baseWidth * scale)) / 2f
    val topOffset = (size.height - (baseHeight * scale)) / 2f

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

fun DrawScope.drawZayin(progress: Float) {
    val color = Color(0xFF1A237E)

    val baseWidth = 30f
    val baseHeight = 64f

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

    val scale = minOf(size.width / baseWidth, size.height / baseHeight)
    val leftOffset = (size.width - (baseWidth * scale)) / 2f
    val topOffset = (size.height - (baseHeight * scale)) / 2f

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

fun DrawScope.drawChet(progress: Float) {
    val color = Color(0xFF1A237E)

    val baseWidth = 55f
    val baseHeight = 64f

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

    val scale = minOf(size.width / baseWidth, size.height / baseHeight)
    val leftOffset = (size.width - (baseWidth * scale)) / 2f
    val topOffset = (size.height - (baseHeight * scale)) / 2f

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