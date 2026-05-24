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
    context: Context
) {
    try {
        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()

        // Convert the bitmap into a Base64 string to embed into the SVG
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

        val width = bitmap.width
        val height = bitmap.height

        // Wrap the Base64 image stream in standard SVG tags
        val svgContent = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $width $height" width="$width" height="$height">
                <image width="$width" height="$height" href="data:image/png;base64,$base64String" />
            </svg>
        """.trimIndent()

        val fileName = "foh_canvas_${System.currentTimeMillis()}.svg"
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(DISPLAY_NAME, fileName)
            put(MIME_TYPE, "image/svg+xml")
            // SVG is typically treated as a document/download format rather than a MediaStore Image
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

fun getSerializedPointsForShape(shapeName: String): String {
    return when (shapeName) {
        "Square" -> "0.0,0.0:0.0,0.0:0.0,0.0;1.0,0.0:1.0,0.0:1.0,0.0;1.0,1.0:1.0,1.0:1.0,1.0;0.0,1.0:0.0,1.0:0.0,1.0"
        "Triangle" -> "0.5,0.0:0.5,0.0:0.5,0.0;1.0,1.0:1.0,1.0:1.0,1.0;0.0,1.0:0.0,1.0:0.0,1.0"
        "Pentagon" -> "0.5,0.0:0.5,0.0:0.5,0.0;1.0,0.4:1.0,0.4:1.0,0.4;0.8,0.9:0.8,0.9:0.8,0.9;0.2,0.9:0.2,0.9:0.2,0.9;0.0,0.4:0.0,0.4:0.0,0.4"
        "Circle" -> "0.5,0.0:0.224,0.0:0.776,0.0;1.0,0.5:1.0,0.224:1.0,0.776;0.5,1.0:0.776,1.0:0.224,1.0;0.0,0.5:0.0,0.776:0.0,0.224"
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