package com.fountofhopedotorg.fohbible.gfx_animator

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.sp
import com.fountofhopedotorg.fohbible.data.BezierNode
import com.fountofhopedotorg.fohbible.data.BoundingBox
import com.fountofhopedotorg.fohbible.data.CanvasElement
import com.fountofhopedotorg.fohbible.data.CrownStructure
import com.fountofhopedotorg.fohbible.data.GradientConfig
import com.fountofhopedotorg.fohbible.data.ProcessingOptions
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import androidx.core.graphics.createBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathSegment
import kotlin.math.sqrt
import androidx.compose.ui.graphics.Color as ComposeColor

fun buildReferenceString(
    bookName: String,
    chapter: Int?,
    startVerse: Int?,
    endVerse: Int?
): String {
    val fullBook = bookName.replaceFirstChar { it.uppercase() }
    return when {
        startVerse == null -> "$fullBook $chapter"
        endVerse == null || startVerse == endVerse -> "$fullBook $chapter:$startVerse"
        else -> "$fullBook $chapter:$startVerse-$endVerse"
    }
}

suspend fun saveCanvasAsImage(
    context: Context,
    elements: List<CanvasElement>,
    gradientConfigs: Map<String, GradientConfig>,
    imageBitmaps: Map<String, Bitmap>,
    fontCache: Map<String, Typeface>,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    scaleFactor: Float = 1f,
    canvasBackgroundColor: ComposeColor? = null,
    canvasBackgroundBrush: Brush? = null,
    timeMs: Long = 0L,
    format: String = "JPG"
) {
    try {
        val exportWidth = (canvasWidthPx * scaleFactor).roundToInt()
        val exportHeight = (canvasHeightPx * scaleFactor).roundToInt()

        val bitmap = createBitmap(exportWidth, exportHeight)
        val canvas = Canvas(bitmap)

        drawFrame(
            canvas = canvas,
            elements = elements,
            timeMs = timeMs,
            gradientConfigs = gradientConfigs,
            imageBitmaps = imageBitmaps,
            scaleFactor = scaleFactor,
            canvasWidth = exportWidth.toFloat(),
            canvasHeight = exportHeight.toFloat(),
            canvasBackgroundColor = canvasBackgroundColor,
            canvasBackgroundBrush = canvasBackgroundBrush,
            fontCache = fontCache,
            textSizePxBase = 60f * context.resources.configuration.fontScale
        )

        val fileName = "foh_canvas_${System.currentTimeMillis()}.${format.lowercase()}"
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, if (format == "JPG") "image/jpeg" else "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FOHBible")
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            withContext(Dispatchers.IO) {
                resolver.openOutputStream(uri).use { out ->
                    if (out != null) {
                        if (format == "JPG") bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        else bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
            }
            bitmap.recycle()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved to Pictures/FOHBible", Toast.LENGTH_LONG).show()
            }
        } else {
            bitmap.recycle()
            throw Exception("Failed to create MediaStore entry.")
        }
    } catch (_: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
suspend fun saveCanvasAsPDF(
    context: Context,
    elements: List<CanvasElement>,
    gradientConfigs: Map<String, GradientConfig>,
    imageBitmaps: Map<String, Bitmap>,
    fontCache: Map<String, Typeface>,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    scaleFactor: Float = 1f,
    canvasBackgroundColor: ComposeColor? = null,
    canvasBackgroundBrush: Brush? = null,
    timeMs: Long = 0L
) {
    try {
        val exportWidth = (canvasWidthPx * scaleFactor).roundToInt()
        val exportHeight = (canvasHeightPx * scaleFactor).roundToInt()

        val bitmap = createBitmap(exportWidth, exportHeight)
        val canvas = Canvas(bitmap)

        drawFrame(
            canvas = canvas,
            elements = elements,
            timeMs = timeMs,
            gradientConfigs = gradientConfigs,
            imageBitmaps = imageBitmaps,
            scaleFactor = scaleFactor,
            canvasWidth = exportWidth.toFloat(),
            canvasHeight = exportHeight.toFloat(),
            canvasBackgroundColor = canvasBackgroundColor,
            canvasBackgroundBrush = canvasBackgroundBrush,
            fontCache = fontCache,
            textSizePxBase = 60f * context.resources.configuration.fontScale
        )

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(exportWidth, exportHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        val fileName = "foh_canvas_${System.currentTimeMillis()}.pdf"
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FOHBible")
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            withContext(Dispatchers.IO) {
                resolver.openOutputStream(uri).use { pdfDocument.writeTo(it) }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "PDF saved to Downloads/FOHBible", Toast.LENGTH_LONG).show()
            }
        } else {
            throw Exception("Failed to create MediaStore entry.")
        }

        pdfDocument.close()
        bitmap.recycle()
    } catch (_: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
suspend fun saveCanvasAsSVG(
    context: Context,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    elements: List<CanvasElement>,
    gradientConfigs: Map<String, GradientConfig>,
    canvasBackgroundColor: Color? = null,
    canvasBackgroundBrush: Brush? = null
) {
    try {
        val allShapes = elements.all { it.content.startsWith("Shape:") }

        val svgContent: String

        if (allShapes) {
            svgContent = buildVectorSvg(elements, canvasWidthPx.toFloat(), canvasHeightPx.toFloat(), gradientConfigs)
        } else {
            val bitmap = renderCanvasToBitmap(
                context = context,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                elements = elements,
                gradientConfigs = gradientConfigs,
                canvasBackgroundColor = canvasBackgroundColor,
                canvasBackgroundBrush = canvasBackgroundBrush,
                timeMs = 0L
            )

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" 
                     viewBox="0 0 $canvasWidthPx $canvasHeightPx" 
                     width="$canvasWidthPx" height="$canvasHeightPx">
                    <image width="$canvasWidthPx" height="$canvasHeightPx" 
                           href="data:image/png;base64,$base64String" />
                </svg>
            """.trimIndent()

            bitmap.recycle()
        }

        val fileName = "foh_canvas_${System.currentTimeMillis()}.svg"
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/svg+xml")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FOHBible")
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            withContext(Dispatchers.IO) {
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(svgContent.toByteArray(Charsets.UTF_8))
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "SVG saved to Downloads/FOHBible", Toast.LENGTH_LONG).show()
            }
        } else {
            throw Exception("Failed to create MediaStore entry.")
        }
    } catch (_: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to save SVG", Toast.LENGTH_SHORT).show()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private suspend fun renderCanvasToBitmap(
    context: Context,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    elements: List<CanvasElement>,
    gradientConfigs: Map<String, GradientConfig>,
    canvasBackgroundColor: Color?,
    canvasBackgroundBrush: Brush?,
    timeMs: Long
): Bitmap = withContext(Dispatchers.IO) {
    val scaleFactor = 1f
    val imageBitmaps = loadImageBitmaps(elements, context, canvasWidthPx.toFloat(), canvasHeightPx.toFloat())
    val fontCache = preloadFonts(elements, context)
    val textSizePxBase = 60f * context.resources.configuration.fontScale

    val bitmap = createBitmap(canvasWidthPx, canvasHeightPx)
    val canvas = Canvas(bitmap)

    drawFrame(
        canvas = canvas,
        elements = elements,
        timeMs = timeMs,
        gradientConfigs = gradientConfigs,
        imageBitmaps = imageBitmaps,
        scaleFactor = scaleFactor,
        canvasWidth = canvasWidthPx.toFloat(),
        canvasHeight = canvasHeightPx.toFloat(),
        canvasBackgroundColor = canvasBackgroundColor,
        canvasBackgroundBrush = canvasBackgroundBrush,
        fontCache = fontCache,
        textSizePxBase = textSizePxBase
    )

    imageBitmaps.values.forEach { it.recycle() }
    bitmap
}

private fun buildVectorSvg(
    elements: List<CanvasElement>,
    canvasWidth: Float,
    canvasHeight: Float,
    gradientConfigs: Map<String, GradientConfig>
): String {
    val sb = StringBuilder()
    sb.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $canvasWidth $canvasHeight" width="$canvasWidth" height="$canvasHeight">""")

    val gradDefs = StringBuilder()
    val elementGradientIds = mutableMapOf<String, String>()

    for (element in elements) {
        val gradConfig = gradientConfigs[element.id] ?: continue
        val gradId = "grad-${element.id}"
        elementGradientIds[element.id] = gradId

        val startColor = gradConfig.startColor
        val endColor = gradConfig.endColor

        gradDefs.append("""
        <linearGradient id="$gradId" gradientUnits="objectBoundingBox"
                        x1="${gradConfig.startOffset.x}" y1="${gradConfig.startOffset.y}"
                        x2="${gradConfig.endOffset.x}"   y2="${gradConfig.endOffset.y}">
            <stop offset="0%" stop-color="${colorToHex(startColor)}" stop-opacity="${startColor.alpha}" />
            <stop offset="100%" stop-color="${colorToHex(endColor)}" stop-opacity="${endColor.alpha}" />
        </linearGradient>
        """.trimIndent())
    }

    sb.append("\n  <defs>\n$gradDefs  </defs>\n")
    sb.append("  <rect width=\"100%\" height=\"100%\" fill=\"white\" />\n")

    for (element in elements) {
        if (!element.isVisible) continue
        val x = element.offset.x
        val y = element.offset.y
        val w = element.width
        val h = element.height
        val color = colorToHex(element.backgroundColor)
        val alpha = element.backgroundColor.alpha
        val rot = element.rotation

        val cx = element.pivotX * w
        val cy = element.pivotY * h

        val transform = buildString {
            append("translate($x, $y)")
            append(" translate($cx, $cy)")
            if (rot != 0f) append(" rotate($rot)")
            if (element.scaleX != 1f || element.scaleY != 1f)
                append(" scale(${element.scaleX}, ${element.scaleY})")
            append(" translate(${-cx}, ${-cy})")
        }

        if (element.content.trim() == "Shape: ThornCrown") {
            val seed = element.id.hashCode().toLong()
            val size = Size(w, h)
            val crown = generateThornCrownPaths(seed, size)

            val strokeWidthScale = minOf(w, h) / 938f
            val vineStrokeWidth = 8f * strokeWidthScale

            val vineD = pathToSvgPathData(crown.vinePath)
            val thornsD = pathToSvgPathData(crown.thornsPath)

            val fillAttr = if (element.id in elementGradientIds) {
                "fill=\"url(#${elementGradientIds[element.id]})\""
            } else {
                "fill=\"$color\" fill-opacity=\"$alpha\""
            }

            sb.append("  <g transform=\"$transform\">\n")
            sb.append("    <path d=\"$vineD\" fill=\"none\" stroke=\"$color\" stroke-opacity=\"$alpha\" stroke-width=\"$vineStrokeWidth\" stroke-linecap=\"round\" stroke-linejoin=\"round\" />\n")
            sb.append("    <path d=\"$thornsD\" $fillAttr />\n")
            sb.append("  </g>\n")
            continue
        }

        val gradId = elementGradientIds[element.id]
        val shape = buildShapeSvg(element, w, h, color, gradId)
        if (shape.isNotEmpty()) {
            sb.append("  <g transform=\"$transform\">\n    $shape\n  </g>\n")
        }
    }
    sb.append("</svg>")
    return sb.toString()
}

private fun pathToSvgPathData(path: Path): String {
    val sb = StringBuilder()
    val iterator = path.iterator()
    val points = FloatArray(8)

    while (iterator.hasNext()) {
        val type = iterator.next(points)
        when (type) {
            PathSegment.Type.Move -> sb.append("M ${points[0]} ${points[1]} ")
            PathSegment.Type.Line -> sb.append("L ${points[0]} ${points[1]} ")
            PathSegment.Type.Quadratic -> sb.append("Q ${points[0]} ${points[1]} ${points[2]} ${points[3]} ")
            PathSegment.Type.Conic -> sb.append("Q ${points[0]} ${points[1]} ${points[2]} ${points[3]} ") // Map to Quad
            PathSegment.Type.Cubic -> sb.append("C ${points[0]} ${points[1]} ${points[2]} ${points[3]} ${points[4]} ${points[5]} ")
            PathSegment.Type.Close -> sb.append("Z ")
            PathSegment.Type.Done -> {}
        }
    }
    return sb.toString().trim()
}

private fun buildShapeSvg(
    element: CanvasElement,
    w: Float,
    h: Float,
    hexColor: String,
    gradientId: String? = null
): String {
    val content = element.content.trim()
    val alpha = element.backgroundColor.alpha

    val fillAttr = if (gradientId != null) {
        "fill=\"url(#$gradientId)\""
    } else {
        "fill=\"$hexColor\" fill-opacity=\"$alpha\""
    }

    when {
        content.startsWith("Shape: Square") -> {
            return """<rect x="0" y="0" width="$w" height="$h" $fillAttr />"""
        }
        content.startsWith("Shape: Circle") -> {
            return """<ellipse cx="${w / 2}" cy="${h / 2}" rx="${w / 2}" ry="${h / 2}" $fillAttr />"""
        }
        content.startsWith("Shape: Triangle") -> {
            val points = "${w / 2},0 $w,$h 0,$h"
            return """<polygon points="$points" $fillAttr />"""
        }
        content.startsWith("Shape: Pentagon") -> {
            val sides = 5
            val angleOffset = -Math.PI / 2
            val centerX = w / 2f
            val centerY = h / 2f
            val radius = minOf(centerX, centerY)

            val pointsStr = (0 until sides).joinToString(" ") { i ->
                val angle = angleOffset + 2.0 * Math.PI * i / sides
                val x = centerX + radius * cos(angle).toFloat()
                val y = centerY + radius * sin(angle).toFloat()
                "$x,$y"
            }

            return """<polygon points="$pointsStr" $fillAttr />"""
        }

        content.startsWith("Shape: Line") || content.startsWith("Shape:CustomLine:") -> {
            val pointsData = if (content.startsWith("Shape: Line")) {
                getSerializedPointsForShape("Line")
            } else {
                content.removePrefix("Shape:CustomLine:")
            }
            val segments = pointsData.split(";").filter { it.isNotEmpty() }
            if (segments.size >= 2) {
                val d = buildPathD(segments, w, h, close = false)
                return """<path d="$d" fill="none" stroke="$hexColor" stroke-opacity="$alpha" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />"""
            }
        }

        content.startsWith("Shape:CustomPolygon:") -> {
            val serialized = content.removePrefix("Shape:CustomPolygon:")
            val segments = serialized.split(";").filter { it.isNotEmpty() }
            if (segments.size >= 2) {
                val d = buildPathD(segments, w, h, close = true)
                return """<path d="$d" $fillAttr stroke="$hexColor" stroke-opacity="$alpha" stroke-width="1" stroke-linecap="round" stroke-linejoin="round" />"""
            }
        }

        content.startsWith("Shape:") -> {
            val shapeName = content.removePrefix("Shape:").trim()
            val pointsData = getSerializedPointsForShape(shapeName)
            if (pointsData.isNotEmpty()) {
                val segments = pointsData.split(";").filter { it.isNotEmpty() }
                if (segments.size >= 2) {
                    val d = buildPathD(segments, w, h, close = true)
                    return """<path d="$d" $fillAttr stroke="$hexColor" stroke-opacity="$alpha" stroke-width="1" stroke-linecap="round" stroke-linejoin="round" />"""
                }
            }
        }
    }
    return ""
}

private fun buildPathD(segments: List<String>, w: Float, h: Float, close: Boolean): String {
    if (segments.isEmpty()) return ""

    val parsed = segments.map { seg ->
        val parts = seg.split(":")
        val anchorCoords = parts[0].split(",")

        val ax = (anchorCoords[0].toFloatOrNull() ?: 0f) * w
        val ay = (anchorCoords[1].toFloatOrNull() ?: 0f) * h
        val anchor = Offset(ax, ay)

        val handleIn = if (parts.size > 1) {
            val hi = parts[1].split(",")
            if (hi.size == 2) Offset(
                (hi[0].toFloatOrNull() ?: 0f) * w,
                (hi[1].toFloatOrNull() ?: 0f) * h
            ) else anchor
        } else anchor

        val handleOut = if (parts.size > 2) {
            val ho = parts[2].split(",")
            if (ho.size == 2) Offset(
                (ho[0].toFloatOrNull() ?: 0f) * w,
                (ho[1].toFloatOrNull() ?: 0f) * h
            ) else anchor
        } else anchor

        Triple(anchor, handleIn, handleOut)
    }

    val d = StringBuilder()

    parsed.forEachIndexed { index, (anchor, handleIn) ->
        if (index == 0) {
            d.append("M ${anchor.x},${anchor.y} ")
        } else {
            val prevHandleOut = parsed[index - 1].third
            d.append("C ${prevHandleOut.x},${prevHandleOut.y} ${handleIn.x},${handleIn.y} ${anchor.x},${anchor.y} ")
        }
    }

    if (close && parsed.size > 1) {
        val first = parsed.first()
        val last = parsed.last()
        d.append("C ${last.third.x},${last.third.y} ${first.second.x},${first.second.y} ${first.first.x},${first.first.y} Z")
    }

    return d.toString().trim()
}

fun getGroupBoundingBox(elements: List<CanvasElement>): BoundingBox? {
    if (elements.isEmpty()) return null
    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
    elements.forEach { element ->
        val x = element.offset.x
        val y = element.offset.y
        val w = element.width
        val h = element.height
        minX = minOf(minX, x)
        minY = minOf(minY, y)
        maxX = maxOf(maxX, x + w)
        maxY = maxOf(maxY, y + h)
    }
    return BoundingBox(minX, minY, maxX, maxY)
}

private fun colorToHex(color: Color): String {
    val red = (color.red * 255).roundToInt().coerceIn(0, 255)
    val green = (color.green * 255).roundToInt().coerceIn(0, 255)
    val blue = (color.blue * 255).roundToInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", red, green, blue)
}

fun getRandomColor(): Color {
    return Color(
        red = Random.nextFloat(),
        green = Random.nextFloat(),
        blue = Random.nextFloat(),
        alpha = 1f
    )
}

fun getElementDisplayName(
    element: CanvasElement,
    currentIndex: Int,
    allElements: List<CanvasElement>
): String {
    if (!element.customName.isNullOrBlank()) {
        return element.customName
    }

    val category = elementCategory(element)
    val count = allElements.take(currentIndex + 1).count { other ->
        elementCategory(other) == category
    }
    return "$category $count"
}

private fun elementCategory(element: CanvasElement): String {
    val content = element.content.trim()
    return when {
        content == "Shape: Square"            -> "Square"
        content == "Shape: Circle"            -> "Circle"
        content == "Shape: Triangle"          -> "Triangle"
        content == "Shape: Line"              -> "Line"
        content == "Shape: Pentagon"          -> "Pentagon"
        content == "Shape: Hexagon"           -> "Hexagon"
        content == "Shape: Star"              -> "Star"
        content == "Shape: Diamond"           -> "Diamond"
        content == "Shape: Heart"             -> "Heart"
        content == "Shape: ArrowRight"        -> "Arrow"
        content == "Shape: Octagon"           -> "Octagon"
        content == "Shape: Cross"             -> "Cross"
        content == "Shape: ThornCrown"        -> "Thorn Crown"
        content == "Shape: Moon"              -> "Moon"
        content == "Shape: DavidStar"         -> "David Star"
        content == "Shape: Gear"              -> "Gear"
        content.startsWith("Shape:CustomPolygon:") -> "Custom Polygon"
        content.startsWith("Shape:CustomLine:")   -> "Custom Line"
        content.startsWith("Shape:")              -> "Shape"
        content.startsWith("Image:")              -> "Image"
        else                                      -> "Text"
    }
}

fun buildProcessedContent(
    reference: String,
    verses: List<Verse>,
    verseProcessor: VerseTextProcessor,
    themeColors: ThemeColors,
    viewModel: AppViewModel
): String {
    val processedLines = verses.map { verse ->
        val processed = verseProcessor.processVerse(
            verseText = verse.text,
            baseFontSize = 16.sp,
            themeColors = themeColors,
            isOldTestament = viewModel.isOldTestament,
            options = ProcessingOptions(showHeaders = false)
        )
        "${verse.verseNumber} ${processed.body}"
    }
    return "$reference\n\n${processedLines.joinToString("\n")}"
}

fun getSerializedPointsForShape(shapeType: String): String {
    return when (shapeType.lowercase()) {
        "line" -> {
            "0,0.5:0,0.5:0,0.5;1,0.5:1,0.5:1,0.5"
        }
        "square" -> {
            listOf(
                "0,0:0,0:0,0",
                "1,0:1,0:1,0",
                "1,1:1,1:1,1",
                "0,1:0,1:0,1"
            ).joinToString(";")
        }
        "triangle" -> {
            listOf(
                "0.5,0:0.5,0:0.5,0",
                "1,1:1,1:1,1",
                "0,1:0,1:0,1"
            ).joinToString(";")
        }
        "circle" -> {
            val c = 0.5522848f
            val points = listOf(
                BezierNode(Offset(0.5f, 0f), Offset(0.5f - c / 2, 0f), Offset(0.5f + c / 2, 0f)),
                BezierNode(Offset(1f, 0.5f), Offset(1f, 0.5f - c/2), Offset(1f, 0.5f + c/2)),
                BezierNode(Offset(0.5f, 1f), Offset(0.5f + c/2, 1f), Offset(0.5f - c/2, 1f)),
                BezierNode(Offset(0f, 0.5f), Offset(0f, 0.5f + c/2), Offset(0f, 0.5f - c/2))
            )
            points.joinToString(";") {
                "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}"
            }
        }
        "heart" -> {
            listOf(
                BezierNode(Offset(0.5f, 0.32f),
                    handleIn = Offset(0.68f, 0.08f),
                    handleOut = Offset(0.32f, 0.08f)),
                BezierNode(Offset(0.06f, 0.44f),
                    handleIn = Offset(0.06f, 0.16f),
                    handleOut = Offset(0.06f, 0.64f)),
                BezierNode(Offset(0.5f, 0.96f),
                    handleIn = Offset(0.36f, 0.80f),
                    handleOut = Offset(0.64f, 0.80f)),
                BezierNode(Offset(0.94f, 0.44f),
                    handleIn = Offset(0.94f, 0.64f),
                    handleOut = Offset(0.94f, 0.16f))
            ).joinToString(";") {
                "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}"
            }
        }
        "cross" -> {
            listOf(
                BezierNode(Offset(0.38f, 0.02f), handleIn = Offset(0.40f, 0.21f), handleOut = Offset(0.50f, 0f)),
                BezierNode(Offset(0.62f, 0.02f), handleIn = Offset(0.50f, 0f), handleOut = Offset(0.60f, 0.21f)),
                BezierNode(Offset(0.60f, 0.21f), handleIn = Offset(0.60f, 0.21f), handleOut = Offset(0.60f, 0.28f)),
                BezierNode(Offset(0.68f, 0.28f), handleIn = Offset(0.60f, 0.28f), handleOut = Offset(0.68f, 0.28f)),
                BezierNode(Offset(0.98f, 0.24f), handleIn = Offset(0.68f, 0.28f), handleOut = Offset(1f, 0.35f)),
                BezierNode(Offset(0.98f, 0.46f), handleIn = Offset(1f, 0.35f), handleOut = Offset(0.68f, 0.42f)),
                BezierNode(Offset(0.68f, 0.42f), handleIn = Offset(0.68f, 0.42f), handleOut = Offset(0.60f, 0.42f)),
                BezierNode(Offset(0.60f, 0.52f), handleIn = Offset(0.60f, 0.42f), handleOut = Offset(0.62f, 0.98f)),
                BezierNode(Offset(0.62f, 0.98f), handleIn = Offset(0.62f, 0.98f), handleOut = Offset(0.50f, 1f)),
                BezierNode(Offset(0.38f, 0.98f), handleIn = Offset(0.50f, 1f), handleOut = Offset(0.40f, 0.52f)),
                BezierNode(Offset(0.40f, 0.52f), handleIn = Offset(0.40f, 0.52f), handleOut = Offset(0.40f, 0.42f)),
                BezierNode(Offset(0.32f, 0.42f), handleIn = Offset(0.40f, 0.42f), handleOut = Offset(0.02f, 0.46f)),
                BezierNode(Offset(0.02f, 0.46f), handleIn = Offset(0.02f, 0.46f), handleOut = Offset(0f, 0.35f)),
                BezierNode(Offset(0.02f, 0.24f), handleIn = Offset(0f, 0.35f), handleOut = Offset(0.32f, 0.28f)),
                BezierNode(Offset(0.32f, 0.28f), handleIn = Offset(0.32f, 0.28f), handleOut = Offset(0.40f, 0.28f)),
                BezierNode(Offset(0.40f, 0.21f), handleIn = Offset(0.40f, 0.28f), handleOut = Offset(0.40f, 0.21f))
            ).joinToString(";") {
                "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}"
            }
        }
        "diamond" -> {
            listOf(
                BezierNode(Offset(0.5f, 0f), handleIn = Offset(0.32f, 0.32f), handleOut = Offset(0.68f, 0.32f)),
                BezierNode(Offset(1f, 0.5f), handleIn = Offset(0.68f, 0.32f), handleOut = Offset(0.68f, 0.68f)),
                BezierNode(Offset(0.5f, 1f), handleIn = Offset(0.68f, 0.68f), handleOut = Offset(0.32f, 0.68f)),
                BezierNode(Offset(0f, 0.5f), handleIn = Offset(0.32f, 0.68f), handleOut = Offset(0.32f, 0.32f))
            ).joinToString(";") {
                "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}"
            }
        }
        "star" -> {
            val outerR = 1f
            val innerR = 0.4f
            val center = 0.5f
            val angleOffset = -Math.PI / 2
            (0 until 10).map { i ->
                val radius = if (i % 2 == 0) outerR/2 else innerR/2
                val angle = angleOffset + Math.PI * i / 5
                val x = (center + radius * cos(angle)).toFloat()
                val y = (center + radius * sin(angle)).toFloat()
                BezierNode(Offset(x, y), Offset(x, y), Offset(x, y))
            }.joinToString(";") {
                "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}"
            }
        }
        "pentagon" -> {
            (0 until 5).map { i ->
                val angle = -Math.PI / 2 + 2 * Math.PI * i / 5
                val x = (0.5f + 0.5f * cos(angle)).toFloat()
                val y = (0.5f + 0.5f * sin(angle)).toFloat()
                BezierNode(Offset(x, y), Offset(x, y), Offset(x, y))
            }.joinToString(";") {
                "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}"
            }
        }
        "hexagon" -> {
            (0 until 6).map { i ->
                val angle = -Math.PI / 2 + 2 * Math.PI * i / 6
                val x = (0.5f + 0.5f * cos(angle)).toFloat()
                val y = (0.5f + 0.5f * sin(angle)).toFloat()
                BezierNode(Offset(x, y), Offset(x, y), Offset(x, y))
            }.joinToString(";") {
                "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}"
            }
        }
        "octagon" -> {
            (0 until 8).map { i ->
                val angle = -Math.PI / 2 + 2 * Math.PI * i / 8
                val x = (0.5f + 0.5f * cos(angle)).toFloat()
                val y = (0.5f + 0.5f * sin(angle)).toFloat()
                BezierNode(Offset(x, y), Offset(x, y), Offset(x, y))
            }.joinToString(";") {
                "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}"
            }
        }
        "arrowright" -> {
            listOf(
                Offset(0f, 0.3f), Offset(0.6f, 0.3f), Offset(0.6f, 0f),
                Offset(1f, 0.5f), Offset(0.6f, 1f), Offset(0.6f, 0.7f), Offset(0f, 0.7f)
            ).map { BezierNode(it, it, it) }.joinToString(";") {
                "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}"
            }
        }
        "davidstar" -> {
            val cx = 0.5f
            val cy = 0.5f
            val rOut = 0.5f
            val rIn = rOut / sqrt(3f)
            val curveFactor = 0.85f
            val vertices = (0 until 12).map { i ->
                val angleRad = (-90f + (i * 30f)) * Math.PI.toFloat() / 180f
                val radius = if (i % 2 == 0) rOut else rIn
                Offset(cx + radius * cos(angleRad), cy + radius * sin(angleRad))
            }

            val controls = vertices.mapIndexed { i, start ->
                val end = vertices[(i + 1) % 12]
                val midX = (start.x + end.x) / 2f
                val midY = (start.y + end.y) / 2f
                Offset(
                    x = cx + (midX - cx) * curveFactor,
                    y = cy + (midY - cy) * curveFactor
                )
            }

            vertices.mapIndexed { i, anchor ->
                val prevIdx = (i - 1 + 12) % 12
                val cPrev = controls[prevIdx]
                val cNext = controls[i]
                val handleInX = anchor.x + (cPrev.x - anchor.x) * (2f / 3f)
                val handleInY = anchor.y + (cPrev.y - anchor.y) * (2f / 3f)

                val handleOutX = anchor.x + (cNext.x - anchor.x) * (2f / 3f)
                val handleOutY = anchor.y + (cNext.y - anchor.y) * (2f / 3f)

                BezierNode(
                    anchor = anchor,
                    handleIn = Offset(handleInX, handleInY),
                    handleOut = Offset(handleOutX, handleOutY)
                )
            }.joinToString(";") {
                "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}"
            }
        }
        "moon" -> {
            // Magic number for a standard circle bezier approximation
            val c = 0.27614f

            listOf(
                BezierNode(
                    anchor = Offset(0.5f, 0f),
                    handleIn = Offset(0.4f, 0.15f),
                    handleOut = Offset(0.5f - c, 0f)
                ),
                BezierNode(
                    anchor = Offset(0f, 0.5f),
                    handleIn = Offset(0f, 0.5f - c),
                    handleOut = Offset(0f, 0.5f + c)
                ),
                BezierNode(
                    anchor = Offset(0.5f, 1f),
                    handleIn = Offset(0.5f - c, 1f),
                    handleOut = Offset(0.4f, 0.85f)
                ),
                BezierNode(
                    anchor = Offset(0.35f, 0.5f),
                    handleIn = Offset(0.35f, 0.75f),
                    handleOut = Offset(0.35f, 0.25f)
                )
            ).joinToString(";") {
                "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}"
            }
        }
        "gear" -> {
            val cx = 0.5f
            val cy = 0.5f
            val rOut = 0.5f
            val rIn = 0.35f
            val teeth = 8

            val nodes = mutableListOf<BezierNode>()
            val step = 2.0 * Math.PI / teeth
            val offsetAngle = -Math.PI / 2
            for (i in 0 until teeth) {
                val a1 = offsetAngle + step * (i + 0.1)
                val a2 = offsetAngle + step * (i + 0.3)
                val a3 = offsetAngle + step * (i + 0.7)
                val a4 = offsetAngle + step * (i + 0.9)

                val p1 = Offset((cx + rIn * cos(a1)).toFloat(), (cy + rIn * sin(a1)).toFloat())
                val p2 = Offset((cx + rOut * cos(a2)).toFloat(), (cy + rOut * sin(a2)).toFloat())
                val p3 = Offset((cx + rOut * cos(a3)).toFloat(), (cy + rOut * sin(a3)).toFloat())
                val p4 = Offset((cx + rIn * cos(a4)).toFloat(), (cy + rIn * sin(a4)).toFloat())

                nodes.add(BezierNode(p1, p1, p1))
                nodes.add(BezierNode(p2, p2, p2))
                nodes.add(BezierNode(p3, p3, p3))
                nodes.add(BezierNode(p4, p4, p4))
            }

            val firstPerimeterPoint = nodes.first().anchor
            val lastPerimeterPoint = nodes.last().anchor
            val midPerimeterPoint = Offset(
                x = (firstPerimeterPoint.x + lastPerimeterPoint.x) / 2f,
                y = (firstPerimeterPoint.y + lastPerimeterPoint.y) / 2f
            )

            nodes.add(BezierNode(midPerimeterPoint, midPerimeterPoint, midPerimeterPoint))

            val rHole = 0.15f
            val c = 0.5522848f * rHole
            val hTop = Offset(0.5f, 0.5f - rHole)
            val hLeft = Offset(0.5f - rHole, 0.5f)
            val hBottom = Offset(0.5f, 0.5f + rHole)
            val hRight = Offset(0.5f + rHole, 0.5f)

            nodes.add(BezierNode(anchor = hTop, handleIn = hTop, handleOut = Offset(0.5f - c, 0.5f - rHole)))
            nodes.add(BezierNode(anchor = hLeft, handleIn = Offset(0.5f - rHole, 0.5f - c), handleOut = Offset(0.5f - rHole, 0.5f + c)))
            nodes.add(BezierNode(anchor = hBottom, handleIn = Offset(0.5f - c, 0.5f + rHole), handleOut = Offset(0.5f + c, 0.5f + rHole)))
            nodes.add(BezierNode(anchor = hRight, handleIn = Offset(0.5f + rHole, 0.5f + c), handleOut = Offset(0.5f + rHole, 0.5f - c)))
            nodes.add(BezierNode(anchor = hTop, handleIn = Offset(0.5f + c, 0.5f - rHole), handleOut = hTop))
            nodes.add(BezierNode(anchor = midPerimeterPoint, handleIn = midPerimeterPoint, handleOut = midPerimeterPoint))
            nodes.joinToString(";") {
                "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}"
            }
        }
        "thorncrown" -> {
            val random = java.util.Random(42L)
            val nodes = mutableListOf<BezierNode>()

            val cx = 0.5f
            val cy = 0.5f
            val baseRadius = 0.35f
            val numVines = 3
            val steps = 60

            repeat(numVines) {
                val twists = 4 + random.nextInt(5)
                val phase = random.nextFloat() * 2f * PI.toFloat()
                val amplitude = random.nextFloat() * 0.03f + 0.015f

                for (i in 0 until steps) {
                    val angle = (i.toFloat() / steps) * 2f * PI.toFloat()
                    val wave = sin(twists * angle + phase)
                    val noise = (random.nextFloat() - 0.5f) * 0.012f
                    val r = baseRadius + (wave * amplitude) + noise

                    val x = cx + r * cos(angle)
                    val y = cy + r * sin(angle)

                    val pt = Offset(x, y)
                    nodes.add(BezierNode(pt, pt, pt))
                }
            }

            val numThorns = 45

            repeat(numThorns) {
                val angle = random.nextFloat() * 2f * PI.toFloat()
                val rBase = baseRadius + (random.nextFloat() - 0.5f) * 0.05f

                val isOutward = random.nextFloat() > 0.15f
                val direction = if (isOutward) 1f else -1f
                val length = 0.03f + random.nextFloat() * 0.06f

                val twistOffset = (random.nextFloat() * 0.5f) - 0.25f
                val tipAngle = angle + twistOffset

                val tipX = cx + (rBase + (length * direction)) * cos(tipAngle)
                val tipY = cy + (rBase + (length * direction)) * sin(tipAngle)
                val tip = Offset(tipX, tipY)

                val baseWidthAngle = 0.02f + random.nextFloat() * 0.02f
                val p1 = Offset(cx + rBase * cos(angle - baseWidthAngle), cy + rBase * sin(angle - baseWidthAngle))
                val p2 = Offset(cx + rBase * cos(angle + baseWidthAngle), cy + rBase * sin(angle + baseWidthAngle))
                val curveIntensity = 0.03f
                val curveDirX = (random.nextFloat() - 0.5f) * curveIntensity
                val curveDirY = (random.nextFloat() - 0.5f) * curveIntensity
                val qControl1X = (p1.x + tip.x) / 2f + curveDirX
                val qControl1Y = (p1.y + tip.y) / 2f + curveDirY
                val p1HandleOut = Offset(p1.x + (qControl1X - p1.x) * (2f / 3f), p1.y + (qControl1Y - p1.y) * (2f / 3f))
                val tipHandleIn = Offset(tip.x + (qControl1X - tip.x) * (2f / 3f), tip.y + (qControl1Y - tip.y) * (2f / 3f))
                val qControl2X = (p2.x + tip.x) / 2f + curveDirX
                val qControl2Y = (p2.y + tip.y) / 2f + curveDirY
                val tipHandleOut = Offset(tip.x + (qControl2X - tip.x) * (2f / 3f), tip.y + (qControl2Y - tip.y) * (2f / 3f))
                val p2HandleIn = Offset(p2.x + (qControl2X - p2.x) * (2f / 3f), p2.y + (qControl2Y - p2.y) * (2f / 3f))
                nodes.add(BezierNode(p1, p1, p1HandleOut))
                nodes.add(BezierNode(tip, tipHandleIn, tipHandleOut))
                nodes.add(BezierNode(p2, p2HandleIn, p2))
            }

            nodes.joinToString(";") {
                "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}"
            }
        }
        else -> ""
    }
}

fun generateThornCrownPaths(seed: Long, size: Size): CrownStructure {
    val random = java.util.Random(seed)
    val vinePath = Path()
    val thornsPath = Path()

    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val minDimension = min(size.width, size.height)
    val scaleFactor = minDimension / 938f

    val baseRadius = 350f * scaleFactor
    val numVines = 8
    val steps = 90

    repeat(numVines) {
        val twists = 4 + random.nextInt(5)
        val phase = random.nextFloat() * 2f * PI.toFloat()
        val amplitude = (random.nextFloat() * 30f + 15f) * scaleFactor

        for (i in 0..steps) {
            val angle = (i.toFloat() / steps) * 2f * PI.toFloat()
            val wave = sin(twists * angle + phase)
            val noise = (random.nextFloat() - 0.5f) * 12f * scaleFactor
            val r = baseRadius + (wave * amplitude) + noise

            val x = centerX + r * cos(angle)
            val y = centerY + r * sin(angle)

            if (i == 0) {
                vinePath.moveTo(x, y)
            } else {
                vinePath.lineTo(x, y)
            }
        }
        vinePath.close()
    }

    val numThorns = 120 + random.nextInt(40)

    repeat(numThorns) {
        val angle = random.nextFloat() * 2f * PI.toFloat()
        val rBase = baseRadius + (random.nextFloat() - 0.5f) * 50f * scaleFactor

        val isOutward = random.nextFloat() > 0.15f
        val direction = if (isOutward) 1f else -1f
        val length = (30f + random.nextFloat() * 60f) * scaleFactor

        val twistOffset = (random.nextFloat() * 0.5f) - 0.25f
        val tipAngle = angle + twistOffset

        val tipX = centerX + (rBase + (length * direction)) * cos(tipAngle)
        val tipY = centerY + (rBase + (length * direction)) * sin(tipAngle)

        val baseWidthAngle = 0.02f + random.nextFloat() * 0.02f
        val p1X = centerX + rBase * cos(angle - baseWidthAngle)
        val p1Y = centerY + rBase * sin(angle - baseWidthAngle)

        val p2X = centerX + rBase * cos(angle + baseWidthAngle)
        val p2Y = centerY + rBase * sin(angle + baseWidthAngle)

        thornsPath.moveTo(p1X, p1Y)

        val curveIntensity = 30f * scaleFactor
        val curveDirX = (random.nextFloat() - 0.5f) * curveIntensity
        val curveDirY = (random.nextFloat() - 0.5f) * curveIntensity

        val controlTipX = (p1X + tipX) / 2f + curveDirX
        val controlTipY = (p1Y + tipY) / 2f + curveDirY
        thornsPath.quadraticTo(controlTipX, controlTipY, tipX, tipY)

        val controlBaseX = (p2X + tipX) / 2f + curveDirX
        val controlBaseY = (p2Y + tipY) / 2f + curveDirY
        thornsPath.quadraticTo(controlBaseX, controlBaseY, p2X, p2Y)

        thornsPath.close()
    }

    return CrownStructure(vinePath, thornsPath)
}