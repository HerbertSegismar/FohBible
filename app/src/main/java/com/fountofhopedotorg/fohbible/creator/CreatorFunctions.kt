package com.fountofhopedotorg.fohbible.creator

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.sp
import com.fountofhopedotorg.fohbible.data.BezierNode
import com.fountofhopedotorg.fohbible.data.BoundingBox
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.CrownStructure
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
    graphicsLayer: GraphicsLayer,
    context: Context,
    format: String = "JPG"
) {
    try {
        val bitmap: ImageBitmap = graphicsLayer.toImageBitmap()
        val androidBitmap = bitmap.asAndroidBitmap()

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
                        if (format == "JPG") androidBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        else androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved to Pictures/FOHBible", Toast.LENGTH_LONG).show()
            }
        } else {
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
    graphicsLayer: GraphicsLayer,
    context: Context
) {
    try {
        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
        val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(softwareBitmap.width, softwareBitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        page.canvas.drawBitmap(softwareBitmap, 0f, 0f, null)
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
    } catch (_: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
suspend fun saveCanvasAsSVG(
    graphicsLayer: GraphicsLayer,
    context: Context,
    canvasNotes: List<CanvasNote>? = null
) {
    try {
        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height
        val density = context.resources.displayMetrics.density
        val canvasDpWidth = bitmapWidth / density
        val canvasDpHeight = bitmapHeight / density
        val scaleDownFactor = 1f / density

        val allShapes = canvasNotes != null && canvasNotes.all { it.content.startsWith("Shape:") }

        val svgContent = if (allShapes) {
            buildVectorSvg(canvasNotes, canvasDpWidth, canvasDpHeight)
        } else {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $canvasDpWidth $canvasDpHeight" width="$canvasDpWidth" height="$canvasDpHeight">
                    <g transform="scale($scaleDownFactor)">
                        <image width="$bitmapWidth" height="$bitmapHeight" href="data:image/png;base64,$base64String" />
                    </g>
                </svg>
            """.trimIndent()
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

private fun buildVectorSvg(
    notes: List<CanvasNote>,
    canvasWidth: Float,
    canvasHeight: Float
): String {
    val sb = StringBuilder()
    sb.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $canvasWidth $canvasHeight" width="$canvasWidth" height="$canvasHeight">""")
    sb.append("\n  <rect width=\"100%\" height=\"100%\" fill=\"white\" />\n")

    for (note in notes) {
        if (!note.isVisible) continue
        val x = note.offset.x
        val y = note.offset.y
        val w = note.width * note.scaleX
        val h = note.height * note.scaleY
        val color = colorToHex(note.backgroundColor)
        val rot = note.rotation

        val cx = w / 2f
        val cy = h / 2f
        val transform = buildString {
            append("translate($x, $y)")
            if (rot != 0f) append(" rotate($rot, $cx, $cy)")
        }

        val shape = buildShapeSvg(note, w, h, color)
        if (shape.isNotEmpty()) {
            sb.append("  <g transform=\"$transform\">$shape</g>\n")
        }
    }
    sb.append("</svg>")
    return sb.toString()
}

private fun buildShapeSvg(note: CanvasNote, w: Float, h: Float, hexColor: String): String {
    val content = note.content.trim()
    val alpha = note.backgroundColor.alpha

    return when {
        content.startsWith("Shape: Square") -> {
            """<rect x="0" y="0" width="$w" height="$h" fill="$hexColor" fill-opacity="$alpha" />"""
        }
        content.startsWith("Shape: Circle") -> {
            """<ellipse cx="${w / 2}" cy="${h / 2}" rx="${w / 2}" ry="${h / 2}" fill="$hexColor" fill-opacity="$alpha" />"""
        }
        content.startsWith("Shape: Triangle") -> {
            val points = "${w / 2},0 $w,$h 0,$h"
            """<polygon points="$points" fill="$hexColor" fill-opacity="$alpha" />"""
        }
        content.startsWith("Shape: Pentagon") -> {
            val pts = listOf(
                0.5f to 0f,
                1f to 0.4f,
                0.8f to 1f,
                0.2f to 1f,
                0f to 0.4f
            )
            val pointsStr = pts.joinToString(" ") { (px, py) ->
                "${px * w},${py * h}"
            }
            """<polygon points="$pointsStr" fill="$hexColor" fill-opacity="$alpha" />"""
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
                """<path d="$d" fill="none" stroke="$hexColor" stroke-opacity="$alpha" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />"""
            } else ""
        }
        content.startsWith("Shape:CustomPolygon:") -> {
            val serialized = content.removePrefix("Shape:CustomPolygon:")
            val segments = serialized.split(";").filter { it.isNotEmpty() }
            if (segments.size >= 2) {
                val d = buildPathD(segments, w, h, close = true)
                """<path d="$d" fill="$hexColor" fill-opacity="$alpha" stroke="$hexColor" stroke-opacity="$alpha" stroke-width="1" stroke-linecap="round" stroke-linejoin="round" />"""
            } else ""
        }
        else -> ""
    }
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

fun getGroupBoundingBox(notes: List<CanvasNote>): BoundingBox? {
    if (notes.isEmpty()) return null
    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
    notes.forEach { note ->
        val x = note.offset.x
        val y = note.offset.y
        val w = note.width
        val h = note.height
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
    note: CanvasNote,
    currentIndex: Int,
    allNotes: List<CanvasNote>
): String {
    if (!note.customName.isNullOrBlank()) {
        return note.customName
    }

    val category = noteCategory(note)
    val count = allNotes.take(currentIndex + 1).count { other ->
        noteCategory(other) == category
    }
    return "$category $count"
}

private fun noteCategory(note: CanvasNote): String {
    val content = note.content.trim()
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
            val rIn = rOut / kotlin.math.sqrt(3f)
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
    val scaleFactor = minDimension / 938f   // ← changed from 1000f

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