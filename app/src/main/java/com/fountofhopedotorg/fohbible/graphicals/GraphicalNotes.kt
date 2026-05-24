package com.fountofhopedotorg.fohbible.graphicals

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.composables.CanvasImageItem
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
import com.fountofhopedotorg.fohbible.functions.getElementDisplayName
import com.fountofhopedotorg.fohbible.functions.getRandomColor
import com.fountofhopedotorg.fohbible.functions.getSerializedPointsForShape
import com.fountofhopedotorg.fohbible.functions.saveCanvasAsImage
import com.fountofhopedotorg.fohbible.functions.saveCanvasAsPDF
import com.fountofhopedotorg.fohbible.functions.saveCanvasAsSVG
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun GraphicalNotesScreen() {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    var showCustomPolygonDialog by rememberSaveable { mutableStateOf(false) }
    var selectedNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    var currentText by rememberSaveable { mutableStateOf("") }
    var referenceInput by rememberSaveable { mutableStateOf("") }
    var fetchError by rememberSaveable { mutableStateOf<String?>(null) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var noteToColorEditId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedInputMode by rememberSaveable { mutableStateOf("Add Text") }
    var noteToEdit by rememberSaveable { mutableStateOf<String?>(null) }
    var editedNoteText by rememberSaveable { mutableStateOf("") }
    var showCanvasElementsTree by rememberSaveable { mutableStateOf(true) }
    var showSaveMenu by rememberSaveable { mutableStateOf(false) }
    var polygonNoteToEditId by rememberSaveable { mutableStateOf<String?>(null) }
    var initialPolygonString by rememberSaveable { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addToCanvas(CanvasNote(content = "Image: $uri"))
        }
    }

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

    val mainScrollState = rememberScrollState()

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
                Triple("Add SVG", Icons.Default.FormatShapes, "Add SVG"),
                Triple("Add Image", Icons.Default.Image, "Add Image")
            )

            modes.forEach { (mode, icon, desc) ->
                val isSelected = selectedInputMode == mode
                IconButton(
                    onClick = { selectedInputMode = mode },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(
                            color = if (isSelected) themeColors.primary.copy(alpha = 0.2f) else Color.Transparent,
                            shape = CircleShape
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
                            viewModel.addToCanvas(CanvasNote(content = currentText))
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
                                        viewModel.fetchedVerses = listOf(result.verse)
                                        viewModel.currentReference = buildReferenceString(result.bookName, result.verse.chapter, result.verse.verseNumber, null)
                                    }
                                    is ReferenceResult.Range -> {
                                        viewModel.fetchedVerses = result.verses
                                        val first = result.verses.first()
                                        val last = result.verses.last()
                                        viewModel.currentReference = buildReferenceString(result.bookName, first.chapter, first.verseNumber, last.verseNumber)
                                    }
                                    is ReferenceResult.Chapter -> {
                                        viewModel.fetchedVerses = result.verses
                                        val first = result.verses.first()
                                        viewModel.currentReference = buildReferenceString(result.bookName, first.chapter, null, null)
                                    }
                                    ReferenceResult.Invalid -> {
                                        viewModel.fetchedVerses = emptyList()
                                        viewModel.currentReference = ""
                                        fetchError = "Invalid reference or verse not found"
                                    }
                                }
                            }) { Text("Fetch") }
                        }

                        if (fetchError != null) {
                            Text(fetchError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                        }

                        if (viewModel.fetchedVerses.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Fetched Verses:", style = MaterialTheme.typography.titleSmall)

                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Column {
                                    Box(modifier = Modifier.fillMaxWidth().background(themeColors.primary.copy(alpha = 0.15f)).padding(12.dp)) {
                                        Text(viewModel.currentReference, style = MaterialTheme.typography.titleMedium, color = themeColors.primary, fontWeight = FontWeight.Bold)
                                    }

                                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState()).padding(12.dp)) {
                                        viewModel.fetchedVerses.forEach { verse ->
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

                                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
                                        val passage = remember(viewModel.currentReference, viewModel.fetchedVerses, themeColors) {
                                            buildPassageText(viewModel.currentReference, viewModel.fetchedVerses, verseProcessor, themeColors, viewModel)
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
                                onClick = { viewModel.addToCanvas(CanvasNote(content = "Shape: Square", backgroundColor = getRandomColor())) }
                            ) {
                                SquareShape(modifier = Modifier.size(35.dp))
                            }
                            ShapeSelectionCard(
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.addToCanvas(CanvasNote(content = "Shape: Circle", backgroundColor = getRandomColor())) }
                            ) {
                                CircleShape(modifier = Modifier.size(35.dp))
                            }
                            ShapeSelectionCard(
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.addToCanvas(CanvasNote(content = "Shape: Triangle", backgroundColor = getRandomColor())) }
                            ) {
                                TriangleShape(modifier = Modifier.size(35.dp))
                            }
                            ShapeSelectionCard(
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.addToCanvas(CanvasNote(content = "Shape: Pentagon", backgroundColor = getRandomColor())) }
                            ) {
                                PolygonShape(
                                    points = pentagonPoints,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            ShapeSelectionCard(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    polygonNoteToEditId = null
                                    initialPolygonString = ""
                                    showCustomPolygonDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShapeLine,
                                    contentDescription = "Custom Polygon",
                                    modifier = Modifier.size(35.dp),
                                    tint = randomColor().copy(0.8f)
                                )
                            }
                        }
                    }
                }
            }
            "Add Image" -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.primary.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Load an image from your device:",
                            style = MaterialTheme.typography.titleSmall,
                            color = themeColors.textColor
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                            Spacer(Modifier.width(8.dp))
                            Text("Choose from Gallery")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCanvasElementsTree = !showCanvasElementsTree }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Canvas Elements", style = MaterialTheme.typography.titleMedium)
            Icon(
                imageVector = if (showCanvasElementsTree) Icons.Default.ArrowDropDown else Icons.AutoMirrored.Filled.ArrowRight,
                contentDescription = if (showCanvasElementsTree) "Collapse" else "Expand",
                tint = themeColors.textColor
            )
        }

        AnimatedVisibility(visible = showCanvasElementsTree) {
            Column {
                if (viewModel.canvasNotes.isEmpty()) {
                    Text(
                        "No elements on canvas yet.",
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    // State for drag reordering
                    var draggedIndex by remember { mutableStateOf<Int?>(null) }
                    var dragOffset by remember { mutableFloatStateOf(0f) }
                    val density = LocalDensity.current
                    val itemHeightPx = remember(density) { with(density) { 56.dp.toPx() } } // approximate row height

                    val listState = rememberLazyListState()

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.heightIn(max = 260.dp)
                    ) {
                        itemsIndexed(
                            viewModel.canvasNotes,
                            key = { _, note -> note.id }
                        ) { index, note ->
                            val isSelected = selectedNoteId == note.id

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .graphicsLayer {
                                        // Visually lift the dragged item
                                        if (draggedIndex == index) {
                                            translationY = dragOffset
                                        }
                                    }
                                    .pointerInput(note.id) {
                                        // Tap to select (short press)
                                        detectTapGestures { selectedNoteId = note.id }
                                    }
                                    .pointerInput(note.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                // Dynamically fetch the current position at drag start
                                                val currentIndex = viewModel.canvasNotes.indexOfFirst { it.id == note.id }
                                                if (currentIndex == -1) return@detectDragGesturesAfterLongPress
                                                draggedIndex = currentIndex
                                                dragOffset = offset.y
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount.y
                                            },
                                            onDragEnd = {
                                                val fromIndex = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                                val currentIndex = viewModel.canvasNotes.indexOfFirst { it.id == note.id }
                                                if (currentIndex == -1 || fromIndex != currentIndex) {
                                                    // If the item moved externally during drag, ignore
                                                    draggedIndex = null
                                                    dragOffset = 0f
                                                    return@detectDragGesturesAfterLongPress
                                                }
                                                val target = (fromIndex + (dragOffset / itemHeightPx).roundToInt())
                                                    .coerceIn(0, viewModel.canvasNotes.size - 1)
                                                if (target != fromIndex) {
                                                    viewModel.reorderCanvasNotes(fromIndex, target)
                                                }
                                                draggedIndex = null
                                                dragOffset = 0f
                                            },
                                            onDragCancel = {
                                                draggedIndex = null
                                                dragOffset = 0f
                                            }
                                        )
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        themeColors.primary.copy(alpha = 0.15f)
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Element icon (unchanged)
                                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                        when {
                                            note.content.startsWith("Shape:") -> {
                                                val shapeName = note.content.removePrefix("Shape: ").trim()
                                                when {
                                                    shapeName.startsWith("Square") -> SquareShape(modifier = Modifier.size(18.dp))
                                                    shapeName.startsWith("Circle") -> CircleShape(modifier = Modifier.size(18.dp))
                                                    shapeName.startsWith("Triangle") -> TriangleShape(modifier = Modifier.size(18.dp))
                                                    shapeName.startsWith("Pentagon") -> PolygonShape(
                                                        points = listOf(
                                                            Offset(0.5f, 0f), Offset(1f, 0.4f), Offset(0.8f, 0.9f),
                                                            Offset(0.2f, 0.9f), Offset(0f, 0.4f)
                                                        ),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    else -> Icon(Icons.Default.ShapeLine, null, modifier = Modifier.size(18.dp), tint = themeColors.primary)
                                                }
                                            }
                                            note.content.startsWith("Image:") -> Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp), tint = themeColors.primary)
                                            else -> Icon(Icons.Default.TextFields, null, modifier = Modifier.size(18.dp), tint = themeColors.primary)
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // Element label
                                    Text(
                                        text = getElementDisplayName(note, index, viewModel.canvasNotes),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    // Vertical up/down arrows (stacked)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) viewModel.reorderCanvasNotes(index, index - 1)
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(24.dp) // small touch target
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Move up",
                                                tint = if (index > 0) themeColors.primary else Color.Gray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (index < viewModel.canvasNotes.size - 1) viewModel.reorderCanvasNotes(index, index + 1)
                                            },
                                            enabled = index < viewModel.canvasNotes.size - 1,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Move down",
                                                tint = if (index < viewModel.canvasNotes.size - 1) themeColors.primary else Color.Gray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            if (note.content.startsWith("Shape: ")) {
                                                if (note.content.startsWith("Shape: CustomPolygon:")) {
                                                    polygonNoteToEditId = note.id
                                                    initialPolygonString = note.content.removePrefix("Shape: CustomPolygon:")
                                                    showCustomPolygonDialog = true
                                                } else {
                                                    val shapeType = note.content.removePrefix("Shape: ")
                                                    val prefilledPoints = getSerializedPointsForShape(shapeType)
                                                    if (prefilledPoints.isNotEmpty()) {
                                                        polygonNoteToEditId = note.id
                                                        initialPolygonString = prefilledPoints
                                                        showCustomPolygonDialog = true
                                                    } else {
                                                        noteToEdit = note.id
                                                        editedNoteText = note.content
                                                    }
                                                }
                                            } else {
                                                noteToEdit = note.id
                                                editedNoteText = note.content
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = themeColors.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.addToCanvas(CanvasNote(content = note.content)) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Duplicate",
                                            tint = themeColors.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val idx = viewModel.canvasNotes.indexOf(note)
                                            if (idx != -1) viewModel.removeFromCanvas(idx)
                                            if (selectedNoteId == note.id) selectedNoteId = null
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(700.dp)
                .clipToBounds()
                .background(if (isDark) Color(0xFF1E2937) else themeColors.primary.copy(0.1f), shape = MaterialTheme.shapes.medium)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { selectedNoteId = null })
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawContent()
                    }
            ) {
                viewModel.canvasNotes.forEach { note ->
                    key(note.id) {
                        when {
                            note.content.startsWith("Shape:") -> CanvasSvgItem(
                            note = note,
                            isSelected = selectedNoteId == note.id,
                            onSelect = { selectedNoteId = note.id },
                            onUpdatePosition = { offset, width, height ->
                                viewModel.updateNotePosition(note.id, offset, width, height)
                            },
                            onColorPickerRequested = {
                                noteToColorEditId = note.id
                                showColorPicker = true
                            },
                                onDeleteRequested = {
                                    val idx = viewModel.canvasNotes.indexOfFirst { it.id == note.id }
                                    if (idx != -1) viewModel.removeFromCanvas(idx)
                                }
                        )
                        note.content.startsWith("Image:") -> CanvasImageItem(
                            note = note,
                            isSelected = selectedNoteId == note.id,
                            onSelect = { selectedNoteId = note.id },
                            onUpdatePosition = { offset, width, height ->
                                viewModel.updateNotePosition(note.id, offset, width, height)
                            },
                            onDeleteRequested = {
                                val idx = viewModel.canvasNotes.indexOfFirst { it.id == note.id }
                                if (idx != -1) viewModel.removeFromCanvas(idx)
                            }
                        )
                        else -> CanvasTextItem(
                            note = note,
                            isSelected = selectedNoteId == note.id,
                            onSelect = { selectedNoteId = note.id },
                            onUpdatePosition = { offset, width, height ->
                                viewModel.updateNotePosition(note.id, offset, width, height)
                            },
                            onColorPickerRequested = {
                                noteToColorEditId = note.id
                                showColorPicker = true
                            },
                            onDeleteRequested = {
                                val idx = viewModel.canvasNotes.indexOfFirst { it.id == note.id }
                                if (idx != -1) viewModel.removeFromCanvas(idx)
                            }
                        )
                        }
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
            Box {
                Button(onClick = { showSaveMenu = true }) {
                    Icon(Icons.Default.Save, contentDescription = "Save As", tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Save As...", color = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand", tint = Color.White)
                }

                DropdownMenu(
                    expanded = showSaveMenu,
                    onDismissRequest = { showSaveMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("PNG") },
                        onClick = {
                            showSaveMenu = false
                            coroutineScope.launch { saveCanvasAsImage(graphicsLayer, context, "PNG") }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("JPG") },
                        onClick = {
                            showSaveMenu = false
                            coroutineScope.launch { saveCanvasAsImage(graphicsLayer, context, "JPG") }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("PDF") },
                        onClick = {
                            showSaveMenu = false
                            coroutineScope.launch { saveCanvasAsPDF(graphicsLayer, context) }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("SVG") },
                        onClick = {
                            showSaveMenu = false
                            coroutineScope.launch { saveCanvasAsSVG(graphicsLayer, context) }
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (noteToEdit != null) {
        AlertDialog(
            onDismissRequest = { noteToEdit = null },
            title = { Text("Edit Canvas Note") },
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
                        val noteId = noteToEdit!!
                        val index = viewModel.canvasNotes.indexOfFirst { it.id == noteId }
                        if (index != -1) {
                            val old = viewModel.canvasNotes[index]
                            val updated = old.copy(content = editedNoteText)
                            viewModel.removeFromCanvas(index)
                            viewModel.addToCanvas(updated)
                        }
                        noteToEdit = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { noteToEdit = null }) { Text("Cancel") }
            }
        )
    }

    if (showColorPicker && noteToColorEditId != null) {
        val targetNote = viewModel.canvasNotes.find { it.id == noteToColorEditId }
        ColorWheelDialog(
            onDismissRequest = {
                showColorPicker = false
                noteToColorEditId = null
            },
            onColorSelected = { selectedColor ->
                viewModel.updateNoteColor(noteToColorEditId!!, selectedColor)
                showColorPicker = false
                noteToColorEditId = null
            },
            initialColor = targetNote?.backgroundColor ?: Color.White
        )
    }

    if (showCustomPolygonDialog) {
        CustomPolygonDialog(
            initialSerializedPoints = initialPolygonString.takeIf { it.isNotEmpty() },
            onDismiss = {
                showCustomPolygonDialog = false
                polygonNoteToEditId = null
                initialPolygonString = ""
            },

            onConfirm = { points ->
                val serialized = points.joinToString(";") { node ->
                    "${node.anchor.x},${node.anchor.y}:${node.handleIn.x},${node.handleIn.y}:${node.handleOut.x},${node.handleOut.y}"
                }
                val contentString = "Shape: CustomPolygon:$serialized"

                if (polygonNoteToEditId != null) {
                    viewModel.updateNoteContent(polygonNoteToEditId!!, contentString)
                    selectedNoteId = polygonNoteToEditId
                } else {
                    viewModel.addToCanvas(CanvasNote(content = contentString, backgroundColor = getRandomColor()))
                }

                showCustomPolygonDialog = false
                polygonNoteToEditId = null
                initialPolygonString = ""
            }
        )
    }
}