package com.fountofhopedotorg.fohbible.graphicals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicalNotesScreen() {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel()
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

    val addedTexts = remember { mutableStateListOf<String>() }
    val canvasNotes = remember { mutableStateListOf<CanvasNote>() }

    var currentText by remember { mutableStateOf("") }
    var referenceInput by remember { mutableStateOf("") }
    var fetchedVerses by remember { mutableStateOf<List<Verse>>(emptyList()) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var currentReference by remember { mutableStateOf("") }

    var showColorPicker by remember { mutableStateOf(false) }
    var noteToColorEdit by remember { mutableStateOf<CanvasNote?>(null) }

    val mainScrollState = rememberScrollState()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(mainScrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColors.primary.copy(alpha = 0.1f))
            ) {
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
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(themeColors.primary.copy(alpha = 0.15f))
                                        .padding(12.dp)
                                ) {
                                    Text(currentReference, style = MaterialTheme.typography.titleMedium, color = themeColors.primary, fontWeight = FontWeight.Bold)
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 280.dp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp)
                                ) {
                                    fetchedVerses.forEach { verse ->
                                        val processed = verseProcessor.processVerse(
                                            verseText = verse.text,
                                            baseFontSize = 16.sp,
                                            themeColors = themeColors,
                                            isOldTestament = viewModel.isOldTestament,
                                            options = ProcessingOptions(showHeaders = false)
                                        )
                                        Text(
                                            text = buildAnnotatedString {
                                                withStyle(SpanStyle(color = themeColors.verseNumber)) {
                                                    append("${verse.verseNumber} ")
                                                }
                                                append(processed.body)
                                            },
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(onClick = {
                                        val passage = buildPassageText(currentReference, fetchedVerses, verseProcessor, themeColors, viewModel)
                                        if (passage.isNotBlank()) addedTexts.add(passage)
                                    }, modifier = Modifier.weight(1f)) {
                                        Text("Add to Notes")
                                    }

                                    Button(onClick = {
                                        val passage = buildPassageText(currentReference, fetchedVerses, verseProcessor, themeColors, viewModel)
                                        if (passage.isNotBlank()) canvasNotes.add(CanvasNote(content = passage))
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
                    if (currentText.isNotBlank()) {
                        addedTexts.add(currentText.trim())
                        currentText = ""
                    }
                }) { Text("Add") }
            }

            Spacer(Modifier.height(20.dp))

            Text("Notes", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                items(addedTexts, key = { it }) { text ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text, modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Button(onClick = { canvasNotes.add(CanvasNote(content = text)) }) {
                                Text("→ Canvas")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Canvas Workspace (Drag to move • Drag corner to resize)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .background(if (isDark) Color(0xFF1E2937) else Color(0xFFF1F5F9), shape = MaterialTheme.shapes.medium)
            ) {
                canvasNotes.forEachIndexed { index, note ->
                    var currentOffset by remember(note.id) { mutableStateOf(note.offset) }
                    var currentWidth by remember(note.id) { mutableFloatStateOf(note.width) }
                    var currentHeight by remember(note.id) { mutableFloatStateOf(note.height) }

                    val density = LocalDensity.current

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(currentOffset.x.roundToInt(), currentOffset.y.roundToInt()) }
                            .width(with(density) { currentWidth.toDp() })
                            .height(with(density) { currentHeight.toDp() })
                            .pointerInput(note.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    currentOffset += dragAmount
                                    canvasNotes[index] = canvasNotes[index].copy(
                                        offset = currentOffset,
                                        width = currentWidth,
                                        height = currentHeight
                                    )
                                }
                            }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(containerColor = note.backgroundColor),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp).fillMaxSize()) {
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 12,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (note.backgroundColor.luminance() < 0.5f) Color.White else Color.Black
                                )

                                Spacer(Modifier.weight(1f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(
                                                brush = Brush.horizontalGradient(listOf(note.backgroundColor, note.backgroundColor.copy(alpha = 0.8f))),
                                                shape = CircleShape
                                            )
                                            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                            .clickable {
                                                noteToColorEdit = note
                                                showColorPicker = true
                                            }
                                    )

                                    IconButton(onClick = { canvasNotes.removeAt(index) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(16.dp)
                                .pointerInput(note.id) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val newWidth = (currentWidth + dragAmount.x).coerceAtLeast(160f)
                                        val newHeight = (currentHeight + dragAmount.y).coerceAtLeast(100f)

                                        currentWidth = newWidth
                                        currentHeight = newHeight

                                        canvasNotes[index] = canvasNotes[index].copy(
                                            offset = currentOffset,
                                            width = newWidth,
                                            height = newHeight
                                        )
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(0.2f), RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
    if (showColorPicker && noteToColorEdit != null) {
        ColorWheelDialog(
            onDismissRequest = {
                showColorPicker = false
                noteToColorEdit = null
            },
            onColorSelected = { selectedColor ->
                val index = canvasNotes.indexOfFirst { it.id == noteToColorEdit?.id }
                if (index != -1) {
                    canvasNotes[index] = canvasNotes[index].copy(backgroundColor = selectedColor)
                }
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