package com.fountofhopedotorg.fohbible.learn

import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.core.graphics.PathParser

private const val EM_HEIGHT = 112f
private const val BASELINE_PATH_Y = 62f
private const val BASELINE_FRACTION = 0.82f
private const val BIG_ALPHA_PATH = "M81.209,1260.46C105.824,1257.02 138.071,1153.04 163.107,1104.6C190.34,1187.24 215.419,1248.11 241.244,1260.04C218.5,1265.22 196.787,1266.27 176.896,1260.04C196.933,1250.47 192.042,1229.57 180.656,1205.72L131.768,1205.3C117.097,1234.9 113.944,1256.53 136.365,1260.46C117.979,1265.23 99.594,1266.38 81.209,1260.46ZM157.792,1144.51L136.749,1192.58L176.445,1192.96L157.792,1144.51Z"
private const val SMALL_ALPHA_PATH = "M378.716,1177.3C379.675,1164.54 386.231,1157.06 400.026,1156.41C387.572,1207.57 388.977,1253.38 419.866,1246.4C405.381,1284.49 389.023,1273.6 374.538,1242.49C364.758,1261.46 345.563,1271.08 334.625,1269.01C288.333,1251.94 292.363,1162.16 348.631,1152.23C359.441,1151.1 372.988,1158.56 378.716,1177.3ZM343.099,1171.54C313.543,1182.94 322.034,1239.6 343.199,1245.83C360.092,1247.03 372.261,1238.77 372.031,1205.72C370.687,1184.41 365.747,1173.5 343.099,1171.54Z"
private const val BIG_BETA_PATH = "M510.289,1106.79L591.769,1105.95C629.902,1121.46 629.128,1163.99 599.291,1177.41C641.966,1193.13 642.274,1246.23 599.291,1263.9L507.782,1264.32C535.404,1254.28 544.092,1106.52 510.289,1106.79ZM551.656,1118.07L552.492,1173.65C618.459,1182.97 616.11,1108.16 551.656,1118.07ZM550.82,1187.02L550.82,1248.02C626.559,1272.77 627.061,1171.48 550.82,1187.02Z"
private const val SMALL_BETA_PATH = "M715.452,1314.88L715.87,1141.05C720.658,1054.33 854.546,1091.84 777.712,1165.71C856.017,1210.49 776.694,1295.83 734.255,1259.31L735.509,1323.65C725.285,1327.78 721.219,1328.58 715.452,1314.88ZM734.673,1238C780.062,1285.8 826.339,1192.28 746.373,1172.81C806.198,1138.08 767.889,1076.34 733.42,1125.18L734.673,1238Z"

private fun createAdaptedGreekPath(pathData: String, svgTranslateX: Float): Path {
    val androidPath = PathParser.createPathFromPathData(pathData)
    val matrix = Matrix()

    val fontAbsoluteTop = 1104.6f
    val fontAbsoluteBaseline = 1260.46f
    val fontHeight = fontAbsoluteBaseline - fontAbsoluteTop

    matrix.postTranslate(svgTranslateX, -fontAbsoluteTop)
    val scale = BASELINE_PATH_Y / fontHeight
    matrix.postScale(scale, scale)

    androidPath.transform(matrix)
    return androidPath.asComposePath()
}

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

private fun splitSvgPathData(pathData: String): List<String> {
    val contours = mutableListOf<String>()
    var startIndex = 0
    var i = 0
    while (i < pathData.length) {
        if (i > 0 && pathData[i] == 'M' && pathData[i - 1] !in "eE") {
            contours.add(pathData.substring(startIndex, i).trim())
            startIndex = i
        }
        i++
    }
    if (startIndex < pathData.length) {
        contours.add(pathData.substring(startIndex).trim())
    }
    return contours
}

private fun DrawScope.drawGreekLetterWithHoles(
    progress: Float,
    pathString: String,
    svgTranslateX: Float,
    isDarkMode: Boolean
) {
    val color = if (isDarkMode) Color.White else Color(0xFF1A237E)
    val strokeEnd = 0.8f
    val strokeProgress = (progress / strokeEnd).coerceIn(0f, 1f)
    val fillProgress = ((progress - strokeEnd) / (1f - strokeEnd)).coerceIn(0f, 1f)
    val contourStrings = splitSvgPathData(pathString)
    val contours = contourStrings.map { createAdaptedGreekPath(it, svgTranslateX) }

    val fullPath = Path().apply {
        fillType = PathFillType.EvenOdd
        contours.forEach { addPath(it) }
    }

    val measures = contours.map { PathMeasure().apply { setPath(it, false) } }
    val totalLength = measures.sumOf { it.length.toDouble() }.toFloat()
    val targetLength = totalLength * strokeProgress

    val animatedPath = Path()
    var accumulated = 0f
    for ((index, measure) in measures.withIndex()) {
        val len = measure.length
        if (accumulated + len <= targetLength) {
            animatedPath.addPath(contours[index])
            accumulated += len
        } else {
            val remaining = targetLength - accumulated
            measure.getSegment(0f, remaining, animatedPath, true)
            break
        }
    }

    val (scale, leftOffset, topOffset) = calculateLayout(fullPath)

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

fun DrawScope.drawBigAlpha(progress: Float, isDarkMode: Boolean = false) {
    drawGreekLetterWithHoles(progress, BIG_ALPHA_PATH, -80.7087f, isDarkMode)
}

fun DrawScope.drawSmallAlpha(progress: Float, isDarkMode: Boolean = false) {
    drawGreekLetterWithHoles(progress, SMALL_ALPHA_PATH, -302.37f, isDarkMode)
}

fun DrawScope.drawBigBeta(progress: Float, isDarkMode: Boolean = false) {
    drawGreekLetterWithHoles(progress, BIG_BETA_PATH, -507.282f, isDarkMode)
}

fun DrawScope.drawSmallBeta(progress: Float, isDarkMode: Boolean = false) {
    drawGreekLetterWithHoles(progress, SMALL_BETA_PATH, -714.952f, isDarkMode)
}