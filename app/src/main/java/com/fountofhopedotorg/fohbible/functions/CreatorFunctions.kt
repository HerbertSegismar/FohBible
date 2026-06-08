package com.fountofhopedotorg.fohbible.functions

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.sp
import com.fountofhopedotorg.fohbible.data.BoundingBox
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.ProcessingOptions
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
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
    canvasWidth: Float,   // dp
    canvasHeight: Float   // dp
): String {
    val sb = StringBuilder()
    sb.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $canvasWidth $canvasHeight" width="$canvasWidth" height="$canvasHeight">""")
    sb.append("\n  <rect width=\"100%\" height=\"100%\" fill=\"white\" />\n")

    for (note in notes) {
        if (!note.isVisible) continue
        val x = note.offset.x            // already dp
        val y = note.offset.y
        val w = note.width * note.scaleX   // visual width (dp)
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

fun getSerializedPointsForShape(shapeName: String): String {
    return when (shapeName) {
        "Square" -> "0.0,0.0:0.0,0.0:0.0,0.0;1.0,0.0:1.0,0.0:1.0,0.0;1.0,1.0:1.0,1.0:1.0,1.0;0.0,1.0:0.0,1.0:0.0,1.0"
        "Triangle" -> "0.5,0.0:0.5,0.0:0.5,0.0;1.0,1.0:1.0,1.0:1.0,1.0;0.0,1.0:0.0,1.0:0.0,1.0"
        "Pentagon" -> "0.5,0.0:0.5,0.0:0.5,0.0;1.0,0.4:1.0,0.4:1.0,0.4;0.8,0.9:0.8,0.9:0.8,0.9;0.2,0.9:0.2,0.9:0.2,0.9;0.0,0.4:0.0,0.4:0.0,0.4"
        "Circle" -> "0.5,0.0:0.224,0.0:0.776,0.0;1.0,0.5:1.0,0.224:1.0,0.776;0.5,1.0:0.776,1.0:0.224,1.0;0.0,0.5:0.0,0.776:0.0,0.224"
        "Line" -> "0.1,0.5:0.1,0.5:0.1,0.5;0.9,0.5:0.9,0.5:0.9,0.5"
        else -> ""
    }
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

    val category = when {
        note.content.startsWith("Image:") -> "Image"
        note.content.startsWith("Shape: Square") -> "Square"
        note.content.startsWith("Shape: Circle") -> "Circle"
        note.content.startsWith("Shape: Triangle") -> "Triangle"
        note.content.startsWith("Shape: Pentagon") -> "Pentagon"
        note.content.startsWith("Shape: Line") -> "Line"
        note.content.startsWith("Shape:CustomPolygon:") -> "Custom Polygon"
        note.content.startsWith("Shape:CustomLine:") -> "Custom Line"
        note.content.startsWith("Shape:") -> "Shape"
        else -> "Text"
    }

    val count = allNotes.take(currentIndex + 1).count { other ->
        val otherCategory = when {
            other.content.startsWith("Image:") -> "Image"
            other.content.startsWith("Shape: Square") -> "Square"
            other.content.startsWith("Shape: Circle") -> "Circle"
            other.content.startsWith("Shape: Triangle") -> "Triangle"
            other.content.startsWith("Shape: Pentagon") -> "Pentagon"
            other.content.startsWith("Shape: Line") -> "Line"
            other.content.startsWith("Shape:CustomPolygon:") -> "Custom Polygon"
            other.content.startsWith("Shape:CustomLine:") -> "Custom Line"
            other.content.startsWith("Shape:") -> "Shape"
            else -> "Text"
        }
        otherCategory == category
    }

    return "$category $count"
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