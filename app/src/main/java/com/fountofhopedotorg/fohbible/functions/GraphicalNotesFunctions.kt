package com.fountofhopedotorg.fohbible.functions

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns.DISPLAY_NAME
import android.provider.MediaStore.MediaColumns.MIME_TYPE
import android.provider.MediaStore.MediaColumns.RELATIVE_PATH
import android.util.Base64
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.sp
import com.fountofhopedotorg.fohbible.data.CanvasNote
import com.fountofhopedotorg.fohbible.data.ProcessingOptions
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.random.Random

fun buildPassageText(
    reference: String,
    verses: List<Verse>,
    processor: VerseTextProcessor,
    themeColors: ThemeColors,
    viewModel: AppViewModel
): String {
    if (verses.isEmpty()) return ""
    val sb = StringBuilder().append(reference).append("\n\n")
    verses.forEach { verse ->
        sb.append("${verse.verseNumber} ")
        val processed = processor.processVerse(
            verseText = verse.text,
            baseFontSize = 16.sp,
            themeColors = themeColors,
            isOldTestament = viewModel.isOldTestament,
            options = ProcessingOptions(showHeaders = false)
        )
        sb.append(processed.body).append("\n")
    }
    return sb.toString().trim()
}

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
            put(DISPLAY_NAME, fileName)
            put(MIME_TYPE, if (format == "JPG") "image/jpeg" else "image/png")
            put(RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FOHBible")
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
                makeText(context, "Saved to Pictures/FOHBible", LENGTH_LONG).show()
            }
        } else {
            throw Exception("Failed to create MediaStore entry.")
        }
    } catch (_: Exception) {
        withContext(Dispatchers.Main) {
            makeText(context, "Failed to save image", LENGTH_SHORT).show()
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
            put(DISPLAY_NAME, fileName)
            put(MIME_TYPE, "application/pdf")
            put(RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FOHBible")
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            withContext(Dispatchers.IO) {
                resolver.openOutputStream(uri).use { pdfDocument.writeTo(it) }
            }
            withContext(Dispatchers.Main) {
                makeText(context, "PDF saved to Downloads/FOHBible", LENGTH_LONG).show()
            }
        } else {
            throw Exception("Failed to create MediaStore entry.")
        }

        pdfDocument.close()
    } catch (_: Exception) {
        withContext(Dispatchers.Main) {
            makeText(context, "Failed to save PDF", LENGTH_SHORT).show()
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
        val width = bitmap.width
        val height = bitmap.height

        val allShapes = canvasNotes != null && canvasNotes.all { it.content.startsWith("Shape:") }

        val svgContent = if (allShapes) {
            buildVectorSvg(width, height, canvasNotes)
        } else {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $width $height" width="$width" height="$height">
                    <image width="$width" height="$height" href="data:image/png;base64,$base64String" />
                </svg>
            """.trimIndent()
        }

        val fileName = "foh_canvas_${System.currentTimeMillis()}.svg"
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(DISPLAY_NAME, fileName)
            put(MIME_TYPE, "image/svg+xml")
            put(RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FOHBible")
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            withContext(Dispatchers.IO) {
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(svgContent.toByteArray(Charsets.UTF_8))
                }
            }
            withContext(Dispatchers.Main) {
                makeText(context, "SVG saved to Downloads/FOHBible", LENGTH_LONG).show()
            }
        } else {
            throw Exception("Failed to create MediaStore entry.")
        }
    } catch (_: Exception) {
        withContext(Dispatchers.Main) {
            makeText(context, "Failed to save SVG", LENGTH_SHORT).show()
        }
    }
}

private fun buildVectorSvg(canvasWidth: Int, canvasHeight: Int, notes: List<CanvasNote>): String {
    val sb = StringBuilder()
    sb.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $canvasWidth $canvasHeight" width="$canvasWidth" height="$canvasHeight">""")
    sb.append("\n  <rect width=\"100%\" height=\"100%\" fill=\"white\" />\n")
    val g = StringBuilder("  <g>\n")

    for (note in notes) {
        if (!note.isVisible) continue
        val x = note.offset.x
        val y = note.offset.y
        val w = note.width
        val h = note.height
        val color = colorToHex(note.backgroundColor)
        val rotation = note.rotation

        val transform = buildString {
            append("translate($x, $y)")
            if (rotation != 0f) {
                append(" rotate($rotation, ${w / 2}, ${h / 2})")
            }
        }

        val shapeElement = buildShapeSvg(note, w, h, color)
        g.append("""    <g transform="$transform">""")
            .append(shapeElement)
            .append("</g>\n")
    }
    g.append("  </g>\n")
    sb.append(g)
    sb.append("</svg>")
    return sb.toString()
}

private fun buildShapeSvg(note: CanvasNote, w: Float, h: Float, hexColor: String): String {
    val content = note.content.trim()
    return when {
        content.startsWith("Shape: Square") -> {
            """<rect x="0" y="0" width="$w" height="$h" fill="$hexColor" />"""
        }
        content.startsWith("Shape: Circle") -> {
            """<ellipse cx="${w / 2}" cy="${h / 2}" rx="${w / 2}" ry="${h / 2}" fill="$hexColor" />"""
        }
        content.startsWith("Shape: Triangle") -> {
            val points = "${w / 2},0  $w,$h  0,$h"
            """<polygon points="$points" fill="$hexColor" />"""
        }
        content.startsWith("Shape: Pentagon") -> {
            val raw = getSerializedPointsForShape("Pentagon")
            val pts = raw.split(";").map { it.split(":")[0] }
            val pointsStr = pts.joinToString(" ") { pt ->
                val (px, py) = pt.split(",").map { it.toFloat() }
                "${px * w},${py * h}"
            }
            """<polygon points="$pointsStr" fill="$hexColor" />"""
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
                """<path d="$d" fill="none" stroke="$hexColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />"""
            } else ""
        }
        content.startsWith("Shape:CustomPolygon:") -> {
            val serialized = content.removePrefix("Shape:CustomPolygon:")
            val segments = serialized.split(";").filter { it.isNotEmpty() }
            if (segments.size >= 2) {
                val d = buildPathD(segments, w, h, close = true)
                """<path d="$d" fill="$hexColor" stroke="$hexColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round" />"""
            } else ""
        }
        else -> ""
    }
}

private fun buildPathD(segments: List<String>, w: Float, h: Float, close: Boolean): String {
    if (segments.isEmpty()) return ""
    val d = StringBuilder()

    for (i in segments.indices) {
        val segment = segments[i]
        val parts = segment.split(":")
        val mainCoords = parts[0].split(",")
        if (mainCoords.size < 2) continue

        val px = (mainCoords[0].toFloatOrNull() ?: 0f) * w
        val py = (mainCoords[1].toFloatOrNull() ?: 0f) * h

        if (i == 0) {
            d.append("M $px $py ")
        } else {
            val handles = if (parts.size > 1) parts[1].split(",") else emptyList()

            when (handles.size) {
                4 -> {
                    val cp1x = (handles[0].toFloatOrNull() ?: 0f) * w
                    val cp1y = (handles[1].toFloatOrNull() ?: 0f) * h
                    val cp2x = (handles[2].toFloatOrNull() ?: 0f) * w
                    val cp2y = (handles[3].toFloatOrNull() ?: 0f) * h
                    d.append("C $cp1x $cp1y, $cp2x $cp2y, $px $py ")
                }
                2 -> {
                    val cpx = (handles[0].toFloatOrNull() ?: 0f) * w
                    val cpy = (handles[1].toFloatOrNull() ?: 0f) * h
                    d.append("Q $cpx $cpy, $px $py ")
                }
                else -> {
                    d.append("L $px $py ")
                }
            }
        }
    }
    if (close) d.append("Z")
    return d.toString().trim()
}

private fun colorToHex(color: Color): String {
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
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