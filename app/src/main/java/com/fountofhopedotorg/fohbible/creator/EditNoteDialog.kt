package com.fountofhopedotorg.fohbible.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fountofhopedotorg.fohbible.data.*
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun EditNoteDialog(
    noteId: String?,
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String?, String) -> Unit,
    isNew: Boolean = false,
    fetchMode: Boolean = false,
    dbHelper: DatabaseHelper? = null,
    viewModel: AppViewModel? = null,
    verseProcessor: VerseTextProcessor? = null,
    themeColors: ThemeColors? = null
) {
    if (noteId != null || isNew || fetchMode) {
        var content by remember { mutableStateOf(initialContent) }
        var referenceInput by remember { mutableStateOf("") }
        var fetchError by remember { mutableStateOf<String?>(null) }
        var fetchedVerses by remember { mutableStateOf<List<Verse>>(emptyList()) }
        var currentReference by remember { mutableStateOf("") }
        val isEditMode = noteId != null
        val isManualNew = isNew && !fetchMode
        val showEditableField = isEditMode || isManualNew

        val canSave = content.isNotBlank()

        if (fetchMode && dbHelper != null && viewModel != null && verseProcessor != null && themeColors != null) {
            LaunchedEffect(referenceInput) {
                if (referenceInput.isBlank()) {
                    fetchError = null
                    fetchedVerses = emptyList()
                    currentReference = ""
                    content = ""
                    return@LaunchedEffect
                }
                delay(500.milliseconds)
                fetchError = null
                when (val result = fetchByReference(referenceInput, dbHelper)) {
                    is ReferenceResult.Single -> {
                        fetchedVerses = listOf(result.verse)
                        val newRef = buildReferenceString(
                            result.bookName,
                            result.verse.chapter,
                            result.verse.verseNumber,
                            null
                        )
                        currentReference = newRef
                        content = buildProcessedContent(
                            newRef,
                            listOf(result.verse),
                            verseProcessor,
                            themeColors,
                            viewModel
                        )
                    }
                    is ReferenceResult.Range -> {
                        fetchedVerses = result.verses
                        val first = result.verses.first()
                        val last = result.verses.last()
                        val newRef = buildReferenceString(
                            result.bookName,
                            first.chapter,
                            first.verseNumber,
                            last.verseNumber
                        )
                        currentReference = newRef
                        content = buildProcessedContent(
                            newRef,
                            result.verses,
                            verseProcessor,
                            themeColors,
                            viewModel
                        )
                    }
                    is ReferenceResult.Chapter -> {
                        fetchedVerses = result.verses
                        val first = result.verses.first()
                        val newRef = buildReferenceString(
                            result.bookName,
                            first.chapter,
                            null,
                            null
                        )
                        currentReference = newRef
                        content = buildProcessedContent(
                            newRef,
                            result.verses,
                            verseProcessor,
                            themeColors,
                            viewModel
                        )
                    }
                    ReferenceResult.Invalid -> {
                        fetchedVerses = emptyList()
                        currentReference = ""
                        content = ""
                        fetchError = "Invalid reference or verse not found"
                    }
                }
            }
        }

        AlertDialog(
            modifier = Modifier.fillMaxWidth(),
            onDismissRequest = onDismiss,
            title = {
                Text(
                    when {
                        fetchMode -> "Fetch Verse"
                        isNew -> "Add Text"
                        else -> "Edit Canvas Note"
                    }
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (dbHelper != null && viewModel != null && verseProcessor != null && themeColors != null) {
                        OutlinedTextField(
                            value = referenceInput,
                            onValueChange = { referenceInput = it },
                            label = { Text("Ref (e.g. John 3:16)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            isError = fetchError != null
                        )

                        fetchError?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        if (fetchedVerses.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(themeColors.primary.copy(alpha = 0.15f))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            currentReference,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = themeColors.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
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
                                                buildAnnotatedString {
                                                    withStyle(SpanStyle(color = themeColors.verseNumber)) {
                                                        append("${verse.verseNumber} ")
                                                    }
                                                    append(processed.body)
                                                },
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showEditableField) {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 250.dp),
                            label = { Text("Note text") },
                            maxLines = 8
                        )
                    }
                }
            },
            confirmButton = {
                if (isNew && fetchMode && viewModel != null) {
                    TextButton(
                        onClick = {
                            viewModel.addToCanvas(
                                CanvasNote(content = content, textColor = getRandomColor())
                            )
                            onDismiss()
                        },
                        enabled = canSave
                    ) {
                        Text("Add to Canvas")
                    }
                } else {
                    TextButton(
                        onClick = { onSave(noteId, content) },
                        enabled = canSave
                    ) {
                        Text(if (isNew) "Add to Canvas" else "Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}