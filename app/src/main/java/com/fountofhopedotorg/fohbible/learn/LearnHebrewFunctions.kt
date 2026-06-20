package com.fountofhopedotorg.fohbible.learn

import androidx.compose.ui.geometry.Offset
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

private fun DrawScope.calculateLayout(letterPath: Path): Triple<Float, Float, Float> {
    val scale = size.height / EM_HEIGHT
    val bounds = letterPath.getBounds()
    val (leftOffset, topOffset) = calculateOffsets(bounds, scale)
    return Triple(scale, leftOffset, topOffset)
}

private fun DrawScope.drawAnimatedLetter(
    progress: Float,
    letterPath: Path,
    isDarkMode: Boolean,
    scale: Float,
    leftOffset: Float,
    topOffset: Float
) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val pathMeasure = PathMeasure().apply { setPath(letterPath, false) }
    val animatedPath = Path()
    pathMeasure.getSegment(0f, pathMeasure.length * strokeProgress, animatedPath, startWithMoveTo = true)

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
            drawPath(path = letterPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawAlef(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(8.747f, 0.5f)
        lineTo(1.209f, 8.9249f)
        lineTo(16.876f, 21.3399f)
        cubicTo(6.949f, 28.6799f, 3.006f, 40.0629f, 4.904f, 56.0739f)
        lineTo(0.5f, 62.7549f)
        lineTo(16.788f, 62.7549f)
        lineTo(22.493f, 50.3099f)
        lineTo(14.511f, 50.3099f)
        cubicTo(9.634f, 43.9339f, 14.85f, 25.2759f, 20.571f, 24.5919f)
        lineTo(56.488f, 62.7259f)
        lineTo(62.252f, 53.1179f)
        lineTo(46.437f, 36.4159f)
        cubicTo(57.166f, 23.5609f, 60.74f, 10.9879f, 55.305f, 0.5f)
        lineTo(45.402f, 9.9599f)
        cubicTo(48.721f, 20.2029f, 48.143f, 27.8479f, 43.185f, 32.5739f)
        lineTo(8.747f, 0.5f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawBet(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(12.078f, -5f)
        lineTo(1.141f, 9.689f)
        lineTo(32.032f, 9.689f)
        cubicTo(34.293f, 10.041f, 35.859f, 10.473f, 36.102f, 13.651f)
        lineTo(35.98f, 52.74f)
        lineTo(8.708f, 52.798f)
        lineTo(0.5f, 62.708f)
        lineTo(44.92f, 62.701f)
        lineTo(53.352f, 52.884f)
        lineTo(44.92f, 52.888f)
        lineTo(45.053f, 16.466f)
        cubicTo(44.905f, 5.608f, 42.116f, 1.553f, 36.924f, 0.539f)
        lineTo(12.212f, 0.608f)
        lineTo(12.078f, -5f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawGimel(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(5.229f, 8.229f)
        lineTo(12.418f, 0f)                         // top aligned to y=0
        cubicTo(34.295f, 12.882f, 30.459f, 20.334f, 32.473f, 55.621f)
        lineTo(26.986f, 62.148f)
        cubicTo(25.651f, 58.581f, 25.168f, 52.913f, 25.094f, 46.067f)
        cubicTo(20.077f, 53.572f, 11.571f, 60.445f, 0.5f, 62.148f)
        cubicTo(3.067f, 58.944f, 4.964f, 54.316f, 5.513f, 49.851f)
        cubicTo(21.405f, 46.75f, 25.739f, 38.921f, 23.052f, 25.873f)
        cubicTo(21.426f, 17.979f, 15.234f, 11.929f, 5.229f, 8.229f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawDalet(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(12.442f, 0.209f)
        lineTo(12.506f, -5.006f)
        lineTo(0.5f, 10.098f)
        lineTo(31.301f, 10.144f)
        lineTo(31.23f, 62.526f)
        lineTo(39.27f, 54.958f)
        lineTo(39.339f, 10.037f)
        lineTo(45.542f, 10.043f)
        cubicTo(46.257f, 5.116f, 47.934f, 2.235f, 50.101f, 0.215f)
        lineTo(39.408f, 0.22f)
        lineTo(39.35f, -6f)
        lineTo(33.666f, 0.223f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawHe(progress: Float, isDarkMode: Boolean = false) {
    val rightSide = Path().apply {
        moveTo(11.844f, -5f)
        cubicTo(8.796f, -0.037f, 5.001f, 4.926f, 0.5f, 9.889f)
        lineTo(40.063f, 10.094f)
        cubicTo(42.577f, 10.589f, 44.379f, 11.93f, 44.309f, 15.493f)
        lineTo(44.131f, 62.972f)
        lineTo(52.833f, 54.932f)
        lineTo(53.106f, 12.508f)
        cubicTo(52.617f, 6.06f, 50.331f, 2.116f, 44.354f, 0.672f)
        lineTo(11.874f, 0.638f)
        lineTo(11.844f, -5f)
        close()
    }

    val leftLeg = Path().apply {
        moveTo(4.59f, 31.378f)
        cubicTo(7.785f, 29.401f, 10.811f, 26.83f, 13.683f, 23.723f)
        lineTo(13.565f, 54.708f)
        lineTo(4.666f, 62.938f)
        lineTo(4.59f, 31.378f)
        close()
    }

    val fullPath = Path().apply {
        addPath(rightSide)
        addPath(leftLeg)
    }
    val (scale, leftOffset, topOffset) = calculateLayout(fullPath)

    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val rightMeasure = PathMeasure().apply { setPath(rightSide, false) }
    val leftMeasure = PathMeasure().apply { setPath(leftLeg, false) }
    val totalLength = rightMeasure.length + leftMeasure.length
    val targetLength = totalLength * strokeProgress

    val animatedPath = Path()
    if (targetLength <= rightMeasure.length) {
        rightMeasure.getSegment(0f, targetLength, animatedPath, true)
    } else {
        rightMeasure.getSegment(0f, rightMeasure.length, animatedPath, true)
        val remaining = targetLength - rightMeasure.length
        leftMeasure.getSegment(0f, remaining, animatedPath, true)
    }

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
            drawPath(path = fullPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawVav(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(0.5f, 8.514f)
        lineTo(7.689f, 0f)
        lineTo(20.932f, 11.163f)
        cubicTo(23.38f, 13.546f, 24.784f, 16.625f, 24.811f, 20.622f)
        lineTo(22.975f, 55.792f)
        cubicTo(19.722f, 58.639f, 16.826f, 58.974f, 14.178f, 62.225f)
        lineTo(15.919f, 27.622f)
        cubicTo(16.401f, 23.671f, 15.411f, 20.852f, 13.27f, 18.919f)
        lineTo(0.5f, 8.514f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawZayin(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(0.5f, 6.245f)
        lineTo(6.175f, 1.25f)
        cubicTo(8.066f, 0f, 9.859f, 0.028f, 11.548f, 1.401f)
        lineTo(29.256f, 15.477f)
        cubicTo(26.963f, 17.471f, 25.571f, 20.067f, 25.094f, 23.272f)
        lineTo(15.938f, 16.763f)
        cubicTo(12.36f, 24.045f, 16.109f, 37.21f, 22.824f, 50.95f)
        lineTo(16.467f, 62.604f)
        cubicTo(9.047f, 41.438f, 4.244f, 21.364f, 12.002f, 15.174f)
        cubicTo(9.129f, 11.512f, 4.806f, 8.884f, 0.5f, 6.245f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawChet(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(0.5f, 0.902f)
        cubicTo(3.984f, -0.802f, 8.216f, -2.892f, 11.095f, -5f)
        lineTo(11.17f, 0.524f)
        lineTo(36.673f, 0.448f)
        cubicTo(47.174f, 3.199f, 52.426f, 12.17f, 54.249f, 21.581f)
        lineTo(54.211f, 55.918f)
        cubicTo(51.414f, 57.525f, 48.633f, 59.761f, 46.643f, 62.578f)
        lineTo(46.757f, 26.197f)
        cubicTo(45.745f, 18.996f, 41.898f, 12.417f, 35.235f, 10.059f)
        lineTo(11.17f, 10.059f)
        lineTo(11.076f, 56.864f)
        cubicTo(8.427f, 58.663f, 6.233f, 60.18f, 3.584f, 62.786f)
        lineTo(3.754f, 10.74f)
        cubicTo(3.805f, 6.325f, 2.669f, 3.097f, 0.5f, 0.902f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawAyin(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(1.73f, 10.216f)
        lineTo(10.243f, 0.378f)
        lineTo(27.932f, 47.486f)
        cubicTo(39.521f, 42.502f, 41.253f, 29.515f, 33.419f, 9.648f)
        lineTo(44.297f, 0f)
        cubicTo(50.541f, 46.657f, 41.362f, 48.624f, 1.38f, 69.992f)
        cubicTo(1.108f, 70.138f, 0.794f, 70.187f, 0.5f, 70.283f)
        lineTo(4.094f, 57.608f)
        lineTo(21.878f, 50.608f)
        cubicTo(16.574f, 35.346f, 10.061f, 21.442f, 1.73f, 10.216f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawKaf(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(2.909f, 9.12f)
        lineTo(15.417f, -6f)
        lineTo(15.601f, 0.157f)
        lineTo(33.155f, 0.176f)
        cubicTo(56.432f, 6.237f, 51.971f, 47.308f, 34.206f, 62.418f)
        lineTo(0.5f, 62.423f)
        lineTo(7.717f, 53.265f)
        lineTo(35.426f, 53.19f)
        cubicTo(42.899f, 44.042f, 46.818f, 14.905f, 28.89f, 9.12f)
        lineTo(2.909f, 9.12f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawLamed(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(9.581f, 0.505f)
        lineTo(9.676f, -17.326f)
        cubicTo(8.983f, -18.463f, 2.771f, -13.714f, 0.879f, -11.934f)
        cubicTo(0.297f, -9.538f, 2.767f, -7.614f, 3.953f, -5.218f)
        cubicTo(3.953f, -5.218f, 3.087f, 2.167f, 2.608f, 6.255f)
        cubicTo(2.529f, 6.925f, 2.742f, 7.597f, 3.192f, 8.099f)
        cubicTo(3.642f, 8.602f, 4.287f, 8.887f, 4.961f, 8.882f)
        cubicTo(11.996f, 8.835f, 30.534f, 8.711f, 30.534f, 8.711f)
        cubicTo(46.964f, 9.419f, 36.792f, 54.614f, 8.541f, 53.312f)
        lineTo(0.5f, 62.559f)
        cubicTo(34.958f, 65.01f, 52.398f, 24.191f, 41.525f, 6.65f)
        cubicTo(39.132f, 2.775f, 34.896f, 0.423f, 30.342f, 0.441f)
        cubicTo(23.323f, 0.453f, 9.581f, 0.505f, 9.581f, 0.505f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawMem(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(1.446f, 10.62f)
        lineTo(8.635f, 0.593f)
        cubicTo(13.148f, 7.136f, 15.316f, 15.334f, 15.541f, 24.903f)
        cubicTo(30.577f, -16f, 60.573f, -2.252f, 57.351f, 47.133f)
        cubicTo(54.18f, 54.86f, 50.969f, 60.501f, 49.311f, 62.93f)
        lineTo(17.622f, 62.93f)
        lineTo(23.96f, 53.282f)
        lineTo(50.73f, 53.282f)
        cubicTo(52.654f, -4.311f, 19.545f, -2.796f, 9.581f, 58.012f)
        lineTo(0.5f, 63.309f)
        cubicTo(7.681f, 42.114f, 14.125f, 25.246f, 1.446f, 10.62f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawNun(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(6.271f, 9.271f)
        lineTo(12.608f, 0f)
        lineTo(26.325f, 10.973f)
        cubicTo(28.015f, 12.834f, 28.702f, 14.694f, 28.879f, 16.555f)
        lineTo(28.784f, 51.176f)
        cubicTo(25.963f, 54.434f, 23.438f, 58.135f, 21.122f, 62.149f)
        lineTo(0.5f, 62.244f)
        lineTo(5.325f, 52.5f)
        lineTo(21.595f, 52.5f)
        lineTo(21.5f, 23.117f)
        cubicTo(19.044f, 18.364f, 13.52f, 13.772f, 6.271f, 9.271f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

// ─── Peh ────────────────────────────────────────────
fun DrawScope.drawPeh(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(16.888f, 36.892f)
        cubicTo(0.868f, 28.399f, 2.954f, 16.061f, 22.564f, 0f)
        cubicTo(36.745f, 4.296f, 50.013f, 19.087f, 50.013f, 19.087f)
        lineTo(49.807f, 48.504f)
        lineTo(42.831f, 63.048f)
        lineTo(0.5f, 62.929f)
        lineTo(7.476f, 52.997f)
        lineTo(41.175f, 53.47f)
        cubicTo(42.154f, 43.556f, 41.264f, 24.969f, 41.264f, 24.969f)
        cubicTo(42.667f, 24.091f, 23.53f, 8.018f, 19.49f, 10.051f)
        cubicTo(12.927f, 13.354f, 12.674f, 25.02f, 22.446f, 29.088f)
        lineTo(16.888f, 36.892f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawSamech(progress: Float, isDarkMode: Boolean = false) {
    val outer = Path().apply {
        moveTo(0.5f, 8.405f)                          // -6
        lineTo(11.095f, -5.5f)                        // -6
        lineTo(11.284f, 0.648f)                       // -6
        cubicTo(11.284f, 0.648f, 37.166f, 0.723f, 43.097f, 0.74f)
        cubicTo(44.636f, 0.746f, 46.163f, 1.004f, 47.619f, 1.504f)
        cubicTo(52.773f, 3.309f, 56.386f, 7.574f, 58.187f, 12.842f)
        cubicTo(58.816f, 14.642f, 59.131f, 16.537f, 59.12f, 18.444f)
        cubicTo(59.204f, 23.851f, 59.069f, 46.604f, 59.055f, 48.895f)
        cubicTo(59.05f, 49.581f, 58.998f, 50.266f, 58.9f, 50.945f)
        cubicTo(58.159f, 56.022f, 56.599f, 58.339f, 54.079f, 60.105f)
        cubicTo(51.393f, 61.982f, 48.191f, 62.981f, 44.914f, 62.964f)
        cubicTo(35.775f, 62.916f, 13.838f, 62.797f, 13.838f, 62.797f)
        cubicTo(8.781f, 62.861f, 5.526f, 59.958f, 4.757f, 52.959f)
        lineTo(4.379f, 12.189f)                       // -6
        cubicTo(4.035f, 10.114f, 2.609f, 8.967f, 0.5f, 8.405f)
        close()
    }

    val inner = Path().apply {
        moveTo(11.284f, 10.297f)                      // -6
        cubicTo(11.284f, 10.297f, 11.341f, 36.112f, 11.367f, 47.624f)
        cubicTo(11.373f, 50.627f, 13.806f, 53.059f, 16.808f, 53.066f)
        cubicTo(27.258f, 53.091f, 49.369f, 53.142f, 51.988f, 53.148f)
        cubicTo(52.084f, 53.148f, 52.179f, 53.133f, 52.269f, 53.102f)
        cubicTo(52.57f, 51.536f, 52.054f, 19f, 52.054f, 19f)   // -6 (25→19)
        cubicTo(52.394f, 14.07f, 48.594f, 11f, 42.879f, 10.108f) // -6
        lineTo(11.284f, 10.297f)
        close()
    }

    val fullPath = Path().apply {
        fillType = PathFillType.EvenOdd
        addPath(outer)
        addPath(inner)
    }
    val (scale, leftOffset, topOffset) = calculateLayout(fullPath)

    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val outerMeasure = PathMeasure().apply { setPath(outer, false) }
    val innerMeasure = PathMeasure().apply { setPath(inner, false) }
    val totalLength = outerMeasure.length + innerMeasure.length
    val targetLength = totalLength * strokeProgress

    val animatedPath = Path()
    if (targetLength <= outerMeasure.length) {
        outerMeasure.getSegment(0f, targetLength, animatedPath, true)
    } else {
        outerMeasure.getSegment(0f, outerMeasure.length, animatedPath, true)
        innerMeasure.getSegment(0f, targetLength - outerMeasure.length, animatedPath, true)
    }

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
            drawPath(path = fullPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawQof(progress: Float, isDarkMode: Boolean = false) {
    val outer = Path().apply {
        moveTo(0.5f, 8.818f)                           // -2 from 10.818
        lineTo(14.098f, -7.5f)                         // -2 from -5.5
        lineTo(14.098f, 0.068f)                        // -2 from 2.068
        cubicTo(14.098f, 0.068f, 30.052f, 0.128f, 39.149f, 0.163f)   // -2
        cubicTo(49.313f, 0.201f, 57.753f, 8.02f, 58.566f, 18.151f)  // -2
        cubicTo(60.152f, 37.466f, 41.432f, 63.437f, 21.382f, 62.287f) // -2
        lineTo(24.693f, 52.308f)                       // -2 from 54.308
        cubicTo(49.64f, 51.185f, 59.809f, 13.146f, 42.122f, 8.936f)  // -2
        lineTo(0.5f, 8.818f)                           // -2
        close()
    }

    val innerLeg = Path().apply {
        moveTo(14.169f, 21.328f)                       // -2 from 23.328
        cubicTo(12.261f, 25.278f, 8.904f, 29.241f, 5.419f, 32.561f)  // -2
        lineTo(5.656f, 83.169f)                        // -2 from 85.169
        cubicTo(7.726f, 83.472f, 10.523f, 80.58f, 14.169f, 73.946f)  // -2
        lineTo(14.169f, 21.328f)                       // -2
        close()
    }

    val fullPath = Path().apply {
        addPath(outer)
        addPath(innerLeg)
    }
    val (scale, leftOffset, topOffset) = calculateLayout(fullPath)

    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)

    val outerMeasure = PathMeasure().apply { setPath(outer, false) }
    val innerMeasure = PathMeasure().apply { setPath(innerLeg, false) }
    val totalLength = outerMeasure.length + innerMeasure.length
    val targetLength = totalLength * strokeProgress

    val animatedPath = Path()
    if (targetLength <= outerMeasure.length) {
        outerMeasure.getSegment(0f, targetLength, animatedPath, true)
    } else {
        outerMeasure.getSegment(0f, outerMeasure.length, animatedPath, true)
        val remaining = targetLength - outerMeasure.length
        innerMeasure.getSegment(0f, remaining, animatedPath, true)
    }

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
            drawPath(path = fullPath, color = color.copy(alpha = fillProgress), style = Fill)
        }
    }
}

fun DrawScope.drawShin(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(0.5f, 10.05f)
        lineTo(8.848f, 0.094f)
        cubicTo(13.339f, 9.702f, 15.756f, 21.372f, 17.409f, 34.763f)
        cubicTo(28.396f, 33.251f, 30.543f, 23.82f, 26.395f, 8.75f)
        lineTo(35.855f, 0.236f)
        cubicTo(40.021f, 18.849f, 35.749f, 33.178f, 19.419f, 41.385f)
        lineTo(20.72f, 51.601f)
        cubicTo(25.292f, 56.281f, 28.913f, 53.16f, 34.436f, 51.01f)
        cubicTo(51.09f, 44.525f, 55.935f, 31.394f, 48.625f, 11.351f)
        lineTo(59.267f, 0f)
        cubicTo(66.619f, 27.521f, 53.812f, 56.714f, 26.636f, 61.889f)
        cubicTo(22.198f, 62.734f, 17.368f, 63.886f, 14.216f, 56.567f)
        cubicTo(10.076f, 37.979f, 5.587f, 22.016f, 0.5f, 10.05f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawTav(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(3.031f, 9.962f)
        lineTo(18.994f, -6f)
        lineTo(19.112f, 0.385f)
        lineTo(45.773f, 0.573f)
        cubicTo(56.009f, 1.778f, 60.188f, 8.316f, 59.703f, 18.665f)
        cubicTo(59.703f, 18.665f, 59.713f, 40.736f, 59.835f, 53.925f)
        lineTo(51.558f, 63.148f)
        lineTo(51.198f, 15.666f)
        cubicTo(51.111f, 11.235f, 48.106f, 10.208f, 44.231f, 10.197f)
        lineTo(24.078f, 10.435f)
        cubicTo(27.213f, 35.902f, 27.013f, 56.876f, 22.896f, 62.793f)
        lineTo(0.5f, 63.03f)
        cubicTo(3.909f, 59.463f, 6.362f, 56.152f, 7.879f, 53.216f)
        lineTo(18.875f, 53.216f)
        cubicTo(19.469f, 43.01f, 18.046f, 25.213f, 14.855f, 9.962f)
        lineTo(3.031f, 9.962f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawResh(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(0.5f, 9.963f)
        lineTo(13.27f, -6f)
        lineTo(13.388f, 0.148f)
        lineTo(30.297f, 0.148f)
        cubicTo(38.308f, 2.327f, 43.057f, 6.878f, 44.131f, 19.101f)
        lineTo(44.131f, 52.648f)
        lineTo(35.263f, 62.581f)
        lineTo(35.381f, 17.294f)
        cubicTo(35.13f, 13.793f, 32.795f, 11.335f, 28.76f, 9.726f)
        lineTo(0.5f, 9.963f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawTet(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(25.809f, 13.626f)
        lineTo(30.428f, 2.079f)
        cubicTo(77.254f, -9f, 68.952f, 70.801f, 14.739f, 61.956f)
        cubicTo(12.176f, 49.411f, 7.991f, 22.751f, 0.5f, 10.601f)
        lineTo(9.16f, 0.497f)
        cubicTo(17.051f, 19.454f, 16.375f, 44.059f, 21.379f, 52.719f)
        cubicTo(58.004f, 63.089f, 67.706f, -4.269f, 25.809f, 13.626f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

fun DrawScope.drawTsadeh(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(4.521f, 7.804f)
        lineTo(11.733f, 0.237f)
        cubicTo(19.222f, 15.066f, 26.711f, 22.822f, 34.2f, 28.497f)
        cubicTo(38.761f, 22.554f, 41.243f, 16.233f, 35.382f, 8.396f)
        lineTo(43.895f, 0f)
        cubicTo(50.897f, 12.181f, 47.812f, 22.268f, 38.102f, 30.98f)
        lineTo(47.206f, 45.217f)
        cubicTo(50.703f, 49.296f, 46.183f, 60.967f, 40.23f, 61.889f)
        cubicTo(32.209f, 63.13f, 0.5f, 62.598f, 0.5f, 62.598f)
        lineTo(7.595f, 52.429f)
        cubicTo(7.595f, 52.429f, 31.046f, 52.632f, 40.821f, 52.548f)
        cubicTo(46.298f, 52.5f, 15.884f, 27.185f, 4.521f, 7.804f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}

// ─── Yod ────────────────────────────────────────────
fun DrawScope.drawYod(progress: Float, isDarkMode: Boolean = false) {
    val path = Path().apply {
        moveTo(0.5f, 9.526f)
        lineTo(9.449f, 0f)
        cubicTo(39.345f, 14.986f, 26.541f, 38.892f, 9.449f, 45.492f)
        cubicTo(23.259f, 24.58f, 22.199f, 19.084f, 0.5f, 9.526f)
        close()
    }
    val (scale, leftOffset, topOffset) = calculateLayout(path)
    drawAnimatedLetter(progress, path, isDarkMode, scale, leftOffset, topOffset)
}