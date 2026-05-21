package com.fountofhopedotorg.fohbible.graphicals

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.composables.ColorWheelDialog
import com.fountofhopedotorg.fohbible.data.*
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicalNotesScreen() {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    val dbHelper = remember(viewModel.currentDbName) {
        DatabaseHelper(context, viewModel.currentDbName)
    }

    DisposableEffect(dbHelper) {
        onDispose { dbHelper.close() }
    }

    val verseProcessor = remember { VerseTextProcessor() }
    val theme = LocalAppTheme.current
    val isDark = theme.darkTheme

    val themeColors = ThemeColors(
        textColor = if (isDark) Color.White else Color.Black,
        verseNumber = theme.primaryColor,
        primary = theme.primaryColor,
        tagColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
        tagBg = if (isDark) Color(0xFF1E293B) else Color.White,
        wordsOfJesus = viewModel.wordsOfJesus,
        searchHighlightBg = theme.primaryColor.copy(alpha = 0.2f),
        highlightIcon = theme.primaryColor
    )
    var currentText by remember { mutableStateOf("") }
    var referenceInput by remember { mutableStateOf("") }
    var fetchedVerses by remember { mutableStateOf<List<Verse>>(emptyList()) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var currentReference by remember { mutableStateOf("") }
    var showColorPicker by remember { mutableStateOf(false) }
    var noteToColorEdit by remember { mutableStateOf<CanvasNote?>(null) }

    val mainScrollState = rememberScrollState()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(mainScrollState)
    ) {
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().background(themeColors.primary.copy(alpha = 0.1f))) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Fetch Bible Verse", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = referenceInput,
                        onValueChange = { referenceInput = it },
                        label = { Text("Reference (e.g., John 3:16)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = fetchError != null
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        fetchError = null
                        when (val result = fetchByReference(referenceInput, dbHelper)) {
                            is ReferenceResult.Single -> {
                                fetchedVerses = listOf(result.verse)
                                currentReference = buildReferenceString(result.bookName, result.verse.chapter, result.verse.verseNumber, null)
                            }
                            is ReferenceResult.Range -> {
                                fetchedVerses = result.verses
                                val first = result.verses.first()
                                val last = result.verses.last()
                                currentReference = buildReferenceString(result.bookName, first.chapter, first.verseNumber, last.verseNumber)
                            }
                            is ReferenceResult.Chapter -> {
                                fetchedVerses = result.verses
                                val first = result.verses.first()
                                currentReference = buildReferenceString(result.bookName, first.chapter, null, null)
                            }
                            ReferenceResult.Invalid -> {
                                fetchedVerses = emptyList()
                                currentReference = ""
                                fetchError = "Invalid reference or verse not found"
                            }
                        }
                    }) { Text("Fetch") }
                }

                if (fetchError != null) {
                    Text(fetchError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                }

                if (fetchedVerses.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Fetched Verses:", style = MaterialTheme.typography.titleSmall)

                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth().background(themeColors.primary.copy(alpha = 0.15f)).padding(12.dp)) {
                                Text(currentReference, style = MaterialTheme.typography.titleMedium, color = themeColors.primary, fontWeight = FontWeight.Bold)
                            }

                            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState()).padding(12.dp)) {
                                fetchedVerses.forEach { verse ->
                                    val processed = verseProcessor.processVerse(
                                        verseText = verse.text,
                                        baseFontSize = 16.sp,
                                        themeColors = themeColors,
                                        isOldTestament = viewModel.isOldTestament,
                                        options = ProcessingOptions(showHeaders = false)
                                    )
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(SpanStyle(color = themeColors.verseNumber)) { append("${verse.verseNumber} ") }
                                            append(processed.body)
                                        },
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val passage = buildPassageText(currentReference, fetchedVerses, verseProcessor, themeColors, viewModel)
                                    if (passage.isNotBlank()) viewModel.addText(passage)
                                }, modifier = Modifier.weight(1f)) {
                                    Text("Add to Notes")
                                }
                                Button(onClick = {
                                    val passage = buildPassageText(currentReference, fetchedVerses, verseProcessor, themeColors, viewModel)
                                    if (passage.isNotBlank()) viewModel.addToCanvas(CanvasNote(content = passage))
                                }, modifier = Modifier.weight(1f)) {
                                    Text("Add to Canvas")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = currentText,
                onValueChange = { currentText = it },
                label = { Text("Enter text") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                viewModel.addText(currentText)
                currentText = ""
            }) { Text("Add") }
        }

        Spacer(Modifier.height(20.dp))
        Text("Notes", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
            items(viewModel.addedTexts, key = { it }) { text ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text, modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Button(onClick = { viewModel.addToCanvas(CanvasNote(content = text)) }) {
                            Text("→ Canvas")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    coroutineScope.launch { saveCanvasAsImage(graphicsLayer, context, "JPG") }
                }) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(4.dp))
                    Text("JPG")
                }
                Button(onClick = {
                    coroutineScope.launch { saveCanvasAsPDF(graphicsLayer, context) }
                }) {
                    Icon(Icons.Default.PictureAsPdf, null)
                    Spacer(Modifier.width(4.dp))
                    Text("PDF")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(700.dp)
                .clipToBounds()
                .background(if (isDark) Color(0xFF1E2937) else Color(0xFFF1F5F9), shape = MaterialTheme.shapes.medium)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawContent()
                    }
            ) {
                viewModel.canvasNotes.forEachIndexed { index, note ->
                    var currentOffset by remember(note.id) { mutableStateOf(note.offset) }
                    var currentWidth by remember(note.id) { mutableFloatStateOf(note.width) }
                    var currentHeight by remember(note.id) { mutableFloatStateOf(note.height) }
                    var currentRotation by remember(note.id) { mutableFloatStateOf(0f) }
                    var startTouchAngle by remember(note.id) { mutableFloatStateOf(0f) }
                    var startCardRotation by remember(note.id) { mutableFloatStateOf(0f) }
                    var cardCoordinates: androidx.compose.ui.layout.LayoutCoordinates? by remember(note.id) { mutableStateOf(null) }
                    var resizeHandleCoords: androidx.compose.ui.layout.LayoutCoordinates? by remember(note.id) { mutableStateOf(null) }
                    var rotateHandleCoords: androidx.compose.ui.layout.LayoutCoordinates? by remember(note.id) { mutableStateOf(null) }

                    val density = LocalDensity.current
                    val contentLines = note.content.lines()
                    val referenceLine = contentLines.firstOrNull() ?: "Note"
                    val bodyText = if (contentLines.size > 1) contentLines.drop(1).joinToString("\n") else ""

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(currentOffset.x.roundToInt(), currentOffset.y.roundToInt()) }
                            .width(with(density) { currentWidth.toDp() })
                            .height(with(density) { currentHeight.toDp() })
                            .graphicsLayer {
                                rotationZ = currentRotation
                            }
                            .pointerInput(note.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val rad = Math.toRadians(currentRotation.toDouble())
                                    val cos = kotlin.math.cos(rad).toFloat()
                                    val sin = kotlin.math.sin(rad).toFloat()
                                    val correctedDrag = androidx.compose.ui.geometry.Offset(
                                        x = dragAmount.x * cos - dragAmount.y * sin,
                                        y = dragAmount.x * sin + dragAmount.y * cos
                                    )
                                    currentOffset += correctedDrag
                                    viewModel.updateNotePosition(note.id, currentOffset, currentWidth, currentHeight)
                                }
                            }
                            .onGloballyPositioned { coordinates ->
                                cardCoordinates = coordinates
                            }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(containerColor = note.backgroundColor),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(note.backgroundColor.copy(alpha = 0.95f))
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = referenceLine,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                        color = if (note.backgroundColor.luminance() < 0.5f) Color.White else Color.Black
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(Brush.horizontalGradient(listOf(note.backgroundColor, note.backgroundColor.copy(alpha = 0.5f))), CircleShape)
                                            .border(1.5.dp, color = if (note.backgroundColor.luminance() < 0.5f) Color.White else Color.Black, CircleShape)
                                            .clickable {
                                                noteToColorEdit = note
                                                showColorPicker = true
                                            }
                                    )
                                    Spacer(Modifier.width(24.dp))

                                    IconButton(onClick = { viewModel.removeFromCanvas(index) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 0.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = bodyText.ifBlank { note.content },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (note.backgroundColor.luminance() < 0.5f) Color.White else Color.Black
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .onGloballyPositioned { coordinates ->
                                    resizeHandleCoords = coordinates
                                }
                                .pointerInput(note.id) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        val cardCoords = cardCoordinates
                                        val handleCoords = resizeHandleCoords

                                        if (cardCoords != null && handleCoords != null) {
                                            val touchInWindow = handleCoords.localToWindow(change.position)
                                            val cardCenterInWindow = cardCoords.localToWindow(
                                                androidx.compose.ui.geometry.Offset(cardCoords.size.width / 2f, cardCoords.size.height / 2f)
                                            )

                                            val deltaX = touchInWindow.x - cardCenterInWindow.x
                                            val deltaY = touchInWindow.y - cardCenterInWindow.y
                                            val rad = Math.toRadians(currentRotation.toDouble())
                                            val cos = kotlin.math.cos(rad).toFloat()
                                            val sin = kotlin.math.sin(rad).toFloat()

                                            val localX = deltaX * cos + deltaY * sin
                                            val localY = -deltaX * sin + deltaY * cos
                                            val newWidth = (currentWidth / 2f + localX).coerceAtLeast(160f)
                                            val newHeight = (currentHeight / 2f + localY).coerceAtLeast(120f)

                                            currentWidth = newWidth
                                            currentHeight = newHeight
                                            viewModel.updateNotePosition(note.id, currentOffset, newWidth, newHeight)
                                        }
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(12.dp)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(32.dp)
                                .onGloballyPositioned { coordinates ->
                                    rotateHandleCoords = coordinates
                                }
                                .pointerInput(note.id) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val cardCoords = cardCoordinates
                                            val handleCoords = rotateHandleCoords
                                            if (cardCoords != null && handleCoords != null) {
                                                val touchInWindow = handleCoords.localToWindow(offset)
                                                val cardCenterInWindow = cardCoords.localToWindow(
                                                    androidx.compose.ui.geometry.Offset(cardCoords.size.width / 2f, cardCoords.size.height / 2f)
                                                )
                                                val deltaX = touchInWindow.x - cardCenterInWindow.x
                                                val deltaY = touchInWindow.y - cardCenterInWindow.y

                                                startTouchAngle = Math.toDegrees(kotlin.math.atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat()
                                                startCardRotation = currentRotation
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val cardCoords = cardCoordinates
                                            val handleCoords = rotateHandleCoords
                                            if (cardCoords != null && handleCoords != null) {
                                                val touchInWindow = handleCoords.localToWindow(change.position)
                                                val cardCenterInWindow = cardCoords.localToWindow(
                                                    androidx.compose.ui.geometry.Offset(cardCoords.size.width / 2f, cardCoords.size.height / 2f)
                                                )
                                                val deltaX = touchInWindow.x - cardCenterInWindow.x
                                                val deltaY = touchInWindow.y - cardCenterInWindow.y

                                                val currentTouchAngle = Math.toDegrees(kotlin.math.atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat()
                                                val angleDiff = currentTouchAngle - startTouchAngle

                                                currentRotation = (startCardRotation + angleDiff) % 360f
                                            }
                                        }
                                    )
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .size(12.dp)
                                    .background(MaterialTheme.colorScheme.tertiary, shape = CircleShape)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }


    if (showColorPicker && noteToColorEdit != null) {
        ColorWheelDialog(
            onDismissRequest = {
                showColorPicker = false
                noteToColorEdit = null
            },
            onColorSelected = { selectedColor ->
                viewModel.updateNoteColor(noteToColorEdit!!.id, selectedColor)
                showColorPicker = false
                noteToColorEdit = null
            },
            initialColor = noteToColorEdit?.backgroundColor ?: Color.White
        )
    }
}

private fun buildPassageText(
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

private fun buildReferenceString(
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

private suspend fun saveCanvasAsImage(
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
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            makeText(context, "Failed to save image", LENGTH_SHORT).show()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private suspend fun saveCanvasAsPDF(
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