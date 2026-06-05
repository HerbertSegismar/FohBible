package com.fountofhopedotorg.fohbible.creator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.fountofhopedotorg.fohbible.data.*
import com.fountofhopedotorg.fohbible.functions.buildProcessedContent
import com.fountofhopedotorg.fohbible.functions.buildReferenceString
import com.fountofhopedotorg.fohbible.functions.getRandomColor
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor

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
        var isFetched by remember { mutableStateOf(false) }

        val canSave = content.isNotBlank()

        AlertDialog(
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
                    if (fetchMode) {
                        if (dbHelper != null && viewModel != null && verseProcessor != null && themeColors != null) {
                            FetchVerseSection(
                                referenceInput = referenceInput,
                                onReferenceChange = { referenceInput = it },
                                fetchError = fetchError,
                                onFetch = {
                                    fetchError = null
                                    when (val result = fetchByReference(referenceInput, dbHelper)) {
                                        is ReferenceResult.Single -> {
                                            fetchedVerses = listOf(result.verse)
                                            currentReference = buildReferenceString(
                                                result.bookName,
                                                result.verse.chapter,
                                                result.verse.verseNumber,
                                                null
                                            )
                                            content = buildProcessedContent(
                                                currentReference,
                                                listOf(result.verse),
                                                verseProcessor,
                                                themeColors,
                                                viewModel
                                            )
                                            isFetched = true
                                        }
                                        is ReferenceResult.Range -> {
                                            fetchedVerses = result.verses
                                            val first = result.verses.first()
                                            val last = result.verses.last()
                                            currentReference = buildReferenceString(
                                                result.bookName,
                                                first.chapter,
                                                first.verseNumber,
                                                last.verseNumber
                                            )
                                            content = buildProcessedContent(
                                                currentReference,
                                                result.verses,
                                                verseProcessor,
                                                themeColors,
                                                viewModel
                                            )
                                            isFetched = true
                                        }
                                        is ReferenceResult.Chapter -> {
                                            fetchedVerses = result.verses
                                            val first = result.verses.first()
                                            currentReference = buildReferenceString(
                                                result.bookName,
                                                first.chapter,
                                                null,
                                                null
                                            )
                                            content = buildProcessedContent(
                                                currentReference,
                                                result.verses,
                                                verseProcessor,
                                                themeColors,
                                                viewModel
                                            )
                                            isFetched = true
                                        }
                                        ReferenceResult.Invalid -> {
                                            fetchedVerses = emptyList()
                                            currentReference = ""
                                            fetchError = "Invalid reference or verse not found"
                                        }
                                    }
                                },
                                fetchedVerses = fetchedVerses,
                                currentReference = currentReference,
                                themeColors = themeColors,
                                viewModel = viewModel,
                                verseProcessor = verseProcessor,
                                showFetchInput = !isFetched
                            )
                        }
                    } else {
                        AddTextSection(
                            currentText = content,
                            onTextChange = { content = it }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (fetchMode && viewModel != null) {
                            viewModel.addToCanvas(
                                CanvasNote(
                                    content = content,
                                    textColor = getRandomColor()
                                )
                            )
                            onDismiss()
                        } else {
                            onSave(noteId, content)
                        }
                    },
                    enabled = canSave
                ) {
                    Text("Add to Canvas")
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