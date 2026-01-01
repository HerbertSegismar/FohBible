package com.example.fohbible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fohbible.ui.theme.FohBibleTheme

// Data class for UI representation
data class BookUi(
    val bookNumber: Int,
    val longName: String,
    val shortName: String,
    val testament: Testament,
    val totalChapters: Int
)

// Conversion from BibleBook to BookUi
fun BibleBook.toBookUi(): BookUi {
    return BookUi(
        bookNumber = number,
        longName = name,
        shortName = abbreviation,
        testament = testament,
        totalChapters = chapters
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NavigationModal(
    onDismissRequest: () -> Unit,
    onPassageSelected: (bookName: String, chapter: Int, verse: Int?) -> Unit = { _, _, _ -> },
    showNavigationModal: Boolean
) {
    // Data from BibleData
    val oldTestamentBooks = remember { BibleData.oldTestamentBooks.map { it.toBookUi() } }
    val newTestamentBooks = remember { BibleData.newTestamentBooks.map { it.toBookUi() } }

    // State for book selection and input
    var selectedBook by remember { mutableStateOf<BookUi?>(null) }
    var chapterInput by remember { mutableStateOf("") }
    var verseInput by remember { mutableStateOf("") }

    // Focus requesters for auto-focus
    val chapterFocusRequester = remember { FocusRequester() }

    // Get the selected BibleBook for validation
    val selectedBibleBook by remember(selectedBook) {
        derivedStateOf {
            selectedBook?.let { BibleData.getBookByNumber(it.bookNumber) }
        }
    }

    // Validate inputs
    val isInputValid by remember(chapterInput, verseInput, selectedBibleBook) {
        derivedStateOf {
            val chapter = chapterInput.toIntOrNull()
            val verse = verseInput.toIntOrNull()

            chapter != null && chapter in 1..(selectedBibleBook?.chapters ?: 0) &&
                    (verse == null || (verse > 0 && verse <= (selectedBibleBook?.getVersesForChapter(chapter) ?: 0)))
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = showNavigationModal
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when {
                                    selectedBook == null -> "Select a Book"
                                    else -> "Enter Chapter & Verse for ${selectedBook?.longName}"
                                },
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (selectedBook != null) {
                                        // Clear inputs and go back to book selection
                                        selectedBook = null
                                        chapterInput = ""
                                        verseInput = ""
                                    } else {
                                        onDismissRequest()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        if (selectedBook == null) {
                            // Book selection view
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            item {
                                TestamentSection(
                                    title = "Old Testament",
                                    books = oldTestamentBooks,
                                    onBookSelected = { book ->
                                        selectedBook = book
                                        // Auto-focus chapter input when book is selected
                                    },
                                    defaultColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    textColor = MaterialTheme.colorScheme.primary
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            item {
                                TestamentSection(
                                    title = "New Testament",
                                    books = newTestamentBooks,
                                    onBookSelected = { book ->
                                        selectedBook = book
                                    },
                                    defaultColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    textColor = MaterialTheme.colorScheme.secondary
                                )
                            }
                        } else {
                            // Chapter/Verse input view
                            selectedBook?.let { book ->
                                // Book info header
                                item {
                                    BookHeader(book = book)
                                }

                                item {
                                    Spacer(modifier = Modifier.height(32.dp))
                                }

                                // Chapter input
                                item {
                                    Column {
                                        Text(
                                            text = "Chapter",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        OutlinedTextField(
                                            value = chapterInput,
                                            onValueChange = { newValue ->
                                                if (newValue.all { it.isDigit() }) {
                                                    chapterInput = newValue
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(chapterFocusRequester),
                                            placeholder = {
                                                Text(
                                                    text = "Enter chapter (1-${book.totalChapters})",
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            },
                                            singleLine = showNavigationModal,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            isError = chapterInput.isNotEmpty() &&
                                                    (chapterInput.toIntOrNull() ?: 0) !in 1..book.totalChapters
                                        )
                                        if (chapterInput.isNotEmpty()) {
                                            val chapter = chapterInput.toIntOrNull()
                                            if (chapter != null && chapter !in 1..book.totalChapters) {
                                                Text(
                                                    text = "Chapter must be between 1 and ${book.totalChapters}",
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                }

                                // Verse input (optional)
                                item {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Verse (Optional)",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                            Text(
                                                text = "• Leave empty for entire chapter",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = verseInput,
                                            onValueChange = { newValue ->
                                                if (newValue.all { it.isDigit() }) {
                                                    verseInput = newValue
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = {
                                                Text(
                                                    text = "Enter verse number",
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            },
                                            singleLine = showNavigationModal,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            isError = verseInput.isNotEmpty() &&
                                                    chapterInput.isNotEmpty() &&
                                                    chapterInput.toIntOrNull()?.let { chapter ->
                                                        val verse = verseInput.toIntOrNull()
                                                        val maxVerse = selectedBibleBook?.getVersesForChapter(chapter)
                                                        verse != null && maxVerse != null && verse !in 1..maxVerse
                                                    } == showNavigationModal
                                        )
                                        if (verseInput.isNotEmpty() && chapterInput.isNotEmpty()) {
                                            val chapter = chapterInput.toIntOrNull()
                                            val verse = verseInput.toIntOrNull()
                                            val maxVerse = chapter?.let { selectedBibleBook?.getVersesForChapter(it) }

                                            if (chapter != null && verse != null && maxVerse != null && verse !in 1..maxVerse) {
                                                Text(
                                                    text = "Verse must be between 1 and $maxVerse for chapter $chapter",
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(32.dp))
                                }

                                // Submit button
                                item {
                                    Button(
                                        onClick = {
                                            val chapter = chapterInput.toInt()
                                            val verse = verseInput.toIntOrNull()
                                            onPassageSelected(book.longName, chapter, verse)
                                            onDismissRequest()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = isInputValid
                                    ) {
                                        Text(
                                            text = if (verseInput.isNotEmpty()) {
                                                "Read ${book.longName} $chapterInput:$verseInput"
                                            } else {
                                                "Read ${book.longName} Chapter $chapterInput"
                                            },
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }

                                item {
                                    // Clear button
                                    Button(
                                        onClick = {
                                            chapterInput = ""
                                            verseInput = ""
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = chapterInput.isNotEmpty() || verseInput.isNotEmpty()
                                    ) {
                                        Text("Clear Input")
                                    }
                                }
                            }
                        }
                    }

                    // Auto-focus chapter input when book is selected
                    LaunchedEffect(selectedBook) {
                        if (selectedBook != null) {
                            chapterFocusRequester.requestFocus()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookHeader(book: BookUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = when (book.testament) {
            Testament.OLD -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            Testament.NEW -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = book.longName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = when (book.testament) {
                        Testament.OLD -> MaterialTheme.colorScheme.primary
                        Testament.NEW -> MaterialTheme.colorScheme.secondary
                    }
                )
                Text(
                    text = "${book.totalChapters} chapters total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (book.testament) {
                            Testament.OLD -> MaterialTheme.colorScheme.primary
                            Testament.NEW -> MaterialTheme.colorScheme.secondary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = book.shortName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestamentSection(
    title: String,
    books: List<BookUi>,
    onBookSelected: (BookUi) -> Unit,
    defaultColor: Color,
    textColor: Color
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${books.size} books",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            books.forEach { book ->
                BookCard(
                    book = book,
                    backgroundColor = defaultColor,
                    textColor = textColor,
                    onClick = { onBookSelected(book) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCard(
    book: BookUi,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 60.dp, max = 80.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = book.shortName,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Chapter count
            Text(
                text = "${book.totalChapters} ch",
                color = textColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, name = "Navigation Modal Light")
@Composable
fun NavigationModalPreviewLight() {
    FohBibleTheme(darkTheme = false) {
        NavigationModal(
            onDismissRequest = {},
            onPassageSelected = TODO(),
            showNavigationModal = TODO(),
        )
    }
}

@Preview(showBackground = true, name = "Navigation Modal Dark")
@Composable
fun NavigationModalPreviewDark() {
    FohBibleTheme(darkTheme = true) {
        NavigationModal(
            onDismissRequest = {},
            onPassageSelected = TODO(),
            showNavigationModal = TODO(),
        )
    }
}