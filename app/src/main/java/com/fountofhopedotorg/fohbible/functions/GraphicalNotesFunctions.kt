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
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.annotation.RequiresApi
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
import kotlin.collections.forEach

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
            makeText(context, "Saved to Pictures/FOHBible", LENGTH_LONG).show()
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
            makeText(context, "PDF saved to Downloads/FOHBible", LENGTH_LONG).show()
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