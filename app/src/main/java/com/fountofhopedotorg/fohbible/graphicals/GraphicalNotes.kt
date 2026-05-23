package com.fountofhopedotorg.fohbible.graphicals

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.composables.CanvasSvgItem
import com.fountofhopedotorg.fohbible.composables.CanvasTextItem
import com.fountofhopedotorg.fohbible.composables.CircleShape
import com.fountofhopedotorg.fohbible.composables.ColorWheelDialog
import com.fountofhopedotorg.fohbible.composables.CustomPolygonDialog
import com.fountofhopedotorg.fohbible.composables.PolygonShape
import com.fountofhopedotorg.fohbible.composables.ShapeSelectionCard
import com.fountofhopedotorg.fohbible.composables.SquareShape
import com.fountofhopedotorg.fohbible.composables.TriangleShape
import com.fountofhopedotorg.fohbible.composables.randomColor
import com.fountofhopedotorg.fohbible.data.*
import com.fountofhopedotorg.fohbible.functions.buildPassageText
import com.fountofhopedotorg.fohbible.functions.buildReferenceString
import com.fountofhopedotorg.fohbible.functions.saveCanvasAsImage
import com.fountofhopedotorg.fohbible.functions.saveCanvasAsPDF
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun GraphicalNotesScreen() {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var showCustomPolygonDialog by remember { mutableStateOf(false) }
    var selectedNoteId by remember { mutableStateOf<String?>(null) }

    val dbHelper = remember(viewModel.currentDbName) {
        DatabaseHelper(context, viewModel.currentDbName)
    }

    DisposableEffect(dbHelper) {
        onDispose { dbHelper.close() }
    }

    val verseProcessor = remember { VerseTextProcessor() }
    val theme = LocalAppTheme.current
    val isDark = theme.darkTheme

    val themeColors = remember(isDark, theme.primaryColor, viewModel.wordsOfJesus) {
        ThemeColors(
            textColor = if (isDark) Color.White else Color.Black,
            verseNumber = theme.primaryColor,
            primary = theme.primaryColor,
            tagColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
            tagBg = if (isDark) Color(0xFF1E293B) else Color.White,
            wordsOfJesus = viewModel.wordsOfJesus,
            searchHighlightBg = theme.primaryColor.copy(alpha = 0.2f),
            highlightIcon = theme.primaryColor
        )
    }

    var currentText by remember { mutableStateOf("") }
    var referenceInput by remember { mutableStateOf("") }
    var fetchedVerses by remember { mutableStateOf<List<Verse>>(emptyList()) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var currentReference by remember { mutableStateOf("") }
    var showColorPicker by remember { mutableStateOf(false) }
    var noteToColorEdit by remember { mutableStateOf<CanvasNote?>(null) }
    val mainScrollState = rememberScrollState()
    var selectedInputMode by remember { mutableStateOf("Add Text") }
    var noteToEdit by remember { mutableStateOf<String?>(null) }
    var editedNoteText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(mainScrollState)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val modes = listOf(
                Triple("Add Text", Icons.Default.TextFields, "Add Text"),
                Triple("Fetch Verse", Icons.Default.Book, "Fetch Verse"),
                Triple("Add SVG", Icons.Default.FormatShapes, "Add SVG")
            )

            modes.forEach { (mode, icon, desc) ->
                val isSelected = selectedInputMode == mode
                IconButton(
                    onClick = { selectedInputMode = mode },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(
                            color = if (isSelected) themeColors.primary.copy(alpha = 0.2f) else Color.Transparent,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = desc,
                        tint = if (isSelected) themeColors.primary else themeColors.textColor.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        when (selectedInputMode) {
            "Add Text" -> {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
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
                            viewModel.addText(currentText)
                            currentText = ""
                        }
                    }) { Text("Add", color = Color.White) }
                }
            }

            "Fetch Verse" -> {
                Box(modifier = Modifier.fillMaxWidth().background(themeColors.primary.copy(alpha = 0.1f))) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
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
                                        val passage = remember(currentReference, fetchedVerses, themeColors) {
                                            buildPassageText(currentReference, fetchedVerses, verseProcessor, themeColors, viewModel)
                                        }
                                        Button(onClick = {
                                            if (passage.isNotBlank()) viewModel.addText(passage)
                                        }, modifier = Modifier.weight(1f)) {
                                            Text("Add to Notes")
                                        }
                                        Button(onClick = {
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
            }

            "Add SVG" -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.primary.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tap a shape to add to canvas:",
                            style = MaterialTheme.typography.titleSmall,
                            color = themeColors.textColor
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val pentagonPoints = listOf(
                                Offset(0.5f, 0f),
                                Offset(1f, 0.4f),
                                Offset(0.8f, 0.9f),
                                Offset(0.2f, 0.9f),
                                Offset(0f, 0.4f)
                            )
                            ShapeSelectionCard(
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.addToCanvas(CanvasNote(content = "Shape: Square")) }
                            ) {
                                SquareShape(modifier = Modifier.size(35.dp))
                            }
                            ShapeSelectionCard(
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.addToCanvas(CanvasNote(content = "Shape: Circle")) }
                            ) {
                                CircleShape(modifier = Modifier.size(35.dp))
                            }
                            ShapeSelectionCard(
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.addToCanvas(CanvasNote(content = "Shape: Triangle")) }
                            ) {
                                TriangleShape(modifier = Modifier.size(35.dp))
                            }
                            ShapeSelectionCard(
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.addToCanvas(CanvasNote(content = "Shape: Pentagon")) }
                            ) {
                                PolygonShape(
                                    points = pentagonPoints,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            ShapeSelectionCard(
                                modifier = Modifier.weight(1f),
                                onClick = { showCustomPolygonDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShapeLine,
                                    contentDescription = "Custom Polygon",
                                    modifier = Modifier.size(35.dp),
                                    tint = randomColor().copy(0.8f),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Notes", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
            items(viewModel.addedTexts, key = { it.hashCode() }) { text ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = text,
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    noteToEdit = text
                                    editedNoteText = text
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    tint = themeColors.primary,
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Note",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.size(4.dp))
                            IconButton(
                                onClick = { viewModel.removeText(text) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Note",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.size(12.dp))
                            Button(
                                onClick = { viewModel.addToCanvas(CanvasNote(content = text)) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("→ Canvas", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(700.dp)
                .clipToBounds()
                .background(if (isDark) Color(0xFF1E2937) else themeColors.primary.copy(0.1f), shape = MaterialTheme.shapes.medium)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            selectedNoteId = null
                        }
                    )
                }
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
                    if (note.content.startsWith("Shape:")) {
                        CanvasSvgItem(
                            note = note,
                            isSelected = selectedNoteId == note.id,
                            onSelect = { selectedNoteId = note.id },
                            onUpdatePosition = { offset, width, height ->
                                viewModel.updateNotePosition(note.id, offset, width, height)
                            },
                            onColorPickerRequested = {
                                noteToColorEdit = note
                                showColorPicker = true
                            },
                            onDeleteRequested = {
                                viewModel.removeFromCanvas(index)
                            }
                        )
                    } else {
                        CanvasTextItem(
                            note = note,
                            isSelected = selectedNoteId == note.id,
                            onSelect = { selectedNoteId = note.id },
                            onUpdatePosition = { offset, width, height ->
                                viewModel.updateNotePosition(note.id, offset, width, height)
                            },
                            onColorPickerRequested = {
                                noteToColorEdit = note
                                showColorPicker = true
                            },
                            onDeleteRequested = {
                                viewModel.removeFromCanvas(index)
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    coroutineScope.launch { saveCanvasAsImage(graphicsLayer, context, "JPG") }
                }) {
                    Icon(Icons.Default.Save, null, tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("JPG", color = Color.White)
                }
                Button(onClick = {
                    coroutineScope.launch { saveCanvasAsPDF(graphicsLayer, context) }
                }) {
                    Icon(Icons.Default.PictureAsPdf, null, tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("PDF", color = Color.White)
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

    if (noteToEdit != null) {
        AlertDialog(
            onDismissRequest = { noteToEdit = null },
            title = { Text("Edit Note") },
            text = {
                OutlinedTextField(
                    value = editedNoteText,
                    onValueChange = { editedNoteText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateText(noteToEdit!!, editedNoteText)
                        noteToEdit = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCustomPolygonDialog) {
        CustomPolygonDialog(
            onDismiss = { showCustomPolygonDialog = false },
            onConfirm = { points ->
                val serialized = points.joinToString(";") { "${it.x},${it.y}" }
                viewModel.addToCanvas(CanvasNote(content = "Shape: CustomPolygon:$serialized"))
                showCustomPolygonDialog = false
            }
        )
    }
}