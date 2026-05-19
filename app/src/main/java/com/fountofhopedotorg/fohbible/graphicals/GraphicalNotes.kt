package com.fountofhopedotorg.fohbible.graphicals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountofhopedotorg.fohbible.data.DatabaseHelper
import com.fountofhopedotorg.fohbible.data.ProcessingOptions
import com.fountofhopedotorg.fohbible.data.ReferenceResult
import com.fountofhopedotorg.fohbible.data.ThemeColors
import com.fountofhopedotorg.fohbible.data.Verse
import com.fountofhopedotorg.fohbible.data.fetchByReference
import com.fountofhopedotorg.fohbible.models.AppViewModel
import com.fountofhopedotorg.fohbible.ui.theme.LocalAppTheme
import com.fountofhopedotorg.fohbible.utils.VerseTextProcessor

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
    var currentText by remember { mutableStateOf("") }
    var referenceInput by remember { mutableStateOf("") }
    var fetchedVerses by remember { mutableStateOf<List<Verse>>(emptyList()) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var currentReference by remember { mutableStateOf("") }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .heightIn(max = 1600.dp)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .background(themeColors.primary.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Fetch Bible Verse",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = referenceInput,
                            onValueChange = { referenceInput = it },
                            label = { Text("Reference (e.g., John 3:16)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = fetchError != null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                fetchError = null
                                when (val result = fetchByReference(referenceInput, dbHelper)) {
                                    is ReferenceResult.Single -> {
                                        fetchedVerses = listOf(result.verse)
                                        currentReference = buildReferenceString(
                                            bookName = result.bookName,
                                            chapter = result.verse.chapter,
                                            startVerse = result.verse.verseNumber,
                                            endVerse = null
                                        )
                                    }
                                    is ReferenceResult.Range -> {
                                        fetchedVerses = result.verses
                                        val first = result.verses.first()
                                        val last = result.verses.last()
                                        currentReference = buildReferenceString(
                                            bookName = result.bookName,
                                            chapter = first.chapter,
                                            startVerse = first.verseNumber,
                                            endVerse = last.verseNumber
                                        )
                                    }
                                    is ReferenceResult.Chapter -> {
                                        fetchedVerses = result.verses
                                        val first = result.verses.first()
                                        currentReference = buildReferenceString(
                                            bookName = result.bookName,
                                            chapter = first.chapter,
                                            startVerse = null,
                                            endVerse = null
                                        )
                                    }
                                    ReferenceResult.Invalid -> {
                                        fetchedVerses = emptyList()
                                        currentReference = ""
                                        fetchError = "Invalid reference or verse not found"
                                    }
                                }
                            }
                        ) {
                            Text("Fetch")
                        }
                    }

                    if (fetchError != null) {
                        Text(
                            text = fetchError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (fetchedVerses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Fetched Verses:",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(themeColors.primary.copy(alpha = 0.15f))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = currentReference,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = themeColors.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = it },
                    label = { Text("Enter text") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (currentText.isNotBlank()) {
                            addedTexts.add(currentText.trim())
                            currentText = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(addedTexts, key = { it }) { text ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = text,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
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
        endVerse == null -> "$fullBook $chapter:$startVerse"
        startVerse == endVerse -> "$fullBook $chapter:$startVerse"
        else -> "$fullBook $chapter:$startVerse-$endVerse"
    }
}